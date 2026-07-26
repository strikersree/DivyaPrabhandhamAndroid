package com.srinivaskannan.divyaprabhandham.ui.browse

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.Division
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.data.Work
import com.srinivaskannan.divyaprabhandham.ui.components.EmptyState
import com.srinivaskannan.divyaprabhandham.ui.components.BackButton
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository
import kotlinx.coroutines.launch

/**
 * The works of one division, each expanding to show its sections.
 *
 * Long-pressing a work pins it to Home — the Android equivalent of the iOS
 * context menu, and the same gesture, so the muscle memory carries over.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DivisionBrowserScreen(
    division: Division,
    onBack: () -> Unit,
    onOpenSection: (String) -> Unit,
    onListen: (workId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val works = repository.works(division)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // A division with a single work reads better pre-expanded.
    val expanded = remember(division.id) {
        mutableStateListOf<String>().also { list ->
            works?.singleOrNull()?.let { list.add(it.id) }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            TopAppBar(
                title = { Text(division.title(appState.scriptChoice)) },
                navigationIcon = { BackButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        if (works == null) {
            EmptyState(
                title = division.title(appState.scriptChoice),
                message = appState.ui(Ui.COMING_SOON),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            works.forEach { work ->
                item(key = "work-${work.id}") {
                    WorkRow(
                        work = work,
                        isExpanded = work.id in expanded,
                        onToggle = {
                            if (work.id in expanded) expanded.remove(work.id)
                            else expanded.add(work.id)
                        },
                        onPin = {
                            val wasPinned = appState.isPinned(work.id)
                            // A long press with no visible result reads as a
                            // missed tap, so every outcome says something.
                            val message = when {
                                wasPinned -> appState.ui(Ui.UNPIN_FROM_HOME)
                                !appState.canPinMore -> appState.ui(Ui.PIN_LIMIT)
                                else -> appState.ui(Ui.PIN_TO_HOME)
                            }
                            if (wasPinned || appState.canPinMore) {
                                appState.togglePin(work.id)
                            }
                            scope.launch { snackbars.showSnackbar(message) }
                        },
                        // In-app player when a recitation is mapped for this
                        // work; otherwise say so rather than opening an empty
                        // player.
                        hasRecitation = repository.hasRecitation(work.id),
                        onListen = {
                            if (repository.hasRecitation(work.id)) onListen(work.id)
                            else scope.launch {
                                snackbars.showSnackbar(appState.ui(Ui.LISTEN_UNAVAILABLE))
                            }
                        },
                    )
                }

                if (work.id in expanded) {
                    items(work.sections.size, key = { "s-${work.sections[it].id}" }) { index ->
                        val section = work.sections[index]
                        Surface(
                            onClick = { onOpenSection(section.id) },
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text(
                                    text = section.title(appState.scriptChoice),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                section.pasuramRange?.let { range ->
                                    Text(
                                        text = "${appState.ui(Ui.PASURAM)} ${range.first}–${range.last}" +
                                            " · ${range.last - range.first + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "divider-${work.id}") { HorizontalDivider() }
            }
        }
    }
}

@Composable
private fun WorkRow(
    work: Work,
    isExpanded: Boolean,
    hasRecitation: Boolean,
    onToggle: () -> Unit,
    onPin: () -> Unit,
    onListen: () -> Unit,
) {
    val appState = LocalAppState.current
    val pinned = appState.isPinned(work.id)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.combinedClickable(
            onClick = onToggle,
            onLongClick = onPin,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = work.title(appState.scriptChoice),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (pinned) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = appState.ui(Ui.PINNED),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Text(
                    text = work.author(appState.scriptChoice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                work.pasuramRange?.let { range ->
                    Text(
                        text = "${work.sections.size} ${appState.ui(Ui.SECTIONS)}" +
                            " · ${appState.ui(Ui.PASURAM)} ${range.first}–${range.last}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onListen) {
                Icon(
                    Icons.Filled.PlayCircle,
                    contentDescription = appState.ui(Ui.LISTEN),
                    // Dimmed when no recitation is mapped, so the affordance
                    // reads as unavailable rather than broken.
                    tint = if (hasRecitation) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
