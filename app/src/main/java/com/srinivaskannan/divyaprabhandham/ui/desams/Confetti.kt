package com.srinivaskannan.divyaprabhandham.ui.desams

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A one-shot confetti burst overlay for a pilgrimage achievement — bigger and
 * longer when a level unlocks ([big]). Pure Compose Canvas, no dependency: a
 * fixed set of particles fall and drift under a single animated progress value,
 * then [onFinished] fires so the caller can clear it.
 *
 * Deliberately lightweight (particles are computed from a seed, not a physics
 * loop) so it stays smooth. If the platform ever exposes a reduced-motion
 * signal we can gate on, this is where to honour it.
 */
@Composable
fun ConfettiCelebration(big: Boolean, onFinished: () -> Unit) {
    val count = if (big) 140 else 70
    val durationMs = if (big) 2600 else 1800

    val particles = remember(big) {
        val seed = Random(System.nanoTime())
        List(count) {
            Particle(
                startX = seed.nextFloat(),
                hueColor = confettiColors[seed.nextInt(confettiColors.size)],
                drift = (seed.nextFloat() - 0.5f) * 0.4f,
                spin = (seed.nextFloat() - 0.5f) * 720f,
                size = 6f + seed.nextFloat() * 8f,
                delay = seed.nextFloat() * 0.25f,
            )
        }
    }

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMs, easing = LinearEasing),
        finishedListener = { onFinished() },
        label = "confetti",
    )

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            val t = ((progress - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val x = (p.startX + p.drift * t) * w + sin(t * 6f + p.startX * 10f) * 12f
            val y = (t * t) * (h + 40f) - 20f
            val alpha = (1f - t).coerceIn(0f, 1f)
            rotate(degrees = p.spin * t, pivot = Offset(x, y)) {
                drawRect(
                    color = p.hueColor.copy(alpha = alpha),
                    topLeft = Offset(x - p.size / 2f, y - p.size / 2f),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size * 1.6f),
                )
            }
        }
    }
}

private data class Particle(
    val startX: Float,
    val hueColor: Color,
    val drift: Float,
    val spin: Float,
    val size: Float,
    val delay: Float,
)

// Warm, festival-leaning palette — golds, saffron, temple reds, a little green.
private val confettiColors = listOf(
    Color(0xFFE8B923),
    Color(0xFFD4691E),
    Color(0xFF9E1B32),
    Color(0xFFE85D75),
    Color(0xFF2E7D5B),
    Color(0xFFF4D35E),
)
