package com.srinivaskannan.divyaprabhandham.sync

import android.app.PendingIntent
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.srinivaskannan.divyaprabhandham.prefs.AccentChoice
import com.srinivaskannan.divyaprabhandham.prefs.AppState
import com.srinivaskannan.divyaprabhandham.prefs.AppearanceChoice
import com.srinivaskannan.divyaprabhandham.prefs.FontChoice
import com.srinivaskannan.divyaprabhandham.prefs.LastRead
import com.srinivaskannan.divyaprabhandham.prefs.ReaderThemeChoice
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import com.srinivaskannan.divyaprabhandham.prefs.WidgetAayiram
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Keeps reading state in step across a person's Android devices, through their
 * own Google account.
 *
 * This replaces the iOS build's NSUbiquitousKeyValueStore mirroring, and the
 * shape is deliberately the same: a small document, last-writer-wins, pushed
 * on change and pulled on launch. What differs is that Android has no ambient
 * per-account key-value store, so the document lives in the hidden
 * `appDataFolder` of the user's Drive and needs an OAuth grant for the
 * `drive.appdata` scope — nothing else, and nothing readable by anyone but
 * this app.
 *
 * SETUP REQUIRED (see README): a Google Cloud project with the Drive API
 * enabled and an Android OAuth client registered against this app's package
 * name and signing certificate. No client ID goes in the source — the grant is
 * matched on package + signature. Until that exists, [authorize] fails
 * silently and the app carries on entirely offline, which is the intended
 * degraded state rather than an error.
 */
class GoogleSyncManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {

    /** Whether a sync is currently in flight, for the Settings row. */
    var isSyncing by mutableStateOf(false)
        private set

    /** When the last successful sync completed, or null if never. */
    var lastSyncedAt by mutableStateOf<Long?>(null)
        private set

    /**
     * The outcome of the last sync attempt, for the Settings row. Distinguishes
     * "signed in but the sync never actually completed" — previously invisible,
     * because every failure resolved to null and was swallowed. [SyncStatus.Idle]
     * until the first attempt.
     */
    var status by mutableStateOf<SyncStatus>(SyncStatus.Idle)
        private set

    /**
     * Set when Google needs the person to approve the Drive scope. The UI
     * launches this and calls [onConsentResult] with the outcome.
     */
    var pendingConsent by mutableStateOf<PendingIntent?>(null)
        private set

    private var cachedToken: String? = null
    private var cachedFileId: String? = null
    private var pushJob: Job? = null

    private val request = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
        .build()

    /**
     * Obtains an access token for the Drive appdata scope, or null if the
     * person has not granted it.
     *
     * [interactive] is the important argument. Google's account chooser may
     * only be raised by something the person just did — turning sync on, or
     * tapping Sync now. Background work (the pull on every resume, the debounced
     * push while reading) asks silently and gives up quietly if the grant is
     * missing. Without that split, every return to the app threw up a sign-in
     * sheet, over and over, whether or not they had already signed in.
     */
    private suspend fun accessToken(interactive: Boolean): String? {
        cachedToken?.let { return it }
        val result = authorize() ?: return null
        if (result.hasResolution()) {
            if (interactive) pendingConsent = result.pendingIntent
            return null
        }
        cachedToken = result.accessToken
        return cachedToken
    }

    private suspend fun authorize(): AuthorizationResult? =
        suspendCancellableCoroutine { continuation ->
            Identity.getAuthorizationClient(context)
                .authorize(request)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resume(null) }
        }

    /** Called by the UI once the consent activity returns. */
    fun onConsentResult(granted: Boolean, appState: AppState) {
        pendingConsent = null
        cachedToken = null
        if (!granted) return
        // Silent on purpose. If the grant still is not usable — an OAuth client
        // that was never registered, say — this must not bounce straight back
        // into the account chooser.
        pull(appState, interactive = false)
    }

    /**
     * Pulls the remote document and adopts it if it is newer than what this
     * device has. Safe to call on every launch and resume, which is exactly why
     * [interactive] defaults to false.
     */
    fun pull(appState: AppState, interactive: Boolean = false) {
        if (!appState.syncEnabled) return
        scope.launch {
            isSyncing = true
            try {
                val token = accessToken(interactive)
                if (token == null) {
                    // No token and no error means consent is pending; the UI
                    // shows that via pendingConsent. Otherwise the grant is
                    // unusable — most often an OAuth client / SHA-1 that is not
                    // registered for this build — and that must be visible
                    // rather than looking like an idle "signed in" state.
                    status = if (pendingConsent != null) SyncStatus.NeedsConsent
                    else SyncStatus.Failed
                    return@launch
                }
                val fileId = cachedFileId ?: DriveAppData.findFileId(token)?.also {
                    cachedFileId = it
                } ?: run {
                    // Nothing remote yet: seed it from this device.
                    val created = DriveAppData.create(token, snapshot(appState))
                    if (created == null) {
                        status = SyncStatus.Failed
                        return@launch
                    }
                    cachedFileId = created
                    lastSyncedAt = System.currentTimeMillis()
                    status = SyncStatus.Synced
                    return@launch
                }
                val remote = DriveAppData.download(token, fileId)
                if (remote == null) {
                    status = SyncStatus.Failed
                    return@launch
                }
                if (remote.updatedAt > appState.changedAt) {
                    apply(remote, appState)
                }
                lastSyncedAt = System.currentTimeMillis()
                status = SyncStatus.Synced
            } finally {
                isSyncing = false
            }
        }
    }

    /**
     * Schedules a push. Debounced, because reading generates a write on every
     * verse that scrolls past and pushing each one would be absurd.
     */
    fun schedulePush(appState: AppState) {
        if (!appState.syncEnabled) return
        pushJob?.cancel()
        pushJob = scope.launch {
            delay(PUSH_DEBOUNCE_MS)
            val token = accessToken(interactive = false) ?: return@launch
            val payload = snapshot(appState)
            val fileId = cachedFileId ?: DriveAppData.findFileId(token)
            if (fileId == null) {
                cachedFileId = DriveAppData.create(token, payload)
            } else {
                cachedFileId = fileId
                DriveAppData.update(token, fileId, payload)
            }
            lastSyncedAt = System.currentTimeMillis()
        }
    }

    /** Forgets local credentials. The remote document is left alone. */
    fun disconnect() {
        pendingConsent = null
        cachedToken = null
        cachedFileId = null
        pushJob?.cancel()
        lastSyncedAt = null
        status = SyncStatus.Idle
    }

    private fun snapshot(appState: AppState) = SyncPayload(
        updatedAt = appState.changedAt,
        bookmarks = appState.bookmarks,
        recentlyViewed = appState.recentlyViewed,
        pinnedWorks = appState.pinnedWorks,
        lastReadSectionId = appState.lastRead?.sectionId,
        lastReadStanzaKey = appState.lastRead?.stanzaKey,
        theme = appState.theme.key,
        fontSize = appState.fontSize,
        accent = appState.accentChoice.key,
        appearance = appState.appearance.key,
        script = appState.scriptChoice.key,
        fontFamily = appState.fontChoice.key,
        widgetAayiram = appState.widgetAayiram.key,
        supporterSince = appState.supporterSince,
        tipPromptSilenced = appState.tipPromptSilenced,
    )

    /**
     * Adopts a remote document. Applied through [AppState.applyRemote] so the
     * writes do not each schedule another push back up — the classic sync echo.
     */
    private fun apply(payload: SyncPayload, appState: AppState) {
        appState.applyRemote {
            bookmarks = payload.bookmarks
            recentlyViewed = payload.recentlyViewed
            pinnedWorks = payload.pinnedWorks
            lastRead = payload.lastReadSectionId?.let {
                LastRead(it, payload.lastReadStanzaKey)
            }
            theme = ReaderThemeChoice.from(payload.theme)
            payload.fontSize?.let { fontSize = it }
            accentChoice = AccentChoice.from(payload.accent)
            appearance = AppearanceChoice.from(payload.appearance)
            scriptChoice = ScriptChoice.from(payload.script)
            fontChoice = FontChoice.from(payload.fontFamily)
            widgetAayiram = WidgetAayiram.from(payload.widgetAayiram)
            supporterSince = payload.supporterSince
            tipPromptSilenced = payload.tipPromptSilenced
            changedAt = payload.updatedAt
        }
    }

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        /**
         * Long enough that a reading session produces one push rather than
         * hundreds, short enough that closing the app right after bookmarking
         * still gets the bookmark up.
         */
        private const val PUSH_DEBOUNCE_MS = 4_000L
    }
}

/**
 * The visible outcome of the last sync attempt. Exists so that "signed in but
 * nothing happened" — the failure mode where an unregistered OAuth client or an
 * unusable grant made every call quietly return null — is no longer
 * indistinguishable from a healthy idle state.
 */
enum class SyncStatus {
    /** No attempt yet, or sync disabled. */
    Idle,

    /** Waiting for the person to approve the Drive scope. */
    NeedsConsent,

    /** Last attempt completed; lastSyncedAt holds when. */
    Synced,

    /**
     * Signed in, but the sync itself failed — most often an OAuth client or
     * SHA-1 fingerprint not registered in the Cloud project for this build, or
     * the Drive scope not granted. Distinct from Idle so the UI can say so.
     */
    Failed,
}
