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
    val titleTe: String,
    val detailTe: String,
    val titleMl: String,
    val detailMl: String,
    val titleDe: String,
    val detailDe: String,
    /** Asset file name, without the .json extension. */
    val resource: String,
    /**
     * Whether this division's verses use the 1–3884 global pasuram numbering.
     * False for post-Aazhwar collections (Desika Prabandham) whose works each
     * restart their own verse numbering — which is why the pasuram index and
     * the essence lookup both have to filter on it.
     */
    val usesGlobalNumbering: Boolean = true,
    /**
     * Whether this division appears as a leather cover in the main
     * division shelf (Home's carousel) and the widget's random-verse pool.
     * False for Podhu Thaniyangal (d6) — matching the iOS and PWA builds'
     * own onShelf: false for it — since it's five short invocatory verses,
     * not a work someone browses or expects as "today's pasuram" on a
     * widget; it only makes sense reached through its pinned collection.
     * Content loading and stanza-key resolution are untouched by this — it
     * only gates the two display-facing lists ([PrabandhamRepository]'s
     * `divisions` property and the widget's verse pool), not
     * [Division.all] itself, which the collection still needs intact.
     */
    val onShelf: Boolean = true,
) {
    fun title(script: ScriptChoice): String = when (script) {
        ScriptChoice.TAMIL -> title
        ScriptChoice.READABLE -> titleR
        ScriptChoice.SCHOLARLY -> titleS
        ScriptChoice.TELUGU -> titleTe
        ScriptChoice.MALAYALAM -> titleMl
        ScriptChoice.DEVANAGARI -> titleDe
    }

    fun detail(script: ScriptChoice): String = when (script) {
        ScriptChoice.TAMIL -> detail
        ScriptChoice.READABLE -> detailR
        ScriptChoice.SCHOLARLY -> detailS
        ScriptChoice.TELUGU -> detailTe
        ScriptChoice.MALAYALAM -> detailMl
        ScriptChoice.DEVANAGARI -> detailDe
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
                titleTe = "ముదలాయిరం",
                detailTe = "పెరియాఴ్వార్ ముదల్ మదురగవి వరై · పాశురం 1–947",
                titleMl = "മുദലായിരം",
                detailMl = "പെരിയാഴ്വാർ മുദൽ മദുരഗവി വരൈ · പാശുരം 1–947",
                titleDe = "मुदलायिरम्",
                detailDe = "पॆरियाऴ्वार् मुदल् मदुरगवि वरै · पाशुरम् 1–947",
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
                titleTe = "ఇరండాం ఆయిరం",
                detailTe = "పెరియ తిరుమొఴి ముదలియన · తిరుమంగైయాఴ్వార్ · పాశురం 948–2081",
                titleMl = "ഇരണ്ഡാം ആയിരം",
                detailMl = "പെരിയ തിരുമൊഴി മുദലിയന · തിരുമങ്ഗൈയാഴ്വാർ · പാശുരം 948–2081",
                titleDe = "इरण्डाम् आयिरम्",
                detailDe = "पॆरिय तिरुमॊऴि मुदलियन · तिरुमङ्गैयाऴ्वार् · पाशुरम् 948–2081",
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
                titleTe = "ఇయఱ్పా",
                detailTe = "తిరువందాదిగళ్, తిరువిరుత్తం, తిరుమడల్గళ్ ముదలియన · పాశురం 2082–2674",
                titleMl = "ഇയറ്പാ",
                detailMl = "തിരുവന്ദാദിഗൾ, തിരുവിരുത്തം, തിരുമഡൽഗൾ മുദലിയന · പാശുരം 2082–2674",
                titleDe = "इयऱ्पा",
                detailDe = "तिरुवन्दादिगळ्, तिरुविरुत्तम्, तिरुमडल्गळ् मुदलियन · पाशुरम् 2082–2674",
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
                titleTe = "తిరువాయ్మొఴి",
                detailTe = "తిరువాయ్మొఴి · ఇరామానుశ నూట్రందాది · పాశురం 2675–3884",
                titleMl = "തിരുവായ്മൊഴി",
                detailMl = "തിരുവായ്മൊഴി · ഇരാമാനുശ നൂറ്റന്ദാദി · പാശുരം 2675–3884",
                titleDe = "तिरुवाय्मॊऴि",
                detailDe = "तिरुवाय्मॊऴि · इरामानुश नूट्रन्दादि · पाशुरम् 2675–3884",
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
                titleTe = "తేశిగ పిరబందం",
                detailTe = "స్వామి వేదాంద తేశిగర్ అరుళిచ్చెయ్ద నూల్గళ్",
                titleMl = "തേശിഗ പിരബന്ദം",
                detailMl = "സ്വാമി വേദാന്ദ തേശിഗർ അരുളിച്ചെയ്ദ നൂൽഗൾ",
                titleDe = "तेशिग पिरबन्दम्",
                detailDe = "स्वामि वेदान्द तेशिगर् अरुळिच्चॆय्द नूल्गळ्",
                resource = "desika_prabandham",
                usesGlobalNumbering = false,
            ),
            Division(
                id = "d6",
                title = "பொது தனியன்கள்",
                detail = "பாசுரங்கள் ஓதுமுன் சேவிக்கும் ஆசார்ய வணக்கத் தனியன்கள்",
                titleR = "Podhu Thaniyangal",
                titleS = "Potu Taṉiyaṉkaḷ",
                detailR = "paasurangal oadhumun chaevikkum aasaarya vanakkath thaniyangal",
                detailS = "pācuraṅkaḷ ōtumuṉ cēvikkum ācārya vaṇakkat taṉiyaṉkaḷ",
                titleTe = "పొదు తనియన్గళ్",
                detailTe = "పాశురంగళ్ ఓదుమున్ శేవిక్కుం ఆశార్య వణక్కత్ తనియన్గళ్",
                titleMl = "പൊദു തനിയൻഗൾ",
                detailMl = "പാശുരങ്ഗൾ ഓദുമുൻ ശേവിക്കും ആശാർയ വണക്കത് തനിയൻഗൾ",
                titleDe = "पॊदु तनियन्गळ्",
                detailDe = "पाशुरङ्गळ् ओदुमुन् शेविक्कुम् आशार्य वणक्कत् तनियन्गळ्",
                resource = "podhu_thaniyangal",
                usesGlobalNumbering = false,
                onShelf = false,
            ),
        )

        fun byId(id: String?): Division? = all.firstOrNull { it.id == id }
    }
}
