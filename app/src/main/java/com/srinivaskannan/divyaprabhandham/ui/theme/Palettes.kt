package com.srinivaskannan.divyaprabhandham.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.srinivaskannan.divyaprabhandham.prefs.AccentChoice
import com.srinivaskannan.divyaprabhandham.prefs.ReaderThemeChoice

/**
 * The reading surface.
 *
 * Sepia and Night are fixed, deliberately chosen colours — they are a reading
 * mode, not a theme, and letting Material You repaint them would defeat the
 * point. Light is the exception: it defers to the active Material scheme, so it
 * picks up the accent and (where enabled) the wallpaper palette, the same way
 * the iOS build let Light follow the system grouped-background colours.
 *
 * The two contrast palettes are pure black/white and are chosen implicitly by
 * the global High Contrast appearance, never from the reader's own picker.
 */
@Immutable
data class ReaderPalette(
    val background: Color,
    val card: Color,
    val text: Color,
    val secondaryText: Color,
    /**
     * In the contrast palettes, cards are separated from the page by an outline
     * rather than a fill difference. Null everywhere else.
     */
    val cardBorder: Color?,
)

@Composable
@ReadOnlyComposable
fun readerPalette(theme: ReaderThemeChoice): ReaderPalette {
    val scheme = MaterialTheme.colorScheme
    return when (theme) {
        ReaderThemeChoice.LIGHT -> ReaderPalette(
            background = scheme.surface,
            card = scheme.surfaceContainerLow,
            text = scheme.onSurface,
            secondaryText = scheme.onSurfaceVariant,
            cardBorder = null,
        )

        ReaderThemeChoice.SEPIA -> ReaderPalette(
            background = Color(0xFFF5E8CC),
            card = Color(0xFFFCF5E0),
            text = Color(0xFF473017),
            secondaryText = Color(0xFF785C36),
            cardBorder = null,
        )

        ReaderThemeChoice.NIGHT -> ReaderPalette(
            background = Color(0xFF121214),
            card = Color(0xFF26262B),
            text = Color(0xFFEDEDE8),
            secondaryText = Color(0xFFADADA8),
            cardBorder = null,
        )

        ReaderThemeChoice.CONTRAST_LIGHT -> ReaderPalette(
            background = Color.White,
            card = Color.White,
            text = Color.Black,
            secondaryText = Color.Black.copy(alpha = 0.8f),
            cardBorder = Color.Black.copy(alpha = 0.55f),
        )

        ReaderThemeChoice.CONTRAST_DARK -> ReaderPalette(
            background = Color.Black,
            card = Color.Black,
            text = Color.White,
            secondaryText = Color.White.copy(alpha = 0.85f),
            cardBorder = Color.White.copy(alpha = 0.60f),
        )
    }
}

/**
 * Background for a pasuram card that must be recited twice by tradition
 * (any stanza whose text opens with "*" — see [Stanza.repeatsTwice]).
 * Blended against the active [ReaderPalette]'s own card colour rather than
 * a fixed value, so it reads as a light lavender tint in Light/Sepia and a
 * muted violet in Night, instead of a colour that only makes sense in one
 * theme. Deliberately a cool purple — every other accent already in use
 * across this app (gold, terracotta, garnet…) is warm, so this reads as
 * distinct rather than blending into an existing meaning.
 */
fun repeatHighlight(palette: ReaderPalette): Color =
    lerp(palette.card, Color(0xFF9575CD), 0.18f)

/** The accent seed, as a Compose colour. */
val AccentChoice.color: Color
    get() = Color(rgb.first, rgb.second, rgb.third)

/**
 * Nine-colour mesh palettes, one per division, running lighter at the top and
 * deeper at the foot so a card's title reads against its own darkest corner.
 * These are the app's strongest visual signature and are carried over from the
 * iOS build unchanged — a reader who knows முதலாயிரம் as the saffron card
 * should find it saffron here too.
 */
object DivisionPalette {

    fun colors(divisionId: String): List<Color> = when (divisionId) {
        "d1" -> saffron
        "d2" -> ocean
        "d3" -> violet
        "d5" -> slate
        else -> rose
    }

    /** The three-stop reduction used for small tiles, where a full mesh is noise. */
    fun sweep(divisionId: String): List<Color> = colors(divisionId).let {
        listOf(it[0], it[4], it[8])
    }

    private val saffron = listOf(
        Color(0xFFFFD170), Color(0xFFFFB257), Color(0xFFFC994C),
        Color(0xFFFC9E4C), Color(0xFFF57842), Color(0xFFE6573D),
        Color(0xFFDB5738), Color(0xFFC73838), Color(0xFF9E2133),
    )

    private val ocean = listOf(
        Color(0xFF85DBDB), Color(0xFF5CC2D1), Color(0xFF42A3C7),
        Color(0xFF4CB2CC), Color(0xFF338AB8), Color(0xFF246BA3),
        Color(0xFF29709E), Color(0xFF1A528A), Color(0xFF0F3870),
    )

    private val violet = listOf(
        Color(0xFFC79EF5), Color(0xFFAD80EB), Color(0xFF9466DB),
        Color(0xFF9E70E6), Color(0xFF8052CC), Color(0xFF6B3DB2),
        Color(0xFF7042AD), Color(0xFF572E8F), Color(0xFF421F70),
    )

    private val rose = listOf(
        Color(0xFFFFA8B8), Color(0xFFFA859E), Color(0xFFF0668A),
        Color(0xFFF5708F), Color(0xFFE04C75), Color(0xFFCC3866),
        Color(0xFFD13D6B), Color(0xFFB2295C), Color(0xFF8F1A4C),
    )

    private val slate = listOf(
        Color(0xFF9EA8B8), Color(0xFF8A94A6), Color(0xFF788294),
        Color(0xFF808A9C), Color(0xFF6B7587), Color(0xFF596375),
        Color(0xFF616B7D), Color(0xFF4C5769), Color(0xFF3B4557),
    )
}
