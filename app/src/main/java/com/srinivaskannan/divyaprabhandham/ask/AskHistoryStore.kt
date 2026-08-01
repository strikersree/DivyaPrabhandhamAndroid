package com.srinivaskannan.divyaprabhandham.ask

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.srinivaskannan.divyaprabhandham.sync.DriveAppData
import com.srinivaskannan.divyaprabhandham.sync.GoogleSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Persists Ask history to the user's Drive appDataFolder — only while signed
 * in. The thread itself stays in memory (AskConversation); this store keeps the
 * durable, cross-device record of past questions and answers, capped at
 * [AskHistory.MAX_ENTRIES].
 *
 * Signed out, every call is a quiet no-op: these are personal spiritual
 * questions, and the decision was to keep them in the person's own Google
 * account, not on the device or anywhere we hold. Nothing is written locally.
 */
class AskHistoryStore(
    private val sync: GoogleSyncManager,
    private val scope: CoroutineScope,
) {
    /** The loaded history, newest first. Empty until loaded or when signed out. */
    var history by mutableStateOf(AskHistory())
        private set

    var loading by mutableStateOf(false)
        private set

    private var fileId: String? = null

    /** Pulls the history from Drive. No-op (and clears) when signed out. */
    fun load() {
        scope.launch {
            val token = sync.currentToken() ?: run {
                history = AskHistory()
                return@launch
            }
            loading = true
            try {
                val id = fileId ?: DriveAppData.findHistoryFileId(token)?.also { fileId = it }
                history = if (id != null) {
                    DriveAppData.downloadHistory(token, id) ?: AskHistory()
                } else {
                    AskHistory()
                }
            } finally {
                loading = false
            }
        }
    }

    /**
     * Records one exchange. Updates the in-memory copy immediately (so the
     * history screen reflects it at once) and writes through to Drive when
     * signed in; silently skips the write when signed out.
     */
    fun record(question: String, answer: String) {
        val entry = AskEntry(question = question, answer = answer, askedAt = System.currentTimeMillis())
        history = history.withEntry(entry)
        scope.launch {
            val token = sync.currentToken() ?: return@launch
            val id = fileId ?: DriveAppData.findHistoryFileId(token)
            if (id == null) {
                fileId = DriveAppData.createHistory(token, history)
            } else {
                fileId = id
                DriveAppData.updateHistory(token, id, history)
            }
        }
    }

    /** Clears history everywhere it lives — memory and the user's Drive file. */
    fun clear() {
        history = AskHistory()
        scope.launch {
            val token = sync.currentToken() ?: return@launch
            val id = fileId ?: DriveAppData.findHistoryFileId(token) ?: return@launch
            fileId = id
            DriveAppData.updateHistory(token, id, AskHistory())
        }
    }
}
