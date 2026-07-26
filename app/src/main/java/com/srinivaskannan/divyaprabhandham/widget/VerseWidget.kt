package com.srinivaskannan.divyaprabhandham.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
// The Intent overload of actionStartActivity lives in the appwidget artifact,
// not in androidx.glance.action — that package only offers the ComponentName
// and reified-Activity forms, neither of which can carry the deep-link URI.
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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

    // Exact mode gives LocalSize the widget's real dimensions on every resize,
    // which is what the type scaling below reads.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetBridge.readSnapshot(context)
        // Read from the context, never hardcoded: debug builds carry an
        // applicationIdSuffix, so a literal package name makes the tap intent
        // resolve to nothing and the widget silently stops opening the app.
        val packageName = context.packageName

        provideContent {
            GlanceTheme {
                val verses = snapshot?.verses(WidgetAayiram.FOLLOW_APP).orEmpty()
                if (verses.isEmpty()) {
                    EmptyWidget(deepLink(packageName, null, null))
                } else {
                    val verse = verses[hourIndex(verses.size)]
                    VerseContent(
                        pasuramLabel = "${snapshot?.uiPasuram.orEmpty()} ${verse.n}",
                        text = verse.t,
                        work = verse.w,
                        deepLink = deepLink(packageName, verse.s, verse.k),
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
        // Type scales with the widget. The height drives the body size (a taller
        // widget can afford larger, and show more lines); everything else is
        // derived from it so the hierarchy holds at every size. Values are
        // clamped so a tiny widget stays legible and a huge one does not turn
        // cartoonish.
        val size = LocalSize.current
        val metrics = widgetMetrics(size)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(metrics.pad)
                .clickable(actionStartActivity(deepLink)),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = pasuramLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = metrics.label,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(GlanceModifier.height(metrics.gap))
            Text(
                text = text,
                maxLines = metrics.bodyLines,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = metrics.body,
                ),
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(GlanceModifier.height(metrics.gap))
            Text(
                text = work,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = metrics.caption,
                ),
            )
        }
    }

    private data class WidgetMetrics(
        val body: androidx.compose.ui.unit.TextUnit,
        val label: androidx.compose.ui.unit.TextUnit,
        val caption: androidx.compose.ui.unit.TextUnit,
        val pad: androidx.compose.ui.unit.Dp,
        val gap: androidx.compose.ui.unit.Dp,
        val bodyLines: Int,
    )

    /**
     * Type and spacing as a function of the widget box.
     *
     * Body size tracks height, linearly between a 2-cell (~110dp) and a
     * 4-cell (~250dp) tall widget, clamped at both ends: never smaller than the
     * old fixed 15sp floor on a small widget, up to 22sp on a large one. Label
     * and caption are fixed offsets below the body so the three-level hierarchy
     * reads the same at every size, and the line count grows with height so a
     * tall widget fills with verse rather than whitespace.
     */
    private fun widgetMetrics(size: DpSize): WidgetMetrics {
        val h = size.height.value
        val t = ((h - 110f) / (250f - 110f)).coerceIn(0f, 1f)
        val body = (15f + t * 7f)             // 15 -> 22
        val pad = (12f + t * 6f)              // 12 -> 18
        val lines = when {
            h < 130f -> 3
            h < 190f -> 5
            h < 240f -> 8
            else -> 12
        }
        return WidgetMetrics(
            body = body.sp,
            label = (body - 3f).sp,
            caption = (body - 4f).sp,
            pad = pad.dp,
            gap = (pad * 0.45f).dp,
            bodyLines = lines,
        )
    }

    @androidx.compose.runtime.Composable
    private fun EmptyWidget(deepLink: Intent) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(14.dp)
                .clickable(actionStartActivity(deepLink)),
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

    private fun deepLink(packageName: String, sectionId: String?, stanzaKey: String?): Intent {
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
            `package` = packageName
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}

class VerseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VerseWidget()
}
