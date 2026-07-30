# Elevated pasuram cards: feasibility

## What exists today

Each pasuram renders as a `Surface` (ReaderScreen.StanzaCard) with:
- `shape = shapes.large` (rounded), `color = palette.card`
- an optional `border` — present only in the two high-contrast themes
- `tonalElevation = 1.dp` when there is no border, `0.dp` when there is
- a bookmarked verse thickens/accents the border instead of elevating

So the card is **flat by design**: it uses tonal elevation (a slight colour
shift) rather than shadow elevation (a cast shadow). That was a deliberate
reading choice, not an oversight.

## Feasibility: is an elevated (shadowed) card possible?

Yes, trivially, in mechanics: `Surface(... shadowElevation = 2.dp)` or an M3
`ElevatedCard` both drop a shadow. It is a one-line change per card. But whether
it is a *good* change depends on four theme-specific problems, which is why this
is worth analysing before doing.

### 1. It interacts badly with two of the five themes
There are five reader themes, and `cardBorder` is non-null in two of them
(high-contrast light and dark), where the card is defined by a hard black/white
outline. A shadow under a hard-outlined card looks muddy — outline and shadow
are two competing ways to separate the card from the page, and together they
read as a rendering bug. So an elevated card would need to stay flat in the two
bordered themes and elevate only in the three borderless ones (paper, sepia,
dark). That is a conditional, not a blanket change.

### 2. Shadows are nearly invisible on the dark themes
Shadow elevation is a black shadow at low alpha. On the sepia and paper themes
it reads; on the two dark themes (dark, high-contrast dark) a black shadow on a
near-black page is close to invisible, so the elevation effort buys almost
nothing there and the card still looks flat. Dark-theme Material uses a lighter
surface tint for elevation precisely because shadows do not carry — which is the
tonal-elevation approach the app already uses.

### 3. Reading ergonomics: shadows add visual noise down a long scroll
The reader is a long vertical list of cards — a decad can be 100+ verses. A
shadow under every card, repeated down a long scroll, adds a lot of visual
buzz; flat cards with generous spacing are calmer to read for a devotional
text, which is the stated priority ("quality of UI and content"). This is the
strongest argument against, and it is subjective — it is a design call, not a
technical limit.

### 4. The bookmarked-verse cue currently uses the border
A bookmarked verse is shown by thickening the border to the accent colour. If
cards become elevated-without-border, that cue needs a replacement (a raised
elevation, an accent left-edge, a tint). Not hard, but it is coupled work the
change drags in.

## Options

A. **Do nothing.** The flat card with tonal elevation is a defensible,
   reading-first choice and consistent across all five themes.

B. **Subtle shadow on the three borderless themes only.** `shadowElevation`
   ~1-2dp on paper/sepia/dark, unchanged on the two bordered themes; re-home the
   bookmark cue to an accent edge. Moderate effort, mostly theme plumbing.
   Gives the "lifted" look on the themes where it reads, without breaking the
   others.

C. **Full M3 ElevatedCard everywhere.** Highest visual change, but fights the
   bordered themes and the dark themes and the long-scroll calm — not
   recommended for this content.

## Recommendation

Option **B** if you want the lifted look — it is the only one that survives all
five themes and the long-scroll reading pattern. Scope: a `shadowElevation`
derived in ReaderPalette (so each theme sets its own, 0 for bordered/dark), plus
moving the bookmark indicator off the border. Roughly one palette field, one
Surface change, and a new bookmark cue. Option A remains a legitimate choice —
the flatness is not a bug.

The choice is aesthetic, not technical, so it should be yours to make with the
tradeoffs above in view.
