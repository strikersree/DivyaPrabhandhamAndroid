package com.srinivaskannan.divyaprabhandham.update

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * "A new version is available", through Play's own In-App Updates.
 *
 * Android, unlike iOS, has an API for this, so it is used rather than a
 * version number scraped from a listing. The FLEXIBLE flow is the one that
 * suits a book: Play downloads the new build in the background while the
 * person carries on reading, and only when it is ready does anything ask
 * them to restart. The IMMEDIATE flow -- a full-screen blocking update --
 * would stop somebody opening this to recite before dawn, and there is no
 * version of this app where a verse is unavailable because a newer build
 * exists.
 *
 * The same rules the iOS checker follows, for the same reasons:
 *
 *  - It never blocks.
 *  - It asks once per version. Dismiss the Play dialog and that version is
 *    snoozed for a week; "not now" is not "never", so a version still
 *    current a month later may ask again.
 *  - It checks at most once a day, on launch.
 *  - Every failure is silent. No Play Store on the device, a sideloaded
 *    build, no network: all of them mean say nothing. A reader on a device
 *    without Play must never see an error about an update they cannot
 *    install anyway.
 */
class InAppUpdate(
    private val context: Context,
    private val manager: AppUpdateManager,
) {

    /** What, if anything, the UI should be saying about an update. */
    enum class State { Idle, Downloading, ReadyToInstall }

    var state by mutableStateOf(State.Idle)
        private set


    private val prefs = context.getSharedPreferences("dp.update", Context.MODE_PRIVATE)

    /** The version the running flow is offering, for the snooze record. */
    private var offeredVersion = -1
    private val listener = InstallStateUpdatedListener { install ->
        state = when (install.installStatus()) {
            InstallStatus.DOWNLOADING, InstallStatus.PENDING -> State.Downloading
            InstallStatus.DOWNLOADED -> State.ReadyToInstall
            else -> State.Idle
        }
    }

    private companion object {
        const val LAST_CHECK = "lastCheck"
        const val SNOOZED_VERSION = "snoozedVersion"
        const val SNOOZED_AT = "snoozedAt"
        const val DAY = 24 * 60 * 60 * 1000L
        const val SNOOZE = 7 * DAY
    }

    fun start() = manager.registerListener(listener)
    fun stop() = manager.unregisterListener(listener)

    /**
     * Called when the app is opened. Returns without touching the network
     * when a check is not due, so it is safe to call on every resume.
     */
    fun checkIfDue(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        activity: Activity,
        now: Long = System.currentTimeMillis(),
    ) {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                // An update downloaded before the app was last killed is
                // still sitting there. Offer the restart again rather than
                // making them wait for another download.
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    state = State.ReadyToInstall
                    return@addOnSuccessListener
                }
                if (now - prefs.getLong(LAST_CHECK, 0L) < DAY) return@addOnSuccessListener
                prefs.edit().putLong(LAST_CHECK, now).apply()
                if (!isOffered(info)) return@addOnSuccessListener
                if (isSnoozed(info.availableVersionCode(), now)) return@addOnSuccessListener
                offeredVersion = info.availableVersionCode()
                runCatching {
                    manager.startUpdateFlowForResult(
                        info, launcher,
                        com.google.android.play.core.appupdate.AppUpdateOptions
                            .newBuilder(AppUpdateType.FLEXIBLE).build(),
                    )
                }.onFailure { android.util.Log.e("InAppUpdate", "start failed: $it") }
                    .onSuccess { android.util.Log.e("InAppUpdate", "flow started") }
            }
            // Silent: see the class note. A device without Play lands here
            // on every launch and must never be told about it.
            .addOnFailureListener { android.util.Log.e("InAppUpdate", "no info: $it") }
    }

    private fun isOffered(info: AppUpdateInfo) =
        info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

    /** Called with the Play dialog's result: a cancel is a "not now". */
    fun onFlowResult(resultCode: Int, now: Long = System.currentTimeMillis()) {
        if (resultCode == Activity.RESULT_OK || offeredVersion < 0) return
        prefs.edit()
            .putInt(SNOOZED_VERSION, offeredVersion)
            .putLong(SNOOZED_AT, now)
            .apply()
    }

    fun isSnoozed(versionCode: Int, now: Long = System.currentTimeMillis()): Boolean =
        prefs.getInt(SNOOZED_VERSION, -1) == versionCode &&
            now - prefs.getLong(SNOOZED_AT, 0L) < SNOOZE

    /** Restarts into the downloaded build. */
    fun completeUpdate() = manager.completeUpdate()

    /** Dismisses the restart prompt without installing. */
    fun dismissRestart() { state = State.Idle }
}


/**
 * The one piece of UI this feature has: a snackbar saying the new build is
 * downloaded, and the restart that installs it.
 *
 * Nothing announces the *start* of a download -- Play's own dialog already
 * did that, and a second banner saying "downloading" would be the app
 * talking about itself while somebody is trying to read. This appears only
 * when there is something to act on, and it can be dismissed; the update
 * installs on the next natural restart either way.
 */
@Composable
fun UpdateRestartBar(
    state: InAppUpdate.State,
    readyLabel: String,
    restartLabel: String,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state != InAppUpdate.State.ReadyToInstall) return
    Snackbar(
        // Clear of the bottom navigation bar and the system inset;
        // a snackbar under the tab bar is a snackbar nobody taps.
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
        action = { TextButton(onClick = onRestart) { Text(restartLabel) } },
        dismissAction = { TextButton(onClick = onDismiss) { Icon(Icons.Filled.Close, null) } },
    ) {
        Text(readyLabel)
    }
}
