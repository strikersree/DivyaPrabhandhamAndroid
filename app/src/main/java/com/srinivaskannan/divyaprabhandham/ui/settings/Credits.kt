package com.srinivaskannan.divyaprabhandham.ui.settings

import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice

/**
 * A credited person or source, bilingual. Held in code rather than the
 * localisation table so the lists can be added to easily over time — mirrors
 * the iOS app's Credits.swift so the two About pages stay identical.
 */
data class Credit(
    val ta: String,
    val en: String,
    val roleTa: String,
    val roleEn: String,
) {
    fun name(s: ScriptChoice): String = if (s == ScriptChoice.TAMIL) ta else en
    fun role(s: ScriptChoice): String = if (s == ScriptChoice.TAMIL) roleTa else roleEn
}

/** The people and sources this app stands on. */
object Credits {
    val reciters: List<Credit> = listOf(
        Credit("கே. மாலோல கண்ணன்", "K. Malola Kannan", "பாராயணம்", "Recitation"),
        Credit("என். எஸ். ரங்கநாதன்", "N. S. Ranganathan", "பாராயணம்", "Recitation"),
    )

    val sources: List<Credit> = listOf(
        Credit(
            "பி. ஆர். ஜி. ஐயங்கார்", "B. R. G. Iyengar",
            "108 திவ்ய தேசங்களின் மங்களாசாசனப் பாசுரப் பட்டியல்",
            "Compiled the 108 Divya Desam mangalasasanam index",
        ),
        Credit(
            "திரு. டி. ஆர். எஸ். ஐயங்கார்", "TRS Iyengar",
            "ஸ்ரீவைஷ்ணவ மரபுத் தளம்", "Srivaishnavam Practices, for publishing that index",
        ),
        Credit(
            "சுந்தர் கிடாம்பி", "Sunder Kidambi",
            "ஸ்ரீ தேசிகப் பிரபந்த பதிப்பு", "Sri Desika Prabandham edition",
        ),
        Credit(
            "மதுரைத் திட்டம்", "Project Madurai",
            "தமிழ் மின்னூல் பாடங்கள்", "Tamil e-text of the Prabandham",
        ),
    )

    val gratitude: List<Credit> = listOf(
        Credit("மாங்குடி பார்த்தசாரதி ரங்கராஜன்", "Maangudi Parthasarathy Rangarajan",
            "மாமா · பாராயணம்", "Uncle · Recitation"),
        Credit("கண்ணன் தேசிகர்", "Kannan Desikar", "தந்தை", "Father"),
        Credit("மைதிலி கண்ணன்", "Mythili Kannan", "தாய்", "Mother"),
        Credit("வாசுதேவன் சந்தானம்", "Vasudevan Santhanam", "மாமனார்", "Father-in-law"),
        Credit("சுபா வாசுதேவன்", "Subha Vasudevan", "மாமியார்", "Mother-in-law"),
        Credit("அபர்ணா வாசுதேவன்", "Aparna Vasudevan", "மனைவி", "Wife"),
    )
}
