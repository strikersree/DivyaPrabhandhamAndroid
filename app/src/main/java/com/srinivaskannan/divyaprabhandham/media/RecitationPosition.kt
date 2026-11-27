package com.srinivaskannan.divyaprabhandham.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.produceState
import com.srinivaskannan.divyaprabhandham.data.AmazonTrack
import com.srinivaskannan.divyaprabhandham.data.AmazonWork
import com.srinivaskannan.divyaprabhandham.data.DecadeMark
import com.srinivaskannan.divyaprabhandham.data.PrabandhamRepository
import com.srinivaskannan.divyaprabhandham.data.RecitationCues
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository
import kotlinx.coroutines.delay

/**
 * Where the recitation Amazon Music is playing has got to, in this app's
 * terms: the decade, and the pasuram estimated to be under way in it.
 * Null fields where the recording isn't timed or the position falls before
 * the first timed section.
 */
@Immutable
data class RecitationPosition(
    val workId: String,
    /** Index into the work's tracks (0 for a one-track work). */
    val track: Int,
    val sectionId: String?,
    /** The stanza's index in its section. */
    val stanzaIndex: Int?,
)

/** The one [RecitationPosition], provided by the navigation shell. */
val LocalRecitationPosition = compositionLocalOf<RecitationPosition?> { null }

/** Milliseconds from the start of the track. */
val DecadeMark.startMs: Long get() = (start * 1000).toLong()

/**
 * Follows Amazon Music's position through [track]'s timed decades. Looks
 * every 300 ms while it plays; the value only changes when the decade or the
 * pasuram does, so whatever reads it recomposes that rarely.
 */
@Composable
fun rememberRecitationPosition(track: AmazonTrack?): State<RecitationPosition?> {
    val remote = LocalAmazonRemote.current
    val repository = LocalRepository.current
    val now = remote.now
    val starting = remote.starting
    return produceState<RecitationPosition?>(null, track, now, starting) {
        while (true) {
            value = if (starting != null) null else locate(remote, repository, track)
            if (now?.isPlaying != true) break
            delay(300)
        }
    }
}

/** The work's timings and the track index, when [track] is the timed recording now loaded. */
private fun timed(
    remote: AmazonRemote,
    repository: PrabandhamRepository,
    track: AmazonTrack?,
): Triple<AmazonWork, Int, List<DecadeMark>>? {
    val n = remote.now ?: return null
    track ?: return null
    val work = repository.amazonTarget(track.workId) ?: return null
    val index = (track.paththu ?: 1) - 1
    // Another album's recording of the same work is not what was timed.
    if (work.trackAt(index) != n.mediaId) return null
    val marks = work.marks(onTrack = index)
    return if (marks.isEmpty()) null else Triple(work, index, marks)
}

private fun locate(
    remote: AmazonRemote,
    repository: PrabandhamRepository,
    track: AmazonTrack?,
): RecitationPosition? {
    val (work, index, marks) = timed(remote, repository, track) ?: return null
    val n = remote.now ?: return null
    val position = n.livePositionMs()
    val section = marks.lastOrNull { it.startMs <= position + 500 }?.section
    val stanza = section?.let {
        val cues = RecitationCues.cues(track!!.workId, work, index, n.durationMs, repository)
        RecitationCues.current(cues, it, position)
    }
    return RecitationPosition(track!!.workId, index, section, stanza)
}

/**
 * Skips to the next or previous timed decade within the loaded track, as the
 * iOS player does. Returns false when there is no decade to go to that way,
 * so the caller can fall back to Amazon Music's own next / previous track.
 *
 * Back restarts the current decade if it has been playing more than three
 * seconds, and goes to the one before otherwise -- the convention of every
 * player's back button, a level down.
 */
fun AmazonRemote.skipDecade(
    repository: PrabandhamRepository,
    track: AmazonTrack?,
    forward: Boolean,
): Boolean {
    val n = now ?: return false
    if (!n.canSeek) return false
    val (_, _, marks) = timed(this, repository, track) ?: return false
    val position = n.livePositionMs()
    if (forward) {
        val next = marks.firstOrNull { it.startMs > position + 500 } ?: return false
        seekTo(next.startMs)
        return true
    }
    val current = marks.indexOfLast { it.startMs <= position + 500 }
    if (current >= 0) {
        val mark = marks[current]
        if (position - mark.startMs > 3000) {
            seekTo(mark.startMs)
            return true
        }
        if (current > 0) {
            seekTo(marks[current - 1].startMs)
            return true
        }
    }
    // Before the first decade there is often an opening of its own (a track
    // whose thaniyan starts a few seconds in): go back to that.
    if (marks.first().startMs >= 1000 && position > 3000) {
        seekTo(0)
        return true
    }
    return false
}
