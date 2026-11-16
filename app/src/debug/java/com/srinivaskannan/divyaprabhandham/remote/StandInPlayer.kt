package com.srinivaskannan.divyaprabhandham.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore

/**
 * A silent player for devices without Amazon Music (the emulator), so the
 * pill's own logic can be exercised before a run on a real phone. A song
 * search plays that track alone, with no queue, and pauses at its end, as
 * Amazon Music was seen to; an album search plays every track in order and
 * answers next/previous, but publishes no queue, again as seen — and a search
 * for an album it does not know by that name plays another one. [albums] maps
 * each album's name to its titles, in album order, and the track ID to report
 * for each; [mistakes] maps a title to the one a search for it plays instead.
 */
class StandInPlayer(
    context: Context,
    private val albums: Map<String, Map<String, String>>,
    private val mistakes: Map<String, String> = emptyMap(),
) {

    constructor(
        context: Context,
        album: String,
        ids: Map<String, String>,
        mistakes: Map<String, String> = emptyMap(),
    ) : this(context, mapOf(album to ids), mistakes)

    /** The album being played. */
    private var album = albums.keys.first()
    private val ids: Map<String, String> get() = albums.getValue(album)

    /** As the last album search named it: the app checks it against the album's. */
    private var artist = "Stand-in player"

    private val handler = Handler(Looper.getMainLooper())

    /** A plain two-tone square, so the app's artwork path has a picture to show. */
    private val cover = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).also {
        val canvas = Canvas(it)
        canvas.drawColor(Color.rgb(0x2E, 0x5E, 0x7E))
        canvas.drawCircle(128f, 128f, 72f, Paint().apply { color = Color.rgb(0xE8, 0xC4, 0x6A) })
    }
    private val session = MediaSession(context, "DPStandIn")
    private var position = 0L
    private var since = 0L
    private var playing = false
    private var queued = emptyList<String>()
    private var index = -1
    private val finish = Runnable {
        if (index in 0 until queued.lastIndex) {
            start(index + 1)
        } else {
            playing = false
            position = 0L
            publish(PlaybackState.STATE_PAUSED)
        }
    }

    init {
        session.setCallback(object : MediaSession.Callback() {
            override fun onPlayFromSearch(query: String?, extras: Bundle?) {
                val wholeAlbum = extras?.getString(MediaStore.EXTRA_MEDIA_FOCUS) ==
                    MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE
                if (wholeAlbum) {
                    val asked = extras?.getString(MediaStore.EXTRA_MEDIA_ALBUM)
                    album = asked?.takeIf { it in albums } ?: albums.keys.first()
                    extras?.getString(MediaStore.EXTRA_MEDIA_ARTIST)?.let { artist = it }
                    queued = ids.keys.toList()
                    session.setQueue(null)
                    start(0)
                } else {
                    val asked = extras?.getString(MediaStore.EXTRA_MEDIA_TITLE) ?: query.orEmpty()
                    val played = mistakes[asked] ?: asked
                    album = albums.entries.firstOrNull { played in it.value }?.key ?: album
                    queued = emptyList()
                    session.setQueue(null)
                    show(played)
                    index = -1
                    position = 0L
                    onPlay()
                }
            }

            override fun onSkipToNext() = step(index + 1)

            override fun onSkipToPrevious() = step(index - 1)

            override fun onPlay() {
                if (position >= DURATION_MS) position = 0L
                playing = true
                since = SystemClock.elapsedRealtime()
                publish(PlaybackState.STATE_PLAYING)
            }

            override fun onPause() {
                position = current()
                playing = false
                publish(PlaybackState.STATE_PAUSED)
            }

            override fun onSeekTo(pos: Long) {
                position = pos.coerceIn(0L, DURATION_MS)
                since = SystemClock.elapsedRealtime()
                publish(if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED)
            }
        })
        publish(PlaybackState.STATE_NONE)
        session.isActive = true
    }

    private fun start(entry: Int) {
        index = entry
        show(queued[entry])
        position = 0L
        playing = true
        since = SystemClock.elapsedRealtime()
        publish(PlaybackState.STATE_PLAYING)
    }

    /** Next and previous start a paused player again, as Amazon Music was seen to. */
    private fun step(entry: Int) {
        if (entry in queued.indices) start(entry)
    }

    private fun show(title: String) {
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, ids[title] ?: "UNKNOWN")
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, DURATION_MS)
                .putBitmap(MediaMetadata.METADATA_KEY_ART, cover)
                .build(),
        )
    }

    fun release() {
        handler.removeCallbacks(finish)
        session.release()
    }

    private fun current(): Long =
        if (playing) position + SystemClock.elapsedRealtime() - since else position

    private fun publish(state: Int) {
        handler.removeCallbacks(finish)
        if (state == PlaybackState.STATE_PLAYING) handler.postDelayed(finish, DURATION_MS - position)
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    ACTIONS or
                        (if (index in 0 until queued.lastIndex) PlaybackState.ACTION_SKIP_TO_NEXT else 0L) or
                        (if (index > 0) PlaybackState.ACTION_SKIP_TO_PREVIOUS else 0L),
                )
                .setState(state, position, if (state == PlaybackState.STATE_PLAYING) 1f else 0f)
                .build(),
        )
    }

    private companion object {
        const val DURATION_MS = 60_000L
        const val ACTIONS = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SEEK_TO or
            PlaybackState.ACTION_PLAY_FROM_SEARCH
    }
}
