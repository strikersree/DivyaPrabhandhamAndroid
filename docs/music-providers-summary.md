# In-app music playback: the full provider landscape

Consolidated finding after studying every realistic option. The question was
"what other providers can help build a mini-player?" The honest answer is that
this is a **structural wall, not a gap in the search** — the same barrier
recurs at every licensed provider, for the same reason.

## Why every provider says no (the pattern)

Playing a full track inside a third-party app means streaming licensed audio.
Every rights-holder guards that behind one of two gates:

1. **Subscription gate** — playback only for the user's own paid account
   (Spotify Premium, Apple Music), which excludes everyone else; and/or
2. **Partner-approval gate** — public release needs a signed commercial
   agreement, a registered business, and often a large existing user base or
   DRM certification.

An independent developer shipping a devotional reader on Play clears neither.

## Provider-by-provider

| Provider | In-app full playback? | Blocker |
|---|---|---|
| **YouTube (Music)** | Only via embed, which these uploads block (error 150) | Uploaders disallow embedding; hand-off works instead |
| **Apple Music** | SDK exists | Android SDK abandoned since 2021, won't build against Android 12+, auth broken on modern devices |
| **Spotify** | Yes, App Remote SDK (healthy) | May-2025 policy: public release needs a registered org with >=250k MAU; dev mode capped at 25 users |
| **Amazon Music** | Web Playback API (all tiers, even Free) | Closed beta, no public SDK, partner-approval + Widevine DRM licensing required |
| **JioSaavn** | No official API at all | Only unofficial scrapers — ToS violation, unlicensed audio, fails Play review |
| **Gaana / Wynk** | No public playback SDK | Wynk shut down Dec 2024; no third-party playback integration offered |

## The two things that would actually work in-app

Both sidestep licensed streaming entirely:

### 1. Self-hosted licensed audio (ExoPlayer/Media3) — the real answer
Host recitation recordings the project has the right to distribute
(public-domain, Creative-Commons, or permission-granted — e.g. the temple/
reciter recordings already credited in About), and play them with Media3.
- True in-app mini-player, real background playback, lock-screen controls.
- Works for **every** user, no login, no subscription, no provider approval.
- Cost: sourcing rights-clear recordings, storage, and CDN bandwidth.
- This is the only path that gives the exact experience asked for AND ships to
  everyone. If recitation is a first-class feature, this is where it should go.

### 2. Keep the YouTube hand-off — the pragmatic answer (current)
Opens the recitation in YouTube Music / YouTube / browser. Not in-app, but
works for the whole audience today with zero licensing exposure.

## Recommendation

There is no licensed streaming provider that an independent developer can embed
as an in-app mini-player and ship publicly — the wall is the music industry's
third-party-streaming terms, not any one company. The two viable in-app routes
are self-hosting rights-clear audio (Option 1, the durable answer) or staying
with the YouTube hand-off (current).

If an in-app mini-player is genuinely wanted, self-hosting is the path worth
planning — starting with the recitations the project can already license
(the credited reciters' own recordings are the natural first source).
