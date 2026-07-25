package com.srinivaskannan.divyaprabhandham

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
        val app = application as DivyaPrabhandhamApp
        val state = app.appState ?: return@registerForActivityResult
        app.sync?.onConsentResult(result.resultCode == RESULT_OK, state)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DivyaPrabhandhamApp
        splash.setKeepOnScreenCondition { app.repository == null || app.appState == null }

        deepLink = parseDeepLink(intent)

        setContent {
            val repository = app.repository
            val appState = app.appState
            val sync = app.sync
            val tipJar = app.tipJar

            if (repository == null || appState == null || sync == null || tipJar == null) {
                // The splash screen is still up; drawing anything here would
                // only flash behind it.
                return@setContent
            }

            LaunchedEffect(Unit) { tipJar.connect(this@MainActivity) }

            // A pending Drive grant needs an activity to launch from, so it is
            // surfaced here rather than inside the sync manager.
            LaunchedEffect(sync.pendingConsent) {
                sync.pendingConsent?.let { pending ->
                    consentLauncher.launch(
                        androidx.activity.result.IntentSenderRequest
                            .Builder(pending.intentSender)
                            .build(),
                    )
                }
            }

            // Pull on resume, so a verse bookmarked on another device is there
            // when this one comes back to the foreground. DisposableEffect,
            // not LaunchedEffect: the observer has to come back off again.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) sync.pull(appState)
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            DivyaPrabhandhamTheme(appState = appState, repository = repository) {
                AppScaffold(
                    sync = sync,
                    tipJar = tipJar,
                    deepLink = deepLink,
                    onDeepLinkHandled = { deepLink = null },
                )
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
