package com.srinivaskannan.divyaprabhandham.ads

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState

/**
 * An adaptive AdMob banner, or nothing at all.
 *
 * Renders nothing when the person has ad-free access — bought the unlock or
 * tipped at any point (see [com.srinivaskannan.divyaprabhandham.prefs.AppState.isAdFree]).
 * Callers can therefore place this unconditionally without wrapping it in
 * their own check, which keeps the ad-free gate in exactly one place instead
 * of once per placement.
 *
 * A permanent "Remove ads" label sits directly under the banner, always
 * available. AdMob banners have no close button — there's nothing to
 * dismiss and no dismissal event to hook, so "when the person tries to
 * close the ad" can't be wired up the way it might for a full-screen format.
 * This label is the standing alternative: the same intent, available at any
 * time rather than only on an interaction this format doesn't support.
 *
 * [onOfferAdFree] is additionally invoked from [AdListener.onAdClosed] — the
 * person returning from a tapped ad's click-through screen — deliberately
 * not the tap itself. Reacting at tap time would put the offer on screen
 * while the ad is still opening, obstructing it and risking AdMob's policies
 * on interfering with ad interaction; waiting until the ad's own screen is
 * dismissed means nothing is competing for that moment. Throttled to once a
 * day via [com.srinivaskannan.divyaprabhandham.prefs.AppState.canShowAdFreeOffer]
 * — the natural trigger can fire several times in one sitting, and an offer
 * every time someone comes back from an ad would read as punishing them for
 * tapping it, which is also self-defeating since ad taps are the revenue.
 * The permanent label is unaffected by that throttle.
 */
@Composable
fun AdBanner(placement: AdConfig.Placement, onOfferAdFree: () -> Unit) {
    val appState = LocalAppState.current
    if (appState.isAdFree) return

    val context = LocalContext.current

    Column(Modifier.fillMaxWidth()) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adaptiveBannerSize(ctx))
                    adUnitId = placement.unitId
                    adListener = object : AdListener() {
                        override fun onAdClosed() {
                            if (appState.canShowAdFreeOffer) {
                                appState.noteAdFreeOfferShown()
                                onOfferAdFree()
                            }
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
        )
        TextButton(onClick = onOfferAdFree, modifier = Modifier.fillMaxWidth()) {
            Text(appState.ui(Ui.REMOVE_ADS))
        }
    }
}

private fun adaptiveBannerSize(context: Context): AdSize {
    val metrics = context.resources.displayMetrics
    val adWidth = (metrics.widthPixels / metrics.density).toInt()
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
}
