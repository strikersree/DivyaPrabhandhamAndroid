package com.srinivaskannan.divyaprabhandham.media

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.srinivaskannan.divyaprabhandham.data.AmazonWork

/**
 * Hands recitation playback to the Amazon Music app.
 *
 * TRIAL, EXPANDING TO THE MUDHALAYIRAM: confirmed on device that once Amazon
 * Music takes over, playback continues in the background when the listener
 * switches back to this app — YouTube either pauses on backgrounding (in-app
 * embed) or requires Premium to keep playing (the YouTube app itself). That is
 * the problem this is meant to fix. It costs the same thing the YouTube
 * hand-off costs: no in-app player or controls, and it requires Amazon Music
 * installed plus a Prime/Unlimited subscription for on-demand play.
 *
 * The URL host (music.amazon.in) is India-specific; a listener on another
 * Amazon Music region should still have the link routed to their installed
 * app by the OS, but catalogue/subscription availability follows their own
 * region. Worth revisiting if this trial expands beyond India.
 */
object AmazonMusicLauncher {

    private const val PKG = "com.amazon.mp3"
    private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$PKG"

    sealed interface Result {
        /** Amazon Music opened directly and should now be playing. */
        data object OpenedApp : Result

        /** Amazon Music isn't installed; sent to its Play Store page instead. */
        data object OpenedPlayStore : Result

        /** Neither Amazon Music nor a browser could handle it. */
        data object Failed : Result
    }

    /**
     * Opens [work] in Amazon Music: its playlist when one is set, otherwise its
     * album (jumping straight to the track when one is set). Targets the
     * Amazon Music package directly (skipping the app chooser); if it isn't
     * installed, falls back to its Play Store listing so the listener can
     * install it and try again.
     *
     * Deliberately does not pre-check whether Amazon Music is installed via
     * PackageManager: on Android 11+, that query requires the package to be
     * declared in the manifest's `<queries>` block, and a missing declaration
     * makes an installed app look absent (this happened once — see commit
     * history). An explicit setPackage() intent needs no such declaration to
     * be *sent*; it simply fails with ActivityNotFoundException if the target
     * truly isn't there, which is what we actually want to react to.
     *
     * @return Failed if [work] has neither a playlist nor an album set.
     */
    fun launch(context: Context, work: AmazonWork): Result {
        val url = targetUrl(work) ?: return Result.Failed
        val toAmazon = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(PKG)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (tryStart(context, toAmazon)) return Result.OpenedApp

        val toStore = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (tryStart(context, toStore)) Result.OpenedPlayStore else Result.Failed
    }

    private fun targetUrl(work: AmazonWork): String? = when {
        !work.playlist.isNullOrBlank() ->
            "https://music.amazon.in/user-playlists/${work.playlist}?do=play"
        !work.album.isNullOrBlank() -> buildString {
            append("https://music.amazon.in/albums/").append(work.album)
            append('?')
            if (!work.track.isNullOrBlank()) append("trackAsin=").append(work.track).append('&')
            append("do=play")
        }
        else -> null
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
