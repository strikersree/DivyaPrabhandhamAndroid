package com.srinivaskannan.divyaprabhandham.ui.collections

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.prefs.UserCollection
import com.srinivaskannan.divyaprabhandham.ui.components.EmptyState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState

/**
 * The Collections tab: static rectangular cards, one per collection, newest
 * first. No drag-reorder, no swipe — rename/delete/pin-to-Home are offered
 * through long-press and a card-level menu instead, matching the iOS
 * implementation's deliberate choice to keep this list plain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    onOpenCollection: (collectionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    var showCreate by rememberSaveable { mutableStateOf(false) }

    if (showCreate) {
        CreateCollectionDialog(
            onDismiss = { showCreate = false },
            onCreate = { name ->
                val created = appState.createCollection(name)
                showCreate = false
                onOpenCollection(created.id)
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(appState.ui(Ui.COLLECTIONS)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = appState.ui(Ui.NEW_COLLECTION))
            }
        },
    ) { padding ->
        val collections = appState.collections.reversed()
        if (collections.isEmpty()) {
            EmptyState(
                title = appState.ui(Ui.NO_COLLECTIONS),
                message = appState.ui(Ui.NO_COLLECTIONS_HINT),
                icon = Icons.Outlined.CollectionsBookmark,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(collections, key = { it.id }) { collection ->
                CollectionCard(
                    collection = collection,
                    pinned = appState.isCollectionPinned(collection.id),
                    onOpen = { onOpenCollection(collection.id) },
                    onTogglePin = { appState.togglePinCollection(collection.id) },
                    onDelete = { appState.deleteCollection(collection.id) },
                    onRename = { newName -> appState.renameCollection(collection.id, newName) },
                )
            }
        }
    }
}

@Composable
private fun CollectionCard(
    collection: UserCollection,
    pinned: Boolean,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
) {
    val appState = LocalAppState.current
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    if (renaming) {
        RenameCollectionDialog(
            initial = collection.name,
            onDismiss = { renaming = false },
            onRename = { newName -> onRename(newName); renaming = false },
        )
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(collection.name) },
            text = { Text(appState.ui(Ui.DELETE_COLLECTION)) },
            confirmButton = {
                TextButton(onClick = { onDelete(); deleting = false }) {
                    Text(appState.ui(Ui.DELETE_COLLECTION))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = false }) { Text(appState.ui(Ui.BACK)) }
            },
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(onClick = onOpen, onLongClick = { menuOpen = true }),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.Filled.Collections,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${collection.pasuramKeys.size} ${appState.ui(Ui.COLLECTION_PASURAM_COUNT)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (pinned) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = appState.ui(Ui.UNPIN_COLLECTION),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(appState.ui(Ui.RENAME_COLLECTION)) },
                onClick = { menuOpen = false; renaming = true },
            )
            DropdownMenuItem(
                text = {
                    Text(appState.ui(if (pinned) Ui.UNPIN_COLLECTION else Ui.PIN_COLLECTION))
                },
                leadingIcon = {
                    Icon(
                        if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = null,
                    )
                },
                onClick = { menuOpen = false; onTogglePin() },
            )
            DropdownMenuItem(
                text = { Text(appState.ui(Ui.DELETE_COLLECTION)) },
                onClick = { menuOpen = false; deleting = true },
            )
        }
    }
}

@Composable
private fun CreateCollectionDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    val appState = LocalAppState.current
    var text by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appState.ui(Ui.NEW_COLLECTION)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(appState.ui(Ui.COLLECTION_NAME_PLACEHOLDER)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(text.trim().ifBlank { appState.ui(Ui.NEW_COLLECTION) }) },
            ) { Text(appState.ui(Ui.CREATE_COLLECTION)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(appState.ui(Ui.BACK)) }
        },
    )
}

@Composable
private fun RenameCollectionDialog(
    initial: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    val appState = LocalAppState.current
    var text by rememberSaveable(initial) { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appState.ui(Ui.RENAME_COLLECTION)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(text.trim().ifBlank { initial }) }) {
                Text(appState.ui(Ui.CREATE_COLLECTION))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(appState.ui(Ui.BACK)) }
        },
    )
}
