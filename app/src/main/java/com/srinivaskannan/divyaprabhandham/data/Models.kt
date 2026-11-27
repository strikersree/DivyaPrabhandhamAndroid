package com.srinivaskannan.divyaprabhandham.data

import androidx.compose.runtime.Immutable
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The bundled corpus, as it sits in `assets/`. The JSON is byte-identical to
 * the iOS build's Resources, so the OCR pipeline and the essence authoring
 * tools keep feeding both apps from one source.
 *
 * Every model carries Tamil as the authoritative form plus optional `_r`
 * (readable) and `_s` (scholarly) romanisations. Structure — numbering,
 * headings, section keys — is always derived from the Tamil, so a bookmark
 * made while reading in English resolves identically in Tamil.
 */

/**
 * A short, original thematic summary, precomputed and bundled (not generated
 * on device). Shown in the reader's essence sheet in the language matching
 * the reader's script setting.
 */
@Immutable
@Serializable
data class Essence(
    val ta: String,
    val en: String,
    /**
     * The Tamil summary in each of the Indic scripts, generated from [ta]
     * by tools_generate_scripts.py on the iOS side. The romanised scripts
     * keep [en], which is a real English summary rather than a
     * transliteration and is the better read for someone who chose them.
     */
    @SerialName("ta_te") val taTe: String? = null,
    @SerialName("ta_ml") val taMl: String? = null,
    @SerialName("ta_de") val taDe: String? = null,
    @SerialName("ta_kn") val taKn: String? = null,
) {
    fun text(script: ScriptChoice): String = when (script) {
        ScriptChoice.TAMIL -> ta
        ScriptChoice.READABLE, ScriptChoice.SCHOLARLY -> en
        ScriptChoice.TELUGU -> taTe ?: ta
        ScriptChoice.MALAYALAM -> taMl ?: ta
        ScriptChoice.DEVANAGARI -> taDe ?: ta
        ScriptChoice.KANNADA -> taKn ?: ta
    }
}

/** Root of each bundled division file. */
@Serializable
data class PrabandhamBook(
    val title: String,
    val subtitle: String,
    val works: List<Work>,
)

/** A major work (e.g. திருப்பல்லாண்டு, திருப்பாவை). */
@Immutable
@Serializable
data class Work(
    val id: String,
    val title: String,
    val author: String,
    val sections: List<BookSection>,
    @SerialName("title_r") val titleR: String? = null,
    @SerialName("title_s") val titleS: String? = null,
    @SerialName("title_te") val titleTe: String? = null,
    @SerialName("title_ml") val titleMl: String? = null,
    @SerialName("title_de") val titleDe: String? = null,
    @SerialName("title_kn") val titleKn: String? = null,
    @SerialName("author_r") val authorR: String? = null,
    @SerialName("author_s") val authorS: String? = null,
    @SerialName("author_te") val authorTe: String? = null,
    @SerialName("author_ml") val authorMl: String? = null,
    @SerialName("author_de") val authorDe: String? = null,
    @SerialName("author_kn") val authorKn: String? = null,
) {
    fun title(script: ScriptChoice): String = when (script) {
        ScriptChoice.TAMIL -> title
        ScriptChoice.READABLE -> titleR ?: title
        ScriptChoice.SCHOLARLY -> titleS ?: title
        ScriptChoice.TELUGU -> titleTe ?: title
        ScriptChoice.MALAYALAM -> titleMl ?: title
        ScriptChoice.DEVANAGARI -> titleDe ?: title
        ScriptChoice.KANNADA -> titleKn ?: title
    }

    fun author(script: ScriptChoice): String = when (script) {
        ScriptChoice.TAMIL -> author
        ScriptChoice.READABLE -> authorR ?: author
        ScriptChoice.SCHOLARLY -> authorS ?: author
        ScriptChoice.TELUGU -> authorTe ?: author
        ScriptChoice.MALAYALAM -> authorMl ?: author
        ScriptChoice.DEVANAGARI -> authorDe ?: author
        ScriptChoice.KANNADA -> authorKn ?: author
    }

    /** Combined pasuram range across every section in this work. */
    val pasuramRange: IntRange?
        get() {
            val ranges = sections.mapNotNull { it.pasuramRange }
            if (ranges.isEmpty()) return null
            return ranges.minOf { it.first }..ranges.maxOf { it.last }
        }

    val pasuramCount: Int get() = pasuramRange?.let { it.last - it.first + 1 } ?: 0

    // Identity by id only: content strings run to tens of KB and hashing them
    // on every list diff would be wasteful.
    override fun equals(other: Any?): Boolean = other is Work && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

/** A section within a work: a decad (பத்து), a set of தனியன்கள், or a full text. */
@Immutable
@Serializable
data class BookSection(
    val id: String,
    val title: String,
    val content: String,
    @SerialName("title_r") val titleR: String? = null,
    @SerialName("title_s") val titleS: String? = null,
    @SerialName("title_te") val titleTe: String? = null,
    @SerialName("title_ml") val titleMl: String? = null,
    @SerialName("title_de") val titleDe: String? = null,
    @SerialName("title_kn") val titleKn: String? = null,
    @SerialName("content_r") val contentR: String? = null,
    @SerialName("content_s") val contentS: String? = null,
    @SerialName("content_te") val contentTe: String? = null,
    @SerialName("content_ml") val contentMl: String? = null,
    @SerialName("content_de") val contentDe: String? = null,
    @SerialName("content_kn") val contentKn: String? = null,
    /** The invocatory verse that opens a Desika Prabandham work, shown as a
     *  prelude above the first pasuram rather than folded into it. Null for
     *  every other division. */
    val thaniyan: String? = null,
    @SerialName("thaniyan_r") val thaniyanR: String? = null,
    @SerialName("thaniyan_s") val thaniyanS: String? = null,
    @SerialName("thaniyan_te") val thaniyanTe: String? = null,
    @SerialName("thaniyan_ml") val thaniyanMl: String? = null,
    @SerialName("thaniyan_de") val thaniyanDe: String? = null,
    @SerialName("thaniyan_kn") val thaniyanKn: String? = null,
) {
    fun title(script: ScriptChoice): String = when (script) {
        ScriptChoice.TAMIL -> title
        ScriptChoice.READABLE -> titleR ?: title
        ScriptChoice.SCHOLARLY -> titleS ?: title
        ScriptChoice.TELUGU -> titleTe ?: title
        ScriptChoice.MALAYALAM -> titleMl ?: title
        ScriptChoice.DEVANAGARI -> titleDe ?: title
        ScriptChoice.KANNADA -> titleKn ?: title
    }

    /** The paththu this section belongs to, for a work whose sections are
     *  numbered paththu-thirumozhi ("3-4. தழைகளும்" is in the third). */
    val paththuNumber: Int?
        get() = title.substringBefore('-', "").toIntOrNull()

    fun content(script: ScriptChoice): String = when (script) {
        ScriptChoice.TAMIL -> content
        ScriptChoice.READABLE -> contentR ?: content
        ScriptChoice.SCHOLARLY -> contentS ?: content
        ScriptChoice.TELUGU -> contentTe ?: content
        ScriptChoice.MALAYALAM -> contentMl ?: content
        ScriptChoice.DEVANAGARI -> contentDe ?: content
        ScriptChoice.KANNADA -> contentKn ?: content
    }

    fun thaniyan(script: ScriptChoice): String? = when (script) {
        ScriptChoice.TAMIL -> thaniyan
        ScriptChoice.READABLE -> thaniyanR ?: thaniyan
        ScriptChoice.SCHOLARLY -> thaniyanS ?: thaniyan
        ScriptChoice.TELUGU -> thaniyanTe ?: thaniyan
        ScriptChoice.MALAYALAM -> thaniyanMl ?: thaniyan
        ScriptChoice.DEVANAGARI -> thaniyanDe ?: thaniyan
        ScriptChoice.KANNADA -> thaniyanKn ?: thaniyan
    }

    /**
     * Stanzas in the given script. Parsing is not free (a regex pass over the
     * whole section) and the reader asks for this on every recomposition, so
     * results are memoised — see [StanzaParser].
     *
     * [script] has no default on purpose. It used to default to Tamil, which
     * made it easy for a display caller to omit it and quietly render the wrong
     * script; requiring it puts that mistake in front of the compiler.
     */
    fun stanzas(script: ScriptChoice): List<Stanza> =
        StanzaParser.stanzas(this, script)

    /** Lowest/highest pasuram numbers appearing in this section, if any. */
    val pasuramRange: IntRange?
        get() {
            val numbers = stanzas(ScriptChoice.TAMIL).mapNotNull { it.number }
            if (numbers.isEmpty()) return null
            return numbers.min()..numbers.max()
        }

    /**
     * Stable key identifying one stanza, for bookmarks and scroll restoration.
     * Numbered pasurams use their book-wide number; unnumbered stanzas
     * (taniyans) fall back to a local index within the section.
     */
    fun key(stanza: Stanza): String =
        if (stanza.number != null) "$id#${stanza.number}" else "$id#i${stanza.index}"

    /**
     * Like [key], but unique for each sub-unit of a split poem: "…#2674.71"
     * rather than every Periya Thirumadal couplet sharing "…#2674". For
     * addressing one exact stanza -- a list item, a scroll target, a Divya
     * Desam verse -- and never for bookmarks or the reading position, which
     * have always stored [key]. [PrabandhamRepository.stanzaForKey] resolves
     * both shapes.
     */
    fun addressKey(stanza: Stanza): String =
        stanza.displayNumber?.let { "$id#$it" } ?: key(stanza)

    override fun equals(other: Any?): Boolean = other is BookSection && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

/** One block of the parsed section: a verse, an attribution header, or a decad heading. */
@Immutable
data class Stanza(
    val index: Int,
    /** Running pasuram number as printed in the source, if any. */
    val number: Int?,
    /**
     * Display label overriding the numeric badge, for sub-numbered units.
     * The Thirumadals are single continuous poems the tradition divides into
     * numbered sub-units, shown as "2673.1", "2673.2"… [number] stays the
     * parent poem's Int (2673) so essence lookup, sharing and
     * jump-to-pasuram all keep resolving on that stable integer; this
     * carries the precise label. Null for every ordinary pasuram.
     */
    val displayNumber: String? = null,
    val text: String,
    /** True for attribution headers ("… அருளிச் செய்தது"). */
    val isHeading: Boolean = false,
    /** True for the leading decad-heading block, rendered as a description. */
    val isDescription: Boolean = false,
    /** True for the closing homage after an Azhwar's last pasuram
     *  ("பெரியாழ்வார் திருவடிகளே சரணம்"). Not recited; shown as a bold line
     *  outside the pasuram cards. */
    val isSalutation: Boolean = false,
    /** True for a decade's closing அடிவரவு, the index of its pasurams'
     *  opening words. Not a verse and not recited; shown as a quiet line
     *  after the last card. */
    val isAdivaravu: Boolean = false,
    /** For the handful of pasurams with an editorial prelude before the verse
     *  itself (see StanzaParser's PRELUDE_MARKER), the character offset in
     *  [text] where the prelude ends and the verse begins. Null otherwise.
     *  The marker itself is stripped during parsing, so [text] is always
     *  clean — this is the only place the split survives. */
    val preludeEnd: Int? = null,
) {
    /**
     * Traditional recitation convention: a pasuram whose text begins with a
     * leading "*" (the same caesura mark used throughout this corpus,
     * appearing here at the very start rather than mid-verse) is recited
     * twice. Applies corpus-wide, not to any one division — the reader and
     * the collection reader both use this to give such cards a distinct
     * highlight. Checked against [text] itself, which is always clean of
     * the prelude marker by the time it reaches here (see [preludeEnd]).
     */
    val repeatsTwice: Boolean
        get() = text.trimStart().startsWith("*")

    /**
     * What the verse badge should read: the sub-unit label where there is one
     * ("2674.78"), otherwise the plain number. Null for unnumbered blocks.
     * Every display site goes through this so a split poem never shows the
     * same number on all of its sub-units.
     */
    val label: String?
        get() = displayNumber ?: number?.toString()
}

/**
 * A recorded recitation for a work.
 *
 * [album] and [tracks] are Apple Music catalogue identifiers and are only
 * meaningful to the iOS build; they are kept so one `recitations.json` can
 * serve both apps. Android reads [youtubePlaylist] and [youtubeVideo], which
 * are absent from the current file — until they are filled in, the Listen
 * action falls back to a YouTube Music search for the work. See
 * media/RecitationLauncher.kt.
 *
 * Audio is never stored or bundled on either platform. These are identifiers
 * that hand playback to the listener's own music app and their own
 * subscription.
 */
/**
 * YouTube video ids for in-app recitation, loaded from youtube.json.
 * A work's own list wins when present; otherwise the division fallback applies,
 * so a division can be given a single pooled playlist before per-work mapping
 * exists.
 */
@Serializable
data class YouTubeCatalogue(
    val works: Map<String, List<String>> = emptyMap(),
    val workPlaylists: Map<String, String> = emptyMap(),
    val playlists: Map<String, String> = emptyMap(),
    val divisions: Map<String, List<String>> = emptyMap(),
)

/**
 * Amazon Music identifiers for the hand-off trial. Once handed off, playback
 * continues in the background properly (confirmed on device) — the problem
 * the YouTube hand-off couldn't solve. See media/AmazonMusicLauncher.kt.
 *
 * Two shapes, matching how the source catalogue is actually organised:
 *  - Shorter works are one continuous track within a shared compilation album:
 *    [album] + [track] (a specific track ASIN within that album).
 *  - The two largest works (Periyazhwar Thirumozhi, Periya Thirumozhi) are far
 *    too long for a single track and are given as a curated [playlist] instead.
 * A work has exactly one of [playlist] or [album]+[track] set.
 *
 * A work recited one paththu to a track also lists those [tracks] (track
 * ASINs within [album], in paththu order), so each paththu can be started on
 * its own. They sit alongside whatever plays the whole work.
 */
@Serializable
data class AmazonWork(
    val album: String? = null,
    val track: String? = null,
    val playlist: String? = null,
    val tracks: List<String> = emptyList(),
    /** Where each decade (or thaniyan section) starts in the recording,
     *  timed by hand. Empty for a work not timed yet. */
    val decades: List<DecadeMark> = emptyList(),
    /** Where in the track to start, for a request built by [decade]. Never
     *  in the catalogue itself. */
    @kotlinx.serialization.Transient val startMs: Long = 0,
) {
    /** The hand-off for paththu [index] (0-based) alone, or null when this
     *  work has no track for it. */
    fun paththu(index: Int): AmazonWork? {
        val asin = tracks.getOrNull(index)?.takeIf { it.isNotBlank() } ?: return null
        return AmazonWork(album = album, track = asin)
    }

    /** The track ASIN at [index] in this work's recording: a paththu's track,
     *  or the work's one track. */
    fun trackAt(index: Int): String? =
        if (tracks.isNotEmpty()) tracks.getOrNull(index) else track.takeIf { index == 0 }

    /** The timing of section [sectionId], if it has one. */
    fun mark(sectionId: String): DecadeMark? = decades.firstOrNull { it.section == sectionId }

    /** The timed sections on track [index], in playback order. */
    fun marks(onTrack: Int): List<DecadeMark> =
        decades.filter { it.track == onTrack }.sortedBy { it.start }

    /** A request that plays section [sectionId] from its start, or null when
     *  it isn't timed. */
    fun decade(sectionId: String): AmazonWork? {
        val mark = mark(sectionId) ?: return null
        val asin = trackAt(mark.track) ?: return null
        return AmazonWork(album = album, track = asin, startMs = (mark.start * 1000).toLong())
    }
}

/**
 * Where one decade, or a thaniyan section, starts in its work's recording.
 * Amazon Music has one track per paththu (or per work); these marks split it.
 */
@Serializable
data class DecadeMark(
    /** The section id ("w2s2"). */
    val section: String,
    /** Index into the work's tracks (0 for a one-track work). */
    val track: Int,
    /** Seconds from the start of the track. */
    val start: Double,
    /**
     * The pasuram numbers in the order they are recited, for the rare section
     * whose recitation doesn't simply run through its pasurams. Null
     * everywhere else, where a pasuram marked `*` is said twice in a row.
     */
    val order: List<Int>? = null,
)

/**
 * Names of the paththus (பத்து) a long work is divided into, for the browser's
 * sub-headers. One row per paththu, one column per [ScriptChoice] in
 * declaration order.
 */
object Paththu {
    private val names = listOf(
        listOf("முதல் பத்து", "mudhal paththu", "mutal pattu", "ముదల్ పత్తు", "മുദൽ പത്തു", "मुदल् पत्तु", "ಮುದಲ್ ಪತ್ತು"),
        listOf("இரண்டாம் பத்து", "irandaam paththu", "iraṇṭām pattu", "ఇరండాం పత్తు", "ഇരണ്ഡാം പത്തു", "इरण्डाम् पत्तु", "ಇರಂಡಾಂ ಪತ್ತು"),
        listOf("மூன்றாம் பத்து", "moondraam paththu", "mūṉṟām pattu", "మూన్ఱాం పత్తు", "മൂന്റാം പത്തു", "मून्ऱाम् पत्तु", "ಮೂನ್ಱಾಂ ಪತ್ತು"),
        listOf("நான்காம் பத்து", "naangaam paththu", "nāṉkām pattu", "నాన్గాం పత్తు", "നാൻഗാം പത്തു", "नान्गाम् पत्तु", "ನಾನ್ಗಾಂ ಪತ್ತು"),
        listOf("ஐந்தாம் பத்து", "aindhaam paththu", "aintām pattu", "ఐందాం పత్తు", "ഐന്ദാം പത്തു", "ऐन्दाम् पत्तु", "ಐಂದಾಂ ಪತ್ತು"),
        listOf("ஆறாம் பத்து", "aaraam paththu", "āṟām pattu", "ఆఱాం పత్తు", "ആറാം പത്തു", "आऱाम् पत्तु", "ಆಱಾಂ ಪತ್ತು"),
        listOf("ஏழாம் பத்து", "aezhaam paththu", "ēḻām pattu", "ఏఴాం పత్తు", "ഏഴാം പത്തു", "एऴाम् पत्तु", "ಏೞಾಂ ಪತ್ತು"),
        listOf("எட்டாம் பத்து", "ettaam paththu", "eṭṭām pattu", "ఎట్టాం పత్తు", "എട്ടാം പത്തു", "ऎट्टाम् पत्तु", "ಎಟ್ಟಾಂ ಪತ್ತು"),
        listOf("ஒன்பதாம் பத்து", "onbadhaam paththu", "oṉpatām pattu", "ఒన్బదాం పత్తు", "ഒൻബദാം പത്തു", "ऒन्बदाम् पत्तु", "ಒನ್ಬದಾಂ ಪತ್ತು"),
        listOf("பத்தாம் பத்து", "paththaam paththu", "pattām pattu", "పత్తాం పత్తు", "പത്താം പത്തു", "पत्ताम् पत्तु", "ಪತ್ತಾಂ ಪತ್ತು"),
        listOf("பதினோராம் பத்து", "padhinoaraam paththu", "patiṉōrām pattu", "పదినోరాం పత్తు", "പദിനോരാം പത്തു", "पदिनोराम् पत्तु", "ಪದಿನೋರಾಂ ಪತ್ತು"),
    )

    /** The name of paththu [number] (1-based), or null past the eleventh. */
    fun name(number: Int, script: ScriptChoice): String? =
        names.getOrNull(number - 1)?.get(script.ordinal)
}

/**
 * One Amazon Music album as its own player reports it: the exact [name] and
 * [artist] an album search needs, and its [tracks] (track ASINs) in playing
 * order. Only a work on an album listed this way can be started without
 * leaving the app; see media/AmazonRemote.kt.
 */
@Serializable
data class AmazonAlbum(
    val name: String,
    val artist: String,
    val tracks: List<String> = emptyList(),
)

/** One track to start inside its album: what the in-app player is asked for. */
data class AmazonStart(val album: AmazonAlbum, val track: String, val startMs: Long = 0)

/** The work an Amazon Music track recites, and which [paththu] (1-based) when
 *  the work is recorded one paththu to a track. */
data class AmazonTrack(val workId: String, val paththu: Int? = null)

@Serializable
data class AmazonCatalogue(
    val works: Map<String, AmazonWork> = emptyMap(),
    /** Keyed by album ASIN. */
    val albums: Map<String, AmazonAlbum> = emptyMap(),
    /** Track ASIN to work ID, for album tracks that are no work's own recording. */
    val also: Map<String, String> = emptyMap(),
)

/**
 * A resolved recitation target: either a playlist id or a list of video ids
 * (at most one of them set). Empty means nothing is mapped for the work.
 * Precedence lives in PrabandhamRepository.recitationTarget.
 */
data class RecitationTarget(
    val playlistId: String? = null,
    val videoIds: List<String> = emptyList(),
)

@Immutable
@Serializable
data class Recitation(
    val album: String? = null,
    val tracks: List<String> = emptyList(),
    @SerialName("yt_playlist") val youtubePlaylist: String? = null,
    @SerialName("yt_video") val youtubeVideo: String? = null,
)

/** One of the 108 Divya Desams, with the pasurams that sing its praise. */
@Immutable
@Serializable
data class DivyaDesam(
    val id: String,
    val name: String,
    @SerialName("name_en") val nameEn: String,
    val place: String,
    @SerialName("place_en") val placeEn: String,
    val region: String,
    @SerialName("region_en") val regionEn: String,
    /** Global pasuram numbers of its mangalasasanam, in order. */
    val pasurams: List<Int> = emptyList(),
    /**
     * Thirumadal couplets that name this temple ("2674.71"). Kept apart from
     * [pasurams] because an Int can only reach a split poem's *first*
     * couplet: Thiru Aadhanoor's only mangalasasanam is Periya Thirumadal
     * 2674.71, which 2674 would have shown as 2674.1, a different verse.
     */
    val couplets: List<String> = emptyList(),
    val perumal: String? = null,
    val thaayar: String? = null,
    @SerialName("perumal_ta") val perumalTa: String? = null,
    @SerialName("thaayar_ta") val thaayarTa: String? = null,
    /**
     * The three Indic scripts, generated from the Tamil by
     * tools_generate_scripts.py on the iOS side. The two romanised scripts
     * deliberately have none: they use [nameEn] and friends, which are real
     * spellings (Srirangam, Tiruchirappalli) rather than a transliteration
     * of the Tamil, and so are better than anything that could be generated.
     */
    @SerialName("name_te") val nameTe: String? = null,
    @SerialName("name_ml") val nameMl: String? = null,
    @SerialName("name_de") val nameDe: String? = null,
    @SerialName("name_kn") val nameKn: String? = null,
    @SerialName("place_te") val placeTe: String? = null,
    @SerialName("place_ml") val placeMl: String? = null,
    @SerialName("place_de") val placeDe: String? = null,
    @SerialName("place_kn") val placeKn: String? = null,
    @SerialName("region_te") val regionTe: String? = null,
    @SerialName("region_ml") val regionMl: String? = null,
    @SerialName("region_de") val regionDe: String? = null,
    @SerialName("region_kn") val regionKn: String? = null,
    @SerialName("perumal_te") val perumalTe: String? = null,
    @SerialName("perumal_ml") val perumalMl: String? = null,
    @SerialName("perumal_de") val perumalDe: String? = null,
    @SerialName("perumal_kn") val perumalKn: String? = null,
    @SerialName("thaayar_te") val thaayarTe: String? = null,
    @SerialName("thaayar_ml") val thaayarMl: String? = null,
    @SerialName("thaayar_de") val thaayarDe: String? = null,
    @SerialName("thaayar_kn") val thaayarKn: String? = null,
) {
    /**
     * Every verse of its mangalasasanam, as identifiers that
     * [PrabandhamRepository.stanzaKeyForIdentifier] resolves: the whole
     * pasurams, then the couplets.
     */
    val verseIdentifiers: List<String>
        get() = pasurams.map { it.toString() } + couplets

    fun name(script: ScriptChoice) = pick(script, name, nameEn, nameTe, nameMl, nameDe, nameKn)
    fun place(script: ScriptChoice) = pick(script, place, placeEn, placeTe, placeMl, placeDe, placeKn)
    fun region(script: ScriptChoice) = pick(script, region, regionEn, regionTe, regionMl, regionDe, regionKn)

    /**
     * Deity names follow the app's script, falling back to the Roman spelling
     * where no Tamil form has been recorded.
     */
    fun perumal(script: ScriptChoice): String? {
        val roman = perumal ?: return null
        val tamil = perumalTa ?: return roman
        return pick(script, tamil, roman, perumalTe, perumalMl, perumalDe, perumalKn)
    }

    fun thaayar(script: ScriptChoice): String? {
        val roman = thaayar ?: return null
        val tamil = thaayarTa ?: return roman
        return pick(script, tamil, roman, thaayarTe, thaayarMl, thaayarDe, thaayarKn)
    }

    /**
     * Falls back to the Tamil, not the English, when an Indic form is
     * missing: these are Tamil place and deity names, and the Tamil is the
     * closer reading for someone who chose another Indic script.
     */
    private fun pick(
        script: ScriptChoice,
        tamil: String, en: String,
        te: String?, ml: String?, de: String?, kn: String?,
    ): String = when (script) {
        ScriptChoice.TAMIL -> tamil
        ScriptChoice.READABLE, ScriptChoice.SCHOLARLY -> en
        ScriptChoice.TELUGU -> te ?: tamil
        ScriptChoice.MALAYALAM -> ml ?: tamil
        ScriptChoice.DEVANAGARI -> de ?: tamil
        ScriptChoice.KANNADA -> kn ?: tamil
    }
}

/** One of the twelve Aazhwars (plus Ramanuja and Desika in the bundled list). */
@Immutable
@Serializable
data class Aazhwar(
    val id: String,
    val order: Int = 0,
    val name: String,
    @SerialName("name_en") val nameEn: String,
    val epithets: List<String> = emptyList(),
    val sthalam: String? = null,
    @SerialName("sthalam_en") val sthalamEn: String? = null,
    @SerialName("bio_ta") val bioTa: String? = null,
    @SerialName("bio_en") val bioEn: String? = null,
    @SerialName("total_verses") val totalVerses: Int? = null,
    val works: List<String> = emptyList(),
) {
    fun name(script: ScriptChoice) = if (script == ScriptChoice.TAMIL) name else nameEn
    fun sthalam(script: ScriptChoice) =
        if (script == ScriptChoice.TAMIL) sthalam else sthalamEn
    fun bio(script: ScriptChoice) = if (script == ScriptChoice.TAMIL) bioTa else bioEn
}
