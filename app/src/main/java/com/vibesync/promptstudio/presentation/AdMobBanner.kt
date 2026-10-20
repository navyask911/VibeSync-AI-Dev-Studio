package com.vibesync.promptstudio.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.vibesync.promptstudio.data.ads.AdMobConfig
import com.vibesync.promptstudio.data.ads.AdMobManager
import androidx.compose.ui.res.stringResource
import com.vibesync.promptstudio.R

@Composable
fun AdMobBanner(
    adMobManager: AdMobManager,
    isOnline: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val inPreview = LocalInspectionMode.current
    val adsReady by adMobManager.adsReady.collectAsStateWithLifecycle()
    val canRequestAds by adMobManager.canRequestAds.collectAsStateWithLifecycle()
    if (!isOnline || !adsReady || !canRequestAds || inPreview) return

    val widthDp = (configuration.screenWidthDp - 32).coerceAtLeast(1)
    var showSlot by remember(context, adsReady, canRequestAds, widthDp) { mutableStateOf(false) }
    val onLoadStateChanged = rememberUpdatedState<(Boolean) -> Unit> { loaded -> showSlot = loaded }

    val adView = remember(context, adsReady, canRequestAds, widthDp) { AdView(context) }

    LaunchedEffect(adView, canRequestAds, isOnline) {
        if (!isOnline) return@LaunchedEffect
        try {
            val size = AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp)
            val request = BannerAdRequest.Builder(AdMobConfig.bannerUnitId, size).build()
            adView.loadAd(
                request,
                object : AdLoadCallback<BannerAd> {
                    override fun onAdLoaded(ad: BannerAd) {
                        adMobManager.recordBannerLoaded()
                        onLoadStateChanged.value(true)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adMobManager.recordBannerFailure(error.code.toString())
                        onLoadStateChanged.value(false)
                    }
                }
            )
        } catch (error: Exception) {
            adMobManager.recordBannerFailure("unknown")
            onLoadStateChanged.value(false)
        }
    }

    DisposableEffect(adView) {
        onDispose { runCatching { adView.destroy() } }
    }

    if (showSlot) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(R.string.advertisement),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                AndroidView(
                    factory = { adView },
                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                )
            }
        }
    }
}
