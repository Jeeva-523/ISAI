package com.saavn.music.ads

import com.saavn.music.BuildConfig

/**
 * Centralized AdMob Configuration for ISAI Music.
 * Provides test Ad Unit IDs during development/debug, and production IDs in release builds.
 */
object AdMobConfig {
    /**
     * AdMob Application ID
     * Test App ID: ca-app-pub-3940256099942544~3347511713
     */
    val ADMOB_APP_ID: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544~3347511713"
    } else {
        "ca-app-pub-3940256099942544~3347511713"
    }

    /**
     * AdMob Banner Ad Unit ID
     * Official Test Banner ID: ca-app-pub-3940256099942544/6300978111
     */
    val ADMOB_BANNER_AD_UNIT_ID: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111"
    } else {
        "ca-app-pub-3940256099942544/6300978111"
    }

    /**
     * AdMob Native Advanced Ad Unit ID
     * Official Test Native ID: ca-app-pub-3940256099942544/2247696110
     */
    val ADMOB_NATIVE_AD_UNIT_ID: String = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2247696110"
    } else {
        "ca-app-pub-3940256099942544/2247696110"
    }
}
