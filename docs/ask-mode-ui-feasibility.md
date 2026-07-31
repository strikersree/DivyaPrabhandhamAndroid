# Ask-mode search UI (chat window): feasibility

## The request

Turn the existing search tab into a conversation window: input bar at the
bottom, messages above, Gemini-like look. No separate Ask toggle — the one
search box *is* Ask. Existing search behaviour folds into it.

Verdict: **feasible and a natural fit, but with one design decision that must be
made deliberately, because a naive "replace search with chat" would lose three
things the current search does well.**

## What the current search does (and must not be lost)

1. **Number jump.** Typing `474` jumps to pasuram 474 instantly, offline.
2. **Instant local filtering.** Typing a word filters works/sections live, as
   you type, offline, free — no network, no latency, no cost.
3. **Recent searches.** The 5-item history just added.

A chat window that sends every input to Gemini would make all three slower
(network round-trip), costly (a paid API call to jump to a pasuram number is
absurd), and offline-broken. So the design cannot be "every keystroke goes to
the model."

## The core decision: how local search and the AI coexist in one box

Three ways to reconcile "one box, no toggle" with keeping fast local search:

### Option A — Hybrid: local results inline, AI on submit (RECOMMENDED)
As the user types, keep showing instant local matches (and the number-jump
chip) exactly as today, rendered as a "here's what I found in the corpus" area.
When the user actually **submits** (taps send / hits enter), that question goes
to Gemini and a chat answer appears. So:
- Typing = instant local search (offline, free) — unchanged power.
- Submitting a question = an AI answer bubble.
The conversation window holds the AI exchange; local matches surface as tappable
cards, either inline in the thread or in a slim area above the input. This keeps
every current strength and adds Ask, with no toggle — the box just does the
cheap thing while typing and the smart thing on submit.

### Option B — Pure chat, with local search as a "tool"
Everything is a chat turn; the app decides per-message whether to answer locally
(number jump, exact title match) or call Gemini. Cleaner conceptually, but a
bare number or partial word is ambiguous, latency creeps into simple lookups,
and offline lookups become error bubbles. More work, worse offline story.

### Option C — Pure chat, everything to Gemini
Simplest to build, but loses number-jump, instant filtering, and all offline
capability, and spends money on trivial lookups. Not acceptable for this app.

## Layout & "Gemini look"

- **Bottom input bar**, pinned above the nav bar, rounded, with a send button —
  imeAction=Send, grows to a few lines for long questions.
- **Message list above**, reverse-scrollable, newest at the bottom, auto-scroll
  on new content. User turns right-aligned, AI turns left-aligned in a distinct
  surface. A subtle "typing" indicator while the proxy call is in flight.
- **Gemini-like touches** that are tasteful, not derivative: a soft
  primary→secondary gradient on the AI avatar/accent, generous spacing, answer
  text that streams in if we later enable streaming, a small "AI-generated —
  verify important details" disclaimer under answers (important for sacred
  content).
- **Local matches** render as tappable pasuram/work cards inside the thread
  (e.g. "I found these in the corpus:" followed by cards) so tapping still opens
  the reader. This is how the two worlds join visibly.
- **Recent searches / suggestions** become the empty-state of the thread —
  starter chips ("Who is Periyaazhwar?", "Meaning of Thiruppavai pasuram 1")
  plus the recent queries.

## Hard constraints this UI must respect

- **Network + errors:** unlike the rest of the app, Ask needs the network. Every
  send needs a pending state, a graceful offline message, and mapping of the
  proxy's 401/429/500/502 to human text (not raw codes).
- **The number-jump and offline lookups must never hit the network.** The
  client decides: pure number -> local jump; otherwise show local matches while
  typing, and only call the proxy on explicit submit.
- **Grounding (unchanged open question):** the app should attach retrieved
  corpus text (verse/essence/temple) to the proxy call as `context`. Meanings/
  commentary grounding still does not exist in the data — the disclaimer covers
  the gap, and that scope decision (meanings in/out) still stands.
- **Cost & abuse:** already handled proxy-side (Phase 3), but the client should
  debounce and only send on explicit submit, never per keystroke.
- **State:** the conversation is per-session in memory for v1 (cleared on
  leaving the tab), which is simplest and privacy-friendly; persistence can come
  later if wanted.

## Effort

Moderate. New: a chat message model + list, a bottom input bar, pending/error
states, the proxy client (mirroring DriveAppData's HttpURLConnection), and the
retrieval that builds `context`. Reused: filteredWorks (local matches),
noteSearch/recentSearches (starter chips), the reader navigation from result
cards.

## Recommendation

Build **Option A** — the hybrid. It delivers exactly what was asked (one box,
no toggle, chat window, Gemini look) while preserving instant offline local
search, the number jump, and zero-cost lookups. The single most important design
rule: **typing searches locally; submitting asks the AI.** That one rule is what
lets the two coexist in a single input without losing anything.

Two things to confirm before building:
1. **Meanings/commentary in or out** for v1 answers (grounding gap).
2. **Proxy endpoint URL** to wire the client to (from Phase 1 deploy).
