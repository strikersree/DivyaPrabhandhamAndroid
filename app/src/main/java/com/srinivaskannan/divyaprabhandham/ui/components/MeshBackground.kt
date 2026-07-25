package com.srinivaskannan.divyaprabhandham.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * An animated mesh gradient, used for the division hero cards.
 *
 * SwiftUI has `MeshGradient` as a primitive; Compose does not, so this
 * approximates one. A 3x3 grid of control points is laid over the card: the
 * four corners are pinned to the edges (so there is never a transparent gap),
 * and the mid-edge and centre points drift on a slow cycle. Each point is then
 * painted as a soft radial gradient in its own colour, and the nine overlapping
 * washes blend into something very close to a real mesh.
 *
 * The alternative was an AGSL shader, which would be a truer mesh and is what
 * you would reach for if this were the whole app — but it needs API 33, needs a
 * fallback path anyway, and this reads identically at card size.
 *
 * The iOS build learned the hard way that `TimelineView(.animation)` silently
 * pauses; the fix there was an explicit animated phase value. Compose's
 * `rememberInfiniteTransition` is that same shape and does not have the
 * problem, but the animation is still driven from one phase float for exactly
 * the same reason: one clock, no drift between the points.
 */
@Composable
fun AnimatedMeshBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    require(colors.size >= 9) { "mesh needs a 3x3 grid of colours" }

    // The transition is created unconditionally — it is cheap, and branching on
    // it would change the composable's call shape between recompositions.
    val animatedPhase by rememberInfiniteTransition(label = "mesh").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "meshPhase",
    )

    // A still frame at mid-cycle when motion is reduced. Not the start of the
    // cycle: that is the most lopsided the mesh ever looks.
    val phase = if (animated) animatedPhase else 0.5f

    Canvas(modifier = modifier) {
        val points = controlPoints(phase, size)
        // Base wash in the centre colour, so no pixel is ever unpainted.
        drawRect(color = colors[4])
        // The radius is generous on purpose: heavy overlap is what turns nine
        // discrete blobs into one continuous field.
        val radius = maxOf(size.width, size.height) * 0.75f
        points.forEachIndexed { index, point ->
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(colors[index], colors[index].copy(alpha = 0f)),
                    center = point,
                    radius = radius,
                ),
            )
        }
    }
}

/**
 * The 3x3 grid. Corners stay pinned; the mid-edge points slide along their
 * edges and the centre drifts diagonally, all from one phase so the whole field
 * moves as a piece. Offsets match the iOS build's, so the two apps breathe at
 * the same rate.
 */
private fun controlPoints(t: Float, size: Size): List<Offset> {
    fun lerp(a: Float, b: Float) = a + (b - a) * t
    fun at(x: Float, y: Float) = Offset(x * size.width, y * size.height)
    return listOf(
        at(0f, 0f),
        at(lerp(0.40f, 0.60f), 0f),                 // top-mid slides across
        at(1f, 0f),
        at(0f, lerp(0.38f, 0.62f)),                 // left-mid slides down
        at(lerp(0.60f, 0.40f), lerp(0.42f, 0.60f)), // centre drifts
        at(1f, lerp(0.62f, 0.38f)),                 // right-mid slides up
        at(0f, 1f),
        at(lerp(0.60f, 0.40f), 1f),                 // bottom-mid slides back
        at(1f, 1f),
    )
}
