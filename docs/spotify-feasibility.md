# Spotify as in-app player: feasibility

## Short answer

The SDK is healthy — much more so than Apple's — but a 2025 Spotify policy
change makes it **unshippable to the public for an app this size**, and the
catalogue-fit problem from the Apple study applies here too. Not recommended.

## Finding 1 — the SDK works, and it's the right shape

Unlike Apple's abandoned Android SDK, Spotify's is maintained:
- The **App Remote SDK** is lightweight (<300 KB, no native code) and does
  exactly what was asked: your app issues playback commands and the Spotify app
  does the actual streaming in the background. Playback and metadata stay in
  sync between the two. A real in-app mini-player driving Spotify is feasible.
- It plays full tracks (not 30-second previews) — **but only for the user's own
  Spotify Premium account**. Free users cannot play on-demand; a single-track
  play requires Premium, and the SDK exposes a user-capabilities call to check
  first. Spotify shut down third-party *streaming* SDKs years ago; App Remote
  (control the installed Spotify app) is the only sanctioned mobile path.

So technically, the mini-player idea is achievable for Spotify-Premium users
with the Spotify app installed.

## Finding 2 — the 2025 access change is the blocker

This is the decisive one. As of **15 May 2025**, to move an app from
development mode to the "extended quota mode" needed for public release, Spotify:
- **only accepts applications from organisations**, not individuals — must be a
  legally registered business applying from a company email;
- requires the service to already have **≥ 250,000 monthly active users**;
- vets commercial viability and can reject at discretion.

Development mode — the only mode available without that approval — is capped at
**25 allowlisted users** (each added by Spotify email by hand), dropping toward
5. Every other user's API calls get a 403.

That is a chicken-and-egg wall the developer community has been vocal about: you
cannot reach 250k users while capped at 25, and you cannot lift the cap without
250k users. For a devotional reader shipping on Play, this means Spotify
playback would work **only for up to 25 hand-added test accounts** and 403 for
the actual public. It cannot ship as a real feature.

## Finding 3 — catalogue fit (same as Apple)

Even setting policy aside, the recitations mapped so far are specific YouTube
uploads. Spotify would need a **separate per-work mapping to Spotify track/album
URIs**, and the specific community recitations may or may not be on Spotify at
all. It would be different recordings, and a second full mapping effort.

## Finding 4 — Premium-only excludes much of the audience

On-demand playback needs full Spotify Premium (not Lite/Mini). For an
India-first Tamil devotional audience, Spotify Premium penetration is lower than
YouTube's reach, so even without the policy wall this would serve a minority.

## Options

### A. Keep YouTube hand-off only. — RECOMMENDED
Works for everyone now, no account, no policy wall, no catalogue remap.

### B. Spotify App Remote in-app player — NOT VIABLE FOR PUBLIC RELEASE
Technically buildable, but the 25-user development-mode cap makes it unusable
for a public Play release unless the app already has 250k MAU and applies as a
registered business. Would also need a Spotify catalogue mapping and exclude
non-Premium and non-Spotify users. High effort for a feature that cannot ship
to the public.

### C. Spotify hand-off (open the Spotify app to a URI) — MINOR, OPTIONAL
Like the YouTube hand-off: a `spotify:` / open.spotify.com link that opens the
Spotify app to the mapped track. No SDK, no quota wall (a plain deep link is not
an API call). Gives Spotify users their app, no in-app player. Needs the Spotify
catalogue mapping. Lower value than it sounds, since most users reach for
YouTube for this content.

## Recommendation

Do not build the in-app Spotify player. The SDK could do it, but Spotify's
May-2025 access policy caps it at 25 users without a 250k-MAU business approval,
so it cannot ship publicly — the same "plays inside the app" goal is exactly
what needs the gated API. If Spotify reach is ever wanted, Option C (a plain
deep-link hand-off, no SDK) is the only piece that both works publicly and
avoids the quota wall — and even that needs a Spotify catalogue mapping and
serves a minority of this audience.

Across all three services now studied — YouTube (works, shipping), Apple Music
(abandoned Android SDK), Spotify (SDK fine, policy wall) — the YouTube hand-off
remains the only recitation path that plays for the whole audience today. The
in-app-mini-player goal keeps failing not on our side but on each provider's
third-party terms.
