# First-run onboarding: feasibility

## The request

On first launch, a guided flow:
Welcome → Font size → Script & menu language → Theme → App icon → Daily reminders
→ Sign in with Google. Each step shows a live preview before confirming. A
"Skip now" escape, a bilingual welcome, and a footer note that everything is
changeable later in Settings.

Verdict: **feasible and a strong fit — six of the seven steps are essentially
free because the settings state and logic already exist.** The onboarding is
mostly a well-designed wrapper around setters the app already has. The one step
with real new engineering is the **app-icon switch**; everything else is UI.

## What already exists (so most steps are low-risk)

- **Font size** — `appState.fontSize` + `updateFontSize()` with min/max clamp.
- **Script & menu language** — `appState.scriptChoice` + `updateScript()`
  (drives all in-app text via `UiText`).
- **Theme** — `appState.theme` + `updateTheme()` (five reader themes).
- **Daily reminders** — full stack already built: `notificationsEnabled`,
  `reminderTimes`, `ReminderScheduler`, `POST_NOTIFICATIONS` permission, receivers.
- **Sign in with Google** — `GoogleSyncManager.authorize()` with the consent
  flow already wired (pending the Cloud OAuth client the owner is setting up).

So Welcome, Font, Script, Theme, Reminders and Sign-in are **new screens over
existing setters** — the live preview is just reading the same state the rest of
the app reads. Low risk.

## The one hard step: switching the app icon

There is currently a single launcher icon and **no alternative-icon support**.
Letting the user pick an icon (e.g. the Thenkalai icon) at runtime requires the
standard Android mechanism:

- Declare the main launcher as an `activity-alias` plus one alias per alternative
  icon, each with its own `android:icon`/`roundIcon`, all but the default
  `android:enabled="false"`.
- At runtime, `PackageManager.setComponentEnabledSetting(...)` disables the
  current alias and enables the chosen one.

Caveats to be honest about:
- **The launcher shows a brief "app installed/added" behaviour and the icon
  updates after a moment** — switching aliases is visible and not perfectly
  instant on every launcher. This is an OS-level constraint, not something we
  control.
- On some OEM launchers the change can be delayed or require a relaunch.
- The app must not be *killed* mid-switch; the standard pattern toggles the
  alias and lets the system settle.

This is well-trodden but it is the only step that adds manifest structure and a
runtime component toggle. It also needs the alternative icon assets (the
Thenkalai icon supplied) processed into the mipmap set (adaptive
foreground/background, round, monochrome) like the existing launcher.

Recommendation for icon step: support **two icons to start** — the current
default and the supplied Thenkalai icon — via two aliases. More can be added
later with the same pattern. If icon-switching proves fiddly on the owner's
device, it can be the one step we defer without touching the rest.

## First-launch gating

A single persisted flag (`onboardingComplete`, in DataStore like the other
prefs). If false on launch, show the onboarding host over the app; on finish or
Skip, set it true. It should **not** sync via Drive — it is device-local (a new
device's first run should still onboard). One additive DataStore key, no schema
concerns.

## Live preview

Each step previews against the very state it sets:
- Font: a sample pasuram line that resizes as the slider moves.
- Script: the same line rendered in Tamil / readable / scholarly as toggled.
- Theme: a mini reader card in the chosen theme's colours.
- Icon: the icon art shown large before applying.
- Reminders: a sample notification-style row at the chosen time.
This is straightforward because the preview just reads the same `appState`
values (or local pending copies) the app already uses.

## Shape & effort

- An `OnboardingHost` (a pager/stepper) shown from `MainActivity` when the flag
  is unset, above the normal content. Per-step composables. A progress
  indicator and Back/Next/Skip.
- Bilingual welcome copy (supplied), and the Settings-reminder footer note.
- Effort: **moderate.** The flow, previews and gating are UI over existing state
  (low risk, some volume). The icon-switch is the one genuinely new mechanism
  and the only part with device-dependent behaviour.

## Recommendation

Build it. Suggest two stages:
- **Stage A:** the full flow — Welcome, Font, Script, Theme, Reminders, Sign-in
  — with live previews, Skip, the footer note, and first-launch gating. All over
  existing state; low risk.
- **Stage B:** the App-icon step — process the Thenkalai icon into a second
  launcher alias and wire the runtime switch. Isolated, and the one step with
  OS-level quirks, so cleaner as its own change.

Decisions to confirm:
1. **Icon options:** just the current icon + the supplied Thenkalai icon to
   start (two), or more?
2. **Skip placement:** Skip on every step (skips the rest) or only on Welcome?
3. **Sign-in step when no OAuth client yet:** show it but let it no-op/inform, or
   hide until the Cloud setup is done?
