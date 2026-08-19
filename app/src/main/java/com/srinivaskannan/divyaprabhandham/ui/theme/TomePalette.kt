package com.srinivaskannan.divyaprabhandham.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Jewel-tone palette for the Sacred Tome cards (the leather-and-gold division
 * covers) — one per Aayiram, plus deep garnet for the Desika tome. Ported
 * verbatim from the iOS build's TomePalette (same RGB values), so the two
 * apps read as the same object rendered on two platforms rather than two
 * separate designs.
 *
 * Kept deliberately separate from [DivisionPalette] (the animated mesh-
 * gradient palette used elsewhere on Home) — the two systems serve different
 * surfaces on iOS too, and merging them would mean either flattening the
 * mesh backgrounds to jewel tones or the tome covers to the mesh's brighter
 * saffron/ocean/violet family, neither of which was asked for.
 */
data class TomeColors(val top: Color, val bottom: Color)

object TomePalette {

    fun forId(id: String): TomeColors = when (id) {
        "d1" -> terracotta
        "d2" -> royalIndigo
        "d3" -> forestGreen
        "d4" -> sandalwood
        "d5" -> deepGarnet // Desika Prabandham
        else -> royalIndigo
    }

    val terracotta = TomeColors(
        top = Color(red = 0.80f, green = 0.42f, blue = 0.28f),
        bottom = Color(red = 0.46f, green = 0.17f, blue = 0.12f),
    )
    val royalIndigo = TomeColors(
        top = Color(red = 0.28f, green = 0.30f, blue = 0.60f),
        bottom = Color(red = 0.10f, green = 0.11f, blue = 0.32f),
    )
    val forestGreen = TomeColors(
        top = Color(red = 0.24f, green = 0.46f, blue = 0.32f),
        bottom = Color(red = 0.07f, green = 0.20f, blue = 0.13f),
    )
    val sandalwood = TomeColors(
        top = Color(red = 0.76f, green = 0.62f, blue = 0.40f),
        bottom = Color(red = 0.44f, green = 0.32f, blue = 0.17f),
    )
    val deepGarnet = TomeColors(
        top = Color(red = 0.56f, green = 0.16f, blue = 0.22f),
        bottom = Color(red = 0.28f, green = 0.05f, blue = 0.10f),
    )

    val gold = Color(red = 0.86f, green = 0.71f, blue = 0.36f)
    val foil = Color(red = 0.96f, green = 0.86f, blue = 0.60f)
    val goldGradientStops = listOf(
        Color(red = 0.98f, green = 0.90f, blue = 0.62f),
        Color(red = 0.80f, green = 0.62f, blue = 0.28f),
        Color(red = 0.98f, green = 0.90f, blue = 0.62f),
    )
    val verdigris = Color(red = 0.28f, green = 0.62f, blue = 0.58f)
}
