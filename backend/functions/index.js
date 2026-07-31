// Ask proxy — the only path from the app to Gemini.
//
// Responsibilities, in order:
//   1. Accept a POST { question, context? } from the app.
//   2. Verify the Firebase App Check token (Phase 3 flips this to enforced).
//   3. Attach the guardrail as Gemini's system_instruction.
//   4. Attach any corpus context the app retrieved (RAG grounding).
//   5. Call Gemini at low temperature.
//   6. Return { answer } — nothing else, never the key or raw provider errors.
//
// The key lives only in this function's environment (GEMINI_API_KEY). It is
// never returned to the client and never shipped in the APK.

const { GoogleGenerativeAI } = require("@google/generative-ai");
const { SYSTEM_INSTRUCTION } = require("./guardrail");

// Model + generation config. Flash-Lite is the cheap workhorse; low temperature
// keeps answers grounded and on-scope. maxOutputTokens caps runaway responses
// (and cost) per call.
const MODEL = process.env.GEMINI_MODEL || "gemini-2.5-flash-lite";
const GENERATION_CONFIG = {
  temperature: 0.2,
  maxOutputTokens: 1024,
};

// Naive per-instance rate limit. Real enforcement is App Check + a billing cap
// (Phase 3); this is a cheap in-memory backstop against a burst from one
// instance. Keyed by App Check subject when present, else caller IP.
const WINDOW_MS = 60_000;
const MAX_PER_WINDOW = 10;
const hits = new Map();

function rateLimited(key) {
  const now = Date.now();
  const rec = hits.get(key) || { count: 0, start: now };
  if (now - rec.start > WINDOW_MS) {
    rec.count = 0;
    rec.start = now;
  }
  rec.count += 1;
  hits.set(key, rec);
  return rec.count > MAX_PER_WINDOW;
}

/**
 * Builds the user turn: the question, plus any retrieved corpus context the app
 * attached. The guardrail already tells the model to rely only on provided
 * context, so wrapping it in a labelled block is enough — no need to restate the
 * rule here.
 */
function buildPrompt(question, context) {
  if (!context || !context.trim()) {
    return question.trim();
  }
  return [
    "CONTEXT FROM THE PRABHANDHAM CORPUS (rely only on this for specific",
    "pasuram text, numbers, and temple details):",
    "-----",
    context.trim(),
    "-----",
    "",
    "QUESTION:",
    question.trim(),
  ].join("\n");
}

// App Check verification. In Phase 1 this is best-effort (logs and continues if
// the Admin SDK is not wired yet); Phase 3 makes a missing/invalid token a hard
// 401. Written so flipping ENFORCE_APP_CHECK=true is the only change needed.
async function verifyAppCheck(req) {
  const token = req.get("X-Firebase-AppCheck");
  const enforce = process.env.ENFORCE_APP_CHECK === "true";
  if (!token) {
    return { ok: !enforce, subject: null };
  }
  try {
    const admin = require("firebase-admin");
    if (!admin.apps.length) admin.initializeApp();
    const decoded = await admin.appCheck().verifyToken(token);
    return { ok: true, subject: decoded.appId };
  } catch (e) {
    console.warn("App Check verification failed:", e.message);
    return { ok: !enforce, subject: null };
  }
}

exports.ask = async (req, res) => {
  // The app calls with POST JSON. Everything else is refused flatly.
  if (req.method !== "POST") {
    res.status(405).json({ error: "method_not_allowed" });
    return;
  }

  const check = await verifyAppCheck(req);
  if (!check.ok) {
    res.status(401).json({ error: "app_check_required" });
    return;
  }

  const rlKey = check.subject || req.ip || "anon";
  if (rateLimited(rlKey)) {
    res.status(429).json({ error: "rate_limited" });
    return;
  }

  const question = (req.body && req.body.question) || "";
  const context = (req.body && req.body.context) || "";
  if (!question.trim()) {
    res.status(400).json({ error: "empty_question" });
    return;
  }

  try {
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey || !apiKey.trim()) {
      // Missing key is a deploy/config problem, not a bad request or an upstream
      // outage — the SDK would otherwise send no key and Gemini returns a
      // confusing "unregistered callers" 403. Say so plainly in the logs and
      // return a distinct status so it is not mistaken for a Gemini failure.
      console.error(
        "GEMINI_API_KEY is not set in the function environment. " +
        "Check the deploy's --set-secrets / --set-env-vars and that the " +
        "runtime service account can read the secret."
      );
      res.status(500).json({ error: "server_misconfigured" });
      return;
    }

    const genAI = new GoogleGenerativeAI(apiKey);
    const model = genAI.getGenerativeModel({
      model: MODEL,
      systemInstruction: SYSTEM_INSTRUCTION,
      generationConfig: GENERATION_CONFIG,
    });

    const result = await model.generateContent(buildPrompt(question, context));
    const answer = result.response.text();
    res.status(200).json({ answer });
  } catch (e) {
    // Never leak provider internals or the key to the client.
    console.error("Gemini call failed:", e.message);
    res.status(502).json({ error: "upstream_error" });
  }
};
