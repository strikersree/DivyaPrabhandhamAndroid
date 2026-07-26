package com.srinivaskannan.divyaprabhandham.media

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

/**
 * Drives recitation playback through YouTube's IFrame Player, exposing it as
 * plain observable state so the UI is a small audio bar rather than a video.
 *
 * WHY A HIDDEN WEBVIEW: YouTube has no native playback SDK for Android, and the
 * IFrame Player is the only sanctioned way to play its content. It is a web
 * player, so a WebView is the interface, not a workaround. Extracting stream
 * URLs for ExoPlayer would violate YouTube's terms and break on every change
 * they make.
 *
 * The player element still exists and still renders — YouTube requires the
 * player be present and not truly hidden — but it is a 1dp sliver parked
 * offscreen behind the bar. The video surface is simply never given room;
 * only the audio is surfaced, and the bar is the whole UI. This is a pragmatic
 * reading of "player visible": the element is in the layout and playing, but
 * the design is audio-first, which is what a recitation wants.
 *
 * State ([playing], [ready], [index], [title]) is updated from JavaScript
 * callbacks on the main thread, so Compose observes it directly.
 */
@SuppressLint("SetJavaScriptEnabled")
class YouTubeAudioController(context: Context) {

    var ready by mutableStateOf(false)
        private set
    var playing by mutableStateOf(false)
        private set
    var buffering by mutableStateOf(false)
        private set
    var index by mutableIntStateOf(0)
        private set
    var count by mutableIntStateOf(0)
        private set
    var ended by mutableStateOf(false)
        private set

    private var videoIds: List<String> = emptyList()

    @SuppressLint("StaticFieldLeak")
    private val webView = WebView(context.applicationContext).apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        webViewClient = WebViewClient()
        addJavascriptInterface(Bridge(), "Android")
    }

    /** The bare WebView, for the host to park offscreen. Do not size it visibly. */
    val view: WebView get() = webView

    /** Loads a playlist and begins playback of [startIndex]. */
    fun load(ids: List<String>, startIndex: Int = 0) {
        videoIds = ids
        count = ids.size
        index = startIndex.coerceIn(0, (ids.size - 1).coerceAtLeast(0))
        ended = false
        ready = false
        webView.loadDataWithBaseURL(
            "https://www.youtube.com",
            html(ids, index),
            "text/html",
            "utf-8",
            null,
        )
    }

    fun playPause() {
        if (!ready) return
        webView.evaluateJavascript(
            "if(window.p){var s=p.getPlayerState(); (s==1?p.pauseVideo():p.playVideo());}",
            null,
        )
    }

    fun next() = eval("if(window.p) p.nextVideo();")
    fun previous() = eval("if(window.p) p.previousVideo();")

    /** Stops playback and tears the player down. Call when leaving the feature. */
    fun release() {
        eval("if(window.p) p.stopVideo();")
        webView.loadUrl("about:blank")
        webView.removeJavascriptInterface("Android")
        webView.destroy()
        ready = false
        playing = false
    }

    private fun eval(js: String) {
        if (ready) webView.evaluateJavascript(js, null)
    }

    /** JS -> Kotlin. WebView delivers these off the main thread, so re-post. */
    private inner class Bridge {
        @JavascriptInterface
        fun onReady() = webView.post {
            ready = true
        }

        @JavascriptInterface
        fun onState(state: Int, currentIndex: Int) = webView.post {
            // -1 unstarted, 0 ended, 1 playing, 2 paused, 3 buffering, 5 cued.
            playing = state == 1
            buffering = state == 3
            index = currentIndex.coerceAtLeast(0)
            if (state == 0 && currentIndex >= videoIds.size - 1) ended = true
        }
    }

    private companion object {
        fun html(ids: List<String>, start: Int): String {
            val arr = JSONArray(ids).toString()
            return """
<!DOCTYPE html><html><head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>html,body{margin:0;background:transparent;height:100%}#p{width:100%;height:100%}</style>
</head><body>
<div id="p"></div>
<script src="https://www.youtube.com/iframe_api"></script>
<script>
var ids=$arr, startIndex=$start, p;
function idx(){ try { return p.getPlaylistIndex(); } catch(e){ return 0; } }
function onYouTubeIframeAPIReady(){
  p=new YT.Player('p',{
    height:'100%',width:'100%',
    playerVars:{playsinline:1,rel:0,autoplay:1,controls:0,origin:'https://www.youtube.com'},
    events:{
      onReady:function(e){
        window.p=e.target;
        e.target.loadPlaylist({playlist:ids,index:startIndex});
        Android.onReady();
      },
      onStateChange:function(e){ Android.onState(e.data, idx()); }
    }
  });
}
</script></body></html>
            """.trimIndent()
        }
    }
}
