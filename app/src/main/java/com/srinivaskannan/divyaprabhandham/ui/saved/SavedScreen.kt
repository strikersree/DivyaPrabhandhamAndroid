package com.srinivaskannan.divyaprabhandham.ui.saved

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.ui.components.EmptyState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository

/**
 * Saved pasurams.
 *
 * Swipe removes a bookmark, which is the Android equivalent of the iOS list's
 * swipe-to-delete. Bookmarks whose section no longer resolves are skipped
 * rather than shown as broken rows — that can happen if the corpus is
 * re-generated and a section id changes between releases.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    onOpenSection: (sectionId: String, stanzaKey: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current

    val rows = appState.bookmarks.mapNotNull { key ->
        repository.stanzaForKey(key)?.let { (section, stanza) -> Triple(key, section, stanza) }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(appState.ui(Ui.SAVED)) }) },
    ) { padding ->
        if (rows.isEmpty()) {
            EmptyState(
                title = appState.ui(Ui.NO_BOOKMARKS),
                message = appState.ui(Ui.NO_BOOKMARKS_HINT),
                icon = Icons.Outlined.StarOutline,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(rows, key = { it.first }) { (key, section, stanza) ->
                val dismissState = rememberSwipeToDismissBoxState()

                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        appState.removeBookmark(key)
                    }
                }

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = appState.ui(Ui.REMOVE_BOOKMARK),
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
                                Icons.Filled.Star,
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
