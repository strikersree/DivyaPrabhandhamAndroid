package com.srinivaskannan.divyaprabhandham.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.srinivaskannan.divyaprabhandham.R
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.data.UiText
import com.srinivaskannan.divyaprabhandham.prefs.AppState
import com.srinivaskannan.divyaprabhandham.prefs.ReadingLog
import com.srinivaskannan.divyaprabhandham.widget.WidgetBridge
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The streak reminder: at 9 PM, three hours before the day ends, if today's two
 * minutes have not been read. Mirrors the iOS one, except that Android can show
 * a live countdown to midnight in the notification itself (a count-down
 * chronometer), which iOS can only do from a server.
 *
 * Armed one day at a time: the receiver re-arms tomorrow's after it fires,
 * and [ReminderScheduler.reschedule] re-arms it on every launch and reboot.
 * A ten-minute window rather than an exact alarm, for the same reason as the
 * daily reminders: it does not warrant SCHEDULE_EXACT_ALARM.
 */
object StreakReminder {

    const val CHANNEL_ID = "dp.streak"
    private const val REQUEST = 4400
    private const val NOTIFICATION_ID = 4401
    private const val HOUR = 21
    private const val WINDOW_MS = 10L * 60 * 1000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val english = WidgetBridge.readSnapshot(context)?.uiEnglish ?: true
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                UiText.string(Ui.STREAK_REMINDER, english),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = UiText.string(Ui.STREAK_REMINDER_FOOTER, english) },
        )
    }

    fun schedule(context: Context, appState: AppState) {
        cancel(context)
        if (!appState.streakRemindersEnabled) return
        arm(context)
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }

    /** Arms the next 9 PM: today's if it is still ahead, otherwise tomorrow's. */
    internal fun arm(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val zone = ZoneId.systemDefault()
        var at = LocalDate.now().atTime(HOUR, 0)
        if (!at.isAfter(LocalDateTime.now())) at = at.plusDays(1)
        alarms.setWindow(
            AlarmManager.RTC_WAKEUP,
            at.atZone(zone).toInstant().toEpochMilli(),
            WINDOW_MS,
            pendingIntent(context),
        )
    }

    /** Takes down a reminder that is showing, once today's reading is done. */
    fun withdraw(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    internal fun post(context: Context) {
        ReadingLog.init(context)
        val today = LocalDate.now()
        if (ReadingLog.metGoal(today)) return
        val streak = ReadingLog.currentStreak(today)
        val english = WidgetBridge.readSnapshot(context)?.uiEnglish ?: true

        val title = when {
            streak == 0 && english -> "⏳ Read today before the day ends"
            streak == 0 -> "⏳ நாள் முடியும் முன் இன்று படியுங்கள்"
            english -> "🔥 Your $streak-day streak ends tonight"
            else -> "🔥 உங்கள் $streak நாள் தொடர் இன்றிரவு முடியும்"
        }
        val midnight = today.plusDays(1).atTime(LocalTime.MIDNIGHT)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        if (midnight <= now) return

        val tapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("divyaprabhandham://resume")).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            context, REQUEST, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(UiText.string(Ui.STREAK_NOTIFY_BODY, english))
            // The countdown: time left until midnight, ticking in the shade.
            .setWhen(midnight)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            // Gone at midnight, when the streak it is about has already ended.
            .setTimeoutAfter(midnight - now)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST,
            Intent(context, StreakReminderReceiver::class.java).setAction(CHANNEL_ID),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

/** Fires at 9 PM: posts the reminder if today is still unread, and arms tomorrow's. */
class StreakReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        StreakReminder.post(context)
        StreakReminder.arm(context)
    }
}
