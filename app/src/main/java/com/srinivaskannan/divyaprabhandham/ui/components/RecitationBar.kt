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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.media.RecitationSession
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState

/**
 * The recitation mini-bar: the whole player UI, since playback is audio-only.
 *
 * It sits just above Continue Reading, app-wide, and only when something is
 * loaded — so a reader who never taps Listen never sees it. Play/pause,
 * skip, a buffering spinner, and a close that stops playback and dismisses the
 * bar. No video surface anywhere; the IFrame player lives offscreen.
 */
@Composable
fun RecitationBar(
    session: RecitationSession,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val controller = session.controller

    // When the last track ends, retire the bar rather than leaving it parked
    // on a stopped player.
    LaunchedEffect(controller?.ended) {
        if (controller?.ended == true) session.stop()
    }

    AnimatedVisibility(
        visible = session.isActive,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Filled.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appState.ui(Ui.LISTEN),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = session.workTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (controller != null && controller.count > 1) {
                    IconButton(
                        onClick = { controller.previous() },
                        enabled = controller.ready,
                    ) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = appState.ui(Ui.BACK))
                    }
                }

                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (controller == null || !controller.ready || controller.buffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        IconButton(onClick = { controller.playPause() }) {
                            Icon(
                                imageVector = if (controller.playing) Icons.Filled.Pause
                                else Icons.Filled.PlayArrow,
                                contentDescription = appState.ui(Ui.LISTEN),
                            )
                        }
                    }
                }

                if (controller != null && controller.count > 1) {
                    IconButton(
                        onClick = { controller.next() },
                        enabled = controller.ready,
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = appState.ui(Ui.NEXT_SECTION))
                    }
                }

                IconButton(onClick = { session.stop() }) {
                    Icon(Icons.Filled.Close, contentDescription = appState.ui(Ui.CLOSE))
                }
            }
        }
    }
}
