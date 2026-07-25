package com.srinivaskannan.divyaprabhandham.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import com.srinivaskannan.divyaprabhandham.prefs.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Re-arms reminders after a reboot or an app update, since AlarmManager alarms
 * do not survive either.
 *
 * `goAsync` is needed because reading the settings back out of DataStore is
 * suspending, and a BroadcastReceiver is otherwise dead the moment onReceive
 * returns.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val pending = goAsync()
        val appContext = context.applicationContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val appState = AppState.create(appContext, scope)
                ReminderScheduler.reschedule(appContext, appState)
            } finally {
                pending.finish()
            }
        }
    }
}
