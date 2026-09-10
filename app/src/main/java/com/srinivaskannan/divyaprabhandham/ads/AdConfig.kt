package com.srinivaskannan.divyaprabhandham.ads

import com.srinivaskannan.divyaprabhandham.BuildConfig

/**
 * AdMob configuration.
 *
 * Mirrors the iOS build's AdConfig exactly — same publisher, same three
 * placements, same safety reasoning — so the two apps make the same product
 * decision rather than two independently-invented ones.
 */
object AdConfig {

    /**
     * The AdMob application id, which also goes in AndroidManifest.xml as
     * the com.google.android.gms.ads.APPLICATION_ID meta-data — the SDK
     * reads it from there, not from here, but keeping it alongside the unit
     * ids makes the pairing obvious. Currently Google's own published
     * SAMPLE_APP_ID (safe: it only ever serves test ads) until this app is
     * registered in the AdMob console under the same publisher account
     * already used on iOS (pub-1293877078722383) and given its own
     * Android-specific app id.
     */
    const val APPLICATION_ID = "ca-app-pub-3940256099942544~3347511713"

    /**
     * Ad unit ids, one per placement. **These are Google's official test
     * ids.** Running real ids in development inflates impressions against
     * ads nobody saw, which is what gets AdMob accounts suspended — so real
     * ids are only used in release builds, and until they're filled in below
     * the app serves test ads everywhere, which is the safe default.
     */
    enum class Placement {
        HOME, SETTINGS, SEARCH;

        /** Google's always-available test banner unit id for Android
         *  (verified against the official AdMob quick-start docs — this is
         *  platform-specific; iOS's equivalent test id is a different
         *  string, ca-app-pub-3940256099942544/2934735716). */
        private val testBanner = "ca-app-pub-3940256099942544/6300978111"

        /** Fill these in from AdMob once the three banner units exist. Left
         *  empty deliberately: an empty id falls back to the test unit
         *  rather than silently failing to fill. */
        private val productionID: String
            get() = when (this) {
                HOME -> ""     // TODO: home banner unit id
                SETTINGS -> "" // TODO: settings banner unit id
                SEARCH -> ""   // TODO: search banner unit id
            }

        val unitId: String
            get() = if (BuildConfig.DEBUG || productionID.isEmpty()) testBanner else productionID
    }
}
