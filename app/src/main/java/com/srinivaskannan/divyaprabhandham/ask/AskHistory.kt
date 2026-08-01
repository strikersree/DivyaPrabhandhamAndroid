package com.srinivaskannan.divyaprabhandham.ask

import kotlinx.serialization.Serializable

/**
 * One saved Q&A exchange — a question and the answer it received, with a
 * timestamp. Deliberately small: the local matches and thread structure are not
 * kept, only the essential exchange, so a hundred of these stay tiny.
 */
@Serializable
data class AskEntry(
    val question: String,
    val answer: String,
    val askedAt: Long,
)

/**
 * The Ask history: the person's recent questions and answers. Capped and
 * most-recent-first. Serialized to a single JSON file in the user's Drive
 * appDataFolder (separate from reading-state.json so a growing history never
 * bloats the file that syncs on every bookmark) — but only when they are signed
 * in. Signed out, there is no history: these are personal spiritual questions,
 * and the request was to save them to the Google account, not locally.
 */
@Serializable
data class AskHistory(
    val version: Int = 1,
    val entries: List<AskEntry> = emptyList(),
) {
    fun withEntry(entry: AskEntry, cap: Int = MAX_ENTRIES): AskHistory {
        // Newest first; drop any exact-duplicate question+answer to avoid a
        // repeated exchange filling the list; then cap.
        val deduped = entries.filterNot {
            it.question == entry.question && it.answer == entry.answer
        }
        return copy(entries = (listOf(entry) + deduped).take(cap))
    }

    companion object {
        const val MAX_ENTRIES = 100
        const val FILE_NAME = "ask-history.json"
    }
}
