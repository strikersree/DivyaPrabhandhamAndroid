# Divya Prabhandham — Android

An Android port of the iOS SwiftUI reader for the நாலாயிர திவ்ய பிரபந்தம், built
with Jetpack Compose and Material 3 Expressive.

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
Android Studio, let it sync, and accept whatever it flags. The only version that
carries design meaning is `material3`: **1.4.0 or newer** is where Material 3
Expressive (`MaterialExpressiveTheme`, `MotionScheme`) became stable. If a sync
pins something older, `ui/theme/Theme.kt` has a two-line fallback documented in
place — nothing else in the app touches the expressive APIs directly.

### 2. Google sync (optional)

Reading state syncs through a hidden folder in the person's own Google Drive.
It needs:

1. A Google Cloud project with the **Drive API** enabled.
2. An **Android OAuth client** registered against
   `com.srinivaskannan.divyaprabhandham` and your signing certificate's SHA-1.

No client ID goes in the source — the grant is matched on package plus
signature. Until this exists, the sync toggle simply never succeeds and the app
stays entirely offline, which is the intended degraded state.

### 3. Tip jar (optional)

Create three **consumable** in-app products in the Play Console with the IDs in
`billing/TipJar.kt`: `tip_small`, `tip_medium`, `tip_large`. Until they exist,
the tip screen says the tip jar is unavailable rather than showing dead buttons.

### 4. Reading fonts (optional, recommended for release)

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

### 5. Gradle wrapper JAR

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
| SwiftUI | Compose + Material 3 Expressive | |
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

`recitations.json` holds Apple Music catalogue IDs, which mean nothing here. The
Listen button hands off to YouTube Music (then YouTube, then a browser) rather
than playing in-process, because the only way to embed YouTube is the IFrame
player in a WebView, and using that for background audio is what YouTube's terms
forbid. Handing off is also closer to what the iOS design promised: audio is
never bundled, and playback happens under the listener's own account.

**The Now Playing pill and full player are gone** — once playback leaves the
app there is nothing to observe or control. The bottom accessory slot is given
over to Continue Reading alone.

To link specific recordings, add `yt_playlist` or `yt_video` to entries in
`recitations.json`; the model already reads them. Until then the button runs a
YouTube Music search for the work's Tamil title, author and "பாராயணம்", which
lands well for most of the corpus.

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
