package com.srinivaskannan.divyaprabhandham.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.srinivaskannan.divyaprabhandham.R
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.data.UiText
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import com.srinivaskannan.divyaprabhandham.widget.WidgetBridge

/**
 * Posts a reminder and, when tapped, resumes reading.
 *
 * The receiver cannot touch AppState — it may run in a cold process with no
 * Compose state alive — so the text is read from the widget snapshot, which the
 * app keeps up to date on disk. That is also what makes the reminder follow the
 * app's script setting rather than the device locale, exactly as on iOS.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val snapshot = WidgetBridge.readSnapshot(context)
        val script = snapshot?.script?.let { ScriptChoice.from(it) } ?: ScriptChoice.TAMIL

        val title = UiText.string(Ui.REMINDER_MESSAGE, script)
        val body = snapshot?.lastReadTitle

        val deepLink = Uri.parse("divyaprabhandham://resume")
        val tapIntent = Intent(Intent.ACTION_VIEW, deepLink).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        ReminderScheduler.ensureChannel(context)

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash)
            .setContentTitle(title)
            .apply { if (body != null) setContentText(body) }
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private companion object {
        const val NOTIFICATION_ID = 4201
    }
}
