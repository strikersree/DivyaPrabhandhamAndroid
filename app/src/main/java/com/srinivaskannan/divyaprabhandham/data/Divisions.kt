package com.srinivaskannan.divyaprabhandham.data

import androidx.compose.runtime.Immutable
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice

/**
 * The four divisions of the நாலாயிர திவ்ய பிரபந்தம், plus the Desika
 * Prabandham.
 *
 * Each division names a bundled asset with the same schema
 * (title/subtitle/works). Dropping a new JSON into `assets/` and adding a row
 * here is enough — the repository loads it, and Home, search and the pasuram
 * index pick it up with no further changes.
 */
@Immutable
data class Division(
    val id: String,
    val title: String,
    val detail: String,
    val titleR: String,
    val titleS: String,
    val detailR: String,
    val detailS: String,
    /** Asset file name, without the .json extension. */
    val resource: String,
    /**
     * Whether this division's verses use the 1–3884 global pasuram numbering.
     * False for post-Aazhwar collections (Desika Prabandham) whose works each
     * restart their own verse numbering — which is why the pasuram index and
     * the essence lookup both have to filter on it.
     */
    val usesGlobalNumbering: Boolean = true,
) {
    fun title(script: ScriptChoice): String = when (script) {
        ScriptChoice.TAMIL -> title
        ScriptChoice.READABLE -> titleR
        ScriptChoice.SCHOLARLY -> titleS
    }

    fun detail(script: ScriptChoice): String = when (script) {
        ScriptChoice.TAMIL -> detail
        ScriptChoice.READABLE -> detailR
        ScriptChoice.SCHOLARLY -> detailS
    }

    companion object {
        val all: List<Division> = listOf(
            Division(
                id = "d1",
                title = "முதலாயிரம்",
                detail = "பெரியாழ்வார் முதல் மதுரகவி வரை · பாசுரம் 1–947",
                titleR = "Mudhalaayiram",
                titleS = "Mutalāyiram",
                detailR = "periyaazhvaar mudhal madhuragavi varai · paasuram 1–947",
                detailS = "periyāḻvār mutal maturakavi varai · pācuram 1–947",
                resource = "prabandham",
            ),
            Division(
                id = "d2",
                title = "இரண்டாம் ஆயிரம்",
                detail = "பெரிய திருமொழி முதலியன · திருமங்கையாழ்வார் · பாசுரம் 948–2081",
                titleR = "Irandaam Aayiram",
                titleS = "Iraṇṭām Āyiram",
                detailR = "periya thirumozhi mudhaliyana · thirumangaiyaazhvaar · paasuram 948–2081",
                detailS = "periya tirumoḻi mutaliyaṉa · tirumaṅkaiyāḻvār · pācuram 948–2081",
                resource = "prabandham_irandam",
            ),
            Division(
                id = "d3",
                title = "இயற்பா",
                detail = "திருவந்தாதிகள், திருவிருத்தம், திருமடல்கள் முதலியன · பாசுரம் 2082–2674",
                titleR = "Iyarpaa",
                titleS = "Iyaṟpā",
                detailR = "thiruvandhaadhigal, thiruviruththam, thirumadalkal mudhaliyana · paasuram 2082–2674",
                detailS = "tiruvantātikaḷ, tiruviruttam, tirumaṭalkaḷ mutaliyaṉa · pācuram 2082–2674",
                resource = "prabandham_iyarpa",
            ),
            Division(
                id = "d4",
                title = "திருவாய்மொழி",
                detail = "திருவாய்மொழி · இராமானுச நூற்றந்தாதி · பாசுரம் 2675–3884",
                titleR = "Thiruvaaymozhi",
                titleS = "Tiruvāymoḻi",
                detailR = "thiruvaaymozhi · iraamaanusa nootrandhaadhi · paasuram 2675–3884",
                detailS = "tiruvāymoḻi · irāmāṉuca nūṟṟantāti · pācuram 2675–3884",
                resource = "prabandham_thiruvaimozhi",
            ),
            Division(
                id = "d5",
                title = "தேசிக பிரபந்தம்",
                detail = "ஸ்வாமி வேதாந்த தேசிகர் அருளிச்செய்த நூல்கள்",
                titleR = "Desika Prabandham",
                titleS = "Tēcika Pirapantam",
                detailR = "svaami vaedhaandha dhaesikar arulichcheydha noolkal",
                detailS = "svāmi vētānta tēcikar aruḷiccceyta nūlkaḷ",
                resource = "desika_prabandham",
                usesGlobalNumbering = false,
            ),
        )

        fun byId(id: String?): Division? = all.firstOrNull { it.id == id }
    }
}
