package com.srinivaskannan.divyaprabhandham.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the person has asked the system to reduce animation.
 *
 * Android expresses this as the developer-options animation scale rather than
 * as an accessibility flag, so there is no Compose equivalent of SwiftUI's
 * `accessibilityReduceMotion`. A scale of zero means "no animations", and the
 * ambient mesh on the division cards is exactly the sort of decorative motion
 * that should stop when it is.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        scale == 0f
    }
}
