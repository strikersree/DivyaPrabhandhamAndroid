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
 * A user-created collection of pasurams — a playlist, not a folder: it holds
 * individual verse keys (the same "sectionId#index" format bookmarks already
 * use), gathered from any work of any division, in the order they were added.
 * Adding a whole work is a convenience that expands to every pasuram key in
 * it at once — the collection itself never stores a reference to the work,
 * only the tracks. Mirrors the iOS implementation's model exactly.
 */
@Serializable
data class UserCollection(
    val id: String,
    val name: String,
    val pasuramKeys: List<String> = emptyList(),
    val createdAt: Long,
    /** True for the app's own permanent recitation collections (Saththumurai)
     *  — undeletable, unrenamable, and re-synced at each launch so code-side
     *  additions reach devices that already have it. False for anything the
     *  person created themselves. */
    val isBuiltIn: Boolean = false,
)

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

    /** Works pinned to Home (up to [MAX_PINNED_WORKS]). Entries are either a
     *  bare work id, or "collection:<id>" for a pinned collection — see
     *  [pinnedCollectionId]. */
    var pinnedWorks: List<String> by mutableStateOf(snapshot.pinnedWorks)

    /** The user's own collections of pasurams, most-recently-created last. */
    var collections: List<UserCollection> by mutableStateOf(snapshot.collections)
        internal set

    /** Divya Desams the user has visited, mapped to the year of the visit. */
    var visitedDesams: Map<String, Int> by mutableStateOf(snapshot.visitedDesams)
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

    /** Language of the app chrome (menus/labels), independent of the content
     *  script. */
    var uiLanguage: UiLanguage by mutableStateOf(snapshot.uiLanguage)
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

    /** Whether the first-run onboarding has been completed or skipped. Device-
     *  local: not synced, so a new device still onboards. */
    var onboardingComplete: Boolean by mutableStateOf(snapshot.onboardingComplete)
        private set

    /** The chosen launcher-icon variant key ("vadakalai" / "thenkalai").
     *  Device-local: the launcher icon is a per-device component state, not
     *  something to sync. */
    var appIconKey: String by mutableStateOf(snapshot.appIconKey)
        private set

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
    fun ui(key: Ui): String = UiText.string(key, uiLanguage.isEnglish)

    val uiEnglish: Boolean get() = uiLanguage.isEnglish

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

    fun isVisited(desamId: String): Boolean = desamId in visitedDesams

    fun visitYear(desamId: String): Int? = visitedDesams[desamId]

    /** Marks a desam visited in [year]; overwrites the year if already visited. */
    fun markVisited(desamId: String, year: Int) {
        visitedDesams = visitedDesams + (desamId to year)
        commit { it[Keys.VISITED] = encodeVisited(visitedDesams) }
    }

    fun clearVisited(desamId: String) {
        if (desamId !in visitedDesams) return
        visitedDesams = visitedDesams - desamId
        commit { it[Keys.VISITED] = encodeVisited(visitedDesams) }
    }

    /** How many physical Divya Desams have been visited (drives the levels). */
    val visitedCount: Int get() = visitedDesams.size

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

    /** Pin or unpin a collection to Home, sharing the same list and cap as
     *  pinned works — see [pinnedWorks]'s "collection:<id>" convention. */
    fun togglePinCollection(collectionId: String) {
        togglePin(pinnedCollectionEntry(collectionId))
    }

    fun isCollectionPinned(collectionId: String): Boolean =
        pinnedCollectionEntry(collectionId) in pinnedWorks

    // MARK: - Collections

    /** Creates a new, empty collection and returns it so the caller can
     *  navigate straight into it. */
    fun createCollection(name: String): UserCollection {
        val collection = UserCollection(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            createdAt = System.currentTimeMillis(),
        )
        collections = collections + collection
        commit { it[Keys.COLLECTIONS] = json.encodeToString(collections) }
        return collection
    }

    fun renameCollection(collectionId: String, newName: String) {
        val target = collection(collectionId) ?: return
        if (target.isBuiltIn) return
        collections = collections.map {
            if (it.id == collectionId) it.copy(name = newName) else it
        }
        commit { it[Keys.COLLECTIONS] = json.encodeToString(collections) }
    }

    fun deleteCollection(collectionId: String) {
        val target = collection(collectionId) ?: return
        if (target.isBuiltIn) return
        collections = collections.filterNot { it.id == collectionId }
        // A deleted collection can no longer be pinned to Home.
        if (isCollectionPinned(collectionId)) togglePin(pinnedCollectionEntry(collectionId))
        commit { it[Keys.COLLECTIONS] = json.encodeToString(collections) }
    }

    fun collection(collectionId: String): UserCollection? =
        collections.firstOrNull { it.id == collectionId }

    fun collectionsContaining(pasuramKey: String): List<UserCollection> =
        collections.filter { pasuramKey in it.pasuramKeys }

    /** Adds one pasuram to a collection; a no-op if it's already in it. */
    fun addToCollection(collectionId: String, pasuramKey: String) {
        addAllToCollection(collectionId, listOf(pasuramKey))
    }

    /** Adds several pasurams (e.g. every verse in a work) to a collection at
     *  once, skipping any already present, preserving the given order. */
    fun addAllToCollection(collectionId: String, pasuramKeys: List<String>) {
        collections = collections.map { c ->
            if (c.id != collectionId) return@map c
            val toAdd = pasuramKeys.filterNot { it in c.pasuramKeys }
            if (toAdd.isEmpty()) c else c.copy(pasuramKeys = c.pasuramKeys + toAdd)
        }
        commit { it[Keys.COLLECTIONS] = json.encodeToString(collections) }
    }

    fun removeFromCollection(collectionId: String, pasuramKey: String) {
        collections = collections.map { c ->
            if (c.id != collectionId) c else c.copy(pasuramKeys = c.pasuramKeys - pasuramKey)
        }
        commit { it[Keys.COLLECTIONS] = json.encodeToString(collections) }
    }

    /**
     * Seeds a built-in, permanent collection on first run, or merges in any
     * new pasuram keys the current app version knows about on every run
     * after that — "seed-or-sync", not "seed-once", so a code-side addition
     * (e.g. this build adding more entries to Desika Prabhandha
     * Saaththumurai) reaches a device that already has the collection from
     * an earlier version. Never removes a key: if a person somehow has one
     * beyond what's currently seeded, it stays. Call once at startup for
     * each built-in collection.
     */
    fun seedOrSyncBuiltInCollection(id: String, name: String, seedKeys: List<String>) {
        val existing = collection(id)
        if (existing == null) {
            collections = collections + UserCollection(
                id = id,
                name = name,
                pasuramKeys = seedKeys,
                createdAt = System.currentTimeMillis(),
                isBuiltIn = true,
            )
            commit { it[Keys.COLLECTIONS] = json.encodeToString(collections) }
            return
        }
        val missing = seedKeys.filterNot { it in existing.pasuramKeys }
        if (missing.isEmpty()) return
        collections = collections.map {
            if (it.id == id) it.copy(pasuramKeys = it.pasuramKeys + missing) else it
        }
        commit { it[Keys.COLLECTIONS] = json.encodeToString(collections) }
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

    fun updateUiLanguage(value: UiLanguage) {
        if (uiLanguage == value) return
        uiLanguage = value
        commit { it[Keys.UI_LANG] = value.key }
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

    fun completeOnboarding() {
        if (onboardingComplete) return
        onboardingComplete = true
        commit { it[Keys.ONBOARDING] = true }
    }

    fun updateAppIconKey(value: String) {
        if (appIconKey == value) return
        appIconKey = value
        commit { it[Keys.APP_ICON] = value }
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
        prefs[Keys.COLLECTIONS] = json.encodeToString(collections)
        prefs[Keys.VISITED] = encodeVisited(visitedDesams)
        prefs[Keys.THEME] = theme.key
        prefs[Keys.FONT_SIZE] = fontSize.toDouble()
        prefs[Keys.ACCENT] = accentChoice.key
        prefs[Keys.APPEARANCE] = appearance.key
        prefs[Keys.SCRIPT] = scriptChoice.key
        prefs[Keys.UI_LANG] = uiLanguage.key
        prefs[Keys.FONT_FAMILY] = fontChoice.key
        prefs[Keys.WIDGET_AAYIRAM] = widgetAayiram.key
        prefs[Keys.TIP_SILENCED] = tipPromptSilenced
        prefs[Keys.ONBOARDING] = onboardingComplete
        prefs[Keys.APP_ICON] = appIconKey
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
        val collections: List<UserCollection>,
        val visitedDesams: Map<String, Int>,
        val theme: ReaderThemeChoice,
        val fontSize: Float,
        val accent: AccentChoice,
        val appearance: AppearanceChoice,
        val script: ScriptChoice,
        val uiLanguage: UiLanguage,
        val font: FontChoice,
        val notificationsEnabled: Boolean,
        val reminderTimes: List<ReminderTime>,
        val widgetAayiram: WidgetAayiram,
        val syncEnabled: Boolean,
        val supporterSince: Long?,
        val lastTipPrompt: Long?,
        val tipPromptSilenced: Boolean,
        val onboardingComplete: Boolean,
        val appIconKey: String,
        val launchCount: Int,
        val changedAt: Long,
    )

    private object Keys {
        val LAST_READ = stringPreferencesKey("dp.lastRead")
        val BOOKMARKS = stringPreferencesKey("dp.bookmarks")
        val VISITED = stringPreferencesKey("dp.visitedDesams")
        val RECENT = stringPreferencesKey("dp.recentlyViewed")
        val SEARCHES = stringPreferencesKey("dp.recentSearches")
        val PINNED = stringPreferencesKey("dp.pinnedWorks")
        val COLLECTIONS = stringPreferencesKey("dp.collections")
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
        val ONBOARDING = booleanPreferencesKey("dp.onboardingComplete")
        val APP_ICON = stringPreferencesKey("dp.appIcon")
        val UI_LANG = stringPreferencesKey("dp.uiLanguage")
        val LAUNCH_COUNT = intPreferencesKey("dp.launchCount")
        val CHANGED_AT = longPreferencesKey("dp.changedAt")
    }

    companion object {
        const val MAX_PINNED_WORKS = 6

        // A pinned collection shares the same list (and cap) as pinned works,
        // distinguished by this string prefix — mirrors the iOS
        // implementation exactly, which found and fixed the same "a pinned
        // entry isn't always a work" gap in its own Home grid.
        private const val PINNED_COLLECTION_PREFIX = "collection:"

        fun pinnedCollectionEntry(collectionId: String): String = PINNED_COLLECTION_PREFIX + collectionId

        /** The collection id if [pinEntry] is a pinned collection, else null. */
        fun pinnedCollectionId(pinEntry: String): String? =
            pinEntry.takeIf { it.startsWith(PINNED_COLLECTION_PREFIX) }
                ?.removePrefix(PINNED_COLLECTION_PREFIX)
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

        // Visited desams are stored one per line as "id:year".
        private fun encodeVisited(map: Map<String, Int>) =
            map.entries.joinToString("\n") { "${it.key}:${it.value}" }

        private fun decodeVisited(raw: String?): Map<String, Int> =
            raw?.split("\n")?.mapNotNull { line ->
                val i = line.lastIndexOf(':')
                if (i <= 0) return@mapNotNull null
                val id = line.substring(0, i)
                val year = line.substring(i + 1).toIntOrNull() ?: return@mapNotNull null
                id to year
            }?.toMap() ?: emptyMap()

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
                collections = prefs[Keys.COLLECTIONS]?.let {
                    runCatching { json.decodeFromString<List<UserCollection>>(it) }.getOrNull()
                } ?: emptyList(),
                visitedDesams = decodeVisited(prefs[Keys.VISITED]),
                theme = ReaderThemeChoice.from(prefs[Keys.THEME]),
                fontSize = if (storedFontSize >= MIN_FONT_SIZE) storedFontSize else DEFAULT_FONT_SIZE,
                accent = AccentChoice.from(prefs[Keys.ACCENT]),
                appearance = AppearanceChoice.from(prefs[Keys.APPEARANCE]),
                script = ScriptChoice.from(prefs[Keys.SCRIPT]),
                uiLanguage = UiLanguage.from(prefs[Keys.UI_LANG]) ?: run {
                    val freshInstall = prefs.asMap().isEmpty()
                    val script = ScriptChoice.from(prefs[Keys.SCRIPT])
                    when {
                        freshInstall -> UiLanguage.ENGLISH
                        script == ScriptChoice.TAMIL -> UiLanguage.TAMIL
                        else -> UiLanguage.ENGLISH
                    }
                },
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
                onboardingComplete = prefs[Keys.ONBOARDING] ?: false,
                appIconKey = prefs[Keys.APP_ICON] ?: "vadakalai",
                launchCount = prefs[Keys.LAUNCH_COUNT] ?: 0,
                changedAt = prefs[Keys.CHANGED_AT] ?: 0L,
            )
            return AppState(store, scope, snapshot)
        }
    }
}
