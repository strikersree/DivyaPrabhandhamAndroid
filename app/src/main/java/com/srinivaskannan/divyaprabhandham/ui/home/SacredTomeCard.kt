package com.srinivaskannan.divyaprabhandham.ui.home

import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srinivaskannan.divyaprabhandham.R
import com.srinivaskannan.divyaprabhandham.data.Division
import com.srinivaskannan.divyaprabhandham.ui.theme.FoilShimmer
import com.srinivaskannan.divyaprabhandham.ui.theme.TomePalette

/**
 * The leather-and-gold "Sacred Tome" cover, one per division, replacing the
 * previous animated-mesh hero card. Ported from the iOS build's
 * SacredTomeCard/TomeFace: a division-tinted leather ground, a gold frame,
 * a centred emblem, and the title beneath it — all sharing one gold-foil
 * shimmer (see [FoilShimmer]).
 *
 * The tap-triggered "cover creaking open" tilt from the original reference
 * was removed: it added a perceptible delay before a division actually
 * opened, which read as sluggishness rather than delight. onClick now fires
 * immediately on tap.
 */
@Composable
fun SacredTomeCard(
    division: Division,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tome = TomePalette.forId(division.id)
    var cardSize by remember { mutableStateOf(Size.Zero) }
    val shimmerModifier = FoilShimmer.rememberFoilShimmerModifier(cardSize)

    // Sized relative to the screen, not a fixed dp value, so one card
    // dominates the row with just a sliver of the next peeking in — matching
    // the reference's proportions rather than the old fixed 320x250, which
    // read small next to iOS's near-full-width card. Height stays close to
    // 1:1 with width (the reference reads roughly square), not the old
    // wider 320:250 ratio.
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = screenWidth - 40.dp

    Box(
        modifier = modifier
            .width(cardWidth)
            .aspectRatio(0.98f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = "$title, $subtitle" }
            .onSizeChanged { cardSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .then(shimmerModifier),
    ) {
        // Division-tinted ground.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(tome.top, tome.bottom))),
        )
        // Desaturated leather grain, blended over the ground by alpha alone
        // (Compose's single-ColorFilter Image can't chain desaturate+tint the
        // way the reference sketch implied) — reads as the same "leather
        // tinted by division colour" result.
        Image(
            painter = painterResource(R.drawable.tome_leather),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            alpha = 0.5f,
            modifier = Modifier.fillMaxSize(),
        )
        // Gold frame.
        Box(
            Modifier
                .fillMaxSize()
                .border(2.dp, TomePalette.gold, RoundedCornerShape(20.dp))
                .padding(3.dp)
                .border(1.dp, TomePalette.gold.copy(alpha = 0.6f), RoundedCornerShape(17.dp)),
        )

        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val emblemRes = tomeEmblemRes(division.id)
            if (emblemRes != null) {
                Image(
                    painter = painterResource(emblemRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(TomePalette.gold),
                    modifier = Modifier.size(140.dp),
                )
            } else {
                // Placeholder until this division's emblem artwork exists —
                // see the "still missing" note in the delivery message.
                Icon(
                    Icons.Filled.AutoStories,
                    contentDescription = null,
                    tint = TomePalette.gold,
                    modifier = Modifier.size(100.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            // On API 33+ the whole card already shimmers uniformly via the
            // graphicsLayer render effect (border, emblem and title read the
            // same gold), so the title just needs to render in gold, not a
            // second, separate animated brush. Below 33, this is the one
            // spot that gets its own shimmer, matching the reference
            // GoldShimmerText, since there's no RenderEffect to carry it.
            if (FoilShimmer.isSupported) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = TomePalette.gold,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = title,
                    style = TextStyle(
                        brush = FoilShimmer.shimmerBrush(),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TomePalette.foil.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Emblem artwork per division — all five now confirmed by filename
 * (mudhalaayiram.PNG, Irandaamaayiram.PNG, Iyarpa_design_3.png,
 * Naangam_aayiram.PNG, Desika_Prabdhandham_.PNG). d4 and d5 replace an
 * earlier inferred guess (Desika was assumed to be the wide lotus; it's
 * actually the tall one — d4 and d5 were swapped from the first pass).
 * The fallback icon in [SacredTomeCard] stays in place for robustness
 * (a future sixth division, or an asset that fails to load) rather than
 * being removed now that every current division has art.
 */
private fun tomeEmblemRes(divisionId: String): Int? = when (divisionId) {
    "d1" -> R.drawable.tome_emblem_d1
    "d2" -> R.drawable.tome_emblem_d2
    "d3" -> R.drawable.tome_emblem_d3
    "d4" -> R.drawable.tome_emblem_d4
    "d5" -> R.drawable.tome_emblem_d5
    else -> null
}
