package com.srinivaskannan.divyaprabhandham.remote

import android.content.Context
import com.srinivaskannan.divyaprabhandham.data.AmazonAlbum
import com.srinivaskannan.divyaprabhandham.media.AmazonMusicLauncher
import com.srinivaskannan.divyaprabhandham.media.AmazonRemote

/**
 * Stands in for Amazon Music in the app itself, so the Now Playing pill and
 * the way a recitation is started can be tried on an emulator, which has no
 * Amazon Music. One silent [StandInPlayer] holding the app's own albums, kept
 * for the life of the process, with [AmazonRemote] pointed at it. Started
 * shuffled, it plays each album out of order, as an account limited to
 * shuffle play would: the start must then give up and hand off.
 */
object AppStandIn {

    private var player: StandInPlayer? = null

    val isOn: Boolean get() = player != null

    fun start(context: Context, albums: Collection<AmazonAlbum>, shuffled: Boolean = false) {
        if (player != null || albums.isEmpty()) return
        player = StandInPlayer(
            context.applicationContext,
            albums.associate { album ->
                val tracks = album.tracks.withIndex().map { (at, id) -> "Track ${at + 1}" to id }
                album.name to (if (shuffled) tracks.reversed() else tracks).toMap()
            },
        )
        AmazonRemote.player = context.packageName
    }

    fun stop() {
        player?.release()
        player = null
        AmazonRemote.player = AmazonMusicLauncher.PKG
    }
}
