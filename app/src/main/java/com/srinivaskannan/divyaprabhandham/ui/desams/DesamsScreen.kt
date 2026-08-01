package com.srinivaskannan.divyaprabhandham.ui.desams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

    // Long-press status flow: the temple whose status sheet is open, and a
    // one-shot celebration trigger (the desam id just marked visited, and
    // whether that visit crossed a level threshold).
    var statusTarget by remember { mutableStateOf<DivyaDesam?>(null) }
    var celebration by remember { mutableStateOf<Celebration?>(null) }

    statusTarget?.let { target ->
        DesamStatusSheet(
            desam = target,
            visited = appState.isVisited(target.id),
            visitedYear = appState.visitYear(target.id),
            onVisited = { year ->
                val before = appState.visitedCount
                appState.markVisited(target.id, year)
                val after = appState.visitedCount
                val leveledUp = com.srinivaskannan.divyaprabhandham.data.Pilgrimage
                    .currentLevel(after)?.takeIf {
                        com.srinivaskannan.divyaprabhandham.data.Pilgrimage.currentLevel(before) != it
                    }
                statusTarget = null
                celebration = Celebration(leveledUp = leveledUp != null)
            },
            onClear = {
                appState.clearVisited(target.id)
                statusTarget = null
            },
            onDismiss = { statusTarget = null },
        )
    }

    celebration?.let {
        ConfettiCelebration(
            big = it.leveledUp,
            onFinished = { celebration = null },
        )
    }

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
                        // The two Eternal Abodes (Thirupparkadal, Paramapadam)
                        // are not places one visits, so they get no status menu.
                        val visitable = desam.regionEn != "Eternal Abodes"
                        DesamRow(
                            desam = desam,
                            visited = appState.isVisited(desam.id),
                            onClick = { onOpenDesam(desam.id) },
                            onLongPress = if (visitable) {
                                { statusTarget = desam }
                            } else null,
                        )
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesamRow(
    desam: DivyaDesam,
    visited: Boolean,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
) {
    val appState = LocalAppState.current
    val script = appState.scriptChoice
    val subtitle = buildString {
        append(desam.place(script))
        desam.perumal(script)?.let { append(" · ").append(it) }
    }
    // Visited temples are tinted to stand out in the list — the "highlight the
    // places you've been" the feature asks for.
    val rowColor = if (visited) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    else MaterialTheme.colorScheme.surface
    Surface(
        color = rowColor,
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongPress,
        ),
    ) {
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
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (visited) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = desam.name(script),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

/** One-shot celebration request: whether the visit crossed a level threshold. */
private data class Celebration(val leveledUp: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesamStatusSheet(
    desam: DivyaDesam,
    visited: Boolean,
    visitedYear: Int?,
    onVisited: (Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val appState = LocalAppState.current
    val script = appState.scriptChoice
    var pickingYear by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Text(
                desam.name(script),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                desam.place(script),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            if (!pickingYear) {
                // Visited (with the year) and, when already visited, a clear.
                Surface(
                    onClick = { pickingYear = true },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.Place, contentDescription = null)
                        Text(
                            if (visited && visitedYear != null)
                                "${appState.ui(Ui.DESAM_VISITED)} · $visitedYear"
                            else appState.ui(Ui.DESAM_VISITED),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                if (visited) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onClear) {
                        Text(appState.ui(Ui.DESAM_CLEAR_VISIT))
                    }
                }
            } else {
                Text(
                    appState.ui(Ui.DESAM_PICK_YEAR),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                YearPicker(
                    initial = visitedYear,
                    onPick = onVisited,
                )
            }
        }
    }
}

/** A scrollable year list, 1900..current, newest first. */
@Composable
private fun YearPicker(initial: Int?, onPick: (Int) -> Unit) {
    val currentYear = remember {
        java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    }
    val years = remember(currentYear) { (currentYear downTo 1900).toList() }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
        items(years, key = { it }) { year ->
            Surface(
                onClick = { onPick(year) },
                color = if (year == initial) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "$year",
                    Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (year == initial) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}
