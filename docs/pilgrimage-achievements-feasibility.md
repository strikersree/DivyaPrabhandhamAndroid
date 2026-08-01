# Pilgrimage tracking & achievements: feasibility

## The idea

Let users mark Divya Desams as **Visited** (with the year) or **Planning to
go**, show a **progress bar** on Home across five levels (10/18/28/58/108), and
**unlock a card** at each level — with a confetti celebration when a temple is
marked visited. Long-press a temple to set its status. The two celestial abodes
(107 & 108) are excluded from marking.

Verdict: **feasible, well-scoped, and a genuinely good fit.** All the data and
persistence patterns it needs already exist. Below is what it takes and the
few real decisions.

## The 107/108 exclusion is clean in the data

The dataset has 106 physical Divya Desams plus the two **Eternal Abodes** —
`thirupparkadal` (Ocean of Milk) and `paramapadam` (Vaikuntam), both marked
`region = "Eternal Abodes"`. These are the classical 107 & 108: not places one
visits on pilgrimage. So the exclusion is a clean filter on that region, not a
hardcoded pair of ids — the long-press menu simply doesn't offer Visited/Planning
for those two, or shows them as a special "reached in devotion" state. The count
that drives levels is out of the 106 visitable temples (or 108 if the top level
is meant to include the two celestial ones as an aspirational cap — a decision
below).

## Data & persistence — already have the pattern

Visited/planning status is exactly the shape of `bookmarks`: a per-item string
state that persists across relaunch and syncs to Drive. Concretely:
- A map of `desamId -> VisitStatus(state, year?)` where state is VISITED /
  PLANNING / none.
- Persisted in DataStore and added to `SyncPayload` (a new field, versioned like
  the others) so pilgrimage progress rides the existing Drive sync — it follows
  the user across devices, which for a lifelong pilgrimage record is the right
  behaviour.
- The level thresholds and unlocked cards are **derived** from the visited count
  — not stored separately — so they can never drift out of sync with reality.

No new storage system, no schema migration beyond one additive sync field.

## The pieces to build

1. **Long-press menu on a temple row.** Compose `combinedClickable`
   (onClick opens the temple as today; onLongClick opens the status menu). The
   menu offers Visited / Planning to go / Clear, suppressed for the two Eternal
   Abodes.
2. **Year picker on Visited.** A scrollable year list (e.g. 1950..current). Store
   the year with the visit. Minor: allow "don't remember / unknown".
3. **Confetti celebration.** A one-shot particle animation overlay when a temple
   is newly marked visited (and a bigger one when a level unlocks). Doable in
   pure Compose (Canvas + a physics loop) or a tiny library; recommend a
   self-contained Compose overlay to avoid a dependency. Respect reduced-motion
   preferences.
4. **Home progress bar.** A segmented bar showing progress toward the next
   level, current level name, and count (e.g. "34 / 58 — Level 4"). Levels:
   L1 10, L2 18, L3 28, L4 58, L5 108. Names TBD (owner will supply).
5. **Unlockable cards.** One distinct card per level, locked (silhouette) until
   the threshold is crossed, then revealed. A cards screen (or a Home carousel)
   showing earned + upcoming. Art per card is the main asset need — placeholder
   art first, final art when ready.

## Decisions to confirm

1. **Denominator: 106 or 108.** Level 5 says 108, but only 106 are visitable.
   Either (a) L5 = all 106 visitable temples and 108 is symbolic, or (b) the two
   Eternal Abodes count as auto-achieved/aspirational so the max reads 108.
   Recommend (a) with the bar labelled "106 temples" and 108 acknowledged in the
   final card's text — honest, and it still lets someone reach the top.
2. **Planning-to-go:** just a status/filter, or also its own count/UI (e.g. a
   "planned" chip on Home)? Simplest first: a status that shows on the row and a
   filter, no separate progress.
3. **Year range & "unknown" option.**
4. **Card art:** placeholder now, final art later? And the five level names
   (owner is supplying).
5. **Reduced motion:** honour the system setting for the confetti (recommended).

## Effort

Moderate. The tracking + persistence + sync + progress bar is straightforward
and reuses proven patterns. The polish — confetti physics and the unlockable
card art/screen — is where the time goes, and it is what makes the feature feel
special. Suggest building in two stages: (A) tracking + Home progress + sync
(the useful core), then (B) confetti + unlockable cards (the delight), once the
level names and card art are ready.

## Recommendation

Build it — it fits the app and the data supports it cleanly. Stage A first
(long-press status, year, Drive-synced progress, Home progress bar with the five
levels), Stage B for the celebration and cards once names + art arrive. Confirm
the 106-vs-108 denominator and the planning-to-go scope before starting Stage A.
