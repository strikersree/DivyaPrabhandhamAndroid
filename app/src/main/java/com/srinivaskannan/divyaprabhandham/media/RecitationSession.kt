package com.srinivaskannan.divyaprabhandham.media

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The one recitation playing right now, app-wide.
 *
 * A single session held above navigation, so starting a recitation and then
 * moving around the app keeps it playing and keeps the mini-bar on screen —
 * the way a music app behaves. Starting a different work replaces it; there is
 * only ever one player, because there is only ever one thing being recited.
 */
class RecitationSession(private val context: Context) {

    var controller by mutableStateOf<YouTubeAudioController?>(null)
        private set

    /** The work whose recitation is loaded, for the bar's label. */
    var workId by mutableStateOf<String?>(null)
        private set
    var workTitle by mutableStateOf("")
        private set
    var workAuthor by mutableStateOf("")
        private set

    val isActive: Boolean get() = controller != null

    /** Starts (or restarts) a work's recitation. */
    fun start(workId: String, title: String, author: String, ids: List<String>) {
        if (ids.isEmpty()) return
        if (this.workId == workId && controller != null) return
        controller?.release()
        this.workId = workId
        workTitle = title
        workAuthor = author
        controller = YouTubeAudioController(context).also { it.load(ids) }
    }

    /** Stops playback and clears the bar. */
    fun stop() {
        controller?.release()
        controller = null
        workId = null
        workTitle = ""
        workAuthor = ""
    }
}
