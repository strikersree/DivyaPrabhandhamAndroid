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
     * person has not granted it. When consent is needed, [pendingConsent] is
     * set and null is returned; the caller should simply give up for now and
     * let the UI drive the grant.
     */
    private suspend fun accessToken(): String? {
        cachedToken?.let { return it }
        val result = authorize() ?: return null
        if (result.hasResolution()) {
            pendingConsent = result.pendingIntent
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
        if (!granted) return
        cachedToken = null
        pull(appState)
    }

    /**
     * Pulls the remote document and adopts it if it is newer than what this
     * device has. Safe to call on every launch and resume.
     */
    fun pull(appState: AppState) {
        if (!appState.syncEnabled) return
        scope.launch {
            isSyncing = true
            try {
                val token = accessToken() ?: return@launch
                val fileId = cachedFileId ?: DriveAppData.findFileId(token)?.also {
                    cachedFileId = it
                } ?: run {
                    // Nothing remote yet: seed it from this device.
                    cachedFileId = DriveAppData.create(token, snapshot(appState))
                    lastSyncedAt = System.currentTimeMillis()
                    return@launch
                }
                val remote = DriveAppData.download(token, fileId) ?: return@launch
                if (remote.updatedAt > appState.changedAt) {
                    apply(remote, appState)
                }
                lastSyncedAt = System.currentTimeMillis()
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
            val token = accessToken() ?: return@launch
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
        cachedToken = null
        cachedFileId = null
        pushJob?.cancel()
        lastSyncedAt = null
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
