# Ask refinements: feasibility

Four requested additions, assessed against the current code.

## 1. Voice input ("talk") for Ask — FEASIBLE, low-moderate

Android's on-device `SpeechRecognizer` / the `RecognizerIntent` speech dialog
turns speech into text with no API cost. Drop a mic button in the input bar;
the recognised text fills the field, then goes through the same submit path.

- **Language matters here.** The audience is Tamil-speaking; the recognizer
  should request `ta-IN` (with English fallback), and Tamil recognition quality
  varies by device/Google app version. Worth setting the locale explicitly.
- **Permission:** `RECORD_AUDIO` must be added to the manifest and requested at
  runtime (API 23+). Not currently present.
- **Two approaches:** (a) the simple `RecognizerIntent` popup (Google's own
  dialog — least code, consistent UX, works everywhere speech is available); or
  (b) inline `SpeechRecognizer` with a custom listening animation (more code,
  nicer feel). Recommend (a) first; it is a small, robust win.
- **Effort:** small. Manifest permission + runtime request + a mic IconButton +
  an ActivityResult launcher.

## 2. Hide Continue Reading on the Ask page — TRIVIAL

The pill is already gated: `if (!isReader) ResumePill(...)`. Add the Ask/search
route to that condition (`!isReader && !isSearch`). One line. The chat input
sits where the pill would, so hiding it also removes a layout collision. Do it.

## 3. Animated chakra instead of the loading dot — FEASIBLE, moderate

Today the "thinking" state is a `CircularProgressIndicator`. Replacing it with a
slowly-rotating chakra is a nice touch and on-brand.

- **Asset gap:** the splash is a flat `.webp` — there is no isolated chakra
  vector to rotate. Options: (a) author a small `chakra.xml` vector drawable
  (clean, scalable, themeable, rotates smoothly via Compose
  `rotate(animatedAngle)`); or (b) crop the chakra from the artwork as a PNG
  (fast but fixed-colour, less crisp). Recommend (a) — a simple dharma-chakra
  vector (hub + spokes + rim) is easy to draw and rotates beautifully.
- Also replaces the plain `AiSpark` gradient dot in the empty state / avatar if
  wanted, for consistency.
- **Effort:** moderate — mostly authoring a tasteful chakra vector; the rotation
  animation is a few lines (`rememberInfiniteTransition`).

## 4. Save Ask responses to the user's Google account + history button — FEASIBLE, moderate-to-large

This is the biggest of the four and has real design decisions.

- **Where it saves:** the app already syncs to Drive appDataFolder
  (`reading-state.json`). Ask history can ride the same mechanism — either
  appended to the existing `SyncPayload` or, better, a **separate**
  `ask-history.json` in the same appdata folder, so a large/growing history does
  not bloat the reading-state file that syncs on every bookmark.
- **Only when signed in:** correct and easy — gate on `appState.syncEnabled` /
  a live token. When signed out, history stays local (Room/DataStore) or is
  simply session-only; the request says "if signed in", so: local-or-nothing
  when signed out, Drive-backed when signed in.
- **History UI:** a history icon top-right of the Ask app bar opening a list of
  past Q&A, tap to re-open in the thread. Straightforward.
- **Design decisions to settle:**
  - **Cap & pruning:** history grows unbounded; pick a cap (e.g. last 100 Q&A)
    or a time window, and prune.
  - **Privacy:** these are the person's spiritual questions — sensitive. Keep it
    in their *own* Drive appdata (invisible to us, already the model), never on
    our side. A clear "clear history" control, like the search-history clear.
  - **Merge semantics:** last-writer-wins like the rest of sync is fine for a
    single user across devices; concurrent edits just union by timestamp.
  - **Currently the Ask thread is in-memory only** (AskConversation, not
    persisted). Persisting introduces a store (Room is the clean choice) plus
    the Drive file — this is the bulk of the work.
- **Effort:** moderate-to-large. New local store + Drive file + history screen +
  save-on-answer wiring + cap/prune + clear control.

## Recommended order

1. **#2 hide the pill** — trivial, do immediately.
2. **#1 voice input** — small, high user value, needs a permission.
3. **#3 animated chakra** — moderate, mostly a vector asset; nice polish.
4. **#4 history to Drive** — largest; worth doing but has the most decisions
   (cap, privacy control, local store). Best as its own focused change.

None are blocked. #4 is the one to scope carefully; the rest are
straightforward.
