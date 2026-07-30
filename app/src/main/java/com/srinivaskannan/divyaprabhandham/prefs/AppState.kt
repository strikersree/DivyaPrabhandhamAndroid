package com.srinivaskannan.divyaprabhandham.prefs

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.data.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "divya_prabhandham")

/** A daily reminder time (local, on device). Up to three are allowed. */
@Serializable
data class ReminderTime(val hour: Int, val minute: Int) {
    fun label(): String = "%02d:%02d".format(hour, minute)
}

/** Where the reader was when the person last navigated away. */
@Serializable
data class LastRead(val sectionId: String, val stanzaKey: String? = null)

/**
 * Everything that should survive relaunch: reading position, bookmarks,
 * recently viewed sections, pins, theme and font settings.
 *
 * Reads are plain Compose state, so screens use this exactly the way they used
 * the iOS `@Observable` object. Writes go to DataStore on a background scope —
 * fire and forget, last write wins, the same contract UserDefaults gave.
 *
 * Two mutation paths exist and the difference matters:
 *
 *  - The public `updateX` / `toggleX` functions are local edits. They persist,
 *    stamp [changedAt], and fire [onChanged] so the sync manager and the widget
 *    can react. They are named `update` rather than `set` on purpose: a
 *    `setFoo(value)` beside a `var foo` with a non-public setter is the same
 *    JVM signature, which is a platform declaration clash. The properties whose
 *    setters are `internal` happen to escape it through name mangling, but
 *    relying on that would be a trap for the next property added here.
 *  - [applyRemote] adopts state that arrived from another device. It persists
 *    but deliberately does *not* fire [onChanged], which is what stops a pulled
 *    change from being pushed straight back up as though it were local.
 */
class AppState private constructor(
    private val store: DataStore<Preferences>,
    private val scope: CoroutineScope,
    snapshot: Snapshot,
) {

    /**
     * Fired after any local change. The app wires this to the sync manager and
     * the widget; nothing inside AppState knows about either.
     */
    var onChanged: (() -> Unit)? = null

    // MARK: - Reading position and collections

    var lastRead: LastRead? by mutableStateOf(snapshot.lastRead)
        internal set

    /** Bookmarked stanza keys, most-recently-bookmarked first. */
    var bookmarks: List<String> by mutableStateOf(snapshot.bookmarks)
        internal set

    var recentlyViewed: List<String> by mutableStateOf(snapshot.recentlyViewed)

    /** The last few text search queries, most-recent-first. Device-local. */
    var recentSearches: List<String> by mutableStateOf(snapshot.recentSearches)
        internal set

    /** Works pinned to Home (up to [MAX_PINNED_WORKS]). */
    var pinnedWorks: List<String> by mutableStateOf(snapshot.pinnedWorks)
        internal set

    // MARK: - Appearance

    var theme: ReaderThemeChoice by mutableStateOf(snapshot.theme)
        internal set

    var fontSize: Float by mutableStateOf(snapshot.fontSize)
        internal set

    var accentChoice: AccentChoice by mutableStateOf(snapshot.accent)
        internal set

    var appearance: AppearanceChoice by mutableStateOf(snapshot.appearance)
        internal set

    var scriptChoice: ScriptChoice by mutableStateOf(snapshot.script)
        internal set

    var fontChoice: FontChoice by mutableStateOf(snapshot.font)
        internal set

    // MARK: - Reminders, widget, sync, tips

    var notificationsEnabled: Boolean by mutableStateOf(snapshot.notificationsEnabled)
        private set

    var reminderTimes: List<ReminderTime> by mutableStateOf(snapshot.reminderTimes)
        private set

    var widgetAayiram: WidgetAayiram by mutableStateOf(snapshot.widgetAayiram)
        internal set

    /** Whether reading state is mirrored to the person's Google account. */
    var syncEnabled: Boolean by mutableStateOf(snapshot.syncEnabled)
        private set

    var supporterSince: Long? by mutableStateOf(snapshot.supporterSince)
        internal set

    var lastTipPrompt: Long? by mutableStateOf(snapshot.lastTipPrompt)
        private set

    var tipPromptSilenced: Boolean by mutableStateOf(snapshot.tipPromptSilenced)
        internal set

    var launchCount: Int by mutableStateOf(snapshot.launchCount)
        private set

    /**
     * When the synced portion of this state last changed, local or remote.
     * This is the version stamp for last-writer-wins, not a display value.
     */
    var changedAt: Long by mutableStateOf(snapshot.changedAt)
        internal set

    // MARK: - Derived

    /**
     * Whether the app should render dark. Null means "follow the system",
     * which is what High Contrast does — it changes the palette, not the
     * light/dark decision.
     */
    val forcedDarkMode: Boolean?
        get() = when (appearance) {
            AppearanceChoice.AUTO -> theme.isDark
            AppearanceChoice.LIGHT -> false
            AppearanceChoice.DARK -> true
            AppearanceChoice.HIGH_CONTRAST -> null
        }

    val isHighContrast: Boolean get() = appearance == AppearanceChoice.HIGH_CONTRAST

    /** UI-chrome string for the current script. */
    fun ui(key: Ui): String = UiText.string(key, scriptChoice)

    fun isBookmarked(key: String): Boolean = key in bookmarks

    val isSupporter: Boolean get() = supporterSince != null

    fun isPinned(workId: String): Boolean = workId in pinnedWorks

    /** Whether another work can still be pinned. */
    val canPinMore: Boolean get() = pinnedWorks.size < MAX_PINNED_WORKS

    /**
     * Whether to show the tip reminder on this launch. Deliberately gentle:
     * never before the app has been used a while, never more than once a week,
     * never again once silenced or once a tip has been left.
     */
    val shouldShowTipPrompt: Boolean
        get() {
            if (isSupporter || tipPromptSilenced) return false
            if (launchCount < 5) return false
            val last = lastTipPrompt ?: return true
            return System.currentTimeMillis() - last >= 7L * 24 * 60 * 60 * 1000
        }

    // MARK: - Local mutations

    fun updateLastRead(value: LastRead?) {
        if (value == lastRead) return
        lastRead = value
        commit { prefs ->
            if (value == null) prefs.remove(Keys.LAST_READ)
            else prefs[Keys.LAST_READ] = json.encodeToString(value)
        }
    }

    fun toggleBookmark(key: String) {
        val list = bookmarks.toMutableList()
        if (!list.remove(key)) list.add(0, key)
        bookmarks = list
        commit { it[Keys.BOOKMARKS] = encodeList(list) }
    }

    fun removeBookmark(key: String) {
        if (key in bookmarks) toggleBookmark(key)
    }

    /**
     * Pin or unpin a work. Pinning is capped at [MAX_PINNED_WORKS]; attempts
     * beyond the cap are ignored rather than evicting an existing pin, which
     * would be a surprising thing for a tap to do.
     */
    fun togglePin(workId: String) {
        val list = pinnedWorks.toMutableList()
        if (!list.remove(workId)) {
            if (list.size >= MAX_PINNED_WORKS) return
            list.add(workId)
        }
        pinnedWorks = list
        commit { it[Keys.PINNED] = encodeList(list) }
    }

    /**
     * Records that a section was opened, keeping the most recent few,
     * most-recent-first, with no duplicates.
     */
    fun noteVisited(sectionId: String) {
        if (recentlyViewed.firstOrNull() == sectionId) return
        val list = recentlyViewed.filter { it != sectionId }.toMutableList()
        list.add(0, sectionId)
        while (list.size > MAX_RECENT) list.removeAt(list.size - 1)
        recentlyViewed = list
        commit { it[Keys.RECENT] = encodeList(list) }
    }

    /**
     * Records a text search query, keeping the last [MAX_SEARCHES],
     * most-recent-first, no duplicates. Device-local (persist, not commit): a
     * search history is personal scratch and has no business syncing between
     * devices. Blank queries and bare pasuram numbers are not recorded — the
     * caller filters those, since a number is a jump, not a search.
     */
    fun noteSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        if (recentSearches.firstOrNull() == q) return
        val list = recentSearches.filter { it != q }.toMutableList()
        list.add(0, q)
        while (list.size > MAX_SEARCHES) list.removeAt(list.size - 1)
        recentSearches = list
        persist { it[Keys.SEARCHES] = encodeList(list) }
    }

    fun clearRecentSearches() {
        recentSearches = emptyList()
        persist { it[Keys.SEARCHES] = "" }
    }

    fun updateTheme(value: ReaderThemeChoice) {
        theme = value
        commit { it[Keys.THEME] = value.key }
    }

    fun updateFontSize(value: Float) {
        fontSize = value.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        commit { it[Keys.FONT_SIZE] = fontSize.toDouble() }
    }

    fun updateAccent(value: AccentChoice) {
        accentChoice = value
        commit { it[Keys.ACCENT] = value.key }
    }

    fun updateAppearance(value: AppearanceChoice) {
        appearance = value
        commit { it[Keys.APPEARANCE] = value.key }
    }

    fun updateScript(value: ScriptChoice) {
        scriptChoice = value
        commit { it[Keys.SCRIPT] = value.key }
    }

    fun updateFontChoice(value: FontChoice) {
        fontChoice = value
        commit { it[Keys.FONT_FAMILY] = value.key }
    }

    fun updateWidgetAayiram(value: WidgetAayiram) {
        widgetAayiram = value
        commit { it[Keys.WIDGET_AAYIRAM] = value.key }
    }

    fun updateSyncEnabled(value: Boolean) {
        syncEnabled = value
        commit { it[Keys.SYNC] = value }
    }

    /**
     * Note a completed tip. Keeps the earliest date, so "Supporter since"
     * reflects the first contribution rather than the most recent.
     */
    fun recordTip() {
        if (supporterSince != null) return
        val now = System.currentTimeMillis()
        supporterSince = now
        commit { it[Keys.SUPPORTER_SINCE] = now }
    }

    fun silenceTipPrompt() {
        tipPromptSilenced = true
        commit { it[Keys.TIP_SILENCED] = true }
    }

    // Device-local only. Reminders are per-device (both the permission and the
    // scheduling are), and the launch/prompt counters describe this install.
    // None of these stamp changedAt or reach the network.

    fun updateNotificationsEnabled(value: Boolean) {
        notificationsEnabled = value
        persist { it[Keys.NOTIFY_ENABLED] = value }
    }

    fun updateReminderTimes(value: List<ReminderTime>) {
        reminderTimes = value.take(MAX_REMINDERS)
        persist { it[Keys.NOTIFY_TIMES] = json.encodeToString(reminderTimes) }
    }

    fun noteTipPromptShown() {
        val now = System.currentTimeMillis()
        lastTipPrompt = now
        persist { it[Keys.LAST_TIP_PROMPT] = now }
    }

    fun noteLaunch() {
        val next = launchCount + 1
        launchCount = next
        persist { it[Keys.LAUNCH_COUNT] = next }
    }

    // MARK: - Remote mutations

    /**
     * Adopts state pulled from another device. The block assigns properties
     * directly; everything is then written in a single DataStore edit, and
     * [onChanged] is deliberately not fired.
     */
    fun applyRemote(block: AppState.() -> Unit) {
        block()
        scope.launch { store.edit { writeAll(it) } }
    }

    // MARK: - Persistence

    /** A local change: stamp the version, persist, notify listeners. */
    private fun commit(block: (MutablePreferences) -> Unit) {
        val now = System.currentTimeMillis()
        changedAt = now
        scope.launch {
            store.edit {
                block(it)
                it[Keys.CHANGED_AT] = now
            }
        }
        onChanged?.invoke()
    }

    /** A device-local change: persist only. */
    private fun persist(block: (MutablePreferences) -> Unit) {
        scope.launch { store.edit(block) }
    }

    private fun writeAll(prefs: MutablePreferences) {
        prefs[Keys.BOOKMARKS] = encodeList(bookmarks)
        prefs[Keys.RECENT] = encodeList(recentlyViewed)
        prefs[Keys.PINNED] = encodeList(pinnedWorks)
        prefs[Keys.THEME] = theme.key
        prefs[Keys.FONT_SIZE] = fontSize.toDouble()
        prefs[Keys.ACCENT] = accentChoice.key
        prefs[Keys.APPEARANCE] = appearance.key
        prefs[Keys.SCRIPT] = scriptChoice.key
        prefs[Keys.FONT_FAMILY] = fontChoice.key
        prefs[Keys.WIDGET_AAYIRAM] = widgetAayiram.key
        prefs[Keys.TIP_SILENCED] = tipPromptSilenced
        prefs[Keys.CHANGED_AT] = changedAt
        supporterSince?.let { prefs[Keys.SUPPORTER_SINCE] = it }
        val read = lastRead
        if (read == null) prefs.remove(Keys.LAST_READ)
        else prefs[Keys.LAST_READ] = json.encodeToString(read)
    }

    // MARK: - Loading

    /** The values read once at startup, before any Compose state exists. */
    data class Snapshot(
        val lastRead: LastRead?,
        val bookmarks: List<String>,
        val recentlyViewed: List<String>,
        val recentSearches: List<String>,
        val pinnedWorks: List<String>,
        val theme: ReaderThemeChoice,
        val fontSize: Float,
        val accent: AccentChoice,
        val appearance: AppearanceChoice,
        val script: ScriptChoice,
        val font: FontChoice,
        val notificationsEnabled: Boolean,
        val reminderTimes: List<ReminderTime>,
        val widgetAayiram: WidgetAayiram,
        val syncEnabled: Boolean,
        val supporterSince: Long?,
        val lastTipPrompt: Long?,
        val tipPromptSilenced: Boolean,
        val launchCount: Int,
        val changedAt: Long,
    )

    private object Keys {
        val LAST_READ = stringPreferencesKey("dp.lastRead")
        val BOOKMARKS = stringPreferencesKey("dp.bookmarks")
        val RECENT = stringPreferencesKey("dp.recentlyViewed")
        val SEARCHES = stringPreferencesKey("dp.recentSearches")
        val PINNED = stringPreferencesKey("dp.pinnedWorks")
        val THEME = stringPreferencesKey("dp.theme")
        val FONT_SIZE = doublePreferencesKey("dp.fontSize")
        val ACCENT = stringPreferencesKey("dp.accent")
        val APPEARANCE = stringPreferencesKey("dp.appearance")
        val SCRIPT = stringPreferencesKey("dp.script")
        val FONT_FAMILY = stringPreferencesKey("dp.fontFamily")
        val NOTIFY_ENABLED = booleanPreferencesKey("dp.notifyEnabled")
        val NOTIFY_TIMES = stringPreferencesKey("dp.notifyTimes")
        val WIDGET_AAYIRAM = stringPreferencesKey("dp.widgetAayiram")
        val SYNC = booleanPreferencesKey("dp.sync")
        val SUPPORTER_SINCE = longPreferencesKey("dp.supporterSince")
        val LAST_TIP_PROMPT = longPreferencesKey("dp.lastTipPrompt")
        val TIP_SILENCED = booleanPreferencesKey("dp.tipSilenced")
        val LAUNCH_COUNT = intPreferencesKey("dp.launchCount")
        val CHANGED_AT = longPreferencesKey("dp.changedAt")
    }

    companion object {
        const val MAX_PINNED_WORKS = 6
        const val MAX_REMINDERS = 3
        const val MAX_RECENT = 8
        const val MAX_SEARCHES = 5
        const val MIN_FONT_SIZE = 15f
        const val MAX_FONT_SIZE = 34f
        const val DEFAULT_FONT_SIZE = 20f

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * String lists are stored newline-joined rather than as DataStore's
         * string-set, because order carries meaning here: bookmarks are
         * newest-first and pins keep the order they were added.
         */
        private fun encodeList(list: List<String>) = list.joinToString("\n")

        private fun decodeList(raw: String?): List<String> =
            raw?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

        suspend fun create(context: Context, scope: CoroutineScope): AppState {
            val store = context.applicationContext.dataStore
            val prefs = store.data.first()
            val storedFontSize = prefs[Keys.FONT_SIZE]?.toFloat() ?: DEFAULT_FONT_SIZE

            val snapshot = Snapshot(
                lastRead = prefs[Keys.LAST_READ]?.let {
                    runCatching { json.decodeFromString<LastRead>(it) }.getOrNull()
                },
                bookmarks = decodeList(prefs[Keys.BOOKMARKS]),
                recentlyViewed = decodeList(prefs[Keys.RECENT]),
                recentSearches = decodeList(prefs[Keys.SEARCHES]),
                pinnedWorks = decodeList(prefs[Keys.PINNED]),
                theme = ReaderThemeChoice.from(prefs[Keys.THEME]),
                fontSize = if (storedFontSize >= MIN_FONT_SIZE) storedFontSize else DEFAULT_FONT_SIZE,
                accent = AccentChoice.from(prefs[Keys.ACCENT]),
                appearance = AppearanceChoice.from(prefs[Keys.APPEARANCE]),
                script = ScriptChoice.from(prefs[Keys.SCRIPT]),
                font = FontChoice.from(prefs[Keys.FONT_FAMILY]),
                notificationsEnabled = prefs[Keys.NOTIFY_ENABLED] ?: false,
                reminderTimes = prefs[Keys.NOTIFY_TIMES]?.let {
                    runCatching { json.decodeFromString<List<ReminderTime>>(it) }.getOrNull()
                } ?: emptyList(),
                widgetAayiram = WidgetAayiram.from(prefs[Keys.WIDGET_AAYIRAM]),
                syncEnabled = prefs[Keys.SYNC] ?: false,
                supporterSince = prefs[Keys.SUPPORTER_SINCE],
                lastTipPrompt = prefs[Keys.LAST_TIP_PROMPT],
                tipPromptSilenced = prefs[Keys.TIP_SILENCED] ?: false,
                launchCount = prefs[Keys.LAUNCH_COUNT] ?: 0,
                changedAt = prefs[Keys.CHANGED_AT] ?: 0L,
            )
            return AppState(store, scope, snapshot)
        }
    }
}
