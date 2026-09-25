package com.srinivaskannan.divyaprabhandham.ui.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.srinivaskannan.divyaprabhandham.prefs.FontChoice
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice

/**
 * Reading typefaces.
 *
 * PORTING NOTE — this is the one place where Android cannot match iOS exactly.
 * The iOS build names four faces that ship with the OS (Tamil Sangam MN,
 * Kohinoor Tamil, Tamil MN, plus Charter/Georgia/Avenir for the romanisations).
 * Android has no equivalent guaranteed set: what a device has for Tamil depends
 * on the vendor, though almost all ship Noto Sans Tamil and Noto Serif Tamil
 * somewhere in the fallback chain.
 *
 * So the four choices resolve in two steps:
 *
 *  1. If a matching font file has been dropped into `res/font/` (see the names
 *     in [BundledFace]), it is used. Nothing else has to change — no code, no
 *     Gradle. This is the route to a pixel-identical reading experience across
 *     devices, and it is what a release build should do.
 *  2. Otherwise the choice falls back to a generic family, and Android's own
 *     font fallback picks a Tamil face for it. This always works, looks good on
 *     most devices, and means the app is never broken by a missing asset.
 *
 * Downloadable Google Fonts were the other option and were rejected: they need
 * Play Services, they fail on devices that lack it, and a devotional reader
 * should not need a network round trip to render its verses.
 */
object ReadingFonts {

    /**
     * Optional font resources. Drop `.ttf`/`.otf` files with exactly these
     * names into `app/src/main/res/font/` to activate them.
     */
    private enum class BundledFace(val resourceName: String) {
        NOTO_SERIF_TAMIL("noto_serif_tamil"),
        NOTO_SANS_TAMIL("noto_sans_tamil"),
        NOTO_SERIF_TELUGU("noto_serif_telugu"),
        NOTO_SANS_TELUGU("noto_sans_telugu"),
        NOTO_SERIF_MALAYALAM("noto_serif_malayalam"),
        NOTO_SANS_MALAYALAM("noto_sans_malayalam"),
        NOTO_SERIF_DEVANAGARI("noto_serif_devanagari"),
        NOTO_SANS_DEVANAGARI("noto_sans_devanagari"),
        NOTO_SERIF_KANNADA("noto_serif_kannada"),
        NOTO_SANS_KANNADA("noto_sans_kannada"),
        HIND_MADURAI("hind_madurai"),
        LITERATA("literata"),
        SOURCE_SERIF("source_serif"),
        NOTO_SERIF("noto_serif"),
        NUNITO_SANS("nunito_sans"),
    }

    private val cache = HashMap<String, FontFamily>()

    /**
     * The family to set verses in, for the given typeface choice and script.
     * Tamil and romanised text get different faces because no single family
     * renders both Tamil and ISO-15919 diacritics well.
     */
    fun family(context: Context, choice: FontChoice, script: ScriptChoice): FontFamily {
        val key = "${choice.key}|${script.key}"
        return cache.getOrPut(key) {
            val (face, fallback) = when (script) {
                ScriptChoice.TAMIL -> when (choice) {
                    FontChoice.TRADITIONAL -> BundledFace.NOTO_SERIF_TAMIL to FontFamily.Serif
                    FontChoice.CLASSIC -> BundledFace.NOTO_SERIF_TAMIL to FontFamily.Serif
                    FontChoice.MODERN -> BundledFace.HIND_MADURAI to FontFamily.SansSerif
                    FontChoice.SANS -> BundledFace.NOTO_SANS_TAMIL to FontFamily.SansSerif
                }
                // No Indic face is bundled, so every choice falls through to
                // the platform's own family for that script (Noto Serif /
                // Sans Telugu and Malayalam on any current device). Keeping
                // the serif/sans split means the picker still does something
                // visible.
                ScriptChoice.TELUGU -> when (choice) {
                    FontChoice.TRADITIONAL -> BundledFace.NOTO_SERIF_TELUGU to FontFamily.Serif
                    FontChoice.CLASSIC -> BundledFace.NOTO_SERIF_TELUGU to FontFamily.Serif
                    FontChoice.MODERN -> BundledFace.NOTO_SANS_TELUGU to FontFamily.SansSerif
                    FontChoice.SANS -> BundledFace.NOTO_SANS_TELUGU to FontFamily.SansSerif
                }
                ScriptChoice.MALAYALAM -> when (choice) {
                    FontChoice.TRADITIONAL -> BundledFace.NOTO_SERIF_MALAYALAM to FontFamily.Serif
                    FontChoice.CLASSIC -> BundledFace.NOTO_SERIF_MALAYALAM to FontFamily.Serif
                    FontChoice.MODERN -> BundledFace.NOTO_SANS_MALAYALAM to FontFamily.SansSerif
                    FontChoice.SANS -> BundledFace.NOTO_SANS_MALAYALAM to FontFamily.SansSerif
                }
                ScriptChoice.DEVANAGARI -> when (choice) {
                    FontChoice.TRADITIONAL -> BundledFace.NOTO_SERIF_DEVANAGARI to FontFamily.Serif
                    FontChoice.CLASSIC -> BundledFace.NOTO_SERIF_DEVANAGARI to FontFamily.Serif
                    FontChoice.MODERN -> BundledFace.NOTO_SANS_DEVANAGARI to FontFamily.SansSerif
                    FontChoice.SANS -> BundledFace.NOTO_SANS_DEVANAGARI to FontFamily.SansSerif
                }
                ScriptChoice.KANNADA -> when (choice) {
                    FontChoice.TRADITIONAL -> BundledFace.NOTO_SERIF_KANNADA to FontFamily.Serif
                    FontChoice.CLASSIC -> BundledFace.NOTO_SERIF_KANNADA to FontFamily.Serif
                    FontChoice.MODERN -> BundledFace.NOTO_SANS_KANNADA to FontFamily.SansSerif
                    FontChoice.SANS -> BundledFace.NOTO_SANS_KANNADA to FontFamily.SansSerif
                }
                else -> when (choice) {
                    FontChoice.TRADITIONAL -> BundledFace.LITERATA to FontFamily.Serif
                    FontChoice.MODERN -> BundledFace.SOURCE_SERIF to FontFamily.Serif
                    FontChoice.CLASSIC -> BundledFace.NOTO_SERIF to FontFamily.Serif
                    FontChoice.SANS -> BundledFace.NUNITO_SANS to FontFamily.SansSerif
                }
            }
            resolve(context, face) ?: fallback
        }
    }

    /**
     * Extra line height for the verse body, as a multiple of the type size.
     * The Indic scripts set taller than Latin — vowel signs sit well above
     * and below the body, and both Telugu and Malayalam hang conjuncts
     * under the line. Cramping them is the fastest way to make a pasuram
     * unreadable.
     */
    fun lineHeightMultiplier(script: ScriptChoice): Float =
        if (script.isIndicScript) 1.62f else 1.45f

    private fun resolve(context: Context, face: BundledFace): FontFamily? {
        val id = context.resources.getIdentifier(
            face.resourceName, "font", context.packageName,
        )
        if (id == 0) return null
        return runCatching { FontFamily(Font(id)) }.getOrNull()
    }
}

/**
 * The Material 3 type scale for app chrome.
 *
 * Sizes are left at the M3 defaults — they are well tuned and Dynamic Type
 * (Android's font scale) rides on top of them automatically. Only the families
 * are set, and only to keep chrome visually distinct from verse text: the
 * reader body has its own typeface control, and chrome that changed with it
 * would feel like the app was shifting underfoot.
 */
fun appTypography(display: FontFamily = FontFamily.Default): Typography {
    val body = FontFamily.Default
    return Typography(
        displayLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp),
        displayMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp),
        displaySmall = TextStyle(fontFamily = display, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
        headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
        headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
        headlineSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
        titleLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
        titleMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
        titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
    )
}
