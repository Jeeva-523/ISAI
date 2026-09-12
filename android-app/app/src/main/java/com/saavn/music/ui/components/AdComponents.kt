package com.saavn.music.ui.components

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.saavn.music.R
import com.saavn.music.ads.AdManager
import com.saavn.music.ads.AdMobConfig
import com.saavn.music.data.model.UserProfile

/**
 * Reusable helper to populate a NativeAdView with NativeAd content.
 */
private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    adView.bodyView = adView.findViewById(R.id.ad_body)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    adView.iconView = adView.findViewById(R.id.ad_app_icon)
    adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

    (adView.headlineView as? TextView)?.text = nativeAd.headline

    val bodyView = adView.bodyView as? TextView
    if (nativeAd.body.isNullOrBlank()) {
        bodyView?.visibility = View.GONE
    } else {
        bodyView?.text = nativeAd.body
        bodyView?.visibility = View.VISIBLE
    }

    val ctaView = adView.callToActionView as? Button
    if (nativeAd.callToAction.isNullOrBlank()) {
        ctaView?.visibility = View.INVISIBLE
    } else {
        ctaView?.text = nativeAd.callToAction
        ctaView?.visibility = View.VISIBLE
    }

    val iconView = adView.iconView as? ImageView
    if (nativeAd.icon == null) {
        iconView?.visibility = View.GONE
    } else {
        iconView?.setImageDrawable(nativeAd.icon?.drawable)
        iconView?.visibility = View.VISIBLE
    }

    val advertiserView = adView.advertiserView as? TextView
    advertiserView?.text = if (!nativeAd.advertiser.isNullOrBlank()) nativeAd.advertiser else "Sponsored"

    adView.setNativeAd(nativeAd)
}

/**
 * 1. Centralized Responsive AdMob Banner View
 * Rules:
 * - FREE users only.
 * - PREMIUM users -> No ad request, no loading, and no banner container.
 * - Collapses cleanly on failure without leaving empty gaps.
 * - Safe areas respected; never covers player or navigation controls.
 */
@Composable
fun AdBannerView(
    userProfile: UserProfile?,
    modifier: Modifier = Modifier
) {
    // Strict Premium Check: If user is premium, NEVER request or render ads
    if (!AdManager.canShowAds(userProfile)) {
        return
    }

    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    // If ad failed to load, cleanly collapse container with 0 height
    if (hasError) {
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isAdLoaded) Modifier.wrapContentHeight() else Modifier.height(0.dp)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdMobConfig.ADMOB_BANNER_AD_UNIT_ID
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                            hasError = false
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                            hasError = true
                        }
                    }
                    loadAd(AdManager.buildSafeAdRequest())
                }
            },
            modifier = if (isAdLoaded) Modifier.fillMaxWidth().wrapContentHeight() else Modifier.height(0.dp)
        )
    }
}

/**
 * 2. Song List Native Ad Item (Ad after every 5 songs)
 * Rules:
 * - FREE users only.
 * - PREMIUM users -> No ad request and no container.
 * - Does not count as a song; maintains correct song numbering.
 * - Distinct from song rows (clearly labeled advertisement).
 * - Collapses cleanly on failure.
 */
@Composable
fun SongListNativeAdItem(
    userProfile: UserProfile?,
    slotIndex: Int,
    modifier: Modifier = Modifier
) {
    if (!AdManager.canShowAds(userProfile)) {
        return
    }

    val context = LocalContext.current
    val slotKey = "song_list_slot_$slotIndex"
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(slotKey, userProfile?.isPremium) {
        if (!AdManager.canShowAds(userProfile)) {
            nativeAd = null
            return@LaunchedEffect
        }
        AdManager.loadNativeAdForSlot(
            context = context,
            slotKey = slotKey,
            userProfile = userProfile,
            onAdLoaded = { ad ->
                nativeAd = ad
                hasError = false
            },
            onAdFailed = {
                hasError = true
            }
        )
    }

    if (hasError || nativeAd == null) {
        return
    }

    AnimatedVisibility(
        visible = nativeAd != null && !hasError,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        nativeAd?.let { ad ->
            AndroidView(
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx)
                        .inflate(R.layout.layout_isai_native_ad, null, false) as NativeAdView
                    populateNativeAdView(ad, view)
                    view
                },
                update = { view ->
                    populateNativeAdView(ad, view)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }
    }
}

/**
 * 3. Playlist Page Native Ad
 * Rules:
 * - FREE users only.
 * - Placed after first group of playlist songs / playlist header.
 * - Distinct from playlist items with clear advertisement label.
 * - Collapses on failure without empty gaps.
 */
@Composable
fun PlaylistNativeAdCard(
    userProfile: UserProfile?,
    playlistId: String,
    modifier: Modifier = Modifier
) {
    if (!AdManager.canShowAds(userProfile)) {
        return
    }

    val context = LocalContext.current
    val slotKey = "playlist_ad_$playlistId"
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(slotKey, userProfile?.isPremium) {
        if (!AdManager.canShowAds(userProfile)) {
            nativeAd = null
            return@LaunchedEffect
        }
        AdManager.loadNativeAdForSlot(
            context = context,
            slotKey = slotKey,
            userProfile = userProfile,
            onAdLoaded = { ad ->
                nativeAd = ad
                hasError = false
            },
            onAdFailed = {
                hasError = true
            }
        )
    }

    if (hasError || nativeAd == null) {
        return
    }

    AnimatedVisibility(
        visible = nativeAd != null && !hasError,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        nativeAd?.let { ad ->
            AndroidView(
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx)
                        .inflate(R.layout.layout_isai_native_ad, null, false) as NativeAdView
                    populateNativeAdView(ad, view)
                    view
                },
                update = { view ->
                    populateNativeAdView(ad, view)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }
    }
}

/**
 * 4. Now Playing Screen Native Ad
 * Rules:
 * - FREE users only.
 * - Positioned above artwork area, clearly labeled [SPONSORED].
 * - Song thumbnail and player controls are completely preserved and distinct.
 * - NEVER plays audio, never interferes with playback/seek/pause.
 * - Collapses cleanly if load fails.
 */
@Composable
fun NowPlayingNativeAdCard(
    userProfile: UserProfile?,
    modifier: Modifier = Modifier
) {
    if (!AdManager.canShowAds(userProfile)) {
        return
    }

    val context = LocalContext.current
    val slotKey = "now_playing_ad_slot"
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(slotKey, userProfile?.isPremium) {
        if (!AdManager.canShowAds(userProfile)) {
            nativeAd = null
            return@LaunchedEffect
        }
        AdManager.loadNativeAdForSlot(
            context = context,
            slotKey = slotKey,
            userProfile = userProfile,
            onAdLoaded = { ad ->
                nativeAd = ad
                hasError = false
            },
            onAdFailed = {
                hasError = true
            }
        )
    }

    if (hasError || nativeAd == null) {
        return
    }

    AnimatedVisibility(
        visible = nativeAd != null && !hasError,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        nativeAd?.let { ad ->
            AndroidView(
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx)
                        .inflate(R.layout.layout_isai_native_ad_compact, null, false) as NativeAdView
                    populateNativeAdView(ad, view)
                    view
                },
                update = { view ->
                    populateNativeAdView(ad, view)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }
    }
}
