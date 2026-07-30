package com.srinivaskannan.divyaprabhandham.media

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.srinivaskannan.divyaprabhandham.data.Work
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import java.net.URLEncoder

/**
 * Hands recitation playback to YouTube Music, or YouTube, or the browser.
 *
 * The link host follows the target: listeners with YouTube Music get a
 * music.youtube.com link; those with only the YouTube app, or neither, get
 * youtube.com (in the app or the browser). The ids are the same either way —
 * a plain YouTube video id resolves on both hosts.
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
        playlistId: String?,
        videoIds: List<String>,
        script: ScriptChoice,
    ): Boolean {
        // Choose the target app first, then build a URL for *that* host. The
        // host and the app have to agree: a music.youtube.com link handed to
        // the plain YouTube app does not open cleanly, and a www.youtube.com
        // link sent to YouTube Music loses the Music queue. So a listener with
        // YouTube Music gets a music.youtube.com link; one with only YouTube
        // gets youtube.com; one with neither gets youtube.com in the browser.
        val targets = buildList {
            if (isInstalled(context, YT_MUSIC)) add(YT_MUSIC to Host.MUSIC)
            if (isInstalled(context, YT)) add(YT to Host.WWW)
            // Browser last, on the standard host.
            add(null to Host.WWW)
        }
        for ((pkg, host) in targets) {
            val uri = Uri.parse(targetUri(host, playlistId, videoIds, work, script))
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                if (pkg != null) setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (tryStart(context, intent)) return true
        }
        return false
    }

    private enum class Host(val base: String) {
        MUSIC("https://music.youtube.com"),
        WWW("https://www.youtube.com"),
    }

    /** Builds the best URL for a host: playlist, else first video, else search. */
    private fun targetUri(
        host: Host,
        playlistId: String?,
        videoIds: List<String>,
        work: Work,
        script: ScriptChoice,
    ): String = when {
        !playlistId.isNullOrBlank() -> "${host.base}/playlist?list=$playlistId"
        videoIds.isNotEmpty() -> "${host.base}/watch?v=${videoIds.first()}"
        else -> searchUri(host, work, script)
    }

    /**
     * A search query aimed at recitation rather than film music: the work's
     * Tamil title (always, even when the app is in English — that is what the
     * uploads are titled), the author, and the word for recitation.
     */
    private fun searchUri(host: Host, work: Work, script: ScriptChoice): String {
        val terms = buildString {
            append(work.title)
            append(' ')
            append(work.author)
            append(' ')
            append(if (script == ScriptChoice.TAMIL) "பாராயணம்" else "paarayanam")
        }
        val query = URLEncoder.encode(terms, "UTF-8")
        return "${host.base}/search?q=$query"
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
