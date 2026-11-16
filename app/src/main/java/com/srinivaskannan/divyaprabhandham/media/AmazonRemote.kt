package com.srinivaskannan.divyaprabhandham.media

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.service.media.MediaBrowserService
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.app.NotificationManagerCompat
import com.srinivaskannan.divyaprabhandham.data.AmazonStart

/**
 * Never reads a notification. Android hands other apps' media sessions only
 * to an app with an enabled notification listener, so this empty one is the
 * key [AmazonRemote] needs — nothing more.
 *
 * The system keeps an enabled listener bound for as long as the phone is on.
 * It is declared in a process of its own (see the manifest) so that what stays
 * alive is this empty service and not the whole app.
 */
class MediaSessionAccess : NotificationListenerService()

/** What Amazon Music's session says is playing, as the pill and the player sheet need it. */
data class NowPlaying(
    val mediaId: String?,
    val title: String?,
    val album: String?,
    val artist: String?,
    /** 0 until the player reports the track's length. */
    val durationMs: Long,
    val artwork: Bitmap?,
    val state: Int,
    val positionMs: Long,
    val positionAt: Long,
    val speed: Float,
    val actions: Long,
) {
    val isPlaying: Boolean get() = state == PlaybackState.STATE_PLAYING

    /** Loaded in the player, playing or not — as opposed to stopped or failed. */
    val isLoaded: Boolean
        get() = state != PlaybackState.STATE_NONE && state != PlaybackState.STATE_STOPPED &&
            state != PlaybackState.STATE_ERROR

    val canSkipNext: Boolean get() = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L
    val canSkipPrevious: Boolean get() = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L
    val canSeek: Boolean get() = actions and PlaybackState.ACTION_SEEK_TO != 0L

    /** The position now, carried forward from the last report while playing. */
    fun livePositionMs(): Long =
        if (isPlaying) positionMs + ((SystemClock.elapsedRealtime() - positionAt) * speed).toLong()
        else positionMs
}

/**
 * A remote control for Amazon Music: the app's mini-player.
 *
 * Amazon Music still does the playing — its streaming API is closed, see
 * docs/mini-player-options-feasibility.md — but with Notification access this
 * can follow its media session, show what is playing, drive play / pause /
 * next / previous, and start a recitation without the listener leaving the
 * app. Everything here was first tried on a phone with the debug probe in
 * app/src/debug/.../remote/, and the comments say what was seen there.
 *
 * How a start works, and why it is this roundabout:
 *  - Amazon Music ignores `playFromUri` and `playFromMediaId`, and a search
 *    for a track's title finds the wrong track for some titles.
 *  - A search for the *album* (exact name and artist) reliably starts it at
 *    its first track, in order — but publishes no queue to jump around in.
 *  - So the track is reached by pressing "next", checking the track ID the
 *    session reports after each press. Each track passed is heard for about
 *    half a second; pausing first does not help, because "next" resumes.
 * Anything unexpected on the way gives up, and the caller hands off to Amazon
 * Music instead ([AmazonMusicLauncher]).
 */
class AmazonRemote(private val context: Context) {

    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val listener = ComponentName(context, MediaSessionAccess::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val prefs = context.getSharedPreferences("amazon_remote", Context.MODE_PRIVATE)

    /** Whether the listener has Notification access. Re-read by [refresh]. */
    var hasAccess by mutableStateOf(false)
        private set

    /**
     * The listener said no to Notification access, so they are not asked on
     * every tap. Kept on this device only: the grant is per device too.
     */
    var declinedAccess by mutableStateOf(prefs.getBoolean(KEY_DECLINED, false))
        private set

    /** What Amazon Music is playing, or null when it has no session. */
    var now by mutableStateOf<NowPlaying?>(null)
        private set

    /** The track ID being started, from the tap until it plays or is given up. */
    var starting by mutableStateOf<String?>(null)
        private set

    /** The track the pill is about: the one being started, else the one loaded. */
    val shownTrack: String?
        get() = starting ?: now?.takeIf { it.isLoaded }?.mediaId

    private var controller: MediaController? = null
    private var watching = false
    private var browser: MediaBrowser? = null

    private var awaitingSession: AmazonStart? = null
    private var pending: Runnable? = null
    private var onStartFailed: (() -> Unit)? = null

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener {
        adopt(it.orEmpty())
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = follow(null)
    }

    // Access

    /** Whether Amazon Music is on the phone. Can change while the app is away, like the grant. */
    var isInstalled by mutableStateOf(installed())
        private set

    private fun installed() = runCatching { context.packageManager.getPackageInfo(player, 0) }.isSuccess

    /** Re-reads the install, the grant and the active sessions. Called whenever the app comes to the front. */
    fun refresh() {
        isInstalled = installed()
        hasAccess = accessGranted()
        if (!hasAccess) {
            stopWatching()
            follow(null)
            return
        }
        // Granted since, from Settings: an earlier "no" no longer stands.
        if (declinedAccess) setDeclined(false)
        try {
            if (!watching) {
                manager.addOnActiveSessionsChangedListener(sessionsChanged, listener, handler)
                watching = true
            }
            adopt(manager.getActiveSessions(listener))
        } catch (_: SecurityException) {
            hasAccess = false
            follow(null)
        }
    }

    fun release() {
        cancelStart()
        stopWatching()
        follow(null)
        browser?.disconnect()
        browser = null
    }

    /**
     * Opens the system screen where Notification access is switched on —
     * this app's own entry where the system has one (Android 11+), the list
     * of apps otherwise.
     */
    fun openAccessSettings(): Boolean {
        val own = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener.flattenToString())
        val all = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        val ownFirst = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && open(own)
        return ownFirst || open(all)
    }

    fun declineAccess() = setDeclined(true)

    private fun setDeclined(declined: Boolean) {
        declinedAccess = declined
        prefs.edit().putBoolean(KEY_DECLINED, declined).apply()
    }

    /** Brings Amazon Music itself to the front: the full player behind the pill. */
    fun openPlayer(): Boolean =
        context.packageManager.getLaunchIntentForPackage(player)?.let(::open) ?: false

    private fun accessGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            context.getSystemService(NotificationManager::class.java)
                .isNotificationListenerAccessGranted(listener)
        } else {
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
        }

    private fun open(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    // Following

    private fun stopWatching() {
        if (watching) manager.removeOnActiveSessionsChangedListener(sessionsChanged)
        watching = false
    }

    private fun adopt(active: List<MediaController>) {
        follow(active.firstOrNull { it.packageName == player })
    }

    private fun follow(next: MediaController?) {
        if (next?.sessionToken == controller?.sessionToken) return
        controller?.unregisterCallback(callback)
        controller = next
        if (next == null) {
            now = null
            return
        }
        next.registerCallback(callback, handler)
        publish()
        awaitingSession?.let { request ->
            awaitingSession = null
            pending?.let(handler::removeCallbacks)
            ask(request)
        }
    }

    private fun publish() {
        val c = controller ?: return
        val metadata = c.metadata
        val state = c.playbackState
        val id = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
        val last = now
        now = NowPlaying(
            mediaId = id,
            title = metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString(),
            album = metadata?.getText(MediaMetadata.METADATA_KEY_ALBUM)?.toString(),
            artist = metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString(),
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            // Every read of the metadata unpacks a fresh copy of the picture:
            // the one already in hand is kept for as long as the track is.
            artwork = last?.artwork?.takeIf { last.mediaId == id }
                ?: ARTWORK_KEYS.firstNotNullOfOrNull { metadata?.getBitmap(it) },
            state = state?.state ?: PlaybackState.STATE_NONE,
            positionMs = state?.position ?: 0L,
            positionAt = state?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = state?.playbackSpeed ?: 1f,
            actions = state?.actions ?: 0L,
        )
    }

    // Transport — the pill's and the player sheet's buttons

    fun playPause() = send { if (now?.isPlaying == true) it.pause() else it.play() }

    fun next() = send { it.skipToNext() }

    /** Amazon Music restarts the track first; pressed again, it goes back one. */
    fun previous() = send { it.skipToPrevious() }

    fun seekTo(positionMs: Long) = send { it.seekTo(positionMs) }

    private fun send(block: (MediaController.TransportControls) -> Unit) {
        val c = controller ?: return
        // A session can be torn down between the report and the press.
        runCatching { block(c.transportControls) }
    }

    // Starting a recitation

    /**
     * Starts [request]'s track without leaving the app. [onFailed] is called
     * if that cannot be done or goes anywhere unexpected, and never after the
     * track has been reached.
     */
    fun start(request: AmazonStart, onFailed: () -> Unit) {
        // A second tap on the same button: asking again would restart the
        // album under the start already on its way.
        if (starting == request.track) return
        cancelStart()
        starting = request.track
        onStartFailed = onFailed
        if (controller != null) ask(request) else wake(request)
    }

    private fun cancelStart() {
        pending?.let(handler::removeCallbacks)
        pending = null
        awaitingSession = null
        onStartFailed = null
        starting = null
    }

    private fun failStart() {
        val failed = onStartFailed
        cancelStart()
        failed?.invoke()
    }

    /**
     * With Amazon Music not running there is no session to send a search to.
     * It refuses a media-browser connection from this app, but asking for one
     * starts it: on the phone its session appeared about a third of a second
     * later, each time, and took the search.
     */
    private fun wake(request: AmazonStart) {
        val service = context.packageManager
            .queryIntentServices(Intent(MediaBrowserService.SERVICE_INTERFACE).setPackage(player), 0)
            .firstOrNull()?.serviceInfo
        if (service == null) {
            failStart()
            return
        }
        awaitingSession = request
        browser?.disconnect()
        browser = MediaBrowser(
            context,
            ComponentName(service.packageName, service.name),
            object : MediaBrowser.ConnectionCallback() {},
            null,
        ).also { it.connect() }
        wait(WAKE_WAIT_MS) { failStart() }
    }

    /** Asks for the album, and waits for it to be the thing playing. */
    private fun ask(request: AmazonStart) {
        val album = request.album
        val before = now
        val asked = SystemClock.elapsedRealtime()
        var nudged = false
        send {
            it.playFromSearch(
                album.name,
                Bundle().apply {
                    putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)
                    putString(MediaStore.EXTRA_MEDIA_ALBUM, album.name)
                    // Without the artist, a search for one album was seen to play another.
                    putString(MediaStore.EXTRA_MEDIA_ARTIST, album.artist)
                },
            )
        }
        poll {
            val waited = SystemClock.elapsedRealtime() - asked
            val n = now
            // A position older than the search is the previous track still going.
            val arrived = n != null && n !== before && n.album == album.name &&
                n.livePositionMs() <= waited
            when {
                // The name alone is not enough: two Iyarpaa albums share one,
                // and another artist's album of the same name was seen to
                // answer a search. Nor is the artist, which Amazon Music was
                // seen to misreport for a track. The album starts at its first
                // track, so the track ID says whether it is the right one.
                arrived && n?.mediaId !in album.tracks -> {
                    failStart()
                    false
                }
                arrived && n?.isPlaying == true -> {
                    walk(request)
                    false
                }
                waited > ALBUM_WAIT_MS -> {
                    failStart()
                    false
                }
                else -> {
                    // After a cold start the album was once seen to load and sit paused.
                    if (arrived && !nudged && waited > NUDGE_AFTER_MS &&
                        n?.state == PlaybackState.STATE_PAUSED
                    ) {
                        nudged = true
                        send { it.play() }
                    }
                    true
                }
            }
        }
    }

    /** Presses "next" until the wanted track is the one the session reports. */
    private fun walk(request: AmazonStart) {
        val order = request.album.tracks
        val target = order.indexOf(request.track)
        var presses = 0
        var pressedOn: String? = null
        var pressedReport: NowPlaying? = null
        var pressedAt = SystemClock.elapsedRealtime()
        poll {
            val n = now
            val id = n?.mediaId
            val since = SystemClock.elapsedRealtime() - pressedAt
            val settled = n != null && (n.isPlaying || n.state == PlaybackState.STATE_PAUSED)
            // Some albums were seen to list a track twice in a row. The same
            // ID then counts as a new track once it has plainly started over
            // since the press: the track pressed on would be further along.
            val again = n != null && id == pressedOn && n !== pressedReport &&
                since > REPEAT_AFTER_MS && n.livePositionMs() < since
            if (n == null || id == null || !settled || (id == pressedOn && !again)) {
                // Still on the track "next" was pressed on, or between tracks.
                val stuck = since > STEP_WAIT_MS
                if (stuck) failStart()
                return@poll !stuck
            }
            val at = order.indexOf(id)
            when {
                // Another album, or past the track: an account that shuffles
                // albums would do this. Not something "next" can fix. A track
                // the list does not know (at < 0) is stepped over like any other.
                n.album != request.album.name || at > target -> failStart()
                at == target -> {
                    if (!n.isPlaying) send { it.play() }
                    cancelStart()
                }
                presses >= order.size + SPARE_PRESSES || !n.canSkipNext -> failStart()
                else -> {
                    presses++
                    pressedOn = id
                    pressedReport = n
                    pressedAt = SystemClock.elapsedRealtime()
                    next()
                    return@poll true
                }
            }
            false
        }
    }

    /** Runs [check] every [POLL_MS] for as long as it returns true. */
    private fun poll(check: () -> Boolean) {
        val look = object : Runnable {
            override fun run() {
                if (pending !== this) return
                if (check() && pending === this) handler.postDelayed(this, POLL_MS)
            }
        }
        pending?.let(handler::removeCallbacks)
        pending = look
        handler.postDelayed(look, POLL_MS)
    }

    private fun wait(ms: Long, then: () -> Unit) {
        val later = Runnable { then() }
        pending?.let(handler::removeCallbacks)
        pending = later
        handler.postDelayed(later, ms)
    }

    companion object {
        /**
         * The player being followed. Only ever Amazon Music in a release
         * build; the debug test screen points it at a silent stand-in so the
         * pill can be exercised on an emulator, which has no Amazon Music.
         */
        var player: String = AmazonMusicLauncher.PKG

        private const val KEY_DECLINED = "declined_access"

        /** Amazon Music was seen to fill the first of these. */
        private val ARTWORK_KEYS = listOf(
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON,
        )

        /** How long Amazon Music gets to show a session after being woken. */
        private const val WAKE_WAIT_MS = 4000L

        /** How long an album search gets to start playing (1.5–3.8 s seen). */
        private const val ALBUM_WAIT_MS = 10_000L
        private const val NUDGE_AFTER_MS = 4000L

        /** How often a start looks at the session, and how long one "next" gets (about 0.5 s seen). */
        private const val POLL_MS = 250L
        private const val STEP_WAIT_MS = 6000L

        /** How long the same track ID is given before it can be a repeated entry. */
        private const val REPEAT_AFTER_MS = 1500L

        /** Presses allowed beyond the album's listed length, for repeated or unlisted tracks. */
        private const val SPARE_PRESSES = 6
    }
}

/** The one [AmazonRemote], provided by the navigation shell. */
val LocalAmazonRemote = staticCompositionLocalOf<AmazonRemote> {
    error("AmazonRemote not provided")
}
