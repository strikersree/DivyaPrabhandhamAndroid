package com.srinivaskannan.divyaprabhandham

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.srinivaskannan.divyaprabhandham.billing.TipJar
import com.srinivaskannan.divyaprabhandham.data.PrabandhamRepository
import com.srinivaskannan.divyaprabhandham.media.RecitationSession
import com.srinivaskannan.divyaprabhandham.notify.ReminderScheduler
import com.srinivaskannan.divyaprabhandham.prefs.AppState
import com.srinivaskannan.divyaprabhandham.sync.GoogleSyncManager
import com.srinivaskannan.divyaprabhandham.widget.WidgetBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Everything the app needs, built once.
 *
 * There is no dependency-injection framework here on purpose: four objects, one
 * of which is expensive to build, is not a graph. What it does need is an
 * explicit async start, because parsing the corpus takes long enough that doing
 * it on the main thread would trip the ANR watchdog on a slow device.
 *
 * The startup result is published as a **single** [Startup] value, and that is
 * load-bearing rather than tidiness. Publishing the four objects into four
 * separate fields meant composition could observe a half-built app: the write
 * to the first field scheduled a recomposition that ran while the rest were
 * still being assigned, and any field that was not snapshot-backed registered
 * no subscription when it was read as null — so nothing ever scheduled the
 * recomposition that would have picked it up. One atomic write, one
 * subscription, no torn state.
 */
class DivyaPrabhandhamApp : Application() {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    sealed interface Startup {
        /** Parsing the corpus. The splash screen stays up. */
        data object Loading : Startup

        data class Ready(
            val appState: AppState,
            val repository: PrabandhamRepository,
            val sync: GoogleSyncManager,
            val tipJar: TipJar,
            val recitation: RecitationSession,
        ) : Startup

        /**
         * Startup threw. Shown to the reader rather than left as a blank
         * window — a reader app that fails to open should say so.
         */
        data class Failed(val message: String) : Startup
    }

    var startup by mutableStateOf<Startup>(Startup.Loading)
        private set

    private var widgetJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.ensureChannel(this)
        scope.launch { start() }
    }

    private suspend fun start() {
        val ready = try {
            val state = AppState.create(this, scope)
            val repo = PrabandhamRepository.load(this)
            val syncManager = GoogleSyncManager(this, scope)

            state.noteLaunch()

            // One listener drives both side effects of a local change: push it
            // to the person's other devices, and refresh what the widget shows.
            // Both are debounced, because reading writes constantly.
            state.onChanged = {
                syncManager.schedulePush(state)
                scheduleWidgetRefresh(repo, state)
            }

            Startup.Ready(
                appState = state,
                repository = repo,
                sync = syncManager,
                tipJar = TipJar(this, state),
                recitation = RecitationSession(this),
            )
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            // Without this the coroutine would take the process down with it,
            // and the reader would see a crash with nothing to act on.
            startup = Startup.Failed(error.message ?: error::class.java.simpleName)
            return
        }

        // Published only once everything above is wired up.
        startup = ready

        ready.sync.pull(ready.appState)
        ReminderScheduler.reschedule(this, ready.appState)
        scheduleWidgetRefresh(ready.repository, ready.appState)
    }

    private fun scheduleWidgetRefresh(repo: PrabandhamRepository, state: AppState) {
        widgetJob?.cancel()
        widgetJob = scope.launch {
            delay(WIDGET_REFRESH_DEBOUNCE_MS)
            WidgetBridge.refresh(this@DivyaPrabhandhamApp, repo, state)
        }
    }

    private companion object {
        /**
         * Rebuilding the snapshot walks every division, so it must not happen
         * once per scrolled verse. Two seconds after things go quiet is soon
         * enough for a widget that updates hourly.
         */
        const val WIDGET_REFRESH_DEBOUNCE_MS = 2_000L
    }
}
