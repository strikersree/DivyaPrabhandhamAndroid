package com.srinivaskannan.divyaprabhandham

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.srinivaskannan.divyaprabhandham.billing.TipJar
import com.srinivaskannan.divyaprabhandham.data.PrabandhamRepository
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
 */
class DivyaPrabhandhamApp : Application() {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Null until [start] finishes. The splash screen holds until it is not. */
    var repository by mutableStateOf<PrabandhamRepository?>(null)
        private set

    var appState by mutableStateOf<AppState?>(null)
        private set

    var sync: GoogleSyncManager? = null
        private set

    var tipJar: TipJar? = null
        private set

    private var widgetJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.ensureChannel(this)
        scope.launch { start() }
    }

    private suspend fun start() {
        val state = AppState.create(this, scope)
        val repo = PrabandhamRepository.load(this)
        val syncManager = GoogleSyncManager(this, scope)

        state.noteLaunch()

        // One listener drives both side effects of a local change: push it to
        // the person's other devices, and refresh what the widget shows. Both
        // are debounced, because reading writes constantly.
        state.onChanged = {
            syncManager.schedulePush(state)
            scheduleWidgetRefresh(repo, state)
        }

        appState = state
        repository = repo
        sync = syncManager
        tipJar = TipJar(this, state)

        syncManager.pull(state)
        ReminderScheduler.reschedule(this, state)
        scheduleWidgetRefresh(repo, state)
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
