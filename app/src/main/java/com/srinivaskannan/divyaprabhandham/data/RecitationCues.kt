package com.srinivaskannan.divyaprabhandham.data

import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import java.util.Collections

/** Where one pasuram is estimated to begin inside its track. */
data class PasuramCue(
    /** The decade (section) the pasuram belongs to. */
    val sectionId: String,
    /** The stanza's index in its section, the same under every script. */
    val stanzaIndex: Int,
    /** Milliseconds from the start of the track. */
    val startMs: Long,
)

/**
 * Estimated pasuram start times, for following a recitation in the reader.
 * A port of the iOS build's RecitationCues.
 *
 * Only the decades are timed by hand ([AmazonWork.decades]). Inside a decade
 * the pasurams share a metre and the reciter keeps an even pace, so each
 * pasuram is given a share of the decade's time in proportion to how long it
 * takes to say: its Tamil syllables, doubled for a pasuram marked as recited
 * twice, plus a fixed allowance for the breath between pasurams. The estimate
 * drifts a little through a decade and is pulled back into line at every
 * decade's mark.
 *
 * A decade ends at the next mark on the same track. The last decade on a
 * track ends with the track; when the track's length isn't known yet, it
 * borrows the pace of the track's other decades instead.
 */
object RecitationCues {

    /** The breath between pasurams, in syllables. Raise it if the highlight
     *  runs ahead early in a decade and behind late in it. */
    private const val PAUSE_WEIGHT = 6.0

    private val cache = Collections.synchronizedMap(HashMap<String, List<PasuramCue>>())

    /** The cues for track [track] of [workId]'s recording, in playback order.
     *  Empty when the track has no decade timings. */
    fun cues(
        workId: String,
        work: AmazonWork,
        track: Int,
        trackDurationMs: Long,
        repository: PrabandhamRepository,
    ): List<PasuramCue> {
        // Seconds are plenty for the key: the reported length jitters by a
        // few milliseconds from one report to the next.
        val key = "$workId#$track#${trackDurationMs / 1000}"
        cache[key]?.let { return it }

        val marks = work.marks(onTrack = track)
        if (marks.isEmpty()) return emptyList()

        // Each decade's pasurams, in the order they are recited, and how long
        // each takes to say.
        val weights: List<List<Pair<Int, Double>>> = marks.map { mark ->
            val section = repository.section(mark.section) ?: return@map emptyList()
            val verses = section.stanzas(ScriptChoice.TAMIL)
                .filter { !it.isHeading && !it.isDescription && !it.isSalutation && !it.isAdivaravu }
            val order = mark.order
            if (order == null) {
                verses.map { it.index to weight(it, repeats = if (it.repeatsTwice) 2 else 1) }
            } else {
                // An explicit order spells every repetition out, however the
                // pasuram is marked. A pasuram said twice running (4, 4) is
                // one cue said twice over, as a starred one is.
                val runs = mutableListOf<Pair<Int, Int>>()
                for (number in order) {
                    val last = runs.lastOrNull()
                    if (last != null && last.first == number) runs[runs.lastIndex] = number to last.second + 1
                    else runs += number to 1
                }
                runs.mapNotNull { (number, repeats) ->
                    verses.firstOrNull { it.number == number }?.let { it.index to weight(it, repeats) }
                }
            }
        }

        // Milliseconds per unit of weight over the decades whose span is
        // known, for a last decade whose end isn't.
        var knownMs = 0.0
        var knownWeight = 0.0
        for (i in 0 until marks.size - 1) {
            knownMs += (marks[i + 1].start - marks[i].start) * 1000
            knownWeight += weights[i].sumOf { it.second }
        }
        val pace = if (knownWeight > 0) knownMs / knownWeight else null

        val cues = mutableListOf<PasuramCue>()
        marks.forEachIndexed { i, mark ->
            val total = weights[i].sumOf { it.second }
            if (total <= 0) return@forEachIndexed
            val startMs = mark.start * 1000
            val span: Double = when {
                i + 1 < marks.size -> (marks[i + 1].start - mark.start) * 1000
                trackDurationMs > startMs -> trackDurationMs - startMs
                pace != null -> total * pace
                else -> {
                    // A lone decade on a track of unknown length: only its
                    // opening pasuram can be placed.
                    weights[i].firstOrNull()?.let {
                        cues += PasuramCue(mark.section, it.first, startMs.toLong())
                    }
                    return@forEachIndexed
                }
            }
            var t = startMs
            for ((index, w) in weights[i]) {
                cues += PasuramCue(mark.section, index, t.toLong())
                t += span * w / total
            }
        }
        cache[key] = cues
        return cues
    }

    /** The pasuram being recited at [positionMs] within [sectionId], if the
     *  position falls inside that decade. A pasuram can appear more than once
     *  (see [DecadeMark.order]); the latest one wins. */
    fun current(cues: List<PasuramCue>, sectionId: String, positionMs: Long): Int? =
        // A quarter second's lead, so the highlight lands as the first
        // syllable does rather than just after it.
        cues.lastOrNull { it.sectionId == sectionId && it.startMs <= positionMs + 250 }?.stanzaIndex

    /** How long [stanza] takes to recite [repeats] times over, with the breath
     *  before the next pasuram. A repeat follows straight on, with no breath of
     *  its own: counting one put the highlight about two seconds late on the
     *  pasuram after a starred one. */
    private fun weight(stanza: Stanza, repeats: Int): Double {
        val syllables = TamilProsody.scan(stanza.text).count { it is TamilProsody.Segment.Syllable }
        return syllables * repeats + PAUSE_WEIGHT
    }
}
