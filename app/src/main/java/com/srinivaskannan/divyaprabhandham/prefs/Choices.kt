package com.srinivaskannan.divyaprabhandham.prefs

/**
 * The user-facing choices that shape how the app looks and reads.
 *
 * These are deliberately plain enums with stable `key` strings: the keys are
 * what get written to DataStore and shipped to the widget, so renaming an
 * enum constant must never change what is persisted.
 */

/**
 * Script the app renders in. `TAMIL` is the original and the default. The two
 * English options transliterate all devotional content in the chosen scheme
 * *and* switch the app's chrome to English. The scheme choice only affects how
 * content is romanised; the English UI labels are identical for both.
 */
enum class ScriptChoice(val key: String) {
    TAMIL("tamil"),
    READABLE("readable"),
    SCHOLARLY("scholarly");

    /** Whether the app chrome should be in English. */
    val usesEnglishUi: Boolean get() = this != TAMIL

    /** Picker label, legible whichever script is currently active. */
    val label: String
        get() = when (this) {
            TAMIL -> "தமிழ் · Original"
            READABLE -> "English · Readable"
            SCHOLARLY -> "English · Scholarly"
        }

    val detail: String
        get() = when (this) {
            TAMIL -> "மூல தமிழ் உரை"
            READABLE -> "Reciter-friendly romanisation (vaadinen, thangam)"
            SCHOLARLY -> "ISO-15919 diacritics (vāṭiṉēṉ, taṅkam)"
        }

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key } ?: TAMIL
    }
}

/**
 * Global appearance. `AUTO` preserves the long-standing behaviour where the
 * reader theme (இரவு etc.) drives the app-wide scheme; light/dark force a
 * scheme; HIGH_CONTRAST follows the system scheme but renders maximum-
 * legibility palettes.
 */
enum class AppearanceChoice(val key: String) {
    AUTO("auto"),
    LIGHT("light"),
    DARK("dark"),
    HIGH_CONTRAST("highContrast");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key } ?: AUTO
    }
}

/**
 * Reading typeface for the verse body. Each choice pairs a Tamil face with a
 * Latin face, because no single family renders both Tamil and ISO-15919 Latin
 * diacritics well; the reader picks the right member for the active script.
 *
 * Unlike iOS, none of these ship with the OS in a form we can rely on across
 * every Android vendor, so they are resolved as Google Fonts downloadable
 * fonts with a system fallback. See ui/theme/Type.kt.
 */
enum class FontChoice(
    val key: String,
    val tamilFamily: String,
    val latinFamily: String,
    val preview: String,
) {
    TRADITIONAL("traditional", "Noto Serif Tamil", "Literata", "Noto Serif Tamil · Literata"),
    MODERN("modern", "Hind Madurai", "Source Serif 4", "Hind Madurai · Source Serif 4"),
    CLASSIC("classic", "Noto Serif Tamil", "Noto Serif", "Noto Serif Tamil · Noto Serif"),
    SANS("sans", "Noto Sans Tamil", "Nunito Sans", "Noto Sans Tamil · Nunito Sans");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key } ?: TRADITIONAL
    }
}

/**
 * Reader palette. The two contrast palettes are not offered in the reader's
 * own theme picker — they are selected implicitly by the global high-contrast
 * appearance.
 */
enum class ReaderThemeChoice(val key: String) {
    LIGHT("light"),
    SEPIA("sepia"),
    NIGHT("night"),
    CONTRAST_LIGHT("contrastLight"),
    CONTRAST_DARK("contrastDark");

    /** Whether this palette should render with dark system chrome. */
    val isDark: Boolean get() = this == NIGHT || this == CONTRAST_DARK

    companion object {
        /** Only the user-pickable themes. */
        val pickable = listOf(LIGHT, SEPIA, NIGHT)
        fun from(key: String?) = entries.firstOrNull { it.key == key } ?: LIGHT
    }
}

/**
 * App-wide accent colour, chosen in Settings. The five named seeds match the
 * iOS build exactly. [DYNAMIC] has no iOS counterpart: it hands the whole
 * scheme over to the wallpaper palette on Android 12 and above, which is the
 * platform's own idea of a personal accent and worth offering here.
 */
enum class AccentChoice(val key: String, val rgb: Triple<Float, Float, Float>) {
    VERMILION("vermilion", Triple(0.760f, 0.380f, 0.180f)),
    GOLD("gold", Triple(0.720f, 0.540f, 0.090f)),
    PEACOCK("peacock", Triple(0.110f, 0.400f, 0.600f)),
    LEAF("leaf", Triple(0.180f, 0.490f, 0.250f)),
    MAROON("maroon", Triple(0.560f, 0.230f, 0.280f)),

    /** Seed is only a swatch stand-in; the real colours come from the wallpaper. */
    DYNAMIC("dynamic", Triple(0.400f, 0.400f, 0.440f));

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key } ?: VERMILION
    }
}

/**
 * Which division the widget draws its hourly verse from. `FOLLOW_APP` defers
 * to the choice made in the app's settings.
 */
enum class WidgetAayiram(val key: String) {
    FOLLOW_APP("followApp"),
    ALL("all"),
    D1("d1"), D2("d2"), D3("d3"), D4("d4"), D5("d5");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key } ?: ALL
    }
}

/**
 * The language of the app's chrome — menus, labels, buttons — chosen
 * independently of the content script. Decoupling these lets someone read the
 * verses in Tamil while navigating an English interface, or vice versa.
 *
 * New installs default to English menus; existing users keep whatever their
 * content script implied before this setting existed (migration preserves them).
 */
enum class UiLanguage(val key: String) {
    ENGLISH("english"),
    TAMIL("tamil");

    val isEnglish: Boolean get() = this == ENGLISH

    companion object {
        fun from(key: String?): UiLanguage? = entries.firstOrNull { it.key == key }
    }
}
