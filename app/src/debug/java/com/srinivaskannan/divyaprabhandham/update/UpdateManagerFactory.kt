package com.srinivaskannan.divyaprabhandham.update

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager

/**
 * Debug builds can pretend an update is waiting:
 *
 *     adb shell am start -n <pkg>/com.srinivaskannan.divyaprabhandham.MainActivity \
 *         --ez fake_update true --ei fake_update_version 99
 *
 * Add `--ez fake_update_auto true` and it also plays the part of the person:
 * accepting Play's dialog, downloading, and finishing, so the install-state
 * listener and the restart snackbar are exercised end to end.
 *
 * Play's own FakeAppUpdateManager drives the same code the real one does, so
 * the flow, the snooze and the restart prompt are all exercised for real.
 * Without this the feature could only be tested through internal app sharing,
 * which needs a Play Console release for every change to it.
 *
 * The daily check gate is real in debug too, so a second run the same day
 * does nothing. Clear it between attempts:
 *
 *     adb shell run-as <pkg> rm -f shared_prefs/dp.update.xml
 */
fun createAppUpdateManager(activity: Activity): AppUpdateManager {
    if (activity.intent?.getBooleanExtra("fake_update", false) != true) {
        return AppUpdateManagerFactory.create(activity)
    }
    val version = activity.intent.getIntExtra("fake_update_version", 99)
    val fake = FakeAppUpdateManager(activity).apply {
        setUpdateAvailable(version)
        setUpdatePriority(0)
        setClientVersionStalenessDays(1)
    }
    if (activity.intent.getBooleanExtra("fake_update_auto", false)) {
        // Delays, not callbacks: the flow has to have been started before the
        // fake will accept it, and that happens a beat after the first frame.
        val main = Handler(Looper.getMainLooper())
        main.postDelayed({ runCatching { fake.userAcceptsUpdate() } }, 2_500)
        main.postDelayed({ runCatching { fake.downloadStarts() } }, 3_000)
        main.postDelayed({ runCatching { fake.downloadCompletes() } }, 4_000)
    }
    return fake
}

