# Gemini-powered "Ask" search: feasibility

## The idea

Extend the search tab with an AI mode: the user asks a natural-language
question about the Prabandham / Divya Desams, and Gemini answers, constrained by
the provided guardrail (in-scope only, refuse everything else, no fabrication,
ground on corpus text when supplied).

Verdict: **feasible and a good fit — with one hard architectural requirement
(a backend proxy) that must not be skipped.** Below is what it takes and the
traps.

## Requirement 1 (non-negotiable): a backend proxy, not a client-side key

The single most important finding. As of 2026, Google's own documentation is
explicit: *"Keys compiled in client-side code can be extracted by users. To
secure client-side apps, run a backend proxy server to make the actual API
calls."* Further:
- Feb 2026 research (Truffle Security, CloudSEK) showed hardcoded Google keys in
  shipped Android apps are trivially decompiled and reused against Gemini — one
  `curl` gets a 200, driving cost and bypassing the guardrail entirely.
- **The Gemini API will reject unrestricted Standard keys, and from September
  2026 rejects Standard keys generally** — a bare embedded key is a dead end by
  design, not just a risk.

Implication for this app: the guardrail (system prompt) and the key **cannot
live in the APK**. If they did, an attacker extracts the key, calls Gemini
directly with *their own* prompt, and the entire "Sri Vaishnava only" guardrail
is void — plus they spend the project's money. So the architecture must be:

    App  ->  our lightweight proxy (holds key + system prompt + does RAG)  ->  Gemini

The proxy is where the guardrail is actually enforced, the key is held, requests
are rate-limited per device, and the corpus context is attached. This is a
small service (a Cloud Function / Cloud Run endpoint), but it is real,
ongoing infrastructure — the first time this project needs a server. The
existing Drive sync is serverless (client-to-Drive); this is not.

## Requirement 2: the guardrail belongs on the server, and is good but not sufficient alone

The supplied guardrail is well-written — clear in-scope list, a refusal
template, injection resistance, and a no-fabrication/grounding rule. Two notes:
- It must be sent as Gemini's **system_instruction**, server-side, never
  client-side. Gemini honours system instructions well, and combined with a
  low temperature this holds the scope reliably for ordinary use.
- System-prompt guardrails are strong but not absolute — determined
  jailbreaks occasionally slip any LLM. Defence in depth on the proxy: a cheap
  pre-classifier (is this plausibly in-domain?) before the main call, output
  validation, and per-device rate limits so even a successful jailbreak cannot
  run up a bill. None of this is possible client-side.

## Requirement 3: grounding (RAG) — the honest gap

The guardrail says: rely only on provided context, do not fabricate pasuram
numbers/lines/temple details, and admit when the corpus lacks the answer. Good
policy — but it depends on there being context to provide, and here the corpus
is thin for that purpose:

- Full pasuram **text** exists for all 4,000 (good — can be retrieved and
  attached).
- **Per-pasuram essences: only 62 of ~4,000** are written. Decad-level
  essences: 315. Divya Desam details exist. There is **no word-meaning
  (padavurai) or commentary corpus** in the app's data.

So true RAG grounding is only possible for the layer that exists (the verse text
itself, decad essences, temple facts). For "line-by-line meaning" or
"commentary" — which the in-scope list explicitly invites — there is no grounding
data, so the model would be answering from its own training, which is exactly
where fabrication risk lives for a sacred text. That is a real tension: the
guardrail promises grounded padavurai, but the data to ground it does not yet
exist in the app.

Options for grounding:
- **Retrieve verse text + essence + temple facts** by the same keyword/number
  search already in the app, attach as context. Solid for summary/theme/temple
  questions.
- For meanings/commentary, either (a) scope them OUT until a padavurai corpus
  exists, or (b) allow them but clearly labelled as AI-generated and
  unverified — which for a devotional audience is a sensitivity call the owner
  should make, not a default.

## Requirement 4: cost (small, but not zero, and abusable)

Current Gemini pricing (mid-2026): Gemini 2.5 Flash-Lite ~$0.10 / $0.40 per 1M
input/output tokens; Flash ~$0.30 / $2.50. A grounded Q&A with a few KB of
attached context is a few thousand tokens; realistic cost is a fraction of a
cent per question on Flash-Lite. For a devotional app's likely volume this is
genuinely cheap — tens of thousands of questions for single-digit dollars.

The catch is abuse, not honest use: an extracted key (Req 1) or an unthrottled
proxy turns "cheap" into an unbounded bill. Hence per-device rate limits and
billing alerts are part of the design, not an afterthought. A free-tier key is
not viable for production (rate limits + prompts used for training).

## Requirement 5: privacy & the sacred-content bar

- On the paid tier / Vertex, prompts are **not** used for training; on the free
  tier they are. Production must be paid tier.
- Answers about sacred text carry a higher accuracy bar than a normal chatbot.
  A wrong pasuram attribution or invented "commentary" is not a small bug here.
  This argues for grounding-only answers where possible and visible "AI-
  generated, may be imperfect" labelling everywhere else.

## Recommendation

Feasible and worthwhile, in this shape:
1. **Stand up a small proxy** (Cloud Run / Cloud Function) holding the key and
   the guardrail as system_instruction, with per-device rate limiting and
   billing alerts. Non-negotiable — a client-side key is both insecure and,
   by Sept 2026, rejected outright.
2. **Model: Gemini 2.5 Flash-Lite** to start (cheapest, ample for grounded Q&A),
   low temperature.
3. **Ground on what exists** — verse text, decad essences, temple data —
   retrieved by the current search, attached to each call.
4. **Scope meanings/commentary carefully**: either defer until a padavurai
   corpus exists, or ship them clearly labelled as unverified AI output. Owner's
   call, given the devotional stakes.
5. **UI**: a distinct "Ask" mode in the search tab, with a visible disclaimer,
   the refusal message wired to the guardrail, and graceful offline/error
   states (this feature, unlike the rest of the app, needs the network).

The feature is a good fit and the guardrail is sound. The two things that must
not be skipped are the **backend proxy** (security + policy enforcement + future
Standard-key rejection) and **honesty about grounding** (only the verse/essence/
temple layer is currently groundable; meanings are not). Everything else is
small.
