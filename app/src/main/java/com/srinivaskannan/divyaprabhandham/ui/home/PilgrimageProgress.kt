package com.srinivaskannan.divyaprabhandham.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.R
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
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // The current tier's earned badge sits beside the title once
                    // the first level is reached.
                    if (current != null) {
                        Image(
                            painter = painterResource(tierBadgeRes(current.index)),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Text(
                        appState.ui(Ui.PILGRIMAGE_TITLE),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
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
    val current = Pilgrimage.currentLevel(visited)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Column(Modifier.padding(horizontal = 20.dp)) {
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
            }
            Spacer(Modifier.height(16.dp))
            // The tiers as a swipeable row of collectible cards — earned ones in
            // full colour, still-locked ones desaturated behind a lock.
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(Pilgrimage.levels, key = { it.index }) { level ->
                    TierCard(level = level, unlocked = Pilgrimage.isUnlocked(level, visited), tamil = tamil)
                }
            }
            // The meaning of the tier the pilgrim currently stands at.
            if (current != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    current.description(tamil),
                    Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
private fun TierCard(level: PilgrimageLevel, unlocked: Boolean, tamil: Boolean) {
    val appState = LocalAppState.current
    Box(
        Modifier
            .width(190.dp)
            .height(320.dp)
            .clip(RoundedCornerShape(18.dp)),
    ) {
        Image(
            painter = painterResource(tierCardRes(level.index)),
            contentDescription = level.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            // Locked tiers are desaturated — the colour returns when earned.
            colorFilter = if (unlocked) null
            else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
        )
        // A dimming veil over still-locked tiers.
        if (!unlocked) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)))
        }
        // Bottom scrim carrying the label, so text stays legible over the art.
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.82f),
                        ),
                    ),
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "${appState.ui(Ui.PILGRIMAGE_TIER)} ${level.index} · ${level.threshold} ${appState.ui(Ui.PILGRIMAGE_TEMPLES)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
            )
            Text(
                level.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White,
            )
            // The meaning is revealed only once the tier is earned.
            if (unlocked) {
                Text(
                    level.subtitle(tamil),
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                )
            }
        }
        if (!unlocked) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.align(Alignment.Center).size(40.dp),
            )
        }
    }
}

/** Maps a tier index (1..5) to its element badge drawable. */
private fun tierBadgeRes(index: Int): Int = when (index) {
    1 -> R.drawable.ic_tier_1
    2 -> R.drawable.ic_tier_2
    3 -> R.drawable.ic_tier_3
    4 -> R.drawable.ic_tier_4
    else -> R.drawable.ic_tier_5
}

/** Maps a tier index (1..5) to its portrait card art. */
private fun tierCardRes(index: Int): Int = when (index) {
    1 -> R.drawable.ic_tier_card_1
    2 -> R.drawable.ic_tier_card_2
    3 -> R.drawable.ic_tier_card_3
    4 -> R.drawable.ic_tier_card_4
    else -> R.drawable.ic_tier_card_5
}
