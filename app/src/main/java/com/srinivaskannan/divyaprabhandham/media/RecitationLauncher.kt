package com.srinivaskannan.divyaprabhandham.media

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.srinivaskannan.divyaprabhandham.data.Recitation
import com.srinivaskannan.divyaprabhandham.data.Work
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import java.net.URLEncoder

/**
 * Hands recitation playback to YouTube Music, or YouTube, or the browser.
 *
 * WHY A HANDOFF AND NOT A PLAYER: the iOS build uses MusicKit, which lets an
 * app drive playback of the Apple Music catalogue inside its own UI under the
 * listener's subscription. YouTube has no equivalent for Android apps. The
 * IFrame player in a WebView is the only way to embed it, and using that to
 * play audio-only in the background is exactly what YouTube's terms forbid — so
 * a mini-player here would be a compliance problem, not a feature.
 *
 * Handing off is also closer to what the iOS design actually promised: audio is
 * never stored or bundled, and playback happens under the listener's own
 * account. They just get YouTube Music's player instead of ours.
 *
 * WHAT THIS COSTS: the Now Playing pill and the full player from the iOS build
 * are gone, because we cannot observe or control playback once it leaves. The
 * bottom accessory slot is given over to Continue Reading alone.
 */
object RecitationLauncher {

    private const val YT_MUSIC = "com.google.android.apps.youtube.music"
    private const val YT = "com.google.android.youtube"

    /**
     * Opens the recitation for a work.
     *
     * Falls back through: an explicit playlist, an explicit video, then a
     * search for the work. The search fallback is what makes the button useful
     * today — `recitations.json` carries no YouTube identifiers yet, and a
     * search for the work's own name plus "paarayanam" lands well for most of
     * the corpus.
     *
     * @return false if nothing on the device could handle it, so the caller can
     *   say so rather than leaving a tap that does nothing.
     */
    fun launch(
        context: Context,
        work: Work,
        recitation: Recitation?,
        script: ScriptChoice,
    ): Boolean {
        val uri = when {
            recitation?.youtubePlaylist != null ->
                "https://music.youtube.com/playlist?list=${recitation.youtubePlaylist}"

            recitation?.youtubeVideo != null ->
                "https://music.youtube.com/watch?v=${recitation.youtubeVideo}"

            else -> searchUri(work, script)
        }
        return open(context, Uri.parse(uri))
    }

    /**
     * A search query aimed at recitation rather than film music: the work's
     * Tamil title (always, even when the app is in English — that is what the
     * uploads are titled), the author, and the word for recitation.
     */
    private fun searchUri(work: Work, script: ScriptChoice): String {
        val terms = buildString {
            append(work.title)
            append(' ')
            append(work.author)
            append(' ')
            append(if (script == ScriptChoice.TAMIL) "பாராயணம்" else "paarayanam")
        }
        val query = URLEncoder.encode(terms, "UTF-8")
        return "https://music.youtube.com/search?q=$query"
    }

    /**
     * Prefers YouTube Music, then YouTube, then whatever handles the link.
     * Explicitly naming the package matters: a plain VIEW intent on a
     * music.youtube.com URL usually lands in a browser tab even when the app is
     * installed, which is a worse place to listen from.
     */
    private fun open(context: Context, uri: Uri): Boolean {
        for (pkg in listOf(YT_MUSIC, YT)) {
            if (!isInstalled(context, pkg)) continue
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (tryStart(context, intent)) return true
        }
        val fallback = Intent(Intent.ACTION_VIEW, uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return tryStart(context, fallback)
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private fun isInstalled(context: Context, pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
