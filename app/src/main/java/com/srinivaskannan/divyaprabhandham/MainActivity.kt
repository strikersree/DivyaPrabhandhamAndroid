package com.srinivaskannan.divyaprabhandham

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DivyaPrabhandhamApp
        splash.setKeepOnScreenCondition {
            app.startup is DivyaPrabhandhamApp.Startup.Loading
        }

        deepLink = parseDeepLink(intent)

        setContent {
            when (val startup = app.startup) {
                // The splash screen is still up; drawing anything here would
                // only flash behind it.
                is DivyaPrabhandhamApp.Startup.Loading -> Unit

                is DivyaPrabhandhamApp.Startup.Failed -> StartupFailure(startup.message)

                is DivyaPrabhandhamApp.Startup.Ready -> {
                    val appState = startup.appState
                    val sync = startup.sync

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
                            deepLink = deepLink,
                            onDeepLinkHandled = { deepLink = null },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseDeepLink(intent)?.let { deepLink = it }
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
