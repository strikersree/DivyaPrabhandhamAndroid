package com.srinivaskannan.divyaprabhandham.remote

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.media.MediaBrowserService
import android.util.Log
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import com.srinivaskannan.divyaprabhandham.media.MediaSessionAccess
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What the followed session says is playing, as the pill needs it. */
data class NowPlaying(
    val packageName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val mediaId: String?,
    val durationMs: Long,
    val state: Int,
    val queueItemId: Long,
    val positionMs: Long,
    val positionAt: Long,
    val speed: Float,
    val actions: Long,
) {
    val isPlaying: Boolean get() = state == PlaybackState.STATE_PLAYING

    /** The position now, carried forward from the last report while playing. */
    fun livePositionMs(): Long =
        if (isPlaying) positionMs + ((SystemClock.elapsedRealtime() - positionAt) * speed).toLong()
        else positionMs
}

/** One entry of the player's queue; [id] is what skipToQueueItem takes. */
data class QueueEntry(val id: Long, val title: String)

/**
 * One track to ask Amazon Music for: [asin] recognises it once it plays, and
 * [title] with [extras] is the structured search that starts it.
 */
class PlayRequest(val label: String, val asin: String, val title: String, val extras: Bundle)

/**
 * Debug-only spike for the remote-control pill: follows Amazon Music's media
 * session (any other player stands in when Amazon Music has none, so the
 * mechanism can be checked on an emulator), mirrors it, drives it, and keeps a
 * log of everything the session reports. The log is the deliverable — it
 * answers what docs/mini-player-options-feasibility.md leaves open.
 */
class RemoteProbe(private val context: Context) {

    private val manager = context.getSystemService(MediaSessionManager::class.java)
    // The app's own listener, so there is one entry in Settings and one grant.
    private val listener = ComponentName(context, MediaSessionAccess::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val logFile = File(context.filesDir, "remote-probe.log")
    private val clock = SimpleDateFormat("HH:mm:ss", Locale.US)

    var hasAccess by mutableStateOf(false)
        private set
    var sessions by mutableStateOf(emptyList<String>())
        private set
    var now by mutableStateOf<NowPlaying?>(null)
        private set
    var queue by mutableStateOf(emptyList<QueueEntry>())
        private set
    var collecting by mutableStateOf(false)
        private set

    var checking by mutableStateOf(false)
        private set

    var stepping by mutableStateOf(false)
        private set

    /** What is left of a play-in-order run; the first entry is the one playing. */
    var chain by mutableStateOf(emptyList<PlayRequest>())
        private set

    /** Track ID and title of everything the session has reported, in the order seen. */
    val seen = mutableStateListOf<Pair<String, String>>()

    /** Survives the process being killed while Amazon Music is in front. */
    val log = mutableStateListOf<String>().apply {
        if (logFile.exists()) addAll(logFile.readLines())
    }

    val listenerComponent: ComponentName get() = listener

    private var controller: MediaController? = null
    private var watching = false
    private var browser: MediaBrowser? = null
    private var pendingStep: Runnable? = null

    private var chainHeard = false
    private var chainNearEnd = false
    private var chainAskedAt = 0L
    private var pendingJump: Runnable? = null
    private var pendingWalk: Runnable? = null
    private val chainTick = object : Runnable {
        override fun run() {
            chainCheck("tick")
            if (chain.isNotEmpty()) handler.postDelayed(this, CHAIN_TICK_MS)
        }
    }

    private var pendingStart: PlayRequest? = null
    private var pendingSince = 0L
    private var wakeStep = 0
    private val wakeNext = Runnable { wake() }

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener {
        adopt(it.orEmpty())
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            note("metadata → ${describe(metadata)}")
            publish()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            note("state → ${describe(state)}")
            publish()
        }

        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {
            note("queue → ${describe(queue)}")
            publish()
        }

        override fun onSessionDestroyed() {
            note("session destroyed")
            follow(null)
        }
    }

    // Following

    /** Re-reads the grant and the active sessions. Safe to call repeatedly. */
    fun refresh() {
        val granted = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        if (granted != hasAccess) note("notification access: ${if (granted) "granted" else "not granted"}")
        hasAccess = granted
        if (!granted) {
            adopt(emptyList())
            return
        }
        try {
            if (!watching) {
                manager.addOnActiveSessionsChangedListener(sessionsChanged, listener, handler)
                watching = true
            }
            adopt(manager.getActiveSessions(listener))
        } catch (e: SecurityException) {
            note("getActiveSessions refused: ${e.message}")
        }
    }

    fun release() {
        stopCollecting()
        stopChecking()
        stopStepping()
        stopChain()
        handler.removeCallbacksAndMessages(null)
        if (watching) manager.removeOnActiveSessionsChangedListener(sessionsChanged)
        watching = false
        controller?.unregisterCallback(callback)
        browser?.disconnect()
    }

    private fun adopt(active: List<MediaController>) {
        val names = active.map { it.packageName }
        if (names != sessions) note("active sessions: ${names.ifEmpty { listOf("none") }}")
        sessions = names
        // A controller obtained through the browser route does not depend on
        // the listener, so losing the session list must not drop it.
        if (active.isEmpty() && browser?.isConnected == true) return
        follow(active.firstOrNull { it.packageName == AMAZON } ?: active.firstOrNull())
    }

    private fun follow(next: MediaController?) {
        if (next?.sessionToken == controller?.sessionToken) return
        controller?.unregisterCallback(callback)
        controller = next
        if (next == null) {
            now = null
            queue = emptyList()
            return
        }
        next.registerCallback(callback, handler)
        note("following ${next.packageName}")
        dump()
        if (next.packageName == AMAZON) pendingStart?.let {
            note("cold start: session appeared ${SystemClock.elapsedRealtime() - pendingSince} ms" +
                " after asking, on wake step $wakeStep")
            deliver(it)
        }
    }

    private fun publish() {
        val c = controller ?: return
        val metadata = c.metadata
        val state = c.playbackState
        now = NowPlaying(
            packageName = c.packageName,
            title = metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()
                ?: metadata?.description?.title?.toString(),
            artist = metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString()
                ?: metadata?.description?.subtitle?.toString(),
            album = metadata?.getText(MediaMetadata.METADATA_KEY_ALBUM)?.toString(),
            mediaId = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID),
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            state = state?.state ?: PlaybackState.STATE_NONE,
            queueItemId = state?.activeQueueItemId ?: -1L,
            positionMs = state?.position ?: 0L,
            positionAt = state?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = state?.playbackSpeed ?: 1f,
            actions = state?.actions ?: 0L,
        )
        queue = c.queue.orEmpty().map {
            QueueEntry(it.queueId, it.description.title?.toString().orEmpty())
        }
        val id = now?.mediaId
        if (id != null && seen.none { it.first == id }) seen.add(id to now?.title.orEmpty())
        chainCheck("report")
    }

    // Transport — what the pill's buttons would call

    fun playPause() = act("play/pause") {
        if (now?.isPlaying == true) it.pause() else it.play()
    }

    fun next() = act("skipToNext") { it.skipToNext() }
    fun previous() = act("skipToPrevious") { it.skipToPrevious() }

    fun seekBy(deltaMs: Long) = act("seek ${deltaMs / 1000}s") {
        it.seekTo(((now?.livePositionMs() ?: 0L) + deltaMs).coerceAtLeast(0L))
    }

    /** Skips most of a long track, so what happens at its end can be watched. */
    fun seekNearEnd() {
        val duration = now?.durationMs ?: 0L
        if (duration <= NEAR_END_LEAD_MS) {
            note("sent nothing (seek near end): no duration")
            return
        }
        act("seek to last ${NEAR_END_LEAD_MS / 1000}s") { it.seekTo(duration - NEAR_END_LEAD_MS) }
    }

    // Experiments — can playback start without leaving this app?

    fun playFromUri(url: String) = act("playFromUri $url") {
        it.playFromUri(Uri.parse(url), null)
    }

    fun playFromMediaId(id: String) = act("playFromMediaId $id") {
        it.playFromMediaId(id, null)
    }

    fun playFromSearch(query: String, extras: Bundle? = null) =
        act("playFromSearch \"$query\" extras=${describe(extras)}") {
            it.playFromSearch(query, extras)
        }

    fun skipTo(entry: QueueEntry) = act("skipToQueueItem ${entry.id} (${entry.title})") {
        it.skipToQueueItem(entry.id)
    }

    // Experiments — a search-started track has no queue; can the pill supply one?

    /**
     * Plays [requests] one after another, asking for the next when the
     * current one ends. The log records which signal marked each ending,
     * which is what a real pill would have to listen for.
     */
    fun playInOrder(requests: List<PlayRequest>) {
        stopChain()
        stopStepping()
        if (controller == null) {
            note("chain: no session to control")
            return
        }
        note("chain: ${requests.size} tracks: ${requests.joinToString { it.label }}")
        chain = requests
        startChainHead()
    }

    fun stopChain() {
        handler.removeCallbacks(chainTick)
        if (chain.isNotEmpty()) note("chain: stopped")
        chain = emptyList()
    }

    private fun startChainHead() {
        handler.removeCallbacks(chainTick)
        val head = chain.firstOrNull()
        if (head == null) {
            note("chain: finished")
            return
        }
        chainHeard = false
        chainNearEnd = false
        chainAskedAt = SystemClock.elapsedRealtime()
        note("chain: asking for ${head.label}")
        playFromSearch(head.title, head.extras)
        handler.postDelayed(chainTick, CHAIN_TICK_MS)
    }

    private fun chainCheck(signal: String) {
        val head = chain.firstOrNull() ?: return
        val n = now ?: return
        val onHead = n.mediaId == head.asin
        if (!chainHeard) {
            // A position near the end here is the previous track's, not yet replaced.
            val fresh = n.durationMs <= 0 || n.livePositionMs() < n.durationMs - END_WINDOW_MS
            if (onHead && n.isPlaying && fresh) {
                chainHeard = true
                note("chain: ${head.label} playing ${SystemClock.elapsedRealtime() - chainAskedAt} ms after asking")
            } else if (SystemClock.elapsedRealtime() - chainAskedAt > START_CHECK_MS) {
                note("chain: ${head.label} not playing ${START_CHECK_MS / 1000}s after asking;" +
                    " player shows ${n.title} (${n.mediaId}) ${STATES[n.state]}")
                stopChain()
            }
            return
        }
        val position = n.livePositionMs()
        if (onHead && !chainNearEnd && n.durationMs > 0 && position >= n.durationMs - END_WINDOW_MS) {
            chainNearEnd = true
            note("chain: ${head.label} in its last ${END_WINDOW_MS / 1000}s")
        }
        val ended = when {
            !onHead -> "player moved to ${n.title} (${n.mediaId})"
            !chainNearEnd -> null
            n.state in RESTING -> "state ${STATES[n.state]} at ${position / 1000}s"
            position < n.durationMs - RESTART_MARGIN_MS -> "restarted, now at ${position / 1000}s"
            position > n.durationMs + OVERRUN_MS -> "no signal, ${OVERRUN_MS / 1000}s past the end"
            else -> null
        } ?: return
        note("chain: ${head.label} ended ($signal): $ended")
        chain = chain.drop(1)
        startChainHead()
    }

    // Experiments — can a whole album be queued from here, and a track picked from it?

    /**
     * Asks for [album] by search and, once its queue shows up, jumps to
     * [request]'s entry in it. If this works the track plays inside a real
     * queue: the next one follows by itself and nothing depends on a title
     * search finding the right track.
     */
    fun playInAlbum(request: PlayRequest, album: String, extras: Bundle?) {
        pendingJump?.let(handler::removeCallbacks)
        note("album: asking for \"$album\", then ${request.label}")
        playFromSearch(album, extras)
        val asked = SystemClock.elapsedRealtime()
        val look = object : Runnable {
            override fun run() {
                val waited = SystemClock.elapsedRealtime() - asked
                val entry = queue.firstOrNull { it.title == request.title }
                when {
                    entry != null -> {
                        note("album: queue of ${queue.size} found after $waited ms")
                        skipTo(entry)
                        handler.postDelayed({
                            val n = now
                            note("album: ${if (n?.mediaId == request.asin) "right track" else "WRONG track"}" +
                                " ${n?.title} (${n?.mediaId}) ${STATES[n?.state]}, queue of ${queue.size}")
                        }, START_CONFIRM_MS)
                    }
                    waited > ALBUM_WAIT_MS -> note("album: \"${request.title}\" not in a queue after" +
                        " ${ALBUM_WAIT_MS / 1000}s; queue of ${queue.size}, player shows ${now?.title}" +
                        " (${now?.album})")
                    else -> {
                        handler.postDelayed(this, 500)
                        return
                    }
                }
                pendingJump = null
            }
        }
        pendingJump = look
        handler.postDelayed(look, ALBUM_SETTLE_MS)
    }

    // Experiments — an album search gives the order but no queue; can "next" reach a track?

    /**
     * Asks for [album] by search, then presses "next" until [target] is the
     * track showing, or through the whole album when there is no target,
     * noting every track passed. Amazon Music publishes no queue after an
     * album search, so counting presses is the only way to a track inside
     * one. With [quiet] the album is paused before the presses and started
     * again on the target, so the tracks passed are not heard.
     */
    fun stepThroughAlbum(album: String, extras: Bundle?, target: PlayRequest?, quiet: Boolean) {
        if (controller == null) {
            note("step: no session to control")
            return
        }
        stopChain()
        stopChecking()
        stopStepping()
        stepping = true
        note("step: asking for \"$album\", then next " +
            (target?.let { "until ${it.label}" } ?: "to the end") + if (quiet) ", paused" else "")
        val before = now
        playFromSearch(album, extras)
        val asked = SystemClock.elapsedRealtime()
        val look = object : Runnable {
            override fun run() {
                val waited = SystemClock.elapsedRealtime() - asked
                val n = now
                when {
                    // A position older than the search is the previous track still going.
                    n != null && n !== before && n.isPlaying && n.album == album &&
                        n.livePositionMs() <= waited -> {
                        note("step: album playing after $waited ms")
                        if (quiet) act("pause") { it.pause() }
                        walk(album, target, quiet)
                    }
                    waited > ALBUM_WAIT_MS -> endWalk("album not playing after ${ALBUM_WAIT_MS / 1000}s;" +
                        " player shows ${n?.title} (${n?.album}) ${STATES[n?.state]}", emptyList(), false)
                    else -> handler.postDelayed(this, STEP_POLL_MS)
                }
            }
        }
        pendingWalk = look
        handler.postDelayed(look, STEP_POLL_MS)
    }

    fun stopStepping() {
        pendingWalk?.let(handler::removeCallbacks)
        pendingWalk = null
        if (stepping) note("step: stopped")
        stepping = false
    }

    private fun walk(album: String, target: PlayRequest?, quiet: Boolean) {
        val passed = mutableListOf<Pair<String, String>>()
        val started = SystemClock.elapsedRealtime()
        var pressedAt = started
        val look = object : Runnable {
            override fun run() {
                val n = now
                val id = n?.mediaId
                val waited = SystemClock.elapsedRealtime() - pressedAt
                val settled = n != null && (n.isPlaying || n.state == PlaybackState.STATE_PAUSED)
                if (n == null || id == null || id == passed.lastOrNull()?.first || !settled) {
                    if (waited > STEP_WAIT_MS) {
                        endWalk("next changed nothing in ${STEP_WAIT_MS / 1000}s; player shows" +
                            " ${n?.title} ${STATES[n?.state]}", passed, target == null)
                    } else {
                        handler.postDelayed(this, STEP_POLL_MS)
                    }
                    return
                }
                if (passed.any { it.first == id }) {
                    endWalk("back at ${n.title}: the album went round", passed, target == null)
                    return
                }
                if (n.album != album) {
                    endWalk("left the album for ${n.title} (${n.album})", passed, true)
                    return
                }
                passed += id to n.title.orEmpty()
                note("step: ${passed.size} → ${n.title} ($id) ${n.durationMs / 1000}s" +
                    " ${STATES[n.state]} after $waited ms")
                when {
                    id == target?.asin -> {
                        note("step: reached ${target.label} with ${passed.size - 1} presses in" +
                            " ${SystemClock.elapsedRealtime() - started} ms")
                        if (quiet) act("play") { it.play() }
                        handler.postDelayed({
                            val m = now
                            note("step: ${if (m?.mediaId == target.asin) "right track" else "WRONG track"}" +
                                " ${m?.title} (${m?.mediaId}) ${STATES[m?.state]}")
                        }, START_CONFIRM_MS)
                        pendingWalk = null
                        stepping = false
                    }
                    passed.size >= STEP_LIMIT ->
                        endWalk("gave up after ${passed.size} tracks", passed, target == null)
                    n.actions and PlaybackState.ACTION_SKIP_TO_NEXT == 0L ->
                        endWalk("no next offered on ${n.title}", passed, target == null)
                    else -> {
                        pressedAt = SystemClock.elapsedRealtime()
                        next()
                        handler.postDelayed(this, STEP_POLL_MS)
                    }
                }
            }
        }
        pendingWalk = look
        handler.post(look)
    }

    private fun endWalk(why: String, passed: List<Pair<String, String>>, pause: Boolean) {
        note("step: ended, $why")
        if (passed.isNotEmpty()) {
            note("step: ${passed.size} tracks in order: " +
                passed.joinToString(" | ") { "${it.second} (${it.first})" })
        }
        if (pause) controller?.transportControls?.pause()
        pendingWalk = null
        stepping = false
    }

    // Experiments — how often does a title search find the right track?

    /** Asks for each of [requests] in turn and records whether the right track came back. */
    fun checkTitles(requests: List<PlayRequest>) {
        if (controller == null) {
            note("check: no session to control")
            return
        }
        stopChain()
        stopStepping()
        checking = true
        note("check: ${requests.size} titles")
        checkStep(requests, 0, 0)
    }

    fun stopChecking() {
        pendingStep?.let(handler::removeCallbacks)
        pendingStep = null
        if (checking) note("check: stopped")
        checking = false
    }

    private fun checkStep(requests: List<PlayRequest>, index: Int, right: Int) {
        if (index >= requests.size) {
            controller?.transportControls?.pause()
            note("check: $right of ${requests.size} titles gave the right track")
            checking = false
            return
        }
        val request = requests[index]
        playFromSearch(request.title, request.extras)
        val read = Runnable {
            val n = now
            val ok = n?.mediaId == request.asin
            note("check: ${request.label} → " +
                if (ok) "right" else "WRONG, got ${n?.title} (${n?.mediaId})")
            checkStep(requests, index + 1, if (ok) right + 1 else right)
        }
        pendingStep = read
        handler.postDelayed(read, CHECK_DWELL_MS)
    }

    // Experiments — can playback start when Amazon Music is not running?

    /**
     * With no session there is nothing to send a search to. Tries each way
     * of waking Amazon Music in the background in turn, and sends the search
     * as soon as a session appears.
     */
    fun coldStart(request: PlayRequest) {
        handler.removeCallbacks(wakeNext)
        if (!isInstalled(AMAZON)) {
            note("cold start: Amazon Music is not installed")
            return
        }
        if (controller?.packageName == AMAZON) {
            note("cold start: Amazon Music already has a session (${STATES[now?.state]}), nothing to wake")
            deliver(request)
            return
        }
        note("cold start: no Amazon Music session")
        pendingStart = request
        pendingSince = SystemClock.elapsedRealtime()
        wakeStep = 0
        wake()
    }

    private fun wake() {
        if (pendingStart == null) return
        wakeStep++
        when (wakeStep) {
            1 -> probeBrowser()
            2 -> wakeByBroadcast()
            3 -> wakeByMediaKey()
            else -> {
                note("cold start: no session after ${(SystemClock.elapsedRealtime() - pendingSince) / 1000}s;" +
                    " only a hand-off would start it")
                pendingStart = null
                return
            }
        }
        handler.postDelayed(wakeNext, WAKE_STEP_MS)
    }

    private fun deliver(request: PlayRequest) {
        pendingStart = null
        handler.removeCallbacks(wakeNext)
        playFromSearch(request.title, request.extras)
        confirmStart(request, 1)
    }

    /**
     * After a Force stop the track was seen to load and then sit paused, so
     * a start is only done once the right track is actually playing.
     */
    private fun confirmStart(request: PlayRequest, check: Int) {
        handler.postDelayed({
            val n = now
            val cued = n?.mediaId == request.asin
            when {
                cued && n?.isPlaying == true -> note("cold start: ${request.label} is playing (check $check)")
                check >= 3 -> note("cold start: gave up; player shows ${n?.title} ${STATES[n?.state]}")
                cued -> {
                    note("cold start: ${request.label} is cued but ${STATES[n?.state]}, pressing play")
                    act("play") { it.play() }
                    confirmStart(request, check + 1)
                }
                else -> {
                    note("cold start: not cued at check $check, asking again")
                    playFromSearch(request.title, request.extras)
                    confirmStart(request, check + 1)
                }
            }
        }, START_CONFIRM_MS)
    }

    /** A media button press addressed to Amazon Music's own receiver. */
    fun wakeByBroadcast() {
        val receiver = context.packageManager
            .queryBroadcastReceivers(Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(AMAZON), 0)
            .firstOrNull()?.activityInfo
        if (receiver == null) {
            note("wake: Amazon Music shows no media button receiver")
            return
        }
        note("wake: media button broadcast to ${receiver.name} (exported=${receiver.exported})")
        for (action in intArrayOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            context.sendBroadcast(
                Intent(Intent.ACTION_MEDIA_BUTTON)
                    .setComponent(ComponentName(receiver.packageName, receiver.name))
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(action, KeyEvent.KEYCODE_MEDIA_PLAY)),
            )
        }
    }

    /**
     * A headset-style play press through the system, which sends it to
     * whichever player it considers last used — not necessarily Amazon Music.
     */
    fun wakeByMediaKey() {
        note("wake: media key PLAY through the system")
        val audio = context.getSystemService(AudioManager::class.java)
        for (action in intArrayOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            audio.dispatchMediaKeyEvent(KeyEvent(action, KeyEvent.KEYCODE_MEDIA_PLAY))
        }
    }

    /**
     * Plays each queue entry for a few seconds so the session reports its
     * track ID, which the queue itself leaves out. This is how the
     * per-paththu IDs for amazon_music.json can be read off an album.
     */
    fun collectQueue() {
        val entries = queue
        if (entries.isEmpty() || controller == null) {
            note("collect: nothing queued")
            return
        }
        collecting = true
        note("collect: ${entries.size} queue entries")
        collectStep(entries, 0)
    }

    fun stopCollecting() {
        pendingStep?.let(handler::removeCallbacks)
        pendingStep = null
        if (collecting) note("collect: stopped")
        collecting = false
    }

    private fun collectStep(entries: List<QueueEntry>, index: Int) {
        val c = controller
        if (c == null || index >= entries.size) {
            c?.transportControls?.pause()
            note("collect: done")
            collecting = false
            return
        }
        val entry = entries[index]
        c.transportControls.skipToQueueItem(entry.id)
        val read = Runnable {
            val m = controller?.metadata
            note("collect: ${index + 1}. ${entry.title} = " +
                "${m?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)} " +
                "(${m?.getText(MediaMetadata.METADATA_KEY_TITLE)}, " +
                "${(m?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L) / 1000}s)")
            collectStep(entries, index + 1)
        }
        pendingStep = read
        handler.postDelayed(read, COLLECT_DWELL_MS)
    }

    /**
     * Tries the route that needs no Notification access at all: binding to
     * the player's MediaBrowserService, as Android Auto does. Players commonly
     * refuse callers they do not recognise; this records which it is.
     */
    fun probeBrowser() {
        val pkg = if (isInstalled(AMAZON)) AMAZON else controller?.packageName
        if (pkg == null) {
            note("browser: Amazon Music not installed and no session to stand in")
            return
        }
        val service = context.packageManager
            .queryIntentServices(Intent(MediaBrowserService.SERVICE_INTERFACE).setPackage(pkg), 0)
            .firstOrNull()?.serviceInfo
        if (service == null) {
            note("browser: $pkg exposes no MediaBrowserService")
            return
        }
        note("browser: connecting to ${service.name}")
        browser?.disconnect()
        browser = MediaBrowser(
            context,
            ComponentName(service.packageName, service.name),
            object : MediaBrowser.ConnectionCallback() {
                override fun onConnected() {
                    val b = browser ?: return
                    note("browser: CONNECTED without notification access, root=${b.root}")
                    follow(MediaController(context, b.sessionToken))
                    publish()
                }

                override fun onConnectionFailed() = note("browser: connection refused")
                override fun onConnectionSuspended() = note("browser: connection suspended")
            },
            null,
        ).also { it.connect() }
    }

    private fun act(what: String, block: (MediaController.TransportControls) -> Unit) {
        val c = controller
        if (c == null) {
            note("sent nothing ($what): no session to control")
            return
        }
        note("sent → $what")
        try {
            block(c.transportControls)
        } catch (e: RuntimeException) {
            note("$what threw ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    // The log

    /** Writes down everything the followed session exposes, in full. */
    fun dump() {
        val c = controller
        if (c == null) {
            note("dump: no session")
            return
        }
        note("dump ${c.packageName}: flags=${c.flags} opensApp=${c.sessionActivity != null}" +
            " extras=${describe(c.extras)}")
        note("dump metadata: ${describe(c.metadata)}")
        note("dump state: ${describe(c.playbackState)}")
        note("dump queue (${c.queueTitle}): ${describe(c.queue)}")
        publish()
    }

    fun note(message: String) {
        val line = "${clock.format(Date())} $message"
        Log.i(TAG, message)
        log.add(line)
        runCatching { logFile.appendText(line + "\n") }
    }

    fun clearLog() {
        log.clear()
        logFile.delete()
    }

    fun report(): String = buildString {
        appendLine("DP remote-control test")
        appendLine("${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Amazon Music installed: ${isInstalled(AMAZON)}")
        appendLine("notification access: $hasAccess")
        appendLine("sessions: $sessions")
        appendLine("tracks seen:")
        seen.forEach { (id, title) -> appendLine("  $id  $title") }
        appendLine()
        log.forEach(::appendLine)
    }

    fun isInstalled(pkg: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess

    private fun describe(metadata: MediaMetadata?): String {
        if (metadata == null) return "null"
        return metadata.keySet().sorted().joinToString("; ") { key ->
            val short = key.removePrefix("android.media.metadata.")
            val text = runCatching { metadata.getText(key) }.getOrNull()
            when {
                key in BITMAP_KEYS -> "$short=<image>"
                text != null -> "$short=$text"
                else -> "$short=${runCatching { metadata.getLong(key) }.getOrNull()}"
            }
        }
    }

    private fun describe(state: PlaybackState?): String {
        if (state == null) return "null"
        val allowed = ACTIONS.filter { state.actions and it.first != 0L }.map { it.second }
        val custom = state.customActions.map { it.action }
        return "${STATES[state.state] ?: state.state} pos=${state.position / 1000}s" +
            " speed=${state.playbackSpeed} queueItem=${state.activeQueueItemId}" +
            " error=${state.errorMessage} actions=$allowed custom=$custom" +
            " extras=${describe(state.extras)}"
    }

    private fun describe(queue: List<MediaSession.QueueItem>?): String {
        if (queue == null) return "null"
        val shown = queue.take(40).joinToString(" | ") {
            "${it.queueId}:${it.description.title} [${it.description.mediaId}]"
        }
        return "${queue.size} items: $shown"
    }

    @Suppress("DEPRECATION")
    private fun describe(extras: Bundle?): String =
        extras?.keySet()?.joinToString(", ", "{", "}") { "$it=${extras.get(it)}" } ?: "null"

    companion object {
        const val AMAZON = "com.amazon.mp3"
        private const val TAG = "DPRemote"

        /** Long enough for Amazon Music to buffer a track and report it. */
        private const val COLLECT_DWELL_MS = 4000L

        private const val NEAR_END_LEAD_MS = 15_000L
        private const val CHAIN_TICK_MS = 1000L

        /** How close to its duration a track must get before a stop counts as its end. */
        private const val END_WINDOW_MS = 5000L
        private const val RESTART_MARGIN_MS = 30_000L
        private const val OVERRUN_MS = 5000L

        /** How long a requested track gets to show up as playing. */
        private const val START_CHECK_MS = 8000L
        private const val START_CONFIRM_MS = 4000L
        private const val WAKE_STEP_MS = 4000L

        /** How long an album search gets to produce a queue. */
        private const val ALBUM_SETTLE_MS = 1500L
        private const val ALBUM_WAIT_MS = 10_000L
        private const val CHECK_DWELL_MS = 5000L

        /** How often a walk looks for the next track, how long one press gets, and when to give up. */
        private const val STEP_POLL_MS = 250L
        private const val STEP_WAIT_MS = 6000L
        private const val STEP_LIMIT = 40

        private val RESTING = setOf(
            PlaybackState.STATE_NONE,
            PlaybackState.STATE_STOPPED,
            PlaybackState.STATE_PAUSED,
        )

        private val BITMAP_KEYS = setOf(
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON,
        )

        private val STATES = mapOf(
            PlaybackState.STATE_NONE to "NONE",
            PlaybackState.STATE_STOPPED to "STOPPED",
            PlaybackState.STATE_PAUSED to "PAUSED",
            PlaybackState.STATE_PLAYING to "PLAYING",
            PlaybackState.STATE_BUFFERING to "BUFFERING",
            PlaybackState.STATE_ERROR to "ERROR",
            PlaybackState.STATE_CONNECTING to "CONNECTING",
            PlaybackState.STATE_SKIPPING_TO_NEXT to "SKIPPING_TO_NEXT",
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS to "SKIPPING_TO_PREVIOUS",
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM to "SKIPPING_TO_QUEUE_ITEM",
        )

        private val ACTIONS = listOf(
            PlaybackState.ACTION_PLAY to "play",
            PlaybackState.ACTION_PAUSE to "pause",
            PlaybackState.ACTION_PLAY_PAUSE to "playPause",
            PlaybackState.ACTION_STOP to "stop",
            PlaybackState.ACTION_SKIP_TO_NEXT to "next",
            PlaybackState.ACTION_SKIP_TO_PREVIOUS to "previous",
            PlaybackState.ACTION_SEEK_TO to "seekTo",
            PlaybackState.ACTION_FAST_FORWARD to "fastForward",
            PlaybackState.ACTION_REWIND to "rewind",
            PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM to "skipToQueueItem",
            PlaybackState.ACTION_PLAY_FROM_MEDIA_ID to "playFromMediaId",
            PlaybackState.ACTION_PLAY_FROM_SEARCH to "playFromSearch",
            PlaybackState.ACTION_PLAY_FROM_URI to "playFromUri",
            PlaybackState.ACTION_PREPARE to "prepare",
            PlaybackState.ACTION_PREPARE_FROM_MEDIA_ID to "prepareFromMediaId",
            PlaybackState.ACTION_PREPARE_FROM_SEARCH to "prepareFromSearch",
            PlaybackState.ACTION_PREPARE_FROM_URI to "prepareFromUri",
        )
    }
}
