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
import com.srinivaskannan.divyaprabhandham.widget.WidgetBridge

/**
 * Posts the sparse tip-jar nudge and, when tapped, opens the Tip Jar.
 *
 * Like [ReminderReceiver], it cannot touch AppState — by the time this fires
 * the process has been cold for two weeks — so the language comes from the
 * widget snapshot the app keeps on disk, and the reminder follows the app's
 * script setting rather than the device locale.
 *
 * Nothing is re-checked here. [ReminderScheduler] cancels and re-arms this
 * alarm on every launch, so the only way to reach this point is not to have
 * opened the app since it was armed, which is also the only way the silenced
 * and supporter flags it was armed under could not have changed.
 */
class SupportReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val english = WidgetBridge.readSnapshot(context)?.uiEnglish ?: false

        val deepLink = Uri.parse("divyaprabhandham://tipjar")
        val tapIntent = Intent(Intent.ACTION_VIEW, deepLink).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            context,
            SUPPORT_TAP_REQUEST,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        ReminderScheduler.ensureSupportChannel(context)

        val body = UiText.string(Ui.TIP_REMINDER_BODY, english)
        val notification = NotificationCompat.Builder(context, ReminderScheduler.SUPPORT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(UiText.string(Ui.TIP_REMINDER_TITLE, english))
            .setContentText(body)
            // The body is a sentence and a half, which a collapsed notification
            // truncates mid-word; BigTextStyle lets it expand to the whole ask.
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private companion object {
        const val NOTIFICATION_ID = 4301
        const val SUPPORT_TAP_REQUEST = 4302
    }
}
