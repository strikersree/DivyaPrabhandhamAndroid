package com.srinivaskannan.divyaprabhandham.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
// The Intent overload of actionStartActivity lives in the appwidget artifact,
// not in androidx.glance.action — that package only offers the ComponentName
// and reified-Activity forms, neither of which can carry the deep-link URI.
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.srinivaskannan.divyaprabhandham.prefs.WidgetAayiram
import java.util.concurrent.TimeUnit

/**
 * The hourly verse widget.
 *
 * Which verse shows is derived from the clock rather than stored, so every
 * device with the same snapshot shows the same verse at the same hour, and
 * nothing has to be written back from the widget process. Tapping opens that
 * exact verse in the reader through the app's deep-link scheme.
 *
 * Glance's `updatePeriodMillis` is a floor, not a promise — Android will
 * coalesce updates to save battery, so in practice the verse changes roughly
 * hourly rather than exactly on the hour. That is the right trade for a widget
 * nobody is timing.
 */
class VerseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetBridge.readSnapshot(context)

        provideContent {
            GlanceTheme {
                val verses = snapshot?.verses(WidgetAayiram.FOLLOW_APP).orEmpty()
                if (verses.isEmpty()) {
                    EmptyWidget()
                } else {
                    val verse = verses[hourIndex(verses.size)]
                    VerseContent(
                        pasuramLabel = "${snapshot?.uiPasuram.orEmpty()} ${verse.n}",
                        text = verse.t,
                        work = verse.w,
                        deepLink = deepLink(verse.s, verse.k),
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun VerseContent(
        pasuramLabel: String,
        text: String,
        work: String,
        deepLink: Intent,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(14.dp)
                .clickable(actionStartActivity(deepLink)),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = pasuramLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = text,
                maxLines = 4,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                ),
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = work,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                ),
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun EmptyWidget() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(14.dp)
                .clickable(actionStartActivity(deepLink(null, null))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "நாலாயிர திவ்ய பிரபந்தம்",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp,
                ),
            )
        }
    }

    /**
     * Picks a verse from the pool by the hour. Multiplying by a large odd
     * number before taking the remainder keeps consecutive hours far apart in
     * the pool, so the widget does not walk the corpus in order.
     */
    private fun hourIndex(poolSize: Int): Int {
        val hours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis())
        return ((hours * 7919) % poolSize).toInt().coerceAtLeast(0)
    }

    private fun deepLink(sectionId: String?, stanzaKey: String?): Intent {
        val uri = if (sectionId == null) {
            Uri.parse("divyaprabhandham://resume")
        } else {
            Uri.parse("divyaprabhandham://open")
                .buildUpon()
                .appendQueryParameter("section", sectionId)
                .apply { if (stanzaKey != null) appendQueryParameter("key", stanzaKey) }
                .build()
        }
        return Intent(Intent.ACTION_VIEW, uri).apply {
            `package` = "com.srinivaskannan.divyaprabhandham"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}

class VerseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VerseWidget()
}
