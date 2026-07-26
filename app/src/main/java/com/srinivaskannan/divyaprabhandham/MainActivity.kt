package com.srinivaskannan.divyaprabhandham

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.srinivaskannan.divyaprabhandham.ui.nav.AppScaffold
import com.srinivaskannan.divyaprabhandham.ui.nav.DeepLink
import com.srinivaskannan.divyaprabhandham.ui.theme.DivyaPrabhandhamTheme

/**
 * The single activity.
 *
 * The splash screen is held until the corpus has finished parsing, which is the
 * honest thing to do: showing an empty Home for a second and then popping five
 * divisions into it looks broken, and there is genuinely nothing to display
 * until the JSON is in memory.
 */
class MainActivity : ComponentActivity() {

    private var deepLink by mutableStateOf<DeepLink?>(null)

    /**
     * Google's consent screen for the Drive scope. Registered here because the
     * result contract has to be set up before the activity starts.
     */
    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val ready = (application as DivyaPrabhandhamApp).startup
        if (ready is DivyaPrabhandhamApp.Startup.Ready) {
            ready.sync.onConsentResult(result.resultCode == RESULT_OK, ready.appState)
        }
    }

    private companion object {
        // Roughly the system splash's own minimum; past this we show the
        // branded Compose screen rather than freezing on the system splash.
        const val SPLASH_HOLD_MS = 900L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DivyaPrabhandhamApp

        // Hold the system splash through the corpus parse when it is quick — its
        // centred-emblem-on-maroon is enough, and it avoids flashing a second
        // splash for a few frames. Only if loading outlasts the system splash
        // does the Compose branded screen take over (see BrandedSplash). On a
        // fast device the user sees one splash and then content; on a slow one
        // the branded screen bridges the wait.
        splash.setKeepOnScreenCondition {
            app.startup is DivyaPrabhandhamApp.Startup.Loading &&
                android.os.SystemClock.uptimeMillis() - startedAt < SPLASH_HOLD_MS
        }

        deepLink = parseDeepLink(intent)

        setContent {
            when (val startup = app.startup) {
                is DivyaPrabhandhamApp.Startup.Loading -> {
                    // Light system icons over the dark artwork.
                    LaunchedEffect(Unit) { applyBarStyle(dark = true) }
                    BrandedSplash()
                }

                is DivyaPrabhandhamApp.Startup.Failed -> StartupFailure(startup.message)

                is DivyaPrabhandhamApp.Startup.Ready -> {
                    val appState = startup.appState
                    val sync = startup.sync

                    // System bar icons follow the app's own light/dark choice,
                    // not just the device's — the two can disagree.
                    val systemDark = isSystemInDarkTheme()
                    val dark = appState.forcedDarkMode ?: systemDark
                    LaunchedEffect(dark) { applyBarStyle(dark) }

                    LaunchedEffect(Unit) { startup.tipJar.connect(this@MainActivity) }

                    // A pending Drive grant needs an activity to launch from,
                    // so it is surfaced here rather than in the sync manager.
                    LaunchedEffect(sync.pendingConsent) {
                        sync.pendingConsent?.let { pending ->
                            consentLauncher.launch(
                                IntentSenderRequest.Builder(pending.intentSender).build(),
                            )
                        }
                    }

                    // Pull on resume, so a verse bookmarked on another device
                    // is there when this one comes back to the foreground.
                    // DisposableEffect, not LaunchedEffect: the observer has to
                    // come back off again.
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) sync.pull(appState)
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    DivyaPrabhandhamTheme(
                        appState = appState,
                        repository = startup.repository,
                    ) {
                        AppScaffold(
                            sync = sync,
                            tipJar = startup.tipJar,
                            recitation = startup.recitation,
                            deepLink = deepLink,
                            onDeepLinkHandled = { deepLink = null },
                        )
                    }
                }
            }
        }
    }

    private fun applyBarStyle(dark: Boolean) {
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = if (dark) SystemBarStyle.dark(transparent)
            else SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = if (dark) SystemBarStyle.dark(transparent)
            else SystemBarStyle.light(transparent, transparent),
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseDeepLink(intent)?.let { deepLink = it }
    }

    override fun onDestroy() {
        // Only tear the player down on a real finish, not on a rotation or
        // other config change — otherwise the recitation would stop every time
        // the screen turned.
        if (isFinishing) {
            (application as DivyaPrabhandhamApp).let { app ->
                (app.startup as? DivyaPrabhandhamApp.Startup.Ready)?.recitation?.stop()
            }
        }
        super.onDestroy()
    }

    /**
     * Reads the widget and notification links:
     *   divyaprabhandham://resume
     *   divyaprabhandham://open?section=<id>&key=<stanzaKey>
     */
    private fun parseDeepLink(intent: Intent?): DeepLink? {
        val uri: Uri = intent?.data ?: return null
        if (uri.scheme != "divyaprabhandham") return null
        return when (uri.host) {
            "resume" -> DeepLink.Resume
            "open" -> {
                val section = uri.getQueryParameter("section") ?: return null
                DeepLink.OpenVerse(section, uri.getQueryParameter("key"))
            }
            else -> null
        }
    }
}

/**
 * The loading screen shown only when the corpus parse outlasts the system
 * splash — on a fast device it never appears, because the system splash is held
 * through the quick parse and then content draws directly. On a slow device it
 * bridges the remaining wait with the full artwork.
 *
 * Cropped rather than fitted, so it fills the screen with no letterbox bars.
 * That is only safe because the asset is pre-padded vertically to a 0.415 ratio
 * — narrower than any normal phone — so the crop is always taken out of the
 * padding rather than the sides. The wordmark reaches within 3.4% of the
 * artwork's edge; cropping the unpadded art to fill a 20:9 screen would have
 * sliced the first and last letters off "Divya Prabhandham".
 */
@Composable
private fun BrandedSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6A0F1A)),
    ) {
        Image(
            painter = painterResource(R.drawable.splash_artwork),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Shown when the corpus could not be loaded. Deliberately plain — no theme,
 * no app state, nothing that depends on the thing that just failed. It exists
 * so a startup failure reads as a failure rather than as a blank window.
 */
@Composable
private fun StartupFailure(message: String) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "நாலாயிர திவ்ய பிரபந்தம்\n\n" +
                        "The verses could not be loaded.\n$message",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
