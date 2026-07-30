package com.srinivaskannan.divyaprabhandham.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.ui.components.EmptyState
import com.srinivaskannan.divyaprabhandham.ui.components.ListRow
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository

/**
 * Search across the corpus, plus jump-to-pasuram.
 *
 * A bare number is treated as a pasuram number first and a text query second,
 * because that is overwhelmingly what it means: someone who types 474 wants
 * Thiruppavai's first verse, not every line that happens to contain "474".
 * Text search matches the active script's forms with Tamil as a fallback, so a
 * query finds its verse whichever script it was typed in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenSection: (sectionId: String, stanzaKey: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current

    var query by rememberSaveable { mutableStateOf("") }
    val trimmed = query.trim()

    val jumpTarget = remember(trimmed) {
        trimmed.toIntOrNull()?.let { number ->
            repository.location(number)?.let { number to it }
        }
    }

    // Full-text search over 3,884 verses is not free; recompute only when the
    // query or the script actually changes, not on every recomposition.
    val results = remember(trimmed, appState.scriptChoice) {
        if (trimmed.isEmpty()) emptyList()
        else repository.filteredWorks(trimmed, appState.scriptChoice)
    }

    // No app bar here either, so the search field needs the inset itself.
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = {
                        // Record only genuine text searches; a bare number is a
                        // jump-to-pasuram, not a search worth remembering.
                        if (trimmed.isNotEmpty() && trimmed.toIntOrNull() == null) {
                            appState.noteSearch(trimmed)
                        }
                    },
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text(appState.ui(Ui.SEARCH_OR_NUMBER)) },
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {}

        if (trimmed.isEmpty()) {
            val recents = appState.recentSearches
            if (recents.isEmpty()) {
                EmptyState(
                    title = appState.ui(Ui.SEARCH),
                    message = appState.ui(Ui.SEARCH_OR_NUMBER),
                )
            } else {
                RecentSearches(
                    searches = recents,
                    onPick = { query = it },
                    onClear = { appState.clearRecentSearches() },
                )
            }
            return@Column
        }

        if (results.isEmpty() && jumpTarget == null) {
            EmptyState(title = appState.ui(Ui.NO_RESULTS))
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            jumpTarget?.let { (number, location) ->
                item(key = "jump") {
                    ListRow(
                        title = "${appState.ui(Ui.PASURAM)} $number",
                        leading = Icons.Filled.Numbers,
                        onClick = { onOpenSection(location.first, location.second) },
                    )
                }
            }

            results.forEach { work ->
                item(key = "work-${work.id}") {
                    Text(
                        text = work.title(appState.scriptChoice),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp,
                        ),
                    )
                }
                items(work.sections.size, key = { "s-${work.sections[it].id}" }) { index ->
                    val section = work.sections[index]
                    ListRow(
                        title = section.title(appState.scriptChoice),
                        subtitle = section.pasuramRange?.let {
                            "${appState.ui(Ui.PASURAM)} ${it.first}–${it.last}"
                        },
                        showChevron = false,
                        onClick = {
                            // Opening a result is a strong signal the query was
                            // meaningful — record it even if the person never
                            // pressed the keyboard's search key. Bare numbers
                            // are jumps, so they are still excluded.
                            if (trimmed.toIntOrNull() == null) appState.noteSearch(trimmed)
                            onOpenSection(section.id, null)
                        },
                    )
                }
            }
        }
    }
}

/**
 * The search tab's resting state once the person has searched before: their
 * last few queries as tappable chips, newest first, with a clear action.
 * Tapping a chip refills the field and re-runs the search.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecentSearches(
    searches: List<String>,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
) {
    val appState = LocalAppState.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = appState.ui(Ui.RECENT_SEARCHES),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            TextButton(onClick = onClear) {
                Text(appState.ui(Ui.CLEAR))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            searches.forEach { term ->
                AssistChip(
                    onClick = { onPick(term) },
                    label = { Text(term, maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            modifier = Modifier.padding(0.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        }
    }
}
