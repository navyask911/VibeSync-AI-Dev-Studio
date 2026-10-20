package com.vibesync.promptstudio.data.ads

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.ExperimentalApi
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.vibesync.promptstudio.data.diagnostics.DiagnosticSeverity
import com.vibesync.promptstudio.data.diagnostics.DiagnosticsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalApi::class)
class AdMobManager(
    private val application: Application,
    private val diagnostics: DiagnosticsStore
) {
    private val consentInformation = UserMessagingPlatform.getConsentInformation(application)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val consentRequestStarted = AtomicBoolean(false)
    private val sdkInitializationStarted = AtomicBoolean(false)
    private val interstitialLoadStarted = AtomicBoolean(false)
    private val preferences = application.getSharedPreferences("vibesync_ad_frequency_v1", 0)

    private val mutableCanRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = mutableCanRequestAds.asStateFlow()

    private val mutableAdsReady = MutableStateFlow(false)
    val adsReady: StateFlow<Boolean> = mutableAdsReady.asStateFlow()

    private val mutablePrivacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = mutablePrivacyOptionsRequired.asStateFlow()

    private val mutableBannerLoadFailed = MutableStateFlow(false)
    val bannerLoadFailed: StateFlow<Boolean> = mutableBannerLoadFailed.asStateFlow()
    private val mutableBannerLoaded = MutableStateFlow(false)
    val bannerLoaded: StateFlow<Boolean> = mutableBannerLoaded.asStateFlow()

    @Volatile
    private var interstitialAd: InterstitialAd? = null

    /**
     * Refresh UMP consent information on app resume and show any required consent form.
     * Ad requests remain disabled unless UMP reports that requests are permitted.
     */
    fun requestConsent(activity: Activity) {
        if (!consentRequestStarted.compareAndSet(false, true)) return
        try {
            consentInformation.requestConsentInfoUpdate(
                activity,
                ConsentRequestParameters.Builder().build(),
                {
                    try {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                            error?.let {
                                diagnostics.record(
                                    DiagnosticSeverity.WARNING, "ads", "consent", "form_unavailable",
                                    "Ad privacy form could not be shown (code " + it.errorCode + ")."
                                )
                            }
                            resolveConsentAndInitialize()
                        }
                    } catch (error: Exception) {
                        diagnostics.record(
                            DiagnosticSeverity.WARNING, "ads", "consent", "form_failed",
                            "Ad privacy form failed safely: " + error.javaClass.simpleName
                        )
                        resolveConsentAndInitialize()
                    }
                },
                { error ->
                    diagnostics.record(
                        DiagnosticSeverity.WARNING, "ads", "consent", "update_failed",
                        "Ad consent information update failed (code " + error.errorCode + ")."
                    )
                    resolveConsentAndInitialize()
                }
            )
        } catch (error: Exception) {
            diagnostics.record(
                DiagnosticSeverity.WARNING, "ads", "consent", "update_exception",
                "Ad consent update could not start: " + error.javaClass.simpleName
            )
            resolveConsentAndInitialize()
        }
    }

    fun showPrivacyOptions(activity: Activity, onFinished: (() -> Unit)? = null) {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                error?.let {
                    diagnostics.record(
                        DiagnosticSeverity.WARNING, "ads", "consent", "privacy_options_failed",
                        "Ad privacy options form failed (code " + it.errorCode + ")."
                    )
                }
                resolveConsentAndInitialize()
                onFinished?.invoke()
            }
        } catch (error: Exception) {
            diagnostics.record(
                DiagnosticSeverity.WARNING, "ads", "consent", "privacy_options_exception",
                "Ad privacy options could not be opened: " + error.javaClass.simpleName
            )
            onFinished?.invoke()
        }
    }

    fun resetBannerFailure() {
        mutableBannerLoadFailed.value = false
        mutableBannerLoaded.value = false
    }

    fun recordBannerLoaded() {
        mutableBannerLoadFailed.value = false
        mutableBannerLoaded.value = true
    }

    fun recordBannerFailure(errorCode: String) {
        mutableBannerLoadFailed.value = true
        mutableBannerLoaded.value = false
        diagnostics.record(
            DiagnosticSeverity.DEBUG, "ads", "banner", "load_failed",
            "Banner ad failed to load (code " + errorCode + "). Core app functionality remains available."
        )
    }

    /**
     * Call only after a successful prompt run. If the preloaded ad is unavailable, skip it rather
     * than queueing a surprise full-screen ad later.
     */
    fun onSuccessfulPromptRun(activity: Activity, studioVisible: Boolean): Boolean {
        val completedRuns = preferences.getInt(KEY_SUCCESSFUL_RUNS, 0) + 1
        preferences.edit().putInt(KEY_SUCCESSFUL_RUNS, completedRuns).apply()
        if (!mutableCanRequestAds.value || !mutableAdsReady.value ||
            !studioVisible || completedRuns < MIN_SUCCESSFUL_RUNS ||
            activity.isFinishing || activity.isDestroyed ||
            (activity as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true
        ) {
            return false
        }

        val now = System.currentTimeMillis()
        val lastShown = preferences.getLong(KEY_LAST_INTERSTITIAL_TIME, 0L)
        if (!AdFrequencyPolicy.shouldShowInterstitial(completedRuns, now - lastShown)) {
            loadInterstitialIfNeeded()
            return false
        }

        val ad = interstitialAd ?: run {
            loadInterstitialIfNeeded()
            return false
        }
        interstitialAd = null
        preferences.edit().putLong(KEY_LAST_INTERSTITIAL_TIME, now).apply()
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                diagnostics.record(DiagnosticSeverity.INFO, "ads", "interstitial", "shown", "Interstitial shown after a completed prompt run.")
            }

            override fun onAdDismissedFullScreenContent() {
                diagnostics.record(DiagnosticSeverity.INFO, "ads", "interstitial", "dismissed", "Interstitial dismissed; returning control to the studio.")
                ad.destroy()
                loadInterstitialIfNeeded()
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                diagnostics.record(
                    DiagnosticSeverity.WARNING, "ads", "interstitial", "show_failed",
                    "Interstitial failed to show (" + fullScreenContentError.javaClass.simpleName + "); returning to the studio."
                )
                ad.destroy()
                loadInterstitialIfNeeded()
            }
        }
        return try {
            ad.show(activity)
            true
        } catch (error: Exception) {
            diagnostics.record(
                DiagnosticSeverity.WARNING, "ads", "interstitial", "show_exception",
                "Interstitial could not be shown: " + error.javaClass.simpleName
            )
            ad.destroy()
            loadInterstitialIfNeeded()
            false
        }
    }

    private fun resolveConsentAndInitialize() {
        val allowed = runCatching { consentInformation.canRequestAds() }.getOrDefault(false)
        mutableCanRequestAds.value = allowed
        mutablePrivacyOptionsRequired.value = runCatching {
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        }.getOrDefault(false)

        if (!allowed) {
            interstitialAd?.destroy()
            interstitialAd = null
            diagnostics.record(
                DiagnosticSeverity.INFO, "ads", "consent", "ads_not_allowed",
                "Ad requests remain disabled because UMP has not authorized requests."
            )
            return
        }
        if (!sdkInitializationStarted.compareAndSet(false, true)) {
            if (mutableAdsReady.value) loadInterstitialIfNeeded()
            return
        }

        // The Next-Gen SDK initialization may perform disk/network I/O; never run it on the UI thread.
        Thread {
            try {
                MobileAds.initialize(
                    application,
                    InitializationConfig.Builder(AdMobConfig.appId).build()
                ) {
                    mainHandler.post {
                        mutableAdsReady.value = true
                        diagnostics.record(
                            DiagnosticSeverity.INFO, "ads", "sdk", "initialized",
                            "Google Mobile Ads Next-Gen SDK initialized after the UMP consent check."
                        )
                        loadInterstitialIfNeeded()
                    }
                }
            } catch (error: Exception) {
                sdkInitializationStarted.set(false)
                diagnostics.record(
                    DiagnosticSeverity.WARNING, "ads", "sdk", "initialization_failed",
                    "Google Mobile Ads SDK initialization failed: " + error.javaClass.simpleName
                )
            }
        }.apply { name = "vibesync-ads-init" }.start()
    }

    private fun loadInterstitialIfNeeded() {
        if (!mutableAdsReady.value || !mutableCanRequestAds.value || interstitialAd != null ||
            !interstitialLoadStarted.compareAndSet(false, true)
        ) return

        try {
            InterstitialAd.load(
                AdRequest.Builder(AdMobConfig.interstitialUnitId).build(),
                object : AdLoadCallback<InterstitialAd> {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialLoadStarted.set(false)
                        interstitialAd = ad
                        diagnostics.record(
                            DiagnosticSeverity.INFO, "ads", "interstitial", "loaded",
                            "Interstitial ad loaded and is available for a later eligible transition."
                        )
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialLoadStarted.set(false)
                        diagnostics.record(
                            DiagnosticSeverity.DEBUG, "ads", "interstitial", "load_failed",
                            "Interstitial ad failed to load (code " + error.code + "). It will not block the completed action."
                        )
                    }
                }
            )
        } catch (error: Exception) {
            interstitialLoadStarted.set(false)
            diagnostics.record(
                DiagnosticSeverity.DEBUG, "ads", "interstitial", "load_exception",
                "Interstitial load failed safely: " + error.javaClass.simpleName
            )
        }
    }

    companion object {
        private const val KEY_SUCCESSFUL_RUNS = "successful_prompt_runs"
        private const val KEY_LAST_INTERSTITIAL_TIME = "last_interstitial_shown_at"
        private const val MIN_SUCCESSFUL_RUNS = 3
        private const val INTERSTITIAL_MIN_INTERVAL_MS = 10L * 60L * 1000L
    }
}

object AdMobConfig {
    // IDs retained for regression checks; runtime values are defined per Gradle build variant.
    const val RELEASE_APP_ID = "ca-app-pub-4783826505860771~3225002433"
    const val RELEASE_BANNER_ID = "ca-app-pub-4783826505860771/3927535003"
    const val RELEASE_INTERSTITIAL_ID = "ca-app-pub-4783826505860771/4941233773"
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    val appId: String get() = com.vibesync.promptstudio.BuildConfig.ADMOB_APP_ID
    val bannerUnitId: String get() = com.vibesync.promptstudio.BuildConfig.ADMOB_BANNER_UNIT_ID
    val interstitialUnitId: String get() = com.vibesync.promptstudio.BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID
}

object AdFrequencyPolicy {
    fun shouldShowInterstitial(successfulRuns: Int, elapsedSinceLastShowMillis: Long): Boolean =
        successfulRuns >= 3 && elapsedSinceLastShowMillis >= 10L * 60L * 1000L
}
