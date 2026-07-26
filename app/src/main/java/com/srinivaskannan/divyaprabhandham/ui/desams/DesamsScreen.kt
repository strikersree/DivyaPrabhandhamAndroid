package com.srinivaskannan.divyaprabhandham.ui.desams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.DivyaDesam
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.ui.components.EmptyState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository

/**
 * The Divya Desams the Aazhwars sang, grouped by traditional region.
 *
 * Region is the only clean location facet in the data: seven naadus, every
 * temple assigned. `place` is deliberately not offered as a filter — its
 * granularity is inconsistent (whole states like கேரளா sit alongside towns like
 * நாங்கூர்), so a picker built on it would read as a data bug. It is still
 * matched by the search field, where mixed granularity costs nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesamsScreen(
    onOpenDesam: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val script = appState.scriptChoice

    var query by rememberSaveable { mutableStateOf("") }
    var region by rememberSaveable { mutableStateOf<String?>(null) }

    val regions = remember(script) {
        repository.divyaDesams.map { it.region(script) }.distinct()
    }

    val groups = remember(query, region, script) {
        val needle = query.trim().lowercase()
        repository.desamsByRegion(script).mapNotNull { (groupRegion, desams) ->
            if (region != null && groupRegion != region) return@mapNotNull null
            if (needle.isEmpty()) return@mapNotNull groupRegion to desams
            // Matches name, place, region and both deity names, so "Kerala",
            // "Ranganatha" and "நாங்கூர்" all find their temples.
            val hits = desams.filter { desam ->
                listOf(
                    desam.name(script), desam.place(script), desam.region(script),
                    desam.perumal(script).orEmpty(), desam.thaayar(script).orEmpty(),
                ).any { it.lowercase().contains(needle) }
            }
            if (hits.isEmpty()) null else groupRegion to hits
        }
    }

    val matchCount = groups.sumOf { it.second.size }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(appState.ui(Ui.DIVYA_DESAMS)) }) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(appState.ui(Ui.DESAM_SEARCH_PROMPT)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Chips rather than a menu: with seven regions every option stays
            // visible, and the active filter is legible at a glance.
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "all") {
                    FilterChip(
                        selected = region == null,
                        onClick = { region = null },
                        label = { Text(appState.ui(Ui.DESAM_ALL_REGIONS)) },
                    )
                }
                items(regions, key = { it }) { name ->
                    FilterChip(
                        selected = region == name,
                        onClick = { region = if (region == name) null else name },
                        label = { Text(name) },
                    )
                }
            }

            if (repository.divyaDesams.isEmpty()) {
                EmptyState(
                    title = appState.ui(Ui.DIVYA_DESAMS),
                    message = appState.ui(Ui.COMING_SOON),
                    icon = Icons.Filled.AccountBalance,
                )
                return@Column
            }
            if (matchCount == 0) {
                EmptyState(title = appState.ui(Ui.NO_RESULTS))
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                groups.forEach { (groupRegion, desams) ->
                    item(key = "region-$groupRegion") {
                        Text(
                            text = groupRegion,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(
                                start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp,
                            ),
                        )
                    }
                    items(desams, key = { it.id }) { desam ->
                        DesamRow(desam) { onOpenDesam(desam.id) }
                    }
                }
            }
        }
    }
}

/**
 * A temple row.
 *
 * Purpose-built rather than the shared ListRow because the verse count is the
 * problem it solves: as a right-aligned trailing column, a label like
 * "21 பாசுரங்கள்" claimed a third of the width and forced long Tamil temple
 * names to wrap after one or two words. Here the count is a compact pill on the
 * same line as the name, sharing the title's baseline, so the name gets almost
 * the full width and wraps only when it genuinely must.
 */
@Composable
private fun DesamRow(desam: DivyaDesam, onClick: () -> Unit) {
    val appState = LocalAppState.current
    val script = appState.scriptChoice
    val subtitle = buildString {
        append(desam.place(script))
        desam.perumal(script)?.let { append(" · ").append(it) }
    }
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = desam.name(script),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Just the number in the pill; the word "பாசுரங்கள்" is what made
            // the old trailing column wide, and the pill's context makes it
            // redundant.
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = "${desam.pasurams.size}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
    }
}
