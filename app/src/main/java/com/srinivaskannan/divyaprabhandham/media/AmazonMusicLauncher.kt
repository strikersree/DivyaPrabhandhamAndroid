package com.srinivaskannan.divyaprabhandham.media

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Hands recitation playback to the Amazon Music app.
 *
 * TRIAL, SCOPED TO THIRUPPAVAI (w3): unlike the YouTube hand-off, once Amazon
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
     * Opens [albumAsin] in Amazon Music, jumping straight to [trackAsin] when
     * given. Targets the Amazon Music package directly (skipping the app
     * chooser); if it isn't installed, falls back to its Play Store listing so
     * the listener can install it and try again.
     */
    fun launch(context: Context, albumAsin: String, trackAsin: String? = null): Result {
        val url = buildString {
            append("https://music.amazon.in/albums/").append(albumAsin)
            append('?')
            if (trackAsin != null) append("trackAsin=").append(trackAsin).append('&')
            append("do=play")
        }
        val toAmazon = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(PKG)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (isInstalled(context) && tryStart(context, toAmazon)) return Result.OpenedApp

        val toStore = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (tryStart(context, toStore)) Result.OpenedPlayStore else Result.Failed
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private fun isInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(PKG, PackageManager.GET_ACTIVITIES)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
