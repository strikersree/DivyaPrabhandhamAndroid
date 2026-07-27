# Recitation playback: feasibility study

## The problem, confirmed

The 13 supplied tracks are `music.youtube.com` links. In the app the bar shows
"பாராயணத்தை இயக்க முடியவில்லை" — the player reported IFrame error **150**.

Per YouTube's own IFrame API reference: error **101** means the video's owner
does not allow it in embedded players, and **150 is a 101 in disguise**. This is
set by the uploader, enforced server-side. No client flag, origin value, or
player parameter overrides it — the earlier "black screen" fix (correct
`https://www.youtube.com` origin) was necessary but cannot defeat an embed
block. These particular uploads simply cannot play outside YouTube's own apps.

So this is not a bug to fix in our player. It is a property of the tracks.

## What "playing in-app" actually requires

Every legal music service draws the same line: **audio is streamed under a
licence**, and the licence is tied either to the end user's paid subscription
or to a content deal. No mainstream service lets an arbitrary app stream full
catalogue tracks to users who have no subscription. Anything that appears to
(the unofficial scraper APIs) is serving unlicensed audio.

That frames the options: the realistic ones play a track **through the
listener's own subscription**, via the service's app installed on the device.

## Options assessed

### A. Different YouTube video IDs (not Music) — RECOMMENDED FIRST STEP
Cost: near zero. Terms: fine. Works offline of any account.
Most Divya Prabhandham recitations exist as ordinary YouTube videos whose
uploaders *do* allow embedding. Our player already works; only the IDs are
blocked. Swapping the 13 `music.youtube.com` IDs for embeddable
`youtube.com/watch` IDs of the same recitations is a `youtube.json` edit — no
code change. This keeps the "plays for everyone, no login, no subscription"
property the app has today. The one catch is per-track verification that each
replacement allows embedding, which the app now reports track by track.

### B. Apple Music (MusicKit for Android) — VIABLE, MATCHES iOS
Cost: moderate. Terms: officially supported. Requires: user's Apple Music
subscription + the Apple Music app installed.
Apple ships an Android MusicKit SDK (`com.apple.android.music.playback`,
`musickitauth`) that plays full catalogue tracks through the user's
subscription. This is the closest analogue to what the iOS build did. Downsides:
it only works for Apple Music subscribers, the SDK is distributed as raw `.aar`
files (awkward dependency management), and third-party developer reports note
playback breaking after Apple Music app updates until the native app is opened
once. Good as an *optional* "play in Apple Music" path; wrong as the only one.

### C. Spotify (App Remote SDK) — VIABLE BUT NARROWING, POOR FIT
Cost: moderate. Terms: supported but tightening hard. Requires: user's Spotify
**Premium** + Spotify app installed.
The App Remote SDK offloads playback to the installed Spotify app; playing a
single track needs Premium. Since 2025 Spotify has sharply restricted developer
access (Premium mandatory even to develop, five test users, extended-quota
approval requires a registered business and 250k MAU). For a devotional reader
this is a bad fit: much of the specific pasuram-recitation catalogue may not be
on Spotify at all, and it excludes free users.

### D. Amazon Music — NOT VIABLE
No public third-party playback SDK for Android app-to-app streaming. Not offered.

### E. JioSaavn — NOT VIABLE (despite being the best audience fit)
Cost: deceptively low. Terms: **violation**. 
JioSaavn has **no official API or SDK**. Every "JioSaavn API" is unofficial —
scrapers returning DRM-free MP3s. Using one means serving unlicensed audio,
which breaks JioSaavn's ToS, fails Play review, exposes the project legally, and
breaks without warning when they change their site. Same category as ripping
YouTube streams. Ruled out despite JioSaavn being the natural service for an
India-first Tamil devotional audience.

### F. Self-host the audio — VIABLE, HEAVIEST, MOST DURABLE
Cost: high (licensing + hosting + bandwidth). Terms: clean if licensed.
Host recitation audio we have the right to distribute (public-domain or
permissively licensed recordings) and play with ExoPlayer/Media3. Total control,
works for everyone, no third-party dependency, real background playback and
lock-screen controls. The cost is real: sourcing rights-clear recordings of the
full corpus, storage, and CDN bandwidth. Best long-term answer if recitation
becomes a first-class feature rather than a convenience link.

## Recommendation

1. **Now:** Option A. Swap the blocked Music IDs for embeddable youtube.com
   IDs. Zero code, keeps playback free and login-less, and the player already
   reports which IDs are blocked so verification is mechanical.
2. **Optional polish:** Option B (Apple Music) as a secondary "open in Apple
   Music" action for subscribers, mirroring iOS — additive, not a dependency.
3. **If recitation becomes central:** Option F (self-hosted licensed audio) is
   the only path giving full control, universal playback, and real background
   audio — at real cost.

Not JioSaavn via unofficial APIs, and not stream extraction from any service:
both ship unlicensed audio and both will fail review and break.
