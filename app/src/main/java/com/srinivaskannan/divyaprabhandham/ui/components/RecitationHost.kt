package com.srinivaskannan.divyaprabhandham.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.srinivaskannan.divyaprabhandham.media.RecitationSession

/**
 * Mounts the recitation player's WebView into the window.
 *
 * The player has to be attached and laid out to produce sound at all — a
 * detached or zero-size WebView is throttled or silent — so it is given a real
 * but tiny 1dp footprint and pushed behind everything with a negative z. The
 * audio plays; the video surface simply never has room to be seen. When no
 * recitation is active there is nothing to mount.
 *
 * Keyed by the controller instance so that starting a different work swaps in
 * the new player's view rather than leaving the old one attached.
 */
@Composable
fun RecitationHost(session: RecitationSession) {
    val controller = session.controller ?: return
    AndroidView(
        factory = { ctx ->
            // Wrap the WebView so Compose owns a fresh container each time and
            // the WebView itself can be moved between containers without the
            // "already has a parent" crash when the player swaps.
            android.widget.FrameLayout(ctx)
        },
        modifier = Modifier
            .size(1.dp)
            .zIndex(-1f),
        update = { frame ->
            val v = controller.view
            (v.parent as? android.view.ViewGroup)?.removeView(v)
            frame.removeAllViews()
            frame.addView(v)
        },
    )
}
