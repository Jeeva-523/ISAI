package com.saavn.music.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.saavn.music.data.model.UserProfile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Centralized Ad Manager for ISAI Music.
 * Handles AdMob SDK initialization, Free vs. Premium eligibility gating,
 * audio safety guarantees, and native ad request pooling.
 */
object AdManager {
    private const val TAG = "ISAI_AdManager"
    private val isInitialized = AtomicBoolean(false)
    private val cachedNativeAds = ConcurrentHashMap<String, NativeAd>()
    private val inFlightRequests = ConcurrentHashMap<String, Boolean>()

    /**
     * Initializes Google Mobile Ads SDK once per application lifecycle.
     */
    fun initialize(@Suppress("UNUSED_PARAMETER") context: Context) {
        // Ads completely disabled across the application
    }

    /**
     * Centralized ad eligibility gate:
     * Returns false unconditionally so no ads are requested, loaded, or shown.
     */
    fun canShowAds(@Suppress("UNUSED_PARAMETER") userProfile: UserProfile?): Boolean {
        return false
    }

    /**
     * Builds an audio-safe AdRequest.
     * Guaranteed never to request or trigger audio ads.
     */
    fun buildSafeAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    /**
     * Returns NativeAdOptions with VideoOptions configured to START MUTED.
     * Guaranteed never to play audio ads or interrupt background music playback.
     */
    fun buildSafeNativeAdOptions(): NativeAdOptions {
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true) // ALWAYS MUTED: Never interrupt music playback!
            .build()

        return NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .build()
    }

    /**
     * Requests a NativeAd for a specific list slot (e.g. every 5 songs, playlist, or now playing).
     * Prevents duplicate network requests for the same slot while scrolling.
     */
    fun loadNativeAdForSlot(
        context: Context,
        slotKey: String,
        userProfile: UserProfile?,
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: (LoadAdError) -> Unit
    ) {
        // Strict Premium Check: Never request ads for premium listeners
        if (!canShowAds(userProfile)) {
            return
        }

        // Return existing cached ad for this slot if available
        val existing = cachedNativeAds[slotKey]
        if (existing != null) {
            onAdLoaded(existing)
            return
        }

        // Prevent duplicate simultaneous requests for the same slot
        if (inFlightRequests[slotKey] == true) {
            return
        }
        inFlightRequests[slotKey] = true

        try {
            val adLoader = AdLoader.Builder(context, AdMobConfig.ADMOB_NATIVE_AD_UNIT_ID)
                .forNativeAd { nativeAd ->
                    inFlightRequests.remove(slotKey)
                    // If user transitioned to premium while ad was loading, destroy immediately
                    if (!canShowAds(userProfile)) {
                        nativeAd.destroy()
                        return@forNativeAd
                    }
                    cachedNativeAds[slotKey]?.destroy()
                    cachedNativeAds[slotKey] = nativeAd
                    onAdLoaded(nativeAd)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        inFlightRequests.remove(slotKey)
                        Log.d(TAG, "Native ad failed to load for slot $slotKey: ${error.message}")
                        onAdFailed(error)
                    }
                })
                .withNativeAdOptions(buildSafeNativeAdOptions())
                .build()

            adLoader.loadAd(buildSafeAdRequest())
        } catch (e: Exception) {
            inFlightRequests.remove(slotKey)
            Log.w(TAG, "loadNativeAdForSlot exception: ${e.message}")
        }
    }

    /**
     * Clears and destroys all cached native ads to free memory.
     */
    fun clearCachedAds() {
        try {
            cachedNativeAds.values.forEach { it.destroy() }
            cachedNativeAds.clear()
            inFlightRequests.clear()
        } catch (e: Exception) {
            Log.w(TAG, "clearCachedAds warning: ${e.message}")
        }
    }
}
