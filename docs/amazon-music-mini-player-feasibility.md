# Amazon Music mini in-app player: feasibility

## The question

Can the Amazon Music Web Playback API (`developer.amazon.com/docs/music/API_playback_sessions.html`)
replace the current hand-off (open Amazon Music, jump to a track) with a real
in-app mini player — play/pause/skip/scrub inside Prabandham itself, never
leaving the app?

## What the API actually is

Verified against the live docs (fetched directly, not from memory — this kind
of detail goes stale fast). It is **not** "give me an audio URL, play it
yourself." It's a session/queue control plane sitting on top of DRM-protected
audio:

- **DRM playback.** Every track's `playbackInformation.url` is a DASH
  manifest (`protocol: DASH`, format `ENCRYPTED_OPUS_FLAC`) requiring a
  **Widevine** license, fetched separately using the response's
  `licenseHeaders`. ExoPlayer/Media3 supports DASH + Widevine natively — this
  is buildable, but it's a real DRM integration, not a config flag.
- **Login with Amazon (LWA) required.** The `Authorization: Bearer` token
  represents an authenticated Amazon account with the `music::playback`
  scope. The user must sign in to Amazon inside our app before anything can
  play — a separate SDK integration with its own sign-in UI and token
  refresh.
- **Mandatory event reporting.** Every track play requires `start`/`stop`
  events to `/v1/playback/event`, with fields like playback offset, rebuffer
  count, and termination reason. This is load-bearing, not optional
  telemetry — it's part of the playback contract.
- **Concurrency handling.** Amazon allows a limited number of concurrent
  streams per account. A conflicting second stream returns a 422
  (`MAX_CONCURRENCY_REACHED`); the client either auto-resolves
  (`takeOverType: AUTO`) or prompts the user to force a takeover. Needs a
  real UX decision, not just error handling.
- **Subscription-tier-aware controls.** Each playqueue entry's `actions`
  object says, live, which transport controls are currently allowed. A Prime
  subscriber's response disables `previous`/`scrub`/`shuffle` and shows a
  counting-down `remainingSkips` on `next`; an Unlimited subscriber gets full
  control. The mini player's UI has to reflect this per track, not assume a
  fixed control set.
- **Device registration.** Requests carry an `X-Amzn-Device-Id` header,
  implying a device-registration step not detailed on this particular page
  (worth reading the Overview/Authentication docs once beta access is live).

## What carries over from the current work

The good news: nothing already done is wasted. Tracks are addressed as
`mrn:1.0:catalog:track:asin:<ASIN>` and albums as
`mrn:1.0:catalog:album:asin:<ASIN>` — built directly from the same ASINs
already mapped for all 44 works. If this path is taken later, the catalogue
mapping work carries straight over; only the launch mechanism changes.

## Why no code is being written yet

Two reasons, both firm:

1. **Still closed beta.** The docs page states this explicitly. Without an
   approved security profile there is no `x-api-key` to authenticate with —
   meaning any implementation right now would be written against a spec with
   no way to verify it against a real response. That's exactly the kind of
   code that quietly rots, or worse, looks done while being untested.
2. **This is a materially bigger build than the redirect trial**, and
   arguably bigger than the earlier self-hosted ExoPlayer option — LWA
   sign-in, Widevine DRM, session/queue lifecycle, mandatory event reporting,
   concurrency handling, and tier-aware controls are five distinct pieces of
   engineering, not one. It deserves the same feasibility-first, staged
   approach used for every other non-trivial feature on this project, not a
   speculative first pass.

## What this does NOT change

The redirect hand-off already built and confirmed working (background
playback survives switching apps, 44 works mapped across Mudhalayiram,
Iyarpa, and Desika Prabandham) keeps running exactly as it is. This is a
future upgrade path, not a replacement in progress.

## Recommended sequencing, once Amazon approves access

Build in isolated, independently testable stages — each one verifiable on
its own before the next depends on it:

1. **LWA sign-in** alone — get an authenticated Bearer token, nothing else.
   Testable in isolation (confirm token acquisition + refresh work).
2. **Session + DRM playback of a single known track** — create a session,
   fetch the Widevine license, confirm ExoPlayer actually plays the DASH
   stream. This is the highest-risk, highest-value step to validate first.
3. **Mini player UI** — transport controls that respect the live `actions`
   object (so a Prime account's UI doesn't offer buttons it can't use).
4. **Event reporting + concurrency handling** — wire the mandatory start/stop
   events and the takeover-conflict UX.
5. **Queue/session persistence** — restoring an active session across app
   restarts (`Restore Playback Session`), so leaving and returning to the app
   doesn't lose the player state.

Each stage should get its own commit and, per the established workflow, its
own device verification before moving to the next.

## Bottom line

Feasible, and worth doing once access lands — it would be a genuinely better
experience than any hand-off (no leaving the app, real controls, no
launcher-visibility quirks to work around). Not something to start coding
against a beta with no credentials. The right move now is to wait for
Amazon's approval and revisit this as a staged build at that point.
