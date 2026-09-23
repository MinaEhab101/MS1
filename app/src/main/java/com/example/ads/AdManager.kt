package com.example.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.BoardWoodMedium
import com.example.ui.theme.TextMuted
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    // Official Google AdMob Test Ad Units
    const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917"

    var isAdsEnabled: Boolean = false

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isAdsEnabled || isInitialized) return
        try {
            val configuration = com.google.android.gms.ads.RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(configuration)

            MobileAds.initialize(context) {
                isInitialized = true
            }
        } catch (_: Exception) {}
    }

    fun loadInterstitial(context: Context) {
        if (!isAdsEnabled) return
        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                INTERSTITIAL_TEST_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                    }
                }
            )
        } catch (_: Exception) {}
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        if (!isAdsEnabled || interstitialAd == null) {
            onDismissed()
            return
        }

        try {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    onDismissed()
                }
            }
            interstitialAd?.show(activity)
        } catch (_: Exception) {
            onDismissed()
        }
    }

    fun loadRewarded(context: Context) {
        if (!isAdsEnabled) return
        try {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                context,
                REWARDED_TEST_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                    }
                }
            )
        } catch (_: Exception) {}
    }

    fun showRewarded(activity: Activity, onRewarded: (Int) -> Unit, onClosed: () -> Unit) {
        if (!isAdsEnabled || rewardedAd == null) {
            // If ad not loaded in test mode, reward directly
            onRewarded(100)
            onClosed()
            return
        }

        try {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewarded(activity)
                    onClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    rewardedAd = null
                    onClosed()
                }
            }
            rewardedAd?.show(activity) { rewardItem ->
                onRewarded(rewardItem.amount)
            }
        } catch (_: Exception) {
            onClosed()
        }
    }
}

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    if (!AdManager.isAdsEnabled) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(BoardWoodMedium),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdManager.BANNER_TEST_ID
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
