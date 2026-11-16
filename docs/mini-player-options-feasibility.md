# A mini-player inside the app: where things stand (November 2026)

## The question

Can Amazon Music be streamed inside an in-app mini-player, the way the iOS
build plays Apple Music? And if not, what else would put a mini-player in the
Android app?

This revisits `amazon-music-mini-player-feasibility.md` (August 2026) and
`music-providers-summary.md` (July 2026). Two things have changed since then,
and one option was never looked at.

## 1. Streaming Amazon Music in-app — still blocked

Checked against Amazon's developer site on 2026-11-15:

- The program overview still says "These Amazon Music APIs are currently in a
  closed Beta" and "we currently don't have a public SDK".
- On 2026-09-17 an Amazon staff member answered an independent Android
  developer asking for playback access: "Unfortunately the API is still in
  closed beta. We are currently not accepting any applications."
- The older Device API is deprecated and no longer onboarding anyone.

So there is no key to be had, and nothing to build against. The August study's
conclusion stands; the only update is that the door is now explicitly shut
rather than merely unanswered. The unofficial libraries that pull stream URLs
and Widevine keys are not an option: they break Amazon's terms and would not
survive Play review.

## 2. New option: a mini-player that remote-controls Amazon Music

Not studied before. Amazon Music keeps doing the playing (as it does today
after the hand-off), but the pill, the title and the transport buttons live in
our app.

How it works, using only public Android APIs:

- Every music app publishes a `MediaSession` (it is what feeds the lock
  screen, Bluetooth and Android Auto).
- An app holding **Notification access** may list the active sessions with
  `MediaSessionManager.getActiveSessions(...)` and take a `MediaController`
  for one of them.
- That controller gives the track title, artwork, position and play state, and
  accepts play / pause / next / previous / seek.

What the listener would see: a mini-player pill in Prabandham showing the
paththu playing, with working pause and skip, that follows Amazon Music as it
moves to the next track.

What it costs:

- **One permission.** Notification access is granted in system Settings, not
  by a normal runtime dialog, and the system wording is alarming ("can read
  all notifications"). We would not read any notification; the grant is just
  the gate Android puts in front of media sessions. Needs an honest
  explanation screen, and the hand-off must keep working for anyone who says
  no.
- **Audio is still Amazon's.** Amazon Music must be installed and the
  listener's own Prime/Unlimited tier still decides what plays on demand.

What is unknown until tried on a phone with Amazon Music (the emulator has
none):

1. Whether the first tap can start playback **without** switching to Amazon
   Music. Controllers can send `playFromUri` / `playFromMediaId`; whether
   Amazon Music honours them from a third party is undocumented. If it does
   not, the first tap still opens Amazon Music as today and the pill takes
   over once the listener comes back.
2. Whether the session's metadata identifies the track well enough (an ASIN,
   or at least the title) to say which paththu is playing and to hide the pill
   when Amazon Music is playing something unrelated.
3. Whether skip/seek are honoured on a Prime (not Unlimited) account.

A spike answering those three is small: an empty `NotificationListenerService`,
a session lookup filtered to `com.amazon.mp3`, and a debug screen printing what
comes back.

### Spike results (2026-11-15, motorola edge 60 fusion, Android 17, Amazon Music 26.40.0)

A debug-only test screen (`app/src/debug/.../remote/`) answered the three
unknowns on a real phone:

1. **Starting without leaving the app — yes, by one route only.**
   `playFromSearch` with structured extras (`EXTRA_MEDIA_FOCUS` = audio item,
   plus Amazon's exact title, artist and album) started the right track 5
   times out of 5, in about a second, with our app still in front. Everything
   else failed: `playFromUri` with the album link, `playFromMediaId` with the
   ASIN, and both with `asin://<ASIN>/` are ignored; a plain-text search
   plays an unrelated track.
2. **Identifying the track — yes.** The session metadata carries the ASIN
   (`MEDIA_ID`), title, album, artist, duration and artwork. After a hand-off
   the queue lists the rest of the album.
3. **Controls — yes.** Pause/play, seek (including while paused) and jumping
   to a queue entry all worked.

Limits found:

- A track started by search plays **alone**: no queue and no "next" action.
  Playing a whole work would mean the pill starting the next paththu itself
  when one ends, or using a hand-off (which does build a queue).
- Amazon Music refuses a `MediaBrowser` connection from our app, so there is
  no route that avoids Notification access.
- Play Protect in India blocks *sideloaded* APKs that declare Notification
  access. That block is for sideloaded files; how Play review treats the
  permission for this use has not been checked.

Second sitting, same day:

- **When a search-started track ends**, Amazon Music reports it paused at
  0:00 and about a second later starts an unrelated track of its own
  choosing. A pill that asks for the next paththu at that moment gets it
  (the change took 1.3–1.5 s), but the unrelated track shows for an instant
  first.
- **A title search is not always right.** Asked for "Periyazhwar
  Thirumozhi-Moondram Pathu" it played the Mudal Pathu, both times. The
  other titles tried (Thiruppavai, 1st and 2nd paththu) were right every
  time. A pill must check the track ID that comes back.
- **Cold start works.** With Amazon Music swiped away or force-stopped there
  is no session, and a search sent then goes nowhere. Binding to its
  `MediaBrowserService` is refused but wakes the app: a session appeared
  in about 0.3 s both times and accepted the search. After the force stop
  the track loaded but stayed paused until play was pressed.

Third sitting, same day:

- **An album search plays the album in order.** `playFromSearch` with
  `EXTRA_MEDIA_FOCUS` = album, the album's exact name and the artist started
  "Nalayira Divyaprabandham: Mudalaayiram" at its first track in about two
  seconds, with our app in front. "Next" and "previous" are offered, and at
  the end of a track the next one followed by itself (1st → 2nd → 3rd
  paththu seen), with nothing unrelated in between.
- **But it publishes no queue.** The session's queue stays empty after an
  album search, so `skipToQueueItem` cannot be used to land on a track.
  Reaching the 3rd paththu means pressing "next" twice and checking the
  track ID.
- **A title search found the right track for 9 of the album's 13 titles.**
  Wrong: the 3rd paththu (plays the 1st, every time), Thirumaalai (plays
  Perumaal Thirumozhi), Amalaathipiran (no answer within 5 s), Kanninum
  Siruthaambu (plays a same-named track from another album).
- **Cold start worked a third time**, and this time the track started
  playing without a second "play".

Fourth sitting, same day:

- **"Next" reaches any track of the album.** Album search, then
  `skipToNext` until the wanted track ID shows: right track in all four
  runs (3rd paththu, Thiruppaavai twice, the last track), and a walk
  through the whole album passed all 13 tracks in order. The album took
  1.5–3.8 s to start and each press about 0.5 s, so the 3rd paththu was
  reached 0.8 s after the album started and the 13th track 5.8 s after.
- **The tracks passed cannot be silenced by pausing.** "Next" on a paused
  player starts it playing again, so each track passed plays for about
  half a second.
- **Album order** (and so the number of presses): the five paththus of
  Periyazhwar Thirumozhi, Thiruppavai, Nachiaar Thirumozhi, Perumaal
  Thirumozhi, Thiruchanda Virutham, Thirumaalai, Thirupalliyezhi,
  Amalaathipiran, Kanninum Siruthaambu. No "next" is offered on the last.
- **The pill's own buttons work after an album start.** Next moves on;
  previous restarts the track, and pressed again goes back one.
- **The album name must be exact.** Asked for "Nalayira Divyaprabandham:
  Thiruvaimozhi" with no artist, Amazon Music played Mudalaayiram
  instead, twice. The name and artist that album reports have not been
  read yet.

So the pill can start a work as: album search, "next" to the track,
check the ID, and fall back to the hand-off if the ID is wrong.

Not yet tested: other account tiers; album search for the Periya Thirumozhi
and Thiruvaaymozhi albums (their names, artist and track IDs are now known);
muting the music stream while stepping.

### What was built from this (2026-11-15)

The pill is now in the app (`media/AmazonRemote.kt`, `ui/components/MusicPill.kt`):

- **Tapping Listen** on a work or paththu whose track is on one of the three
  mapped albums starts it without leaving the app: album search, "next" to
  the track, track ID checked. If Amazon Music was closed it is woken first.
  Anything that does not go to plan (wrong album, tracks out of order, no
  answer) falls back to the hand-off that opens Amazon Music.
- **The pill** takes the Continue Reading slot while a mapped track is loaded:
  paththu and work name, previous / play-pause / next. It follows whatever
  Amazon Music does, and hides when Amazon Music plays something the app
  does not know.
- **Tapping the pill** opens a player sheet (`ui/components/PlayerSheet.kt`),
  laid out like the iOS full player: the cover Amazon Music reports, work and
  paththu, a scrubber, back and forward 10 seconds, previous / play-pause /
  next, and a row that opens Amazon Music itself.
- **Notification access** is asked for once, in the app's own words, on the
  first Listen tap. "Open Amazon Music" there is remembered as a no, and
  Listen then hands off as before. Settings has a row to turn it on or off
  later. The listener service is empty and runs in its own process.
- **Amazon Music not installed**: a dialog says what is needed (the app, and
  a Prime or Unlimited membership) and offers the Play Store, instead of
  jumping there.
- All 44 mapped works are driven from the pill, from six albums whose names,
  artists and track order were read off the phone (2026-11-16). Iyarpaa and
  Ramanusa Noottrandhadi now come from one album, "Nalayira
  Divyaprabandham: Eyarpa" by Malola Kannan & N S Ranganathan (B00DHGE2RQ).
  Another album has the same name, so a start checks the first track's ID
  rather than the name or the artist (which Amazon Music was seen to
  misreport for a track), and hands off if it is not on the album.
- Some queues listed a track twice in a row after a hand-off. Whether an
  album search plays it twice is not known; the start steps over a repeat.

Checked on the emulator against a stand-in player (debug build only) that
behaves as Amazon Music did in the sittings above, then on the phone against
Amazon Music on one account: the pill, the player sheet and
starts from each album worked.

### What the listener's Amazon tier allows (reported, not tested here)

Everything above was seen on one account. From Amazon's India announcement
of 2026-06-02 and the coverage of it:

- **Unlimited** (₹99 a month with Prime, ₹119 without): on demand, no ads.
- **Prime** in India: still on demand, but with ads since 2026-07-02 and no
  downloads. Outside India, Prime plays albums in shuffle mode only.
- **Neither**: a free, ad-supported, shuffle-based tier was announced for
  India; whether it has launched was not confirmed.

Shuffle mode breaks "album, then next to the track": the order is not the
album's. The pill must therefore check the track ID after starting and show
what is really playing when it is not what was asked for. How an ad shows up
in the session has not been seen.

## 3. Changed: Apple Music on Android is no longer a dead end

The July study ruled Apple Music out because Apple's Android SDK was abandoned
and would not build. A public write-up from 2026-11-08 by another app's
developer (Parachord, Pixel 9a, Android 17) reports otherwise:

- **MusicKit JS in a hidden WebView** plays full tracks on Android, and passed
  a three-track, screen-off, forced-Doze test with auto-advance.
- Apple's Android SDK is now at 1.1.4 and also played, but its licence forbids
  committing or hosting the AARs, so it is awkward to build with.

This is someone else's result, not verified here. But it matters for this app
more than most: `recitations.json` already ships the Apple Music IDs for every
work, including one track per paththu, because the iOS build uses them. No new
mapping is needed.

What it would take:

- A WebView-hosted player (MusicKit JS), a foreground service to keep it alive,
  and a Media3 session so the lock screen and the pill work.
- Sign-in with an Apple ID inside the app.
- A MusicKit developer token. It expires after at most six months and the
  signing key cannot ship in the APK, so either a tiny token endpoint or a
  fresh token with each release.

Who it serves: only listeners with an Apple Music subscription, which on
Android is a minority. For them it would be the real thing — in-app audio,
per-paththu queue, same recordings as iOS.

## 4. Unchanged: self-hosted audio is the only route that reaches everyone

Media3/ExoPlayer playing recordings the project has the right to distribute.
No login, no subscription, no other app, full control of the queue. The
blocker is rights, not engineering: the recordings mapped today belong to
Amutham Music. It needs either their permission or a different, rights-clear
set of recitations.

## Comparison

| | Audio plays in | Works for | Blocked on |
|---|---|---|---|
| Amazon Web Playback API | our app | every Amazon tier | Amazon: closed beta, no applications |
| Remote-control pill | Amazon Music | Amazon Music users | built; a phone sitting with the real app, then Play review of Notification access |
| Apple Music via MusicKit JS | our app | Apple Music subscribers | a spike; token hosting |
| Self-hosted (Media3) | our app | everyone | rights to the recordings |

## Recommendation

1. ~~Spike the remote-control pill first.~~ Done and built (see above). What
   is left: a sitting against Amazon Music itself, other account tiers, and
   how Play review treats Notification access.
2. In parallel, ask Amutham Music (or the reciter) about permission to host
   the recordings. If that comes through, self-hosting replaces everything
   else.
3. Treat Apple Music via MusicKit JS as an optional extra for subscribers,
   worth a spike only if the audience has enough of them.
4. Do not wait on Amazon's API.

## Sources

- https://developer.amazon.com/docs/music/get_started_program-overview.html
- https://community.amazondeveloper.com/t/playback-api-access-for-an-independent-android-developer/29173
- https://github.com/Parachord/parachord-mobile/issues/553
- https://www.aboutamazon.in/news/entertainment/amazon-music-unlimited-india-price-plans
- https://www.musicbusinessworldwide.com/amazon-music-goes-standalone-in-india-with-1-unlimited-tier-for-prime-members-as-they-lose-ad-free-listening/
