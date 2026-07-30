# Apple Music as in-app player: feasibility

## The request

Add an Apple Music sign-in in Settings. If the user is an Apple Music
subscriber, tapping a pasuram's Play opens an in-app mini-player and plays
there; if not, fall back to the YouTube Music hand-off already built.

## Finding 1 — the Android MusicKit SDK is effectively abandoned

This is the decisive constraint. The official Apple Music SDK for Android
(`musickitauth-release-1.1.2.aar`, `mediaplayback-release-1.1.1.aar`):

- **Last meaningful update was December 2021.** Multiple developer-forum
  threads through 2024-2025 ask whether it is maintained; Apple has not shipped
  a refresh.
- **It does not cleanly build against modern Android.** Its bundled manifest is
  missing `android:exported` on `SDKUriHandlerActivity`, which breaks the
  manifest-merger on Android 12+ (our target is 36). There is a manifest-
  override workaround, but developers report that *after* applying it,
  authentication then fails on newer Android — the login web screen is replaced
  by being dumped on the Apple Music Play Store page.
- **It has not been updated for the 16 KB memory-page-size requirement** Google
  now enforces for apps targeting Android 15+, which is a Play submission risk.
- Distributed as raw `.aar` files, not a Maven dependency — awkward to manage
  and pin.

In short: shipping the Android MusicKit SDK today means bundling a four-year-old
binary that fights the toolchain, has known-broken auth on current devices, and
carries a Play-compliance risk. For an app whose stated priority is UI and
content quality, that is a shaky foundation.

## Finding 2 — the recordings are not the same

The YouTube tracks mapped so far are specific amateur/community recitation
uploads. Apple Music *does* carry Divya Prabandham recitation — e.g. "Nalayira
Divya Prabandham, Vol. 1 - Mudal Ayiram" (Vadakalai and Thenkalai variants),
Iyarpa Vol. 4, Thiruvaimozhi Vol. 3, Periya Thirumozhi Vol. 2 — but these are
*different* commercial recordings, not the uploads the user curated. So Apple
Music cannot play "the same track" by id the way the YouTube hand-off does. It
would need a **separate mapping** of each work to an Apple Music catalogue id,
which is a second full mapping effort on top of the YouTube one just finished.

## Finding 3 — what Apple Music integration actually requires

- An **Apple Developer Program membership** ($99/yr) to obtain a MusicKit
  private key (.p8) and generate developer tokens (JWT, ES256).
- Developer tokens must be **minted server-side or refreshed carefully** — forum
  reports of tokens silently invalidating after days are common; a static token
  baked into the app is not viable.
- The user must have an **active Apple Music subscription**; a user token is
  obtained through the SDK auth flow. Non-subscribers cannot play full tracks
  (only 30-second previews via the API).
- Play Billing / App Store rules: playback stays free to the user, fine, but the
  integration terms (MusicKit 3.3.6) restrict monetising access.

## Options

### A. Do not integrate Apple Music. Keep YouTube hand-off. — RECOMMENDED
Zero risk, already working. The YouTube hand-off already covers everyone
(YouTube Music, YouTube, or browser) regardless of subscription. Apple Music
buys an in-app mini-player for the subset of users who (a) subscribe to Apple
Music and (b) accept different recordings — at the cost of a broken SDK.

### B. Apple Music hand-off (no SDK, no in-app player) — VIABLE, LOW RISK
Mirror the YouTube approach: if the Apple Music app is installed, offer a
second "Play in Apple Music" path that opens the Apple Music app to the mapped
album/track via a `music.apple.com` URL. No SDK, no developer token, no
in-app player — just an intent to another app, exactly like the YouTube
hand-off. This gives Apple Music users their preferred app without any of the
SDK's problems. It does NOT satisfy the "plays inside our app" part of the
request, because that part is what requires the broken SDK. Needs the separate
Apple-catalogue mapping (Finding 2).

### C. Full in-app Apple Music player via MusicKit SDK — NOT RECOMMENDED
This is what was literally asked for, and it is technically the only option that
plays inside the app. But it means shipping the abandoned 1.1.x SDK with its
build and auth problems on modern Android, an Apple Developer membership, token
infrastructure, and a second catalogue mapping — for the slice of users who
subscribe to Apple Music. High effort, real Play-compliance and
breaks-on-new-Android risk, ongoing maintenance exposure. Hard to justify for a
devotional reader.

## Recommendation

Do not build the in-app Apple Music player (Option C): it depends on an SDK
Apple has effectively abandoned, which will not build cleanly against Android
16 and has auth broken on current devices. The "plays inside the app" goal is
exactly the part that requires that SDK, so it cannot be met safely today.

If Apple Music reach matters for the audience, Option B (a hand-off to the
Apple Music app, alongside the existing YouTube hand-off) delivers most of the
value — Apple Music subscribers listen in their app — with none of the SDK
risk. It needs a per-work Apple Music catalogue mapping, which the user would
supply the way the YouTube ids were supplied.

The in-app-mini-player idea is sound in principle; it is Apple's neglected
Android SDK, not the idea, that makes it unsafe right now. If Apple ever ships a
modern MusicKit for Android, Option C should be revisited.
