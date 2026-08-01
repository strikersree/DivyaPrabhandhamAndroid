package com.srinivaskannan.divyaprabhandham.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.PrabandhamRepository
import com.srinivaskannan.divyaprabhandham.prefs.AccentChoice
import com.srinivaskannan.divyaprabhandham.prefs.AppState
import com.srinivaskannan.divyaprabhandham.prefs.ReaderThemeChoice

/**
 * Ambient access to the two objects essentially every screen needs. They are
 * created once, live for the process, and never change identity — so
 * `staticCompositionLocalOf` is right: reads do not need to be tracked.
 */
val LocalAppState = staticCompositionLocalOf<AppState> {
    error("AppState not provided")
}
val LocalRepository = staticCompositionLocalOf<PrabandhamRepository> {
    error("PrabandhamRepository not provided")
}
val LocalAskHistory = staticCompositionLocalOf<com.srinivaskannan.divyaprabhandham.ask.AskHistoryStore> {
    error("AskHistoryStore not provided")
}

/**
 * Material 3 shapes.
 *
 * Rounder than the M3 defaults, following the expressive direction and — more
 * to the point — matching the iOS build's 20–24pt cards, so a reader moving
 * between the two apps sees the same object. Verse cards use `large`.
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Pure black-on-white / white-on-black schemes for the High Contrast
 * appearance. Generated tonal palettes bottom out around tone 10/90, which is
 * the right call for normal use and the wrong one here: this appearance exists
 * for people who need the maximum, so the scheme is authored rather than
 * derived.
 */
private fun highContrastScheme(dark: Boolean): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            primaryContainer = Color.Black,
            onPrimaryContainer = Color.White,
            secondary = Color.White,
            onSecondary = Color.Black,
            secondaryContainer = Color.Black,
            onSecondaryContainer = Color.White,
            tertiary = Color.White,
            onTertiary = Color.Black,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = Color.Black,
            onSurfaceVariant = Color.White,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color.Black,
            surfaceContainerHighest = Color.Black,
            outline = Color.White,
            outlineVariant = Color.White.copy(alpha = 0.6f),
            inverseSurface = Color.White,
            inverseOnSurface = Color.Black,
            error = Color(0xFFFF8A80),
            onError = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = Color.Black,
            onPrimary = Color.White,
            primaryContainer = Color.White,
            onPrimaryContainer = Color.Black,
            secondary = Color.Black,
            onSecondary = Color.White,
            secondaryContainer = Color.White,
            onSecondaryContainer = Color.Black,
            tertiary = Color.Black,
            onTertiary = Color.White,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color.White,
            onSurfaceVariant = Color.Black,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color.White,
            surfaceContainer = Color.White,
            surfaceContainerHigh = Color.White,
            surfaceContainerHighest = Color.White,
            outline = Color.Black,
            outlineVariant = Color.Black.copy(alpha = 0.6f),
            inverseSurface = Color.Black,
            inverseOnSurface = Color.White,
            error = Color(0xFF8C0009),
            onError = Color.White,
        )
    }

/** Whether wallpaper-derived colour is available on this device. */
val supportsDynamicColor: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The app theme.
 *
 * Material 3, with the expressive direction expressed through the parts of the
 * system that are actually public: generated tonal palettes, wallpaper colour,
 * the rounder shape scale in [AppShapes], and adaptive navigation.
 *
 * NOT `MaterialExpressiveTheme`. In material3 1.4.0 that function, the
 * `MotionScheme` interface and the `ExperimentalMaterial3ExpressiveApi` marker
 * are all declared `internal`, so they cannot be called from outside the
 * library — the code has landed but the API has not been opened up. Opting in
 * does not help; internal is not the same as experimental.
 *
 * To adopt it once a release makes it public, this is the only place that
 * changes: swap [MaterialTheme] for `MaterialExpressiveTheme` and pass
 * `motionScheme = MotionScheme.expressive()`. Nothing else in the app touches
 * those APIs.
 */
@Composable
fun DivyaPrabhandhamTheme(
    appState: AppState,
    repository: PrabandhamRepository,
    askHistory: com.srinivaskannan.divyaprabhandham.ask.AskHistoryStore,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = appState.forcedDarkMode ?: systemDark
    val context = LocalContext.current

    val colorScheme = when {
        appState.isHighContrast ->
            highContrastScheme(dark)

        appState.accentChoice == AccentChoice.DYNAMIC && supportsDynamicColor ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        else -> TonalPalettes.scheme(appState.accentChoice.color, dark)
    }

    CompositionLocalProvider(
        LocalAppState provides appState,
        LocalRepository provides repository,
        LocalAskHistory provides askHistory,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appTypography(),
            shapes = AppShapes,
            content = content,
        )
    }
}

/**
 * The reader palette in force right now.
 *
 * While the global High Contrast appearance is active the reader palette is
 * fixed to match the system light/dark scheme; the person's own theme choice is
 * kept untouched in storage and comes back the moment contrast is turned off.
 */
@Composable
fun currentReaderTheme(appState: AppState): ReaderThemeChoice {
    val systemDark = isSystemInDarkTheme()
    if (!appState.isHighContrast) return appState.theme
    val dark = appState.forcedDarkMode ?: systemDark
    return if (dark) ReaderThemeChoice.CONTRAST_DARK else ReaderThemeChoice.CONTRAST_LIGHT
}
