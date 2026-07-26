package com.srinivaskannan.divyaprabhandham.ui.settings

import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import java.util.concurrent.TimeUnit

/**
 * A short "synced N ago" phrase, in Tamil or English per the app's script.
 *
 * Deliberately coarse — just now, minutes, hours, days, weeks — because a
 * last-synced line does not need second precision and the coarse buckets read
 * cleanly in both scripts. English uses the singular for one unit ("a minute
 * ago"); Tamil takes the plain numbered form, which is natural there.
 */
object RelativeTime {

    fun syncedAgo(nowMs: Long, thenMs: Long, script: ScriptChoice): String {
        val tamil = script == ScriptChoice.TAMIL
        val delta = (nowMs - thenMs).coerceAtLeast(0)

        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours = TimeUnit.MILLISECONDS.toHours(delta)
        val days = TimeUnit.MILLISECONDS.toDays(delta)
        val weeks = days / 7

        return when {
            minutes < 1L -> if (tamil) "இப்போது ஒத்திசைக்கப்பட்டது" else "Synced just now"
            minutes < 60L -> phrase(tamil, minutes, "நிமிடம்", "நிமிடங்கள்", "minute", "minutes")
            hours < 24L -> phrase(tamil, hours, "மணி நேரம்", "மணி நேரம்", "hour", "hours")
            days < 7L -> phrase(tamil, days, "நாள்", "நாட்கள்", "day", "days")
            else -> phrase(tamil, weeks, "வாரம்", "வாரங்கள்", "week", "weeks")
        }
    }

    private fun phrase(
        tamil: Boolean,
        value: Long,
        taOne: String,
        taMany: String,
        enOne: String,
        enMany: String,
    ): String = if (tamil) {
        // "5 நிமிடங்களுக்கு முன் ஒத்திசைக்கப்பட்டது"
        "$value ${if (value == 1L) taOne else taMany}க்கு முன் ஒத்திசைக்கப்பட்டது"
    } else {
        // "Synced 5 minutes ago" / "Synced a minute ago" / "Synced an hour ago"
        val count = if (value == 1L) {
            if (enOne.first() in "aeiou") "an" else "a"
        } else {
            value.toString()
        }
        "Synced $count ${if (value == 1L) enOne else enMany} ago"
    }
}
