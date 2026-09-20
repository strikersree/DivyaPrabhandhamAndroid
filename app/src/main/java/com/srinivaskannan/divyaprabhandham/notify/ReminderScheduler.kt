package com.srinivaskannan.divyaprabhandham.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.srinivaskannan.divyaprabhandham.R
import com.srinivaskannan.divyaprabhandham.prefs.AppState
import java.util.Calendar

/**
 * Daily reading reminders, entirely on device.
 *
 * AlarmManager rather than WorkManager: these need to fire at a specific wall
 * clock time the person chose, and WorkManager explicitly does not promise
 * that — it batches work for battery. `setInexactRepeating` is used rather than
 * an exact alarm because a reading nudge does not warrant the
 * `SCHEDULE_EXACT_ALARM` permission, which Play scrutinises and which users are
 * right to be suspicious of. A reminder that lands a few minutes late is fine.
 *
 * Alarms do not survive a reboot, so [BootReceiver] re-arms them.
 */
object ReminderScheduler {

    const val CHANNEL_ID = "dp.reminders"

    /** The tip-jar nudge gets its own channel — see the note in strings.xml. */
    const val SUPPORT_CHANNEL_ID = "dp.support"

    private const val REQUEST_BASE = 4200
    private const val SUPPORT_REQUEST = 4300

    /**
     * How far out the support nudge is armed. Long on purpose: [reschedule]
     * runs on every launch and cancels it first, so for anyone who opens the
     * app it is perpetually pushed another two weeks away and never fires.
     * It only ever reaches someone who has not opened the app in that whole
     * window — exactly the person the in-app prompt can never reach.
     */
    private const val SUPPORT_DELAY_MS = 14L * 24 * 60 * 60 * 1000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun ensureSupportChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(SUPPORT_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            SUPPORT_CHANNEL_ID,
            context.getString(R.string.notification_channel_support),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_support_desc)
        }
        manager.createNotificationChannel(channel)
    }

    /** Cancels everything pending and re-arms from the current settings. */
    fun reschedule(context: Context, appState: AppState) {
        ensureChannel(context)
        cancelAll(context)
        if (!appState.notificationsEnabled) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        appState.reminderTimes.take(AppState.MAX_REMINDERS).forEachIndexed { index, time ->
            val trigger = nextOccurrence(time.hour, time.minute)
            alarms.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                trigger,
                AlarmManager.INTERVAL_DAY,
                pendingIntent(context, index),
            )
        }
        scheduleSupportReminder(context, appState, alarms)
    }

    /**
     * The sparse tip-jar nudge, mirroring iOS's NotificationManager.
     *
     * Deliberately separate from the daily reading reminders rather than
     * folded into them, so a devotional reading prompt never doubles as a
     * donation pitch. Inexact, like the reading alarms: a nudge two weeks out
     * does not warrant SCHEDULE_EXACT_ALARM.
     *
     * Skipped for supporters. iOS does send it to them, reasoning they may
     * give again — but this app's own in-app prompt already exempts them
     * ([AppState.shouldShowTipPrompt]), and a notification is more assertive
     * than a sheet, so asking here would be a stranger thing to do than not.
     * Silencing the tip prompt stops it for everyone.
     */
    private fun scheduleSupportReminder(
        context: Context,
        appState: AppState,
        alarms: AlarmManager,
    ) {
        if (appState.tipPromptSilenced || appState.isSupporter) return
        ensureSupportChannel(context)
        alarms.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + SUPPORT_DELAY_MS,
            supportPendingIntent(context),
        )
    }

    fun cancelAll(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        repeat(AppState.MAX_REMINDERS) { index ->
            alarms.cancel(pendingIntent(context, index))
        }
        alarms.cancel(supportPendingIntent(context))
    }

    private fun pendingIntent(context: Context, index: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction("$CHANNEL_ID.$index")
        return PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + index,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun supportPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SupportReminderReceiver::class.java)
            .setAction(SUPPORT_CHANNEL_ID)
        return PendingIntent.getBroadcast(
            context,
            SUPPORT_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** The next time today's clock reaches hour:minute, tomorrow if it already has. */
    private fun nextOccurrence(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }
}
