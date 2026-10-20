package com.vibesync.promptstudio.presentation

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.vibesync.promptstudio.data.ads.AdMobManager

@Composable
fun AdMobBanner(
    adMobManager: AdMobManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val adsReady by adMobManager.adsReady.collectAsStateWithLifecycle()
    val canRequestAds by adMobManager.canRequestAds.collectAsStateWithLifecycle()
    if (!adsReady || !canRequestAds) return

    val widthDp = (configuration.screenWidthDp - 32).coerceAtLeast(320)
    var showSlot by remember(context, adsReady, canRequestAds, widthDp) { mutableStateOf(true) }
    val onLoadStateChanged = rememberUpdatedState<(Boolean) -> Unit> { loaded ->
        showSlot = loaded
    }

    val adView = remember(context, adsReady, canRequestAds, widthDp) {
        AdView(context).apply {
            adUnitId = com.vibesync.promptstudio.data.ads.AdMobConfig.bannerUnitId
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    onLoadStateChanged.value(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    adMobManager.recordBannerFailure(error.code)
                    onLoadStateChanged.value(false)
                }
            }
        }
    }

    LaunchedEffect(adView) {
        runCatching { adView.loadAd(AdRequest.Builder().build()) }
            .onFailure { adMobManager.recordBannerFailure(-1) }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.adListener = null
            adView.destroy()
        }
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
                    "Advertisement",
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
