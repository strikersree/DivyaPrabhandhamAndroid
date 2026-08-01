package com.srinivaskannan.divyaprabhandham.data

/**
 * The pilgrimage achievement levels, driven by how many physical Divya Desams
 * the user has marked visited. Out of 106 visitable temples (the two Eternal
 * Abodes are not places one visits, so the top level is 106, with 108
 * acknowledged as the symbolic whole in the final card's text).
 *
 * Level names are placeholders until the owner supplies the final ones; the
 * thresholds are fixed: 10 / 18 / 28 / 58 / 106.
 */
data class PilgrimageLevel(
    val index: Int,
    val threshold: Int,
    val title: String,
    val subtitleTa: String,
    val subtitleEn: String,
    val descriptionTa: String,
    val descriptionEn: String,
) {
    fun subtitle(tamil: Boolean) = if (tamil) subtitleTa else subtitleEn
    fun description(tamil: Boolean) = if (tamil) descriptionTa else descriptionEn
}

object Pilgrimage {
    // Level 5 is 106 (all visitable temples), not 108 — the two celestial
    // abodes cannot be visited. The final tier's meaning still honours the full
    // 108 / the twelve Azhwars. Names and meanings supplied by the owner.
    val levels: List<PilgrimageLevel> = listOf(
        PilgrimageLevel(
            index = 1, threshold = 10,
            title = "Anbukku Adiyar",
            subtitleTa = "அன்பின் தொண்டர்",
            subtitleEn = "Devotee of Love",
            descriptionTa = "அன்பின் விளக்கை ஏற்றிய முதல் ஆழ்வார்கள் (பொய்கை, பூதத், பேய்) அருளியது.",
            descriptionEn = "Inspired by the first Azhwars (Poygai, Bhoothath, Pey) who light the lamp of love.",
        ),
        PilgrimageLevel(
            index = 2, threshold = 18,
            title = "Pasuram Padhan",
            subtitleTa = "பாசுரம் பயில்பவர்",
            subtitleEn = "Learner of Divine Songs",
            descriptionTa = "ஆழ்வார்கள் பாடிய அடிப்படைக் கோயில் தொகுதிகளைத் திறத்தல்.",
            descriptionEn = "Unlocking the foundational temple clusters sung by the saints.",
        ),
        PilgrimageLevel(
            index = 3, threshold = 28,
            title = "Kshetra Sevakar",
            subtitleTa = "க்ஷேத்திர சேவகர்",
            subtitleEn = "Servant of Sacred Lands",
            descriptionTa = "தெய்வீகத் தலங்களை முனைந்து தரிசிக்கும் அர்ப்பணிப்புள்ள யாத்ரீகராக அங்கீகரிக்கப்படுதல்.",
            descriptionEn = "Recognized as a dedicated pilgrim actively visiting divine shrines.",
        ),
        PilgrimageLevel(
            index = 4, threshold = 58,
            title = "Divya Desam Sadhak",
            subtitleTa = "திவ்ய தேச சாதகர்",
            subtitleEn = "Practitioner of Shrines",
            descriptionTa = "பாதி வழியைக் கடந்து — ஆழ்ந்த ஆன்மீக நெறியைக் காட்டுதல்.",
            descriptionEn = "Past the halfway mark — showing deep spiritual discipline.",
        ),
        PilgrimageLevel(
            index = 5, threshold = 106,
            title = "Azhwar Thondar",
            subtitleTa = "ஆழ்வார் தொண்டர்",
            subtitleEn = "Eternal Servant of Azhwars",
            descriptionTa = "பன்னிரு ஆழ்வார்களின் அடிச்சுவடுகளில் முழு யாத்திரையையும் நிறைவு செய்தல்.",
            descriptionEn = "Completing the entire Yatra in the footsteps of the 12 Azhwars.",
        ),
    )

    /** The highest level whose threshold the count has reached, or null. */
    fun currentLevel(count: Int): PilgrimageLevel? =
        levels.lastOrNull { count >= it.threshold }

    /** The next level to aim for, or null once all are earned. */
    fun nextLevel(count: Int): PilgrimageLevel? =
        levels.firstOrNull { count < it.threshold }

    fun isUnlocked(level: PilgrimageLevel, count: Int): Boolean = count >= level.threshold
}
