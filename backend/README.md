# Ask backend (serverless proxy for Gemini)

This is the server that stands between the app and the Gemini API. It exists for
reasons that are not optional (see docs/gemini-ask-feasibility.md):

- **The API key never ships in the app.** It lives here, as an environment
  secret. A key compiled into the APK is extracted in minutes and, from
  September 2026, is rejected by Gemini outright.
- **The guardrail is enforced here.** The Sri Vaishnava-only system instruction
  is attached server-side, so a caller cannot swap it out. If the guardrail
  lived in the app, an extracted key would let anyone call Gemini with their own
  prompt and the guardrail would be meaningless.
- **Abuse is contained here.** App Check (Phase 3), per-device rate limiting and
  billing caps all live on this side.

## Layout

- `functions/index.js` — the Cloud Function: verifies App Check, applies the
  guardrail, calls Gemini, returns only the answer text.
- `functions/guardrail.js` — the system instruction (single source of truth).
- `functions/package.json` — dependencies.
- `.env.example` — the secrets the function needs (never commit the real .env).

## Phases

1. **This proxy** — deploy `functions/` to Cloud Functions / Cloud Run.
2. **Guardrail tuning** — iterate `guardrail.js` text in Google AI Studio, paste
   the final version back here.
3. **Secure** — turn on App Check enforcement, set a billing budget + rate limit.
4. **App integration** — the Android app calls this endpoint (never Gemini
   directly), attaching retrieved corpus context.

Deploy target and exact commands are in `functions/DEPLOY.md`.
