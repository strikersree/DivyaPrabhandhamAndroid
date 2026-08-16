# Divya Prabhandham — Android

An Android port of the iOS SwiftUI reader for the நாலாயிர திவ்ய பிரபந்தம், built
with Jetpack Compose and Material 3.

The bundled corpus in `app/src/main/assets/` is byte-identical to the iOS
build's `Resources/`, so the OCR pipeline and the essence-authoring tools keep
feeding both apps from one source. Nothing about the verse content is
Android-specific.

---

## Before the first build

Three things need setting up outside the code. The app runs without all three —
it just loses the corresponding feature rather than failing — so none of them
blocks getting it on a device.

### 1. Dependency versions

Every version in `gradle/libs.versions.toml` was pinned offline and **could not
be resolved** against the Google or Maven repositories. Open the project in
Android Studio, let it sync, and accept whatever it flags.

**On Material 3 Expressive:** as of `material3:1.4.0`, `MaterialExpressiveTheme`,
the `MotionScheme` interface and the `ExperimentalMaterial3ExpressiveApi` marker
are all declared `internal` — the code has shipped but the API is not open, and
opting in does not help, because internal is not experimental. The app therefore
uses the stable `MaterialTheme` and carries the expressive direction through the
parts that *are* public: generated tonal palettes, wallpaper colour, the rounder
shape scale, and adaptive navigation.

`ui/theme/Theme.kt` is the only file that would change when a release opens those
APIs up — swap `MaterialTheme` for `MaterialExpressiveTheme` and pass
`motionScheme = MotionScheme.expressive()`.

### 2. Google sync (optional)

Reading state syncs through a hidden folder in the person's own Google Drive.
It needs:

1. A Google Cloud project with the **Drive API** enabled.
2. An **Android OAuth client** registered against
   `com.srinivaskannan.divyaprabhandham` and your signing certificate's SHA-1
   (matched at runtime — no ID needed in source for this one).
3. A **Web application OAuth client**, whose client ID goes in
   `GoogleSyncManager.WEB_CLIENT_ID` — it's public by design (unlike a client
   *secret*, which must never appear in app code), and is what lets the
   authorization survive app restarts via `requestOfflineAccess()` rather than
   needing re-consent on every cold start.

Until both exist, the sync toggle simply never succeeds and the app stays
entirely offline, which is the intended degraded state.

### 3. Tip jar (optional)

Create three **consumable** in-app products in the Play Console with the IDs in
`billing/TipJar.kt`: `tip_small`, `tip_medium`, `tip_large`. Until they exist,
the tip screen says the tip jar is unavailable rather than showing dead buttons.

### 4. Artwork (already wired in)

Nothing to do — noted here because two things about it are not obvious.

**The app icon is the supplied artwork, whole.** Not split apart, not
redrawn — but it does need two things done to it, both because of how Android
composites icons.

First, the source PNG has a pale grey canvas around its rounded square. That
has to go: when a launcher falls back to a non-adaptive bitmap it generates a
background by sampling the image's edge pixels, and a pale edge produces a
light ring with the artwork sitting inset inside it — the icon appears to have
two frames. The pale surround is trimmed and the rounded corners flooded with
the artwork's own maroon (`#680923`, sampled from its ground), so the square is
opaque edge to edge and no layer contains a pale pixel.

Second, sizing. The adaptive icon's background is that same flat maroon, and
the artwork is the foreground, scaled so the Tamil wordmark — the outermost
element that carries meaning — lands on the circular viewport. That puts the
artwork at 97% of the visible area, so it fills the icon rather than floating
in it, and its own square edge is invisible against the matching background.
The namam lands at 112px against a 132px safe radius, comfortably inside.

What this costs: on launchers that mask to a **circle**, the decorative frame's
corner lotuses fall outside and are trimmed. Squircle and rounded-square masks
keep them. Fitting those corners too would have shrunk the artwork to 77% of
the viewport, which is the inset look this is meant to avoid — so the corners
were the thing to give up.

Full-bleed legacy bitmaps (`ic_launcher`, `ic_launcher_round`) ship alongside
the adaptive XML, so any non-adaptive path shows the artwork on maroon instead
of a generated pale background.

The themed icon (Android 13+) is a namam silhouette — a monochrome layer is
alpha-only, so the full artwork cannot be used there.

`store/play-icon-512.png` is the Play Console icon: the complete artwork with
its corners flooded to maroon, opaque edge to edge.

**The splash is a Compose screen, not the system splash.** Android 12+ only
lets the system splash show a centred icon on a flat colour, so a full-bleed
image is impossible there. The system splash therefore shows the emblem on the
artwork's maroon and hands over on the first frame to a Compose screen that
draws the artwork edge to edge until the corpus finishes parsing. Both use the
same `#50071B`, so the handover has no seam.

### 5. Reading fonts (optional, recommended for release)

The four reading typefaces fall back to generic families, and Android's own font
fallback picks a Tamil face for each. To pin them down across every device, drop
font files into `app/src/main/res/font/` with these exact names — no code or
Gradle change needed:

```
noto_serif_tamil   noto_sans_tamil   hind_madurai
literata           source_serif      noto_serif      nunito_sans
```

See `ui/theme/Type.kt` for why this is a runtime lookup rather than downloadable
Google Fonts.

### 6. Gradle wrapper JAR

`gradle/wrapper/gradle-wrapper.properties` is here but `gradle-wrapper.jar` is
binary and could not be generated offline. Either open the project in Android
Studio, which supplies it, or run `gradle wrapper` once with a local Gradle.

---

## Verifying without a compiler

There is no Kotlin compiler in the environment this port was written in, exactly
as there was none for Swift. `audit.py` stands in for one:

```
python3 audit.py
```

It checks delimiter balance, package/directory agreement, that every
`Ui.SOMETHING` reference exists in the table, that internal imports resolve,
that the manifest and `R.*` references point at real resources, that every
`libs.*` alias exists in the catalog, and that every bundled JSON parses. Run it
after every edit. It cannot catch type errors — Android Studio is still the
first real compile.

Two data invariants were also verified directly against the corpus during the
port: the stanza parser reproduces **3,884 global pasurams, 1–3884, with no
gaps**, and all five division mesh palettes plus the five accent seeds are
byte-identical to the iOS build's values.

---

## What changed, and why

| iOS | Android | Note |
|---|---|---|
| SwiftUI | Compose + Material 3 | Expressive theme API is still `internal`; see above |
| `@Observable` + `didSet` | `AppState` on Compose state + DataStore | Same shape; two mutation paths, local and remote |
| iCloud key-value store | Google Drive `appDataFolder` | Last-writer-wins on one small document, as before |
| Sign in with Apple | *(removed)* | Sync needs a Drive grant, not an identity |
| MusicKit / Apple Music | Hand-off to YouTube Music | See below |
| WidgetKit | Glance | Verse chosen from the clock, so nothing writes back |
| `UNUserNotificationCenter` | AlarmManager + `NotificationCompat` | Inexact alarms, to avoid `SCHEDULE_EXACT_ALARM` |
| StoreKit | Play Billing | Consumable tips |
| Liquid Glass | Material 3 tonal surfaces | Not imitated, per instruction |
| `MeshGradient` | Nine radial gradients on a Canvas | Compose has no mesh primitive |
| `ContentUnavailableView` | `EmptyState` | Material has no equivalent |
| iPhone/iPad split | `NavigationSuiteScaffold` | Adaptive layout removes the need for a separate Book tab |

### Recitations

Recitations play **audio-only, in a mini-bar**, through YouTube's official
IFrame Player. There is no native YouTube playback SDK on Android and no legal
way to feed its audio to ExoPlayer, so the IFrame player in a WebView is the
interface. The WebView is parked offscreen as a 1dp sliver
(`ui/components/RecitationHost.kt`) — attached to the window so it produces
sound, but never given room to show video. The entire visible UI is the
`RecitationBar`: title, play/pause, skip, buffering spinner, close.

The bar is app-wide and survives navigation: one `RecitationSession` held above
the nav graph (`media/RecitationSession.kt`), so a recitation started from the
browser keeps playing as you move around. It stops when the last track ends,
when closed, or when the app is finished (not on rotation). This is the deepest
divergence from the iOS build, where MusicKit drove true background audio under
the listener's Apple Music subscription; embedded YouTube may not play with the
app gone.

Playback is driven by `youtube.json`, keyed by work id with a per-division
fallback. To map a specific work, add its id under `works` with a list of video
ids; that wins over the division pool.

NOTE ON THE BLACK SCREEN: the earlier full-video player rendered black because
`loadDataWithBaseURL` needs a real `https://www.youtube.com` base URL for the
IFrame API's origin check — a `null` or `file://` origin makes the player load
its chrome but refuse to start. That base URL is set now, and since the player
is audio-only there is no video surface to mis-render regardless.

NOTE ON PLAYBACK FAILING: if a track loads but stays silent, the likely cause
is that the specific `music.youtube.com` upload disallows embedding (IFrame
error 101/150) — some music uploads simply cannot be played from an embedded
player, and nothing app-side changes that. The player now reports these: it
skips an un-embeddable track and moves to the next, and the bar shows "This
track can't play in-app" if none can play. If every supplied track is blocked,
the fix is to map embeddable video ids (the regular youtube.com watch id of the
same recitation, where the uploader allows embedding) in youtube.json.

### Sync is not continuous

Drive `appDataFolder` gives a small synced document with no backend of ours,
which is the closest thing to `NSUbiquitousKeyValueStore`. Conflict resolution
is last-writer-wins on the whole document — matching the iOS contract. A
per-field merge would lose fewer edits but would also let an unbookmarked verse
come back from the dead, which is worse.

Android Auto Backup is left enabled as well (`res/xml/backup_rules.xml`), so a
new device restores reading state even for someone who never turns sync on.

---

## Known issue carried over from iOS

Section `b5w13s1` (துவயச்சுருக்கு, Desika Prabandham) opens with a taniyan
numbered `1`, then restarts its verses at `1`. Both produce the stanza key
`b5w13s1#1`, so bookmarking one marks both, and a lookup returns the taniyan.

**This behaves identically on iOS** — it is the stanza-key scheme, not a port
regression. It affects one section out of 337. The key scheme was kept identical
deliberately, so bookmarks mean the same thing on both platforms; fixing it is
worth doing in both at once, or not at all.

---

## Layout

```
data/          corpus models, stanza parser, repository, UI string table
prefs/         AppState (DataStore) and the user-facing choice enums
sync/          Drive appdata client and the sync manager
media/         YouTube Music hand-off
billing/       Play Billing tip jar
notify/        reminder scheduling and receivers
widget/        Glance widget and its snapshot bridge
ui/theme/      Material 3 tonal palette generation, reader palettes, typography
ui/nav/        routes and the adaptive navigation shell
ui/*/          one package per screen
```

The stanza parser (`data/StanzaParser.kt`) is the piece to be most careful with:
pasuram numbering drives bookmark keys, the jump index, Divya Desam
cross-references and the Margazhi day lookup, so a wrong block boundary silently
moves a bookmark onto a different verse.
