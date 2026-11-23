package com.srinivaskannan.divyaprabhandham.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.AmazonTrack
import com.srinivaskannan.divyaprabhandham.data.Paththu
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.media.LocalAmazonRemote
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository
import kotlinx.coroutines.delay

/**
 * The "Now Playing" pill: what Amazon Music is reciting, with its controls.
 *
 * The counterpart of the iOS build's `NowPlayingPill`, and it takes the same
 * slot as [ResumePill] while there is something to show. The difference is
 * underneath: on iOS the app plays the audio itself, here Amazon Music does
 * and this is a remote control for it (see `AmazonRemote`). Tapping the pill
 * opens [PlayerSheet], the full set of controls.
 *
 * [track] is the recitation it is about, or null to hide it — the caller
 * passes one only for tracks this app knows, so the pill stays out of the way
 * while Amazon Music plays anything else.
 */
@Composable
fun MusicPill(track: AmazonTrack?, modifier: Modifier = Modifier, compact: Boolean = false) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val remote = LocalAmazonRemote.current
    val work = track?.let { repository.work(it.workId) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }

    AnimatedVisibility(
        visible = work != null,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier,
    ) {
        if (track == null || work == null) return@AnimatedVisibility
        val isStarting = remote.starting != null
        val now = remote.now
        if (compact) {
            CompactPill(
                isStarting = isStarting,
                isPlaying = now?.isPlaying == true,
                onOpen = { showPlayer = true },
            )
            return@AnimatedVisibility
        }
        Surface(
            onClick = { showPlayer = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = when {
                            isStarting -> appState.ui(Ui.PLAYER_STARTING)
                            else -> track.paththu?.let { Paththu.name(it, appState.scriptChoice) }
                                ?: appState.ui(Ui.NOW_PLAYING)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = work.title(appState.scriptChoice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isStarting) {
                    // Reaching a track takes a few seconds, and "next" is
                    // being pressed on the way: no buttons to press into that.
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = LocalContentColor.current,
                            strokeWidth = 2.dp,
                        )
                    }
                } else {
                    IconButton(
                        onClick = remote::previous,
                        enabled = now?.canSkipPrevious == true,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(Icons.Filled.SkipPrevious, appState.ui(Ui.PLAYER_PREVIOUS))
                    }
                    IconButton(onClick = remote::playPause, modifier = Modifier.size(48.dp)) {
                        if (now?.isPlaying == true) {
                            Icon(Icons.Filled.Pause, appState.ui(Ui.PLAYER_PAUSE))
                        } else {
                            Icon(Icons.Filled.PlayArrow, appState.ui(Ui.PLAYER_PLAY))
                        }
                    }
                    IconButton(
                        onClick = remote::next,
                        enabled = now?.canSkipNext == true,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(Icons.Filled.SkipNext, appState.ui(Ui.PLAYER_NEXT))
                    }
                }
            }
        }
    }

    if (showPlayer) {
        // Between two tracks there can be a moment with nothing known loaded:
        // the sheet rides that out on the last track, and closes only when
        // the recitation is really gone.
        var last by remember { mutableStateOf(track) }
        LaunchedEffect(track) {
            if (track != null) {
                last = track
            } else {
                delay(GONE_GRACE_MS)
                showPlayer = false
            }
        }
        (track ?: last)?.let { PlayerSheet(track = it, onDismiss = { showPlayer = false }) }
    }
}

/**
 * The pill in fullscreen reading: a small capsule at the end of the line with
 * the note (opens the player sheet) and play/pause, so the text keeps the
 * screen's width.
 */
@Composable
private fun CompactPill(isStarting: Boolean, isPlaying: Boolean, onOpen: () -> Unit) {
    val appState = LocalAppState.current
    val remote = LocalAmazonRemote.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            onClick = onOpen,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = appState.ui(Ui.NOW_PLAYING),
                    modifier = Modifier.size(20.dp),
                )
                if (isStarting) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = LocalContentColor.current,
                            strokeWidth = 2.dp,
                        )
                    }
                } else {
                    IconButton(onClick = remote::playPause, modifier = Modifier.size(44.dp)) {
                        if (isPlaying) {
                            Icon(Icons.Filled.Pause, appState.ui(Ui.PLAYER_PAUSE))
                        } else {
                            Icon(Icons.Filled.PlayArrow, appState.ui(Ui.PLAYER_PLAY))
                        }
                    }
                }
            }
        }
    }
}

private const val GONE_GRACE_MS = 1500L
