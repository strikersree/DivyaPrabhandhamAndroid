package com.srinivaskannan.divyaprabhandham.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.DrawableRes
import androidx.core.graphics.createBitmap
import com.srinivaskannan.divyaprabhandham.R
import com.srinivaskannan.divyaprabhandham.prefs.FontChoice
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import java.util.Calendar
import kotlin.math.max

/**
 * Sharing a pasuram as a picture -- the Android half of the iOS share card.
 *
 * The text share has always existed and still does; this adds the other half,
 * so a verse sent to somebody without the app arrives looking like the page it
 * came from rather than a wall of unformatted Unicode.
 *
 * Drawn with android.graphics rather than Compose. A Compose tree has to be
 * attached to a window before it will lay out, and a share card needs no
 * window -- Canvas plus StaticLayout draws straight into a Bitmap, and
 * StaticLayout is also what measures the fit, so what is measured is exactly
 * what is drawn.
 *
 * GEOMETRY IS THE ARTWORK'S OWN, IN PIXELS. The iOS renderer works in points
 * at scale 2 against the same 1414x2000 images; every constant here is that
 * one doubled, and the two must stay in step or the same verse will sit
 * differently on the same card on the two platforms.
 */

/** Which background the card uses, chosen by the clock. */
enum class ShareCardTime {
    DAY, EVENING, NIGHT;

    @get:DrawableRes
    val drawable: Int
        get() = when (this) {
            DAY -> R.drawable.share_card_day
            EVENING -> R.drawable.share_card_evening
            NIGHT -> R.drawable.share_card_night
        }

    /**
     * Ink for the verse, and the quieter one for the heading and attribution.
     *
     * ALL THREE GROUNDS ARE LIGHT -- measured off the art, not inferred from
     * the names. The night card is a night *scene* washed back to a pale grey
     * (mean luminance 190 of 255) so a verse can sit on it, so its ink is dark
     * like the others; it only drops the warmth, because that ground is
     * neutral grey where the other two are cream.
     */
    val ink: Int
        get() = when (this) {
            DAY, EVENING -> 0xFF3A2E17.toInt()
            NIGHT -> 0xFF252220.toInt()
        }

    /** The brown of each card's own band, so the heading belongs to it. */
    val quietInk: Int
        get() = when (this) {
            DAY -> 0xFF67461A.toInt()
            EVENING -> 0xFF825B1D.toInt()
            NIGHT -> 0xFF4A413A.toInt()
        }

    companion object {
        /**
         * Day 5am-5pm, evening 5pm-9pm, night 9pm-5am. Coarse on purpose --
         * this picks a picture, not a muhurtham -- and gives the evening the
         * sandhya window, when the lamps in that card's own scene are lit.
         */
        fun current(calendar: Calendar = Calendar.getInstance()): ShareCardTime =
            when (calendar.get(Calendar.HOUR_OF_DAY)) {
                in 5..16 -> DAY
                in 17..20 -> EVENING
                else -> NIGHT
            }
    }
}

/**
 * A pasuram on its way out of the app. Values only -- no view, no repository
 * -- so the render can happen off the main thread when somebody asks for it.
 */
data class SharedPasuram(
    /** "பாசுரம் 30", or the section's title for a thaniyan with no number. */
    val heading: String,
    val prelude: String?,
    val verse: String,
    /** "— திருப்பாவை, நாலாயிர திவ்ய பிரபந்தம்" */
    val attribution: String,
    val script: ScriptChoice,
    val font: FontChoice,
    val time: ShareCardTime,
) {
    val fileName: String
        get() = (heading.ifEmpty { "pasuram" }).replace(' ', '-') + ".png"
}

object ShareCardRenderer {

    private const val WIDTH = 1414
    private const val HEIGHT = 2000

    /** The clear field: inside the printed border, above the brown band. */
    private val FIELD = Rect(66, 192, 1354, 1586)
    private const val INSET_X = 92
    private const val INSET_Y = 80
    /** Breathing room between the heading, the verse and the attribution. */
    private const val GAP = 48

    private const val HEADING_SIZE = 42f
    private const val ATTRIBUTION_SIZE = 34f

    /** The size ladder, and the two floors the fit falls back through. */
    private const val MAX_VERSE = 64f
    private const val NO_WRAP_FLOOR = 36f
    private const val FLOOR = 26f

    private val textLeft get() = FIELD.left + INSET_X
    private val textWidth get() = FIELD.width() - INSET_X * 2
    private val textTop get() = FIELD.top + INSET_Y
    private val textBottom get() = FIELD.bottom - INSET_Y

    fun render(context: Context, pasuram: SharedPasuram): Bitmap? {
        val background = BitmapFactory.decodeResource(context.resources, pasuram.time.drawable)
            ?: return null
        val out = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(out)
        canvas.drawBitmap(background, null, Rect(0, 0, WIDTH, HEIGHT),
                          Paint(Paint.FILTER_BITMAP_FLAG))
        background.recycle()

        val heading = layout(pasuram.heading, headingPaint(pasuram), 0f)
        val attribution = layout(pasuram.attribution, attributionPaint(pasuram), 0f)

        // What is left for the verse once the two fixed blocks have their
        // room. The fit below is measured against exactly this.
        val verseTop = textTop + heading.height + GAP
        val verseBottom = textBottom - attribution.height - GAP
        val available = verseBottom - verseTop

        val size = fittedVerseSize(pasuram, available)
        val blocks = verseBlocks(pasuram, size)
        val blockHeight = blocks.sumOf { it.height } + (blocks.size - 1) * (size * 0.55f).toInt()

        draw(canvas, heading, textLeft.toFloat(), textTop.toFloat())
        var y = verseTop + max(0, (available - blockHeight) / 2)
        for ((index, block) in blocks.withIndex()) {
            draw(canvas, block, textLeft.toFloat(), y.toFloat())
            y += block.height
            if (index < blocks.lastIndex) y += (size * 0.55f).toInt()
        }
        draw(canvas, attribution, textLeft.toFloat(), (textBottom - attribution.height).toFloat())
        return out
    }

    /**
     * The size the verse is set at.
     *
     * Two passes, because where a line of a pasuram ends is not arbitrary: it
     * is the metre. The first looks for the largest size at which every line
     * still stands on its own, so the card reproduces the poem's own
     * lineation; only if that cannot be had at a readable size does the
     * second let lines wrap, which is the lesser of the two losses against
     * type too small to read.
     */
    fun fittedVerseSize(pasuram: SharedPasuram, available: Int): Float {
        largest(pasuram, available, withoutWrapping = true, floor = NO_WRAP_FLOOR)?.let { return it }
        return largest(pasuram, available, withoutWrapping = false, floor = FLOOR) ?: FLOOR
    }

    private fun largest(
        pasuram: SharedPasuram,
        available: Int,
        withoutWrapping: Boolean,
        floor: Float,
    ): Float? {
        var size = MAX_VERSE
        while (size >= floor) {
            val fitsColumn = !withoutWrapping || standsOnOwnLines(pasuram, size)
            if (fitsColumn) {
                val blocks = verseBlocks(pasuram, size)
                val height = blocks.sumOf { it.height } + (blocks.size - 1) * (size * 0.55f).toInt()
                if (height <= available) return size
            }
            size -= 2f
        }
        return null
    }

    /** True when no line of the verse (or prelude) is wider than the column. */
    private fun standsOnOwnLines(pasuram: SharedPasuram, size: Float): Boolean {
        fun fits(text: String, paint: TextPaint) =
            text.split('\n').all { paint.measureText(it) <= textWidth }
        if (!fits(pasuram.verse, versePaint(pasuram, size))) return false
        val prelude = pasuram.prelude
        return prelude.isNullOrEmpty() || fits(prelude, preludePaint(pasuram, size))
    }

    /** The prelude (where there is one) and the verse, in drawing order. */
    private fun verseBlocks(pasuram: SharedPasuram, size: Float): List<StaticLayout> {
        val blocks = mutableListOf<StaticLayout>()
        pasuram.prelude?.takeIf { it.isNotEmpty() }?.let {
            blocks += layout(it, preludePaint(pasuram, size), size * 0.8f * 0.3f)
        }
        blocks += layout(pasuram.verse, versePaint(pasuram, size), size * 0.35f)
        return blocks
    }

    private fun layout(text: String, paint: TextPaint, extraLineSpacing: Float): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(extraLineSpacing, 1f)
            .setIncludePad(false)
            .build()

    private fun draw(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun versePaint(pasuram: SharedPasuram, size: Float) =
        paint(typefaceFor(pasuram), size, pasuram.time.ink)

    private fun preludePaint(pasuram: SharedPasuram, size: Float) =
        paint(Typeface.create(typefaceFor(pasuram), Typeface.ITALIC), size * 0.8f,
              pasuram.time.quietInk)

    private fun headingPaint(pasuram: SharedPasuram) =
        paint(Typeface.create(Typeface.SERIF, Typeface.BOLD), HEADING_SIZE, pasuram.time.quietInk)

    private fun attributionPaint(pasuram: SharedPasuram) =
        paint(Typeface.SERIF, ATTRIBUTION_SIZE, pasuram.time.quietInk)

    private fun paint(face: Typeface, size: Float, colour: Int) = TextPaint().apply {
        isAntiAlias = true
        typeface = face
        textSize = size
        color = colour
    }

    /**
     * Mirrors what ReadingFonts actually resolves to. No Indic face is
     * bundled, so every script falls through to the platform family and only
     * the serif/sans split survives -- which is exactly what this asks for,
     * so the card is set in the same face as the page it came from.
     */
    private fun typefaceFor(pasuram: SharedPasuram): Typeface = when {
        pasuram.script.isIndicScript -> when (pasuram.font) {
            FontChoice.TRADITIONAL, FontChoice.CLASSIC -> Typeface.SERIF
            FontChoice.MODERN, FontChoice.SANS -> Typeface.SANS_SERIF
        }
        // The romanised scripts pair three serifs against one sans.
        pasuram.font == FontChoice.SANS -> Typeface.SANS_SERIF
        else -> Typeface.SERIF
    }
}
