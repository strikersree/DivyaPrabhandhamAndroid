package com.srinivaskannan.divyaprabhandham.data

import android.content.Context
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Calendar

/**
 * The loaded corpus and every lookup built on top of it.
 *
 * This is the Android counterpart of the iOS `PrabandhamStore`, with one
 * structural difference that matters: iOS builds the store synchronously in
 * `init` and lets SwiftUI wait. Doing that on Android would run ~5.7 MB of
 * JSON parsing plus a full stanza pass on the main thread and trip the ANR
 * watchdog on slower devices. So [load] is a suspend function, the work happens
 * on [Dispatchers.IO], and the splash screen holds until it resolves.
 *
 * Once loaded the object is effectively immutable, so it is safe to read from
 * composition without further synchronisation.
 */
class PrabandhamRepository private constructor(
    /** Works per division id, for every division whose asset is bundled. */
    val loadedWorks: Map<String, List<Work>>,
    val essences: Map<Int, Essence>,
    val decadEssences: Map<String, Essence>,
    val recitations: Map<String, Recitation>,
    private val youtube: YouTubeCatalogue,
    private val amazon: AmazonCatalogue = AmazonCatalogue(),
    val divyaDesams: List<DivyaDesam>,
    val aazhwars: List<Aazhwar>,
) {

    val title = "நாலாயிர திவ்ய பிரபந்தம்"

    /** All divisions, live or upcoming. */
    val divisions: List<Division> get() = Division.all

    /** Works for a division, or null if its content is not bundled yet. */
    fun works(division: Division): List<Work>? = loadedWorks[division.id]

    /** Every loaded work across divisions, in canonical order. */
    val allWorks: List<Work> = Division.all.mapNotNull { loadedWorks[it.id] }.flatten()

    val flattenedSections: List<BookSection> = allWorks.flatMap { it.sections }

    private val sectionsById: Map<String, BookSection> =
        flattenedSections.associateBy { it.id }

    private val worksById: Map<String, Work> = allWorks.associateBy { it.id }

    private val workBySectionId: Map<String, Work> =
        allWorks.flatMap { work -> work.sections.map { it.id to work } }.toMap()

    private val divisionByWorkId: Map<String, Division> =
        loadedWorks.entries.flatMap { (divId, works) ->
            val division = Division.byId(divId)
            if (division == null) emptyList() else works.map { it.id to division }
        }.toMap()

    private val sectionOrder: Map<String, Int> =
        flattenedSections.withIndex().associate { (i, s) -> s.id to i }

    /**
     * Section ids belonging to divisions on the 1–3884 global numbering.
     * Desika Prabandham restarts numbering per work, so its verse 1 must not
     * resolve to global pasuram 1 and show Thiruppallandu's essence.
     */
    private val globallyNumberedSectionIds: Set<String> =
        Division.all.filter { it.usesGlobalNumbering }
            .flatMap { loadedWorks[it.id].orEmpty() }
            .flatMap { work -> work.sections.map { it.id } }
            .toSet()

    /** Global pasuram number -> section id, for jump-to-pasuram. */
    val pasuramIndex: Map<Int, String> = buildMap {
        for (division in Division.all) {
            if (!division.usesGlobalNumbering) continue
            for (work in loadedWorks[division.id].orEmpty()) {
                for (section in work.sections) {
                    for (stanza in section.stanzas(ScriptChoice.TAMIL)) {
                        stanza.number?.let { put(it, section.id) }
                    }
                }
            }
        }
    }

    val totalPasurams: Int = pasuramIndex.keys.maxOrNull() ?: 0

    // MARK: - Essences

    /**
     * The essence for a pasuram, if one has been authored.
     *
     * The section is required, not optional convenience: [essences] is keyed by
     * global number, so looking up a bare number from a per-work-numbered
     * division silently returns another Aazhwar's essence. Taking the section
     * makes that mistake unrepresentable.
     */
    fun essence(number: Int, sectionId: String): Essence? {
        if (sectionId !in globallyNumberedSectionIds) return null
        return essences[number]
    }

    /** The essence for a whole decad/section, if one has been authored. */
    fun decadEssence(sectionId: String): Essence? = decadEssences[sectionId]

    /** The recitation mapping for a work, if one exists. */
    fun recitation(workId: String): Recitation? = recitations[workId]

    /**
     * What to play for a work's recitation, with precedence resolved in one
     * place so callers do not have to. Order, most specific first:
     *   1. the work's own video ids       (works[workId])
     *   2. the work's own playlist         (workPlaylists[workId])
     *   3. the division's playlist         (playlists[division])
     *   4. the division's pooled video ids (divisions[division])
     * A work mapped to its own recitation is therefore never shadowed by a
     * division-level pool or playlist.
     */
    fun recitationTarget(workId: String): RecitationTarget {
        youtube.works[workId]?.takeIf { it.isNotEmpty() }
            ?.let { return RecitationTarget(videoIds = it) }
        youtube.workPlaylists[workId]?.takeIf { it.isNotBlank() }
            ?.let { return RecitationTarget(playlistId = it) }
        val divisionId = divisionForWork(workId)?.id
        if (divisionId != null) {
            youtube.playlists[divisionId]?.takeIf { it.isNotBlank() }
                ?.let { return RecitationTarget(playlistId = it) }
            youtube.divisions[divisionId]?.takeIf { it.isNotEmpty() }
                ?.let { return RecitationTarget(videoIds = it) }
        }
        return RecitationTarget()
    }

    fun hasRecitation(workId: String): Boolean =
        with(recitationTarget(workId)) { playlistId != null || videoIds.isNotEmpty() } ||
            amazonTarget(workId) != null

    /**
     * Amazon Music mapping for a work, if this work has been added to the
     * trial catalogue (amazon_music.json). Null means fall back to the
     * YouTube hand-off as before.
     */
    fun amazonTarget(workId: String): AmazonWork? = amazon.works[workId]

    // MARK: - Lookups

    fun section(id: String?): BookSection? = id?.let { sectionsById[it] }

    fun work(id: String): Work? = worksById[id]

    fun workContaining(sectionId: String): Work? = workBySectionId[sectionId]

    fun divisionForWork(workId: String): Division? = divisionByWorkId[workId]

    fun nextSection(id: String): BookSection? {
        val idx = sectionOrder[id] ?: return null
        return flattenedSections.getOrNull(idx + 1)
    }

    fun previousSection(id: String): BookSection? {
        val idx = sectionOrder[id] ?: return null
        if (idx == 0) return null
        return flattenedSections.getOrNull(idx - 1)
    }

    /** Section + stanza key for a given pasuram number, for jump-to-pasuram. */
    fun location(pasuram: Int): Pair<String, String>? {
        val sectionId = pasuramIndex[pasuram] ?: return null
        return sectionId to "$sectionId#$pasuram"
    }

    /**
     * Resolves a bookmark/scroll key ("<sectionID>#<n>" or "<sectionID>#i<i>")
     * back to its section and stanza.
     *
     * Two scripts are in play and conflating them was a real bug. The key is
     * resolved against the Tamil, always, because keys are derived from the
     * authoritative structure and must mean the same thing whatever the reader
     * has selected. The stanza that comes *back* is in [script], because it is
     * going on screen. Returning the Tamil stanza to a caller displaying it
     * meant bookmark previews and Divya Desam verses stayed Tamil in English
     * mode, then appeared to switch language when tapped through to the reader.
     *
     * Transliteration preserves line count and the parser derives block
     * structure from the Tamil, so the two lists are index-aligned; the Tamil
     * is used as a fallback if they somehow are not.
     */
    fun stanzaForKey(
        key: String,
        script: ScriptChoice = ScriptChoice.TAMIL,
    ): Pair<BookSection, Stanza>? {
        val hashIndex = key.lastIndexOf('#')
        if (hashIndex < 0) return null
        val section = section(key.substring(0, hashIndex)) ?: return null
        val tamil = section.stanzas(ScriptChoice.TAMIL)
        val index = tamil.indexOfFirst { section.key(it) == key }
        if (index < 0) return null
        val stanza = if (script == ScriptChoice.TAMIL) {
            tamil[index]
        } else {
            section.stanzas(script).getOrNull(index) ?: tamil[index]
        }
        return section to stanza
    }

    // MARK: - Divya Desams

    /** Divya Desams grouped by traditional region, preserving order. */
    fun desamsByRegion(script: ScriptChoice): List<Pair<String, List<DivyaDesam>>> {
        val order = mutableListOf<String>()
        val groups = mutableMapOf<String, MutableList<DivyaDesam>>()
        for (desam in divyaDesams) {
            val key = desam.region(script)
            if (key !in groups) order += key
            groups.getOrPut(key) { mutableListOf() } += desam
        }
        return order.map { it to (groups[it] ?: emptyList()) }
    }

    // MARK: - Search

    fun filteredWorks(query: String, script: ScriptChoice = ScriptChoice.TAMIL): List<Work> {
        val q = query.trim()
        if (q.isEmpty()) return allWorks
        fun hit(s: String) = s.contains(q, ignoreCase = true)
        return allWorks.mapNotNull { work ->
            // Match the active script's forms, with Tamil as a fallback so
            // results are found whichever script the query is typed in.
            if (hit(work.title(script)) || hit(work.title)) return@mapNotNull work
            val sections = work.sections.filter {
                hit(it.title(script)) || hit(it.title) ||
                    hit(it.content(script)) || hit(it.content)
            }
            if (sections.isEmpty()) null else work.copy(sections = sections)
        }
    }

    /**
     * Builds grounding context for an Ask question: corpus text the model should
     * rely on rather than invent. Assembled from what the app actually has —
     * matching verse text, section/decad essences, and Divya Desam facts — and
     * capped so the proxy call stays small.
     *
     * This is the "R" in RAG. Where the corpus has no data for a question
     * (word meanings, commentary), context comes back thinner or empty and the
     * model answers from its own knowledge under the disclaimer — a deliberate
     * choice, since a padavurai corpus does not yet exist.
     */
    fun askContext(query: String, script: ScriptChoice, maxChars: Int = 6000): String {
        val q = query.trim()
        if (q.isEmpty()) return ""
        val parts = mutableListOf<String>()

        // A bare number: attach that pasuram's text and essence directly.
        q.toIntOrNull()?.let { number ->
            location(number)?.let { (sectionId, _) ->
                section(sectionId)?.let { section ->
                    parts += "Pasuram $number (${section.title(script)}):\n" +
                        section.content(script)
                    essence(number, sectionId)?.let { parts += "Essence: ${it.text(script)}" }
                }
            }
        }

        // Text matches: the first few matching sections, with their essences.
        if (parts.isEmpty()) {
            val works = filteredWorks(q, script).take(3)
            for (work in works) {
                for (section in work.sections.take(2)) {
                    parts += "${work.title(script)} — ${section.title(script)}:\n" +
                        section.content(script).take(1200)
                    decadEssence(section.id)?.let { parts += "Essence: ${it.text(script)}" }
                    if (parts.sumOf { it.length } > maxChars) break
                }
                if (parts.sumOf { it.length } > maxChars) break
            }
        }

        // Divya Desam matches: temple facts are strong grounding for place
        // questions and cheap to include.
        divyaDesams.asSequence()
            .filter {
                it.name(script).contains(q, ignoreCase = true) ||
                    it.place(script).contains(q, ignoreCase = true)
            }
            .take(3)
            .forEach { desam ->
                parts += buildString {
                    append("Divya Desam: ${desam.name(script)}")
                    append(" (${desam.place(script)})")
                    desam.perumal(script)?.let { append("; Perumal: $it") }
                    desam.thaayar(script)?.let { append("; Thaayar: $it") }
                    append("; ${desam.pasurams.size} pasurams")
                }
            }

        return parts.joinToString("\n\n").take(maxChars)
    }

    // MARK: - Thiruppavai / Margazhi daily verse

    /**
     * The traditional Margazhi recitation window is Dec 16 – Jan 14, covering
     * the 30 pasurams of திருப்பாவை, one a day. Returns today's day number
     * (1..30) if we are currently in that window, else null.
     */
    fun margazhiDayToday(calendar: Calendar = Calendar.getInstance()): Int? {
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        if (month == 12 && day >= 16) return day - 15   // Dec 16 -> 1 ... Dec 31 -> 16
        if (month == 1 && day <= 14) return 16 + day    // Jan 1 -> 17 ... Jan 14 -> 30
        return null
    }

    /**
     * Global pasuram number for a given Thiruppavai day (1..30).
     * Thiruppavai's first pasuram is book-wide number 474.
     */
    fun thiruppavaiPasuram(day: Int): Int? =
        if (day in 1..30) 473 + day else null

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        /**
         * Parses every bundled asset. Call from a coroutine; it is deliberately
         * not cheap. A division whose asset is missing is skipped rather than
         * fatal, which is what lets a new division ship by dropping in a file.
         */
        suspend fun load(context: Context): PrabandhamRepository = withContext(Dispatchers.IO) {
            val assets = context.assets

            fun readOrNull(name: String): String? = runCatching {
                assets.open("$name.json").bufferedReader().use { it.readText() }
            }.getOrNull()

            val works = buildMap {
                for (division in Division.all) {
                    val text = readOrNull(division.resource) ?: continue
                    val book = runCatching { json.decodeFromString<PrabandhamBook>(text) }
                        .getOrNull() ?: continue
                    put(division.id, book.works)
                }
            }
            check(works.isNotEmpty()) { "no division content bundled" }

            // Essences are keyed by pasuram number as strings in JSON.
            val essences: Map<Int, Essence> = readOrNull("essences")?.let { text ->
                runCatching { json.decodeFromString<Map<String, Essence>>(text) }.getOrNull()
            }.orEmpty().mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap()

            val decadEssences: Map<String, Essence> = readOrNull("decad_essences")?.let { text ->
                runCatching { json.decodeFromString<Map<String, Essence>>(text) }.getOrNull()
            }.orEmpty()

            val recitations: Map<String, Recitation> = readOrNull("recitations")?.let { text ->
                runCatching { json.decodeFromString<Map<String, Recitation>>(text) }.getOrNull()
            }.orEmpty()

            val youtube: YouTubeCatalogue = readOrNull("youtube")?.let { text ->
                runCatching { json.decodeFromString<YouTubeCatalogue>(text) }.getOrNull()
            } ?: YouTubeCatalogue()

            val amazon: AmazonCatalogue = readOrNull("amazon_music")?.let { text ->
                runCatching { json.decodeFromString<AmazonCatalogue>(text) }.getOrNull()
            } ?: AmazonCatalogue()

            val desams: List<DivyaDesam> = readOrNull("divyadesams")?.let { text ->
                runCatching { json.decodeFromString<List<DivyaDesam>>(text) }.getOrNull()
            }.orEmpty()

            val aazhwars: List<Aazhwar> = readOrNull("azhwars")?.let { text ->
                runCatching { json.decodeFromString<List<Aazhwar>>(text) }.getOrNull()
            }.orEmpty()

            val repo = PrabandhamRepository(
                loadedWorks = works,
                essences = essences,
                decadEssences = decadEssences,
                recitations = recitations,
                youtube = youtube,
                amazon = amazon,
                divyaDesams = desams,
                aazhwars = aazhwars,
            )
            // Constructing the repository already builds the pasuram index,
            // and with it the stanza cache for every section — all of it here,
            // off the main thread, so the first reader scroll does not pay for it.
            repo
        }
    }
}
