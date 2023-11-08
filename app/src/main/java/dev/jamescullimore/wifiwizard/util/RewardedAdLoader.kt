package dev.jamescullimore.wifiwizard.util

import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dev.jamescullimore.wifiwizard.BuildConfig
import dev.jamescullimore.wifiwizard.MainActivity

object RewardedAdLoader {
    var rewardedAd: RewardedAd? = null

    fun loadRewardedAd(context: Context) {
        RewardedAd.load(
            context,
            BuildConfig.REWARD_AD,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) { rewardedAd = null }
                override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
            })
    }

    fun showRewardedAd(activity: MainActivity) {
        rewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { loadRewardedAd(activity) }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) { rewardedAd = null }
        }
        rewardedAd?.show(activity) {}
    }
}