package com.srinivaskannan.divyaprabhandham.ui.theme

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

/**
 * The gold-foil shimmer for Sacred Tome cards — the AGSL port of the iOS
 * build's Metal `.colorEffect` shader ("foilShimmer" in Shaders.metal).
 *
 * The technique it ports is the one that actually matters: gold is
 * classified by reading each pixel's own rendered *colour* — luminance and
 * "warmth" (how far a pixel leans red/green over blue) — not by guessing
 * where the gold shapes are in the layout. That's what makes one shader
 * apply uniformly across the border, emblem and title regardless of how
 * they're drawn. Ported verbatim, not re-derived: the luma weights, the
 * warmth formula, and both threshold pairs below are the exact values from
 * the Metal source, found because a luminance-only threshold provably cannot
 * separate this app's gold from its sandalwood leather — their brightness is
 * near-identical (0.635 vs 0.637); only warmth (gold clusters 0.49-0.53,
 * sandalwood/terracotta top out at 0.41) cleanly tells them apart.
 *
 * ONE DELIBERATE ADAPTATION: the iOS shader drives its glint sweep off live
 * device roll/pitch (CoreMotion). This port drives it off an animated
 * [progress] value instead — a continuously looping diagonal sweep — rather
 * than adding a SensorManager listener in this pass. The gold *classification*
 * (the actual "true equivalent" part) is unchanged; only the trigger for
 * *which direction* the highlight currently sweeps is simpler. Device-tilt
 * driving can be added later without touching the classification math.
 *
 * Patina (leather aging/verdigris) is intentionally NOT ported: it's
 * currently disabled in the iOS build itself (a live performance
 * investigation, not a shipped feature), so this mirrors the *current* iOS
 * behaviour rather than a dormant one.
 */
object FoilShimmer {

    /** Minimum SDK for AGSL RuntimeShader (Android 13 / Tiramisu). Below
     *  this, [rememberFoilShimmerModifier] returns a plain, unmodified
     *  Modifier and the caller should apply [shimmerBrush] to text/border
     *  content directly instead — see SacredTomeCard for the split. */
    val isSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    // language=AGSL
    private const val SHADER_SRC = """
        uniform shader content;
        uniform float2 size;
        uniform float progress; // 0..1, looping

        half4 main(float2 fragCoord) {
            half4 c = content.eval(fragCoord);
            if (c.a < 0.004) { return c; }

            // ---------- Gold classification: luminance + warmth ----------
            // Verbatim from the Metal source (see class doc above).
            half luma = half(dot(float3(c.rgb), float3(0.299, 0.587, 0.114)));
            half warmth = half(clamp((float(c.r) + float(c.g)) * 0.5 - float(c.b) * 0.7, 0.0, 1.0));
            half lumaMask = smoothstep(0.55, 0.72, luma);
            half warmthMask = smoothstep(0.44, 0.50, warmth);
            half goldMask = lumaMask * warmthMask;
            if (goldMask < 0.01) { return c; }

            // ---------- Glint: a diagonal band sweeping with progress ----------
            float2 uv = fragCoord / size;
            float diag = (uv.x + uv.y) * 0.5;
            float sweep = fract(diag - progress);
            float band = smoothstep(0.0, 0.10, sweep) * (1.0 - smoothstep(0.10, 0.22, sweep));
            half glint = half(band) * goldMask;

            half3 foil = half3(0.96, 0.86, 0.60); // TomePalette.foil
            c.rgb = mix(c.rgb, foil, glint * 0.55);
            return c;
        }
    """

    /**
     * The shimmer as a graphicsLayer render effect, applied to the whole
     * card face uniformly — the "true equivalent" path, API 33+ only.
     * Returns an unmodified [Modifier] below that, so callers can chain it
     * unconditionally and rely on [isSupported] only to decide whether to
     * *also* apply [shimmerBrush] as the fallback.
     */
    @Composable
    fun rememberFoilShimmerModifier(cardSize: Size): Modifier {
        if (!isSupported || cardSize.width <= 0f || cardSize.height <= 0f) return Modifier
        val transition = rememberInfiniteTransition(label = "foilShimmer")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "foilShimmerProgress",
        )
        val shader = remember { RuntimeShader(SHADER_SRC) }
        return Modifier.graphicsLayer {
            shader.setFloatUniform("size", cardSize.width, cardSize.height)
            shader.setFloatUniform("progress", progress)
            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "content")
                .asComposeRenderEffect()
        }
    }

    /**
     * Pre-33 fallback: a looping linear-gradient gold brush, in the shape of
     * the reference GoldShimmerText — applied directly to text/border
     * content rather than as a whole-card post-process, since RenderEffect
     * isn't available. Solid [TomePalette.gold] should be used for the
     * emblem tint on these devices instead of attempting a shimmer there;
     * chasing the same effect with per-pixel Compose masking would mean
     * hand-rolling the very shader logic this fallback exists to avoid.
     */
    @Composable
    fun shimmerBrush(): Brush {
        val transition = rememberInfiniteTransition(label = "goldBrush")
        val offset by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "goldBrushOffset",
        )
        return Brush.linearGradient(
            colors = TomePalette.goldGradientStops,
            start = Offset(offset - 200f, offset - 200f),
            end = Offset(offset + 200f, offset + 200f),
        )
    }
}
