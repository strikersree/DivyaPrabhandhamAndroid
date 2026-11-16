package com.srinivaskannan.divyaprabhandham.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.AmazonTrack
import com.srinivaskannan.divyaprabhandham.data.Paththu
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.media.LocalAmazonRemote
import com.srinivaskannan.divyaprabhandham.media.NowPlaying
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * The full player, opened from [MusicPill]: artwork, what is being recited, a
 * scrubber, and the transport controls. The counterpart of the iOS build's
 * `FullPlayerView`, laid out the same way.
 *
 * Like the pill it is a remote control: every control here is a request to
 * Amazon Music, and what is shown is what Amazon Music reports back. The last
 * row opens Amazon Music itself, for anything this does not cover.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSheet(track: AmazonTrack, onDismiss: () -> Unit) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val remote = LocalAmazonRemote.current
    val work = repository.work(track.workId)
    val now = remote.now
    val isStarting = remote.starting != null
    val total = now?.durationMs?.takeIf { it > 0 }

    var position by remember { mutableLongStateOf(now?.livePositionMs() ?: 0L) }
    LaunchedEffect(now) {
        while (true) {
            position = now?.livePositionMs() ?: 0L
            delay(TICK_MS)
        }
    }

    // Where a seek was sent, shown until the player reports from there: its
    // answer takes a moment, and the thumb would jump back in the meantime.
    var sought by remember(now?.mediaId) { mutableStateOf<Long?>(null) }
    LaunchedEffect(sought, now) {
        val target = sought ?: return@LaunchedEffect
        if (abs((now?.livePositionMs() ?: 0L) - target) > SEEK_SLACK_MS) delay(SEEK_WAIT_MS)
        sought = null
    }
    val shown = (sought ?: position).coerceIn(0L, total ?: Long.MAX_VALUE)
    val canSeek = !isStarting && now?.canSeek == true
    val seek: (Long) -> Unit = { to ->
        val target = to.coerceIn(0L, total ?: Long.MAX_VALUE)
        sought = target
        remote.seekTo(target)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Artwork(now)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = work?.title(appState.scriptChoice).orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                track.paththu?.let { Paththu.name(it, appState.scriptChoice) }?.let { paththu ->
                    Text(
                        text = paththu,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val detail = if (isStarting) appState.ui(Ui.PLAYER_STARTING) else now?.artist
                if (!detail.isNullOrBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Scrubber(shown = shown, total = total, enabled = canSeek, onSeek = seek)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = remote::previous,
                    enabled = !isStarting && now?.canSkipPrevious == true,
                ) {
                    Icon(Icons.Filled.SkipPrevious, appState.ui(Ui.PLAYER_PREVIOUS))
                }
                IconButton(onClick = { seek(shown - JUMP_MS) }, enabled = canSeek) {
                    Icon(
                        Icons.Filled.Replay10,
                        appState.ui(Ui.PLAYER_BACK_10),
                        modifier = Modifier.size(32.dp),
                    )
                }
                if (isStarting) {
                    // As on the pill: "next" is being pressed on the way to
                    // the track, and there is nothing to press into that.
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    }
                } else {
                    FilledIconButton(onClick = remote::playPause, modifier = Modifier.size(72.dp)) {
                        if (now?.isPlaying == true) {
                            Icon(
                                Icons.Filled.Pause,
                                appState.ui(Ui.PLAYER_PAUSE),
                                modifier = Modifier.size(40.dp),
                            )
                        } else {
                            Icon(
                                Icons.Filled.PlayArrow,
                                appState.ui(Ui.PLAYER_PLAY),
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                }
                IconButton(onClick = { seek(shown + JUMP_MS) }, enabled = canSeek) {
                    Icon(
                        Icons.Filled.Forward10,
                        appState.ui(Ui.PLAYER_FORWARD_10),
                        modifier = Modifier.size(32.dp),
                    )
                }
                IconButton(
                    onClick = remote::next,
                    enabled = !isStarting && now?.canSkipNext == true,
                ) {
                    Icon(Icons.Filled.SkipNext, appState.ui(Ui.PLAYER_NEXT))
                }
            }

            TextButton(onClick = { remote.openPlayer() }) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(appState.ui(Ui.OPEN_AMAZON_MUSIC))
            }
        }
    }
}

/** The cover Amazon Music reports for the track, or a plain tile until it does. */
@Composable
private fun Artwork(now: NowPlaying?) {
    val shape = RoundedCornerShape(16.dp)
    val cover = now?.artwork?.let { art -> remember(art) { art.asImageBitmap() } }
    // On a phone held sideways a full-size cover would push the controls off
    // the sheet: it gives way to them there.
    val side = minOf(280, LocalConfiguration.current.screenHeightDp * 2 / 5)
    Box(
        modifier = Modifier
            .widthIn(max = side.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(12.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null) {
            Image(
                bitmap = cover,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun Scrubber(shown: Long, total: Long?, enabled: Boolean, onSeek: (Long) -> Unit) {
    // Set while the thumb is held; the seek is sent once, on release.
    var dragged by remember { mutableStateOf<Float?>(null) }
    val at = dragged?.toLong() ?: shown

    Column(modifier = Modifier.fillMaxWidth()) {
        if (total != null) {
            Slider(
                value = at.toFloat(),
                onValueChange = { dragged = it },
                onValueChangeFinished = {
                    dragged?.let { onSeek(it.toLong()) }
                    dragged = null
                },
                valueRange = 0f..total.toFloat(),
                enabled = enabled,
            )
        } else {
            // Track length not reported yet: elapsed time only, rather than
            // a scrubber that cannot be positioned.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val times = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
            val faint = MaterialTheme.colorScheme.onSurfaceVariant
            Text(timeString(at), style = times, color = faint)
            Text(
                text = if (total != null) "-" + timeString((total - at).coerceAtLeast(0L)) else "--:--",
                style = times,
                color = faint,
            )
        }
    }
}

private fun timeString(ms: Long): String {
    val seconds = (ms + 500) / 1000
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}

private const val TICK_MS = 500L
private const val JUMP_MS = 10_000L

/** How near a report has to be to count as the seek's answer, and how long to wait for one. */
private const val SEEK_SLACK_MS = 2000L
private const val SEEK_WAIT_MS = 2500L
