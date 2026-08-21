package com.srinivaskannan.divyaprabhandham.ui.theme

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI

/**
 * The gold-foil shimmer for Sacred Tome cards — the AGSL port of the iOS
 * build's Metal `.colorEffect` shader ("foilShimmer" in Shaders.metal).
 *
 * The technique that actually matters is unchanged from the first pass and
 * is NOT touched by anything below: gold is classified by reading each
 * pixel's own rendered *colour* — luminance and "warmth" (how far a pixel
 * leans red/green over blue) — not by guessing where the gold shapes are in
 * the layout. Ported verbatim: the luma weights, the warmth formula, and
 * both threshold pairs are the exact values from the Metal source, because
 * a luminance-only threshold provably cannot separate this app's gold from
 * its sandalwood leather (0.635 vs 0.637 brightness); only warmth (gold
 * clusters 0.49-0.53, sandalwood/terracotta top out at 0.41) tells them
 * apart.
 *
 * WHAT CHANGED FROM THE FIRST PASS: that version drove the glint sweep off
 * a continuously looping animated value rather than device tilt, flagged
 * explicitly as a stopgap. It read as "shimmer that plays by itself and
 * ignores the phone" — fixed here with a real SensorManager listener
 * (TYPE_ROTATION_VECTOR) feeding two tilt uniforms into the shader, so the
 * highlight band's direction actually follows how the phone is held. A
 * device without a rotation sensor gets a fixed, centred glint rather than
 * a crash or an empty effect.
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
        uniform float2 tilt; // (roll, pitch), each roughly -1..1

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

            // ---------- Glint: a band sweeping along the device-tilt direction ----------
            float2 uv = fragCoord / size;
            float2 centred = uv - 0.5;
            float2 lightDir = tilt;
            float dirLen = length(lightDir);
            float2 dir = dirLen > 0.02 ? lightDir / dirLen : float2(0.70710678, 0.70710678);
            float proj = dot(centred, dir) + 0.5;
            float sweep = fract(proj);
            float band = smoothstep(0.0, 0.10, sweep) * (1.0 - smoothstep(0.10, 0.22, sweep));
            half glint = half(band) * goldMask;

            half3 foil = half3(0.96, 0.86, 0.60); // TomePalette.foil
            c.rgb = mix(c.rgb, foil, glint * 0.55);
            return c;
        }
    """

    /**
     * Device tilt as (roll, pitch), each roughly -1..1, smoothed with a
     * light low-pass filter so small hand tremor doesn't make the glint
     * flicker. Backed by TYPE_ROTATION_VECTOR (present on effectively every
     * device with a compass/gyro combination); if the sensor genuinely isn't
     * there, stays at (0,0) — a fixed, centred glint rather than nothing.
     */
    @Composable
    private fun rememberDeviceTilt(): State<Offset> {
        val context = LocalContext.current
        val tilt = remember { mutableStateOf(Offset.Zero) }
        DisposableEffect(context) {
            val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            if (manager == null || sensor == null) {
                return@DisposableEffect onDispose {}
            }
            var smoothX = 0f
            var smoothY = 0f
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    // orientation[1] = pitch, orientation[2] = roll, radians.
                    // Normalize a modest +/-45deg range of hand-holding tilt
                    // to roughly -1..1 rather than the full +/-90, so an
                    // ordinary amount of tilt already sweeps the glint fully.
                    val rawX = (orientation[2] / (PI.toFloat() / 4f)).coerceIn(-1f, 1f)
                    val rawY = (orientation[1] / (PI.toFloat() / 4f)).coerceIn(-1f, 1f)
                    val alpha = 0.15f
                    smoothX += (rawX - smoothX) * alpha
                    smoothY += (rawY - smoothY) * alpha
                    tilt.value = Offset(smoothX, smoothY)
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { manager.unregisterListener(listener) }
        }
        return tilt
    }

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
        val tilt by rememberDeviceTilt()
        val shader = remember { RuntimeShader(SHADER_SRC) }
        return Modifier.graphicsLayer {
            shader.setFloatUniform("size", cardSize.width, cardSize.height)
            shader.setFloatUniform("tilt", tilt.x, tilt.y)
            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "content")
                .asComposeRenderEffect()
        }
    }

    /**
     * Pre-33 fallback: a gold gradient brush whose direction follows the
     * same device-tilt state as the AGSL path, so the two tiers feel like
     * one effect rather than a shimmering one and a static one. Solid
     * [TomePalette.gold] should be used for the emblem tint on these
     * devices instead of attempting the same effect there; chasing it with
     * per-pixel Compose masking would mean hand-rolling the very shader
     * logic this fallback exists to avoid.
     */
    @Composable
    fun shimmerBrush(): Brush {
        val tilt by rememberDeviceTilt()
        return Brush.linearGradient(
            colors = TomePalette.goldGradientStops,
            start = Offset(200f - tilt.x * 200f, 200f - tilt.y * 200f),
            end = Offset(-200f - tilt.x * 200f, -200f - tilt.y * 200f),
        )
    }
}
