package com.srinivaskannan.divyaprabhandham.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * Builds full Material 3 colour schemes from a single seed colour.
 *
 * The app offers five accents and wants a real M3 scheme for each — every
 * container, every "on" role, every surface tier. Hand-authoring 5 x 2 x ~35
 * colours would be unmaintainable and would drift out of tune, so the tonal
 * palettes are generated the way Material generates them: fix a hue and a
 * chroma, then walk the tone (perceptual lightness) axis and read off the tones
 * the spec assigns to each role.
 *
 * Material's own implementation works in CAM16/HCT. This works in CIELCh,
 * which is a close and much smaller approximation: the hue and tone axes agree,
 * and chroma is gamut-mapped by simply reducing it until the colour is
 * representable in sRGB. In practice the results are indistinguishable at the
 * tones M3 actually uses, and it keeps the app free of another dependency.
 *
 * The role -> tone assignments below are the M3 "tonal spot" scheme, which is
 * the same one the platform uses for wallpaper-derived colour.
 */
internal object TonalPalettes {

    /** A single tonal palette: one hue and chroma, sampled by tone. */
    class Tonal(private val hueDeg: Double, private val chroma: Double) {
        private val cache = HashMap<Int, Color>()

        /** The colour at tone [t] (0 = black, 100 = white). */
        fun tone(t: Int): Color = cache.getOrPut(t) {
            lchToColor(t.toDouble(), chroma, hueDeg)
        }
    }

    private data class Seed(val hue: Double, val chroma: Double)

    private val schemeCache = HashMap<String, ColorScheme>()

    /**
     * A complete light or dark scheme seeded from [seedColor].
     * Results are memoised: this runs on every theme read and the maths is
     * far from free.
     */
    fun scheme(seedColor: Color, dark: Boolean): ColorScheme {
        val key = "${seedColor.value}|$dark"
        return schemeCache.getOrPut(key) { build(seedColor, dark) }
    }

    private fun build(seedColor: Color, dark: Boolean): ColorScheme {
        val seed = seedOf(seedColor)

        // Tonal spot: a saturated primary, a muted secondary, a tertiary
        // rotated round the wheel, and two near-neutral palettes that keep a
        // trace of the seed hue so surfaces feel of a piece with the accent.
        val primary = Tonal(seed.hue, maxOf(seed.chroma, 36.0))
        val secondary = Tonal(seed.hue, 16.0)
        val tertiary = Tonal(seed.hue + 60.0, 24.0)
        val neutral = Tonal(seed.hue, 6.0)
        val neutralVariant = Tonal(seed.hue, 8.0)
        val error = Tonal(25.0, 84.0)

        return if (dark) {
            darkColorScheme(
                primary = primary.tone(80),
                onPrimary = primary.tone(20),
                primaryContainer = primary.tone(30),
                onPrimaryContainer = primary.tone(90),
                inversePrimary = primary.tone(40),
                secondary = secondary.tone(80),
                onSecondary = secondary.tone(20),
                secondaryContainer = secondary.tone(30),
                onSecondaryContainer = secondary.tone(90),
                tertiary = tertiary.tone(80),
                onTertiary = tertiary.tone(20),
                tertiaryContainer = tertiary.tone(30),
                onTertiaryContainer = tertiary.tone(90),
                error = error.tone(80),
                onError = error.tone(20),
                errorContainer = error.tone(30),
                onErrorContainer = error.tone(90),
                background = neutral.tone(6),
                onBackground = neutral.tone(90),
                surface = neutral.tone(6),
                onSurface = neutral.tone(90),
                surfaceVariant = neutralVariant.tone(30),
                onSurfaceVariant = neutralVariant.tone(80),
                surfaceTint = primary.tone(80),
                inverseSurface = neutral.tone(90),
                inverseOnSurface = neutral.tone(20),
                outline = neutralVariant.tone(60),
                outlineVariant = neutralVariant.tone(30),
                scrim = neutral.tone(0),
                surfaceBright = neutral.tone(24),
                surfaceDim = neutral.tone(6),
                surfaceContainerLowest = neutral.tone(4),
                surfaceContainerLow = neutral.tone(10),
                surfaceContainer = neutral.tone(12),
                surfaceContainerHigh = neutral.tone(17),
                surfaceContainerHighest = neutral.tone(22),
            )
        } else {
            lightColorScheme(
                primary = primary.tone(40),
                onPrimary = primary.tone(100),
                primaryContainer = primary.tone(90),
                onPrimaryContainer = primary.tone(10),
                inversePrimary = primary.tone(80),
                secondary = secondary.tone(40),
                onSecondary = secondary.tone(100),
                secondaryContainer = secondary.tone(90),
                onSecondaryContainer = secondary.tone(10),
                tertiary = tertiary.tone(40),
                onTertiary = tertiary.tone(100),
                tertiaryContainer = tertiary.tone(90),
                onTertiaryContainer = tertiary.tone(10),
                error = error.tone(40),
                onError = error.tone(100),
                errorContainer = error.tone(90),
                onErrorContainer = error.tone(10),
                background = neutral.tone(98),
                onBackground = neutral.tone(10),
                surface = neutral.tone(98),
                onSurface = neutral.tone(10),
                surfaceVariant = neutralVariant.tone(90),
                onSurfaceVariant = neutralVariant.tone(30),
                surfaceTint = primary.tone(40),
                inverseSurface = neutral.tone(20),
                inverseOnSurface = neutral.tone(95),
                outline = neutralVariant.tone(50),
                outlineVariant = neutralVariant.tone(80),
                scrim = neutral.tone(0),
                surfaceBright = neutral.tone(98),
                surfaceDim = neutral.tone(87),
                surfaceContainerLowest = neutral.tone(100),
                surfaceContainerLow = neutral.tone(96),
                surfaceContainer = neutral.tone(94),
                surfaceContainerHigh = neutral.tone(92),
                surfaceContainerHighest = neutral.tone(90),
            )
        }
    }

    // MARK: - Colour space plumbing

    private const val EPSILON = 216.0 / 24389.0
    private const val KAPPA = 24389.0 / 27.0
    private const val WHITE_X = 0.95047
    private const val WHITE_Y = 1.00000
    private const val WHITE_Z = 1.08883

    private fun seedOf(color: Color): Seed {
        // Only hue and chroma are taken from the seed; tone comes from the
        // role table, which is what keeps every accent equally legible.
        val (_, a, b) = rgbToLab(color.red.toDouble(), color.green.toDouble(), color.blue.toDouble())
        val chroma = hypot(a, b)
        var hue = Math.toDegrees(atan2(b, a))
        if (hue < 0) hue += 360.0
        return Seed(hue, chroma)
    }

    private fun srgbToLinear(c: Double): Double =
        if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun linearToSrgb(c: Double): Double =
        if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1.0 / 2.4) - 0.055

    private fun rgbToLab(r: Double, g: Double, b: Double): Triple<Double, Double, Double> {
        val lr = srgbToLinear(r)
        val lg = srgbToLinear(g)
        val lb = srgbToLinear(b)
        val x = (0.4124564 * lr + 0.3575761 * lg + 0.1804375 * lb) / WHITE_X
        val y = (0.2126729 * lr + 0.7151522 * lg + 0.0721750 * lb) / WHITE_Y
        val z = (0.0193339 * lr + 0.1191920 * lg + 0.9503041 * lb) / WHITE_Z
        fun f(t: Double) = if (t > EPSILON) cbrt(t) else (KAPPA * t + 16.0) / 116.0
        val fx = f(x); val fy = f(y); val fz = f(z)
        return Triple(116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz))
    }

    /**
     * Converts an LCh colour to sRGB, reducing chroma until the result fits in
     * the gamut. Clipping channels instead would shift the hue, which shows up
     * as muddy containers at the extremes of the tone axis.
     */
    private fun lchToColor(lStar: Double, chroma: Double, hueDeg: Double): Color {
        var low = 0.0
        var high = chroma
        var best = lchToRgb(lStar, 0.0, 0.0)
        val hueRad = Math.toRadians(hueDeg)
        repeat(12) {
            val mid = (low + high) / 2.0
            val candidate = lchToRgb(lStar, mid, hueRad)
            if (candidate.inGamut) {
                best = candidate
                low = mid
            } else {
                high = mid
            }
        }
        return Color(
            red = best.r.coerceIn(0.0, 1.0).toFloat(),
            green = best.g.coerceIn(0.0, 1.0).toFloat(),
            blue = best.b.coerceIn(0.0, 1.0).toFloat(),
        )
    }

    private class Rgb(val r: Double, val g: Double, val b: Double) {
        val inGamut: Boolean
            get() = r >= -0.0001 && r <= 1.0001 &&
                g >= -0.0001 && g <= 1.0001 &&
                b >= -0.0001 && b <= 1.0001
    }

    private fun lchToRgb(lStar: Double, chroma: Double, hueRad: Double): Rgb {
        val a = chroma * cos(hueRad)
        val bb = chroma * sin(hueRad)
        val fy = (lStar + 16.0) / 116.0
        val fx = fy + a / 500.0
        val fz = fy - bb / 200.0
        fun inv(f: Double): Double {
            val cube = f * f * f
            return if (cube > EPSILON) cube else (116.0 * f - 16.0) / KAPPA
        }
        val x = inv(fx) * WHITE_X
        val y = if (lStar > KAPPA * EPSILON) fy * fy * fy * WHITE_Y else lStar / KAPPA * WHITE_Y
        val z = inv(fz) * WHITE_Z
        val lr = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z
        val lg = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z
        val lb = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z
        return Rgb(linearToSrgb(lr), linearToSrgb(lg), linearToSrgb(lb))
    }

}
