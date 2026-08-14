package com.srinivaskannan.divyaprabhandham.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.Division
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.data.Work
import com.srinivaskannan.divyaprabhandham.ui.components.AnimatedMeshBackground
import com.srinivaskannan.divyaprabhandham.ui.components.ListRow
import com.srinivaskannan.divyaprabhandham.prefs.AppState
import com.srinivaskannan.divyaprabhandham.ui.components.rememberReduceMotion
import com.srinivaskannan.divyaprabhandham.ui.theme.DivisionPalette
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository

/**
 * Home.
 *
 * The order is the reading order a returning person wants: the four thousand
 * first as a carousel of hero cards, then whatever they pinned, then today's
 * Margazhi verse if we are in the season, then where they have been. Continue
 * Reading is not here — it lives in the bottom accessory pill, app-wide, so it
 * is reachable from every tab rather than only this one.
 */
@Composable
fun HomeScreen(
    onOpenDivision: (String) -> Unit,
    onOpenSection: (sectionId: String, stanzaKey: String?) -> Unit,
    onOpenFavourites: () -> Unit,
    onOpenCollection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val margazhiDay = repository.margazhiDayToday()

    LazyColumn(
        // Home has no app bar, so nothing else is holding the content clear of
        // the status bar. Without this the title draws over the clock.
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item(key = "divisions") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = appState.ui(Ui.FOUR_THOUSANDS),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(repository.divisions, key = { it.id }) { division ->
                        DivisionHeroCard(
                            division = division,
                            works = repository.works(division),
                            onClick = { onOpenDivision(division.id) },
                        )
                    }
                }
            }
        }

        item(key = "pilgrimage") {
            PilgrimageProgressCard(
                visited = appState.visitedCount,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item(key = "favourites") {
            ListRow(
                title = appState.ui(Ui.FAVOURITES),
                leading = Icons.Filled.Star,
                leadingTint = MaterialTheme.colorScheme.primary,
                onClick = onOpenFavourites,
            )
        }

        if (appState.pinnedWorks.isNotEmpty()) {
            item(key = "pinned") {
                PinnedSection(
                    onOpenSection = { onOpenSection(it, null) },
                    onOpenCollection = onOpenCollection,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (margazhiDay != null) {
            val pasuram = repository.thiruppavaiPasuram(margazhiDay)
            val location = pasuram?.let { repository.location(it) }
            if (location != null) {
                item(key = "margazhi") {
                    MargazhiCard(
                        day = margazhiDay,
                        onClick = { onOpenSection(location.first, location.second) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }

        if (appState.recentlyViewed.isNotEmpty()) {
            item(key = "recent") {
                SectionHeader(appState.ui(Ui.RECENTLY_VIEWED))
            }
            items(
                items = appState.recentlyViewed.take(6),
                key = { "recent-$it" },
            ) { sectionId ->
                val section = repository.section(sectionId)
                if (section != null) {
                    RowCard(
                        title = section.title(appState.scriptChoice),
                        subtitle = repository.workContaining(sectionId)
                            ?.title(appState.scriptChoice),
                        onClick = { onOpenSection(sectionId, null) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }

        if (appState.bookmarks.isNotEmpty()) {
            item(key = "bookmarks") {
                SectionHeader(appState.ui(Ui.BOOKMARKS))
            }
            items(
                items = appState.bookmarks.take(8),
                key = { "bookmark-$it" },
            ) { key ->
                val found = repository.stanzaForKey(key, appState.scriptChoice)
                if (found != null) {
                    val (section, stanza) = found
                    RowCard(
                        title = stanza.number?.let { "${appState.ui(Ui.PASURAM)} $it" }
                            ?: section.title(appState.scriptChoice),
                        subtitle = stanza.text.lineSequence().firstOrNull(),
                        leading = Icons.Filled.Star,
                        onClick = { onOpenSection(section.id, key) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

/**
 * A division as a large poster card: an animated mesh in its own palette, with
 * the title and pasuram range set into the darker foot of the card.
 */
@Composable
private fun DivisionHeroCard(
    division: Division,
    works: List<Work>?,
    onClick: () -> Unit,
) {
    val appState = LocalAppState.current
    val reduceMotion = rememberReduceMotion()
    val highContrast = appState.isHighContrast

    val subtitle = remember(works, division, appState.scriptChoice) {
        buildSubtitle(division, works, appState)
    }
    val label = "${division.title(appState.scriptChoice)}, $subtitle"

    Box(
        modifier = Modifier
            .width(320.dp)
            .height(250.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = label },
    ) {
        if (highContrast) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        } else {
            AnimatedMeshBackground(
                colors = DivisionPalette.colors(division.id),
                animated = !reduceMotion,
                modifier = Modifier.fillMaxSize(),
            )
            // A scrim so the title stays legible wherever the mesh happens to
            // be light when it drifts.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.55f),
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = division.title(appState.scriptChoice),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (highContrast) MaterialTheme.colorScheme.onSurface else Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (highContrast) MaterialTheme.colorScheme.onSurfaceVariant
                else Color.White.copy(alpha = 0.92f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun buildSubtitle(
    division: Division,
    works: List<Work>?,
    appState: com.srinivaskannan.divyaprabhandham.prefs.AppState,
): String {
    if (works == null) return division.detail(appState.scriptChoice)
    if (!division.usesGlobalNumbering) return "${works.size} ${appState.ui(Ui.WORKS)}"
    val ranges = works.mapNotNull { it.pasuramRange }
    if (ranges.isEmpty()) return division.detail(appState.scriptChoice)
    val lo = ranges.minOf { it.first }
    val hi = ranges.maxOf { it.last }
    return "${appState.ui(Ui.PASURAM)} $lo–$hi · ${works.size} ${appState.ui(Ui.WORKS)}"
}

/** Pinned works and collections as square tiles. A pin entry is either a bare
 *  work id, or "collection:<id>" — see [AppState.pinnedCollectionEntry]. This
 *  union-aware rendering exists because the earlier version assumed every
 *  entry was a work: a pinned collection would have silently rendered as an
 *  empty gap in the row. Mirrors a real bug the iOS build found and fixed. */
@Composable
private fun PinnedSection(
    onOpenSection: (String) -> Unit,
    onOpenCollection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = appState.ui(Ui.PINNED),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        // A row rather than a grid: at most six pins, and a horizontal strip
        // keeps Home scannable in one screen rather than pushing the recents
        // below the fold.
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(appState.pinnedWorks, key = { it }) { pinEntry ->
                val collectionId = AppState.pinnedCollectionId(pinEntry)
                if (collectionId != null) {
                    val collection = appState.collection(collectionId)
                    if (collection != null) {
                        PinnedTile(
                            title = collection.name,
                            author = "${collection.pasuramKeys.size} ${appState.ui(Ui.COLLECTION_PASURAM_COUNT)}",
                            palette = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer,
                            ),
                            onClick = { onOpenCollection(collectionId) },
                        )
                    }
                } else {
                    val work = repository.work(pinEntry)
                    if (work != null) {
                        PinnedTile(
                            title = work.title(appState.scriptChoice),
                            author = work.author(appState.scriptChoice),
                            palette = DivisionPalette.sweep(
                                repository.divisionForWork(pinEntry)?.id ?: "d1",
                            ),
                            onClick = {
                                work.sections.firstOrNull()?.let { onOpenSection(it.id) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinnedTile(
    title: String,
    author: String,
    palette: List<Color>,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(150.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    Brush.linearGradient(palette),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(14.dp),
            )
        }
        Text(
            text = author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MargazhiCard(
    day: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "${appState.ui(Ui.MARGAZHI)} · ${appState.ui(Ui.DAY)} $day / 30",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = appState.ui(Ui.TODAY_THIRUPPAVAI_HINT),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 4.dp),
    )
}

@Composable
private fun RowCard(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Icon(
                    leading,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
