package com.srinivaskannan.divyaprabhandham.ui.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.data.Work
import com.srinivaskannan.divyaprabhandham.ui.components.BackButton
import com.srinivaskannan.divyaprabhandham.ui.components.EmptyState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository

/**
 * The pasurams inside one collection, in the order they were added. Swipe
 * removes a pasuram from *this* collection (it stays wherever else it might
 * be) — a per-item action, distinct from the collection cards on the main
 * list, which deliberately have no swipe.
 *
 * "Add a whole work" lives here rather than as a long-press in Browse: the
 * Browse work row's long-press is already pin-to-Home, and overloading it
 * risked a real regression on a feature that already works. Adding it from
 * inside the collection you're building is also, if anything, the more
 * natural place for it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    collectionId: String,
    onBack: () -> Unit,
    onOpenSection: (sectionId: String, stanzaKey: String?) -> Unit,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val collection = appState.collection(collectionId)
    var showAddWork by remember { mutableStateOf(false) }

    if (collection == null) {
        // Deleted from another device mid-view — back out gracefully.
        LaunchedEffect(Unit) { onBack() }
        return
    }

    if (showAddWork) {
        AddWorkSheet(
            onDismiss = { showAddWork = false },
            onPick = { work ->
                appState.addAllToCollection(collectionId, repository.allPasuramKeys(work.id))
                showAddWork = false
            },
        )
    }

    val rows = collection.pasuramKeys.mapNotNull { key ->
        repository.stanzaForKey(key, appState.scriptChoice)?.let { (section, stanza) -> Triple(key, section, stanza) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(collection.name) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { showAddWork = true }) {
                        Icon(Icons.Filled.Add, contentDescription = appState.ui(Ui.ADD_WHOLE_WORK))
                    }
                },
            )
        },
    ) { padding ->
        if (rows.isEmpty()) {
            EmptyState(
                title = appState.ui(Ui.NO_COLLECTIONS),
                message = appState.ui(Ui.NO_COLLECTIONS_HINT),
                icon = Icons.Outlined.QueueMusic,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(rows, key = { it.first }) { (key, section, stanza) ->
                val dismissState = rememberSwipeToDismissBoxState()

                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        appState.removeFromCollection(collectionId, key)
                    }
                }

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            Modifier.fillMaxSize().padding(horizontal = 24.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = appState.ui(Ui.REMOVE_FROM_COLLECTION),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                ) {
                    Surface(
                        onClick = { onOpenSection(section.id, key) },
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = stanza.number
                                        ?.let { "${appState.ui(Ui.PASURAM)} $it" }
                                        ?: section.title(appState.scriptChoice),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = stanza.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                repository.workContaining(section.id)?.let { work ->
                                    Text(
                                        text = "${work.title(appState.scriptChoice)} · " +
                                            section.title(appState.scriptChoice),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A flat, searchless list of every work, for adding one wholesale. Small
 *  enough (a few dozen works across all divisions) that a plain scrollable
 *  sheet is enough — no search needed yet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWorkSheet(onDismiss: () -> Unit, onPick: (Work) -> Unit) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(repository.allWorks, key = { it.id }) { work ->
                Surface(
                    onClick = { onPick(work) },
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        work.title(appState.scriptChoice),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}
