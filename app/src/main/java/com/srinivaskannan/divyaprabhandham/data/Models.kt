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
) {
    fun text(script: ScriptChoice): String = if (script == ScriptChoice.TAMIL) ta else en
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
    @SerialName("author_r") val authorR: String? = null,
    @SerialName("author_s") val authorS: String? = null,
) {
    fun title(script: ScriptChoice): String = when (script) {
        ScriptChoice.TAMIL -> title
        ScriptChoice.READABLE -> titleR ?: title
        ScriptChoice.SCHOLARLY -> titleS ?: title
    }

    fun author(script: ScriptChoice): String = when (script) {
        ScriptChoice.TAMIL -> author
        ScriptChoice.READABLE -> authorR ?: author
        ScriptChoice.SCHOLARLY -> authorS ?: author
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
    @SerialName("content_r") val contentR: String? = null,
    @SerialName("content_s") val contentS: String? = null,
) {
    fun title(script: ScriptChoice): String = when (script) {
        ScriptChoice.TAMIL -> title
        ScriptChoice.READABLE -> titleR ?: title
        ScriptChoice.SCHOLARLY -> titleS ?: title
    }

    fun content(script: ScriptChoice): String = when (script) {
        ScriptChoice.TAMIL -> content
        ScriptChoice.READABLE -> contentR ?: content
        ScriptChoice.SCHOLARLY -> contentS ?: content
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

    override fun equals(other: Any?): Boolean = other is BookSection && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

/** One block of the parsed section: a verse, an attribution header, or a decad heading. */
@Immutable
data class Stanza(
    val index: Int,
    /** Running pasuram number as printed in the source, if any. */
    val number: Int?,
    val text: String,
    /** True for attribution headers ("… அருளிச் செய்தது"). */
    val isHeading: Boolean = false,
    /** True for the leading decad-heading block, rendered as a description. */
    val isDescription: Boolean = false,
)

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
 */
@Serializable
data class AmazonWork(
    val album: String? = null,
    val track: String? = null,
    val playlist: String? = null,
)

@Serializable
data class AmazonCatalogue(
    val works: Map<String, AmazonWork> = emptyMap(),
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
    val perumal: String? = null,
    val thaayar: String? = null,
    @SerialName("perumal_ta") val perumalTa: String? = null,
    @SerialName("thaayar_ta") val thaayarTa: String? = null,
) {
    fun name(script: ScriptChoice) = if (script == ScriptChoice.TAMIL) name else nameEn
    fun place(script: ScriptChoice) = if (script == ScriptChoice.TAMIL) place else placeEn
    fun region(script: ScriptChoice) = if (script == ScriptChoice.TAMIL) region else regionEn

    /**
     * Deity names follow the app's script, falling back to the Roman spelling
     * where no Tamil form has been recorded.
     */
    fun perumal(script: ScriptChoice): String? =
        if (script == ScriptChoice.TAMIL) perumalTa ?: perumal else perumal

    fun thaayar(script: ScriptChoice): String? =
        if (script == ScriptChoice.TAMIL) thaayarTa ?: thaayar else thaayar
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
