package com.srinivaskannan.divyaprabhandham.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Where reading time was spent: a division read in the reader, a Divya Desam's
 * pasurams, or a collection read straight through. Stored by [key] so the log
 * stays plain JSON — the same keys as the iOS build.
 */
sealed interface ReadingPlace {
    val key: String

    data class Division(val id: String) : ReadingPlace {
        override val key get() = "div:$id"
    }

    data class Desam(val id: String) : ReadingPlace {
        override val key get() = "desam:$id"
    }

    data class Collection(val id: String) : ReadingPlace {
        override val key get() = "coll:$id"
    }

    companion object {
        fun fromKey(key: String): ReadingPlace? {
            val i = key.indexOf(':')
            if (i <= 0) return null
            val id = key.substring(i + 1)
            return when (key.substring(0, i)) {
                "div" -> Division(id)
                "desam" -> Desam(id)
                "coll" -> Collection(id)
                else -> null
            }
        }
    }
}

/** One calendar day's reading. */
@Serializable
data class ReadingDay(
    val seconds: Double = 0.0,
    /** [ReadingPlace.key] → seconds. */
    val places: Map<String, Double> = emptyMap(),
) {
    operator fun plus(other: ReadingDay) = ReadingDay(
        seconds = seconds + other.seconds,
        places = (places.keys + other.places.keys).associateWith {
            (places[it] ?: 0.0) + (other.places[it] ?: 0.0)
        },
    )
}

/**
 * Reading time per calendar day, and the streak built on it: a day counts once
 * at least [DAILY_GOAL] seconds were spent reading pasurams. Mirrors the iOS
 * ReadingLog.
 *
 * SharedPreferences rather than AppState's DataStore, because the streak
 * reminder's receiver has to know synchronously, in a cold process, whether
 * today's reading is already done.
 *
 * Synced through the person's Google Drive, but not last-writer-wins like the
 * rest of the reading state: two devices can both read on the same day, so each
 * install writes only its own log, to a file of its own, and adds every other
 * device's log to it (see GoogleSyncManager).
 */
object ReadingLog {

    /** Two minutes a day keeps the streak. */
    const val DAILY_GOAL = 120.0

    /** "yyyy-MM-dd" → that day's reading, on this device and every synced one. */
    var days: Map<String, ReadingDay> by mutableStateOf(emptyMap())
        private set

    /** Set when today's reading first reaches the goal, to the streak it makes;
     *  the root composable shows it once and clears it. */
    var celebration: Int? by mutableStateOf(null)

    /** Fired when this device's own log changes, for the Drive push. */
    var onOwnChanged: (() -> Unit)? = null

    /** Fired the moment today's goal is reached here or on another device, so
     *  a streak reminder already showing can be withdrawn. */
    var onGoalMet: (() -> Unit)? = null

    private var ownDays: Map<String, ReadingDay> = emptyMap()
    /** Other devices' logs by their Drive file name, as last downloaded. Kept
     *  locally too, so the history stays whole if sync is turned off. */
    private var otherDevices: Map<String, Map<String, ReadingDay>> = emptyMap()

    private var prefs: SharedPreferences? = null
    private val json = Json { ignoreUnknownKeys = true }

    private const val PREFS = "reading_log"
    private const val KEY_OWN = "own"
    private const val KEY_OTHERS = "others"
    private const val KEY_DEVICE = "deviceId"
    private const val KEY_ASKED_PERMISSION = "askedNotificationPermission"

    private data class Active(val id: Long, val place: ReadingPlace, var since: Long)
    private var active: Active? = null
    private var nextSessionId = 0L

    /** Loads the log. Safe to call more than once (the app and a receiver). */
    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        ownDays = p.getString(KEY_OWN, null)?.let {
            runCatching { json.decodeFromString<Map<String, ReadingDay>>(it) }.getOrNull()
        } ?: emptyMap()
        otherDevices = p.getString(KEY_OTHERS, null)?.let {
            runCatching { json.decodeFromString<Map<String, Map<String, ReadingDay>>>(it) }.getOrNull()
        } ?: emptyMap()
        rebuild()
    }

    private fun rebuild() {
        val merged = ownDays.toMutableMap()
        for (log in otherDevices.values) {
            for ((key, day) in log) merged[key] = (merged[key] ?: ReadingDay()) + day
        }
        days = merged
    }

    // MARK: - Sessions

    /** Starts timing [place], closing whatever was timed before. */
    fun begin(place: ReadingPlace): Long {
        flush()
        val id = ++nextSessionId
        active = Active(id, place, System.currentTimeMillis())
        return id
    }

    fun end(id: Long) {
        if (active?.id != id) return
        flush()
        active = null
    }

    /** Credits the time since the last flush to today. Called on a short timer
     *  while a reading screen is up, so the goal registers the moment it is
     *  reached rather than when the screen closes. */
    fun flush(now: Long = System.currentTimeMillis()) {
        val current = active ?: return
        val elapsed = (now - current.since) / 1000.0
        current.since = now
        // A long gap means the timer was starved (the process was frozen
        // without the lifecycle reaching us); don't count it.
        if (elapsed <= 0 || elapsed >= 90) return
        add(elapsed, current.place)
    }

    private fun add(seconds: Double, place: ReadingPlace) {
        val key = dayKey(LocalDate.now())
        val wasMet = (days[key]?.seconds ?: 0.0) >= DAILY_GOAL
        val added = ReadingDay(seconds, mapOf(place.key to seconds))
        ownDays = ownDays + (key to ((ownDays[key] ?: ReadingDay()) + added))
        days = days + (key to ((days[key] ?: ReadingDay()) + added))
        prefs?.edit()?.putString(KEY_OWN, json.encodeToString(ownDays))?.apply()
        onOwnChanged?.invoke()
        if (!wasMet && (days[key]?.seconds ?: 0.0) >= DAILY_GOAL) {
            celebration = currentStreak()
            onGoalMet?.invoke()
        }
    }

    // MARK: - Sync

    /** This install's id; a reinstall starts a fresh log, and the old one's
     *  file keeps its history. */
    val deviceId: String
        get() {
            val p = prefs ?: return "unknown"
            p.getString(KEY_DEVICE, null)?.let { return it }
            val id = UUID.randomUUID().toString()
            p.edit().putString(KEY_DEVICE, id).apply()
            return id
        }

    fun ownJson(): String = json.encodeToString(ownDays)

    fun decode(raw: String): Map<String, ReadingDay>? =
        runCatching { json.decodeFromString<Map<String, ReadingDay>>(raw) }.getOrNull()

    /** Adopts the other devices' logs, as just downloaded. */
    fun applyOtherDevices(others: Map<String, Map<String, ReadingDay>>) {
        if (others == otherDevices) return
        val wasMetToday = metGoal(LocalDate.now())
        otherDevices = others
        prefs?.edit()?.putString(KEY_OTHERS, json.encodeToString(others))?.apply()
        rebuild()
        if (!wasMetToday && metGoal(LocalDate.now())) onGoalMet?.invoke()
    }

    // MARK: - Notification permission

    /** Whether the app has already asked for notification permission on the
     *  first goal met — asked once, like iOS. */
    var askedNotificationPermission: Boolean
        get() = prefs?.getBoolean(KEY_ASKED_PERMISSION, false) ?: false
        set(value) { prefs?.edit()?.putBoolean(KEY_ASKED_PERMISSION, value)?.apply() }

    // MARK: - Days

    private val keyFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun dayKey(date: LocalDate): String = date.format(keyFormat)

    fun day(date: LocalDate): ReadingDay? = days[dayKey(date)]

    fun seconds(date: LocalDate): Double = day(date)?.seconds ?: 0.0

    fun metGoal(date: LocalDate): Boolean = seconds(date) >= DAILY_GOAL

    /** The first day anything was read: days before it are not "missed". */
    val firstDay: LocalDate?
        get() = days.keys.minOrNull()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    // MARK: - Streaks

    /** Consecutive goal days ending today — or ending yesterday while today is
     *  still unread, since today's reading can yet keep it going. */
    fun currentStreak(today: LocalDate = LocalDate.now()): Int {
        var date = if (metGoal(today)) today else today.minusDays(1)
        var count = 0
        while (metGoal(date)) {
            count++
            date = date.minusDays(1)
        }
        return count
    }

    val longestStreak: Int
        get() {
            val goalDays = days.filterValues { it.seconds >= DAILY_GOAL }.keys
                .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sorted()
            var best = 0
            var run = 0
            var previous: LocalDate? = null
            for (date in goalDays) {
                run = if (previous?.plusDays(1) == date) run + 1 else 1
                best = maxOf(best, run)
                previous = date
            }
            return best
        }

    val daysMeetingGoal: Int get() = days.values.count { it.seconds >= DAILY_GOAL }

    val totalSeconds: Double get() = days.values.sumOf { it.seconds }

    /** The division read longest in the reader, over all time. */
    val favouriteDivisionId: String?
        get() {
            val totals = mutableMapOf<String, Double>()
            for (day in days.values) {
                for ((key, seconds) in day.places) {
                    val place = ReadingPlace.fromKey(key)
                    if (place is ReadingPlace.Division) {
                        totals[place.id] = (totals[place.id] ?: 0.0) + seconds
                    }
                }
            }
            return totals.maxByOrNull { it.value }?.key
        }

    /** "1h 20m", "12m", "<1m"; Tamil "1 ம 20 நி". */
    fun durationText(seconds: Double, tamil: Boolean): String {
        val minutes = (seconds / 60).toInt()
        val h = minutes / 60
        val m = minutes % 60
        val hUnit = if (tamil) " ம" else "h"
        val mUnit = if (tamil) " நி" else "m"
        if (minutes == 0) return if (seconds > 0) "<1$mUnit" else "0$mUnit"
        if (h == 0) return "$m$mUnit"
        return if (m == 0) "$h$hUnit" else "$h$hUnit $m$mUnit"
    }
}
