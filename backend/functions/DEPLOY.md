# Deploying the Ask proxy

## Phase 1 — local run

    cd backend/functions
    npm install
    cp .env.example .env          # put your real GEMINI_API_KEY in .env
    npm run dev                   # serves ask on http://localhost:8080

Test:

    curl -s -X POST http://localhost:8080 \
      -H "Content-Type: application/json" \
      -d '{"question":"Who composed Thiruppallandu?"}'

Out-of-scope test (should get the refusal, not an answer):

    curl -s -X POST http://localhost:8080 \
      -H "Content-Type: application/json" \
      -d '{"question":"What is the weather in Chennai today?"}'

## Phase 1 — deploy to Cloud Functions (2nd gen)

    gcloud functions deploy ndp-ask \
      --gen2 --runtime=nodejs20 --region=asia-south1 \
      --source=. --entry-point=ask --trigger-http \
      --set-secrets=GEMINI_API_KEY=ndp-gemini-key:latest \
      --min-instances=0 --max-instances=3

- `--max-instances=3` is a first hard cost ceiling: even under attack the
  function cannot fan out unboundedly.
- `asia-south1` (Mumbai) is closest to the likely audience; change if needed.
- Store the key as a Secret Manager secret (`ndp-gemini-key`), not a plain env
  var, so it is encrypted at rest and not visible in the console.

## Phase 3 — securing it (do before public launch)

1. **App Check.** Register the app in Firebase, enable App Check with the Play
   Integrity provider, then redeploy with `--set-env-vars=ENFORCE_APP_CHECK=true`.
   After that, only requests carrying a valid App Check token from the real,
   Play-signed app are served; a stolen endpoint URL called with curl gets 401.
2. **Billing cap.** Set a Cloud Billing budget with alerts (e.g. $5, $20) and,
   ideally, a hard cap via a budget-triggered function that disables billing.
3. **Key restriction.** In the Cloud Console restrict GEMINI_API_KEY to the
   Generative Language API only.
4. **Rate limits.** The in-memory limiter here is per-instance; for real
   per-user limits back it with Firestore or Redis keyed on the App Check appId.

## The contract the app depends on

Request:  `POST { "question": string, "context"?: string }`
Response: `200 { "answer": string }`
          `400 empty_question | 401 app_check_required | 429 rate_limited | 502 upstream_error`

The app must send `context` (retrieved pasuram/essence/temple text) when it has
it, and must treat every non-200 as a graceful, human-readable error — this is
the one feature that needs the network.
