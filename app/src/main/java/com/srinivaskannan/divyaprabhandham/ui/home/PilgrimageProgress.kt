package com.srinivaskannan.divyaprabhandham.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.Pilgrimage
import com.srinivaskannan.divyaprabhandham.data.PilgrimageLevel
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState

/**
 * The pilgrimage progress card on Home: how many Divya Desams visited, a bar
 * toward the next level, and the current level name. Tapping it opens the cards
 * sheet — the earned and still-locked level cards.
 */
@Composable
fun PilgrimageProgressCard(visited: Int, modifier: Modifier = Modifier) {
    val appState = LocalAppState.current
    val tamil = appState.scriptChoice == ScriptChoice.TAMIL
    var showCards by remember { mutableStateOf(false) }

    val current = Pilgrimage.currentLevel(visited)
    val next = Pilgrimage.nextLevel(visited)
    // Progress toward the next threshold (or full when all levels earned).
    val target = next?.threshold ?: Pilgrimage.levels.last().threshold
    val floor = current?.threshold ?: 0
    val fraction = if (next == null) 1f
    else ((visited - floor).toFloat() / (target - floor).toFloat()).coerceIn(0f, 1f)

    if (showCards) {
        PilgrimageCardsSheet(visited = visited, onDismiss = { showCards = false })
    }

    Surface(
        onClick = { showCards = true },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    appState.ui(Ui.PILGRIMAGE_TITLE),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Place, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp))
                    Text(
                        "$visited / 106",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )

            Text(
                text = when {
                    current != null && next != null ->
                        "${current.title} · ${next.threshold - visited} ${appState.ui(Ui.PILGRIMAGE_TO_NEXT)}"
                    current != null ->
                        current.title
                    next != null ->
                        "${next.threshold - visited} ${appState.ui(Ui.PILGRIMAGE_TO_FIRST)}"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@androidx.compose.runtime.Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun PilgrimageCardsSheet(visited: Int, onDismiss: () -> Unit) {
    val appState = LocalAppState.current
    val tamil = appState.scriptChoice == ScriptChoice.TAMIL
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                appState.ui(Ui.PILGRIMAGE_CARDS_TITLE),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                appState.ui(Ui.PILGRIMAGE_CARDS_BODY),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(Pilgrimage.levels, key = { it.index }) { level ->
                    LevelCard(level = level, unlocked = Pilgrimage.isUnlocked(level, visited), tamil = tamil)
                }
            }
        }
    }
}

/**
 * One level card — revealed when earned, a locked silhouette until then. Shows
 * the tier's title, its meaning, the temple threshold, and the significance.
 * The art is a placeholder gradient block for now; final per-level artwork drops
 * into the art slot once supplied.
 */
@Composable
private fun LevelCard(level: PilgrimageLevel, unlocked: Boolean, tamil: Boolean) {
    val appState = LocalAppState.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (unlocked) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Placeholder art slot — final artwork replaces the coloured block.
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (unlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(64.dp),
                ) {}
                if (!unlocked) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${appState.ui(Ui.PILGRIMAGE_TIER)} ${level.index} · ${level.threshold} ${appState.ui(Ui.PILGRIMAGE_TEMPLES)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    level.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    level.subtitle(tamil),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
                // The significance is revealed only once the tier is earned —
                // there is something to look forward to behind the lock.
                if (unlocked) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        level.description(tamil),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
