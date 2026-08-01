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
    val nameTa: String,
    val nameEn: String,
) {
    fun name(tamil: Boolean) = if (tamil) nameTa else nameEn
}

object Pilgrimage {
    // Level 5 is 106 (all visitable temples), not 108 — the two celestial
    // abodes cannot be visited. Confirmed with the owner.
    val levels: List<PilgrimageLevel> = listOf(
        PilgrimageLevel(1, 10, "நிலை 1", "Level 1"),
        PilgrimageLevel(2, 18, "நிலை 2", "Level 2"),
        PilgrimageLevel(3, 28, "நிலை 3", "Level 3"),
        PilgrimageLevel(4, 58, "நிலை 4", "Level 4"),
        PilgrimageLevel(5, 106, "நிலை 5", "Level 5"),
    )

    /** The highest level whose threshold the count has reached, or null. */
    fun currentLevel(count: Int): PilgrimageLevel? =
        levels.lastOrNull { count >= it.threshold }

    /** The next level to aim for, or null once all are earned. */
    fun nextLevel(count: Int): PilgrimageLevel? =
        levels.firstOrNull { count < it.threshold }

    fun isUnlocked(level: PilgrimageLevel, count: Int): Boolean = count >= level.threshold
}
