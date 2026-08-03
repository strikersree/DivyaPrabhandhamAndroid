package com.srinivaskannan.divyaprabhandham.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Switches the launcher icon by toggling activity-aliases. Each icon variant is
 * a launcher `activity-alias` in the manifest; exactly one is enabled at a time.
 * Enabling one and disabling the others is what changes the home-screen icon.
 *
 * Honest caveats (OS-level, not ours to remove): the change is visible — the
 * launcher briefly re-adds the app and the icon updates a moment later — and on
 * some OEM launchers it can lag or need a relaunch. We use DONT_KILL_APP so the
 * running app is not torn down mid-switch.
 */
enum class AppIcon(val alias: String) {
    VADAKALAI("com.srinivaskannan.divyaprabhandham.MainActivityVadakalai"),
    THENKALAI("com.srinivaskannan.divyaprabhandham.MainActivityThenkalai");

    companion object {
        fun from(key: String?): AppIcon =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: VADAKALAI
    }
}

object AppIconSwitcher {

    /** Enables [target]'s alias and disables the others. No-op if already set. */
    fun apply(context: Context, target: AppIcon) {
        val pm = context.packageManager
        val pkg = context.packageName
        AppIcon.entries.forEach { icon ->
            val state = if (icon == target) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            val component = ComponentName(pkg, icon.alias)
            // Only write when it differs, to avoid needless launcher churn.
            if (pm.getComponentEnabledSetting(component) != state) {
                pm.setComponentEnabledSetting(
                    component,
                    state,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
