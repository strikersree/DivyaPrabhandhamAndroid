package com.srinivaskannan.divyaprabhandham.ask

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.util.UUID

/**
 * One turn in the Ask conversation. A user question, an AI answer, an error, or
 * a set of local corpus matches shown as tappable cards. Kept deliberately
 * simple — the thread lives in memory for the session and is not persisted in
 * v1 (privacy-friendly, and a conversation about verses is scratch, not a
 * document to keep).
 */
sealed interface AskMessage {
    val id: String

    data class User(
        override val id: String = UUID.randomUUID().toString(),
        val text: String,
    ) : AskMessage

    data class Assistant(
        override val id: String = UUID.randomUUID().toString(),
        val text: String,
    ) : AskMessage

    data class Failure(
        override val id: String = UUID.randomUUID().toString(),
        val kind: AskError,
    ) : AskMessage

    /** Local corpus hits (work/section ids) shown as cards under a user turn. */
    data class LocalMatches(
        override val id: String = UUID.randomUUID().toString(),
        val sectionIds: List<String>,
    ) : AskMessage
}

/**
 * The Ask conversation for the current session. Holds the message thread and a
 * pending flag while the proxy call is in flight. Not a ViewModel to stay
 * consistent with the app's @Observable-style state objects; owned by the
 * search screen and cleared when the tab is left.
 */
class AskConversation {
    val messages = mutableStateListOf<AskMessage>()

    var pending by mutableStateOf(false)
        private set

    fun addUser(text: String) {
        messages.add(AskMessage.User(text = text))
    }

    fun addLocalMatches(sectionIds: List<String>) {
        if (sectionIds.isNotEmpty()) {
            messages.add(AskMessage.LocalMatches(sectionIds = sectionIds))
        }
    }

    fun addAnswer(text: String) {
        messages.add(AskMessage.Assistant(text = text))
    }

    fun addFailure(kind: AskError) {
        messages.add(AskMessage.Failure(kind = kind))
    }

    fun updatePending(value: Boolean) {
        pending = value
    }

    fun clear() {
        messages.clear()
        pending = false
    }
}
