package com.srinivaskannan.divyaprabhandham.media

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray

/**
 * Plays recitations inside the app, through YouTube's official IFrame Player.
 *
 * WHY A WEBVIEW: YouTube has no native playback SDK for Android apps. The
 * IFrame Player API is the sanctioned way to embed its content, and it is a web
 * player, so a WebView is not a workaround — it is the interface. The
 * alternative, extracting stream URLs and feeding them to ExoPlayer, is a terms
 * violation and breaks whenever YouTube changes anything.
 *
 * WHY THE PLAYER STAYS VISIBLE: embedding comes with conditions. The player has
 * to be on screen and at least 200x200dp, playback cannot continue hidden or in
 * the background, and ads must not be circumvented. So the recitation screen
 * shows the player rather than reducing it to an audio bar, and playback is
 * paused when the screen goes away. That is a real difference from the iOS
 * build, where MusicKit could drive audio-only playback under the listener's
 * Apple Music subscription.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlaylistPlayer(
    videoIds: List<String>,
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
) {
    val html = remember(videoIds, startIndex) { buildHtml(videoIds, startIndex) }

    val webView = remember { mutableListOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            // Stop the audio the moment the screen goes: leaving it playing
            // behind the app is exactly what embedded playback may not do.
            webView[0]?.let { view ->
                view.evaluateJavascript("if (window.player) player.stopVideo();", null)
                view.loadUrl("about:blank")
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
            }
            webView[0] = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webView[0] = this
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    // The listener already tapped Listen, so treat that as the
                    // gesture rather than making them tap play a second time.
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    // The IFrame player pulls sub-resources that can be flagged
                    // mixed against the https base; without this some load but
                    // the video does not.
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }
                setBackgroundColor(android.graphics.Color.BLACK)
                webViewClient = WebViewClient()
                // Required for fullscreen and for the player to report itself
                // correctly to the IFrame API.
                webChromeClient = WebChromeClient()
                // A real https base URL matters: the IFrame API checks origin,
                // and a null or file:// origin makes the player refuse to load.
                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    html,
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
    )
}

/**
 * The player page. The whole playlist is handed to the IFrame API at once, so
 * the next recitation follows without the app having to drive it.
 */
private fun buildHtml(videoIds: List<String>, startIndex: Int): String {
    val ids = JSONArray(videoIds).toString()
    val start = startIndex.coerceIn(0, (videoIds.size - 1).coerceAtLeast(0))
    return """
<!DOCTYPE html>
<html>
  <head>
    <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no">
    <style>
      html, body { margin: 0; padding: 0; background: #000; height: 100%; overflow: hidden; }
      #player { width: 100%; height: 100%; }
    </style>
  </head>
  <body>
    <div id="player"></div>
    <script src="https://www.youtube.com/iframe_api"></script>
    <script>
      var ids = $ids;
      var startIndex = $start;
      var player;
      function onYouTubeIframeAPIReady() {
        player = new YT.Player('player', {
          height: '100%',
          width: '100%',
          videoId: ids[startIndex],
          playerVars: {
            playsinline: 1,
            rel: 0,
            autoplay: 1,
            origin: 'https://www.youtube.com'
          },
          events: {
            onReady: function (e) {
              if (ids.length > 1) {
                e.target.loadPlaylist({ playlist: ids, index: startIndex });
              }
            }
          }
        });
        window.player = player;
      }
    </script>
  </body>
</html>
    """.trimIndent()
}
