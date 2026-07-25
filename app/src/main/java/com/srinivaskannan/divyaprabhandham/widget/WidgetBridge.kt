package com.srinivaskannan.divyaprabhandham.widget

import android.content.Context
import com.srinivaskannan.divyaprabhandham.data.Division
import com.srinivaskannan.divyaprabhandham.data.PrabandhamRepository
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.data.UiText
import com.srinivaskannan.divyaprabhandham.prefs.AppState
import com.srinivaskannan.divyaprabhandham.prefs.WidgetAayiram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** One verse in the widget's rotation pool. */
@Serializable
data class WidgetVerse(
    /** Global pasuram number, or the per-work number for Desika Prabandham. */
    val n: Int,
    /** Verse text, already in the script the app is currently set to. */
    val t: String,
    /** Work title. */
    val w: String,
    /** Author. */
    val a: String,
    /** Section id, so a tap can open exactly this verse. */
    val s: String,
    /** Stanza key within the section. */
    val k: String,
    /** True when the number is per-work rather than the 1–3884 series. */
    val local: Boolean = false,
)

/**
 * What the app hands the widget: a pool of verses per division plus the current
 * reading position, all in the app's current script.
 */
@Serializable
data class WidgetSnapshot(
    val pools: Map<String, List<WidgetVerse>> = emptyMap(),
    val appAayiram: String = WidgetAayiram.ALL.key,
    val script: String? = null,
    val lastReadTitle: String? = null,
    val lastReadSubtitle: String? = null,
    val lastReadSectionId: String? = null,
    val lastReadStanzaKey: String? = null,
    val uiContinueReading: String = "",
    val uiPasuram: String = "",
) {
    /** The pool to draw from for a given widget configuration. */
    fun verses(choice: WidgetAayiram): List<WidgetVerse> {
        val effective = if (choice == WidgetAayiram.FOLLOW_APP) {
            WidgetAayiram.from(appAayiram)
        } else {
            choice
        }
        return when (effective) {
            WidgetAayiram.FOLLOW_APP, WidgetAayiram.ALL -> pools.values.flatten()
            else -> pools[effective.key] ?: pools.values.flatten()
        }
    }
}

/**
 * Keeps the widget's snapshot file current.
 *
 * The widget runs in a separate process with no access to the repository, and
 * parsing 5.7 MB of JSON in a Glance worker would be absurd. So the app renders
 * a small pool of ready-to-display verses to disk whenever something relevant
 * changes — the script setting, the reading position, the chosen division.
 *
 * On iOS this lived in an App Group container shared with the widget extension.
 * On Android the widget is part of the same app, so plain internal storage is
 * enough and nothing has to be shared out.
 */
object WidgetBridge {

    private const val FILE_NAME = "widget_snapshot.json"

    /**
     * Verses sampled per division. Enough that the hourly rotation does not
     * visibly repeat within a few days, small enough that the file stays well
     * under a hundred kilobytes and parses instantly.
     */
    private const val POOL_SIZE = 60

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    fun readSnapshot(context: Context): WidgetSnapshot? = runCatching {
        val f = file(context)
        if (!f.exists()) return null
        json.decodeFromString<WidgetSnapshot>(f.readText())
    }.getOrNull()

    /** Rebuilds and writes the snapshot. Safe to call often; it is debounced by the caller. */
    suspend fun refresh(
        context: Context,
        repository: PrabandhamRepository,
        appState: AppState,
    ) = withContext(Dispatchers.IO) {
        val script = appState.scriptChoice

        val pools = Division.all.associate { division ->
            val works = repository.works(division).orEmpty()
            val verses = mutableListOf<WidgetVerse>()
            // Walk works round-robin rather than taking the first N verses, so
            // a division's pool represents the whole division and not just
            // whichever work happens to sort first.
            val perWork = (POOL_SIZE / works.size.coerceAtLeast(1)).coerceAtLeast(1)
            for (work in works) {
                var taken = 0
                for (section in work.sections) {
                    for (stanza in section.stanzas(script)) {
                        if (taken >= perWork) break
                        if (stanza.isHeading || stanza.isDescription) continue
                        val number = stanza.number ?: continue
                        verses += WidgetVerse(
                            n = number,
                            t = stanza.text,
                            w = work.title(script),
                            a = work.author(script),
                            s = section.id,
                            k = section.key(stanza),
                            local = !division.usesGlobalNumbering,
                        )
                        taken++
                    }
                    if (taken >= perWork) break
                }
            }
            division.id to verses
        }

        val lastRead = appState.lastRead
        val section = lastRead?.let { repository.section(it.sectionId) }

        val snapshot = WidgetSnapshot(
            pools = pools,
            appAayiram = appState.widgetAayiram.key,
            script = script.key,
            lastReadTitle = section?.title(script),
            lastReadSubtitle = section?.let {
                repository.workContaining(it.id)?.title(script)
            },
            lastReadSectionId = lastRead?.sectionId,
            lastReadStanzaKey = lastRead?.stanzaKey,
            uiContinueReading = UiText.string(Ui.CONTINUE_READING, script),
            uiPasuram = UiText.string(Ui.PASURAM, script),
        )

        runCatching { file(context).writeText(json.encodeToString(snapshot)) }
    }
}
