package com.vibesync.promptstudio.data.ads

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.vibesync.promptstudio.data.diagnostics.DiagnosticSeverity
import com.vibesync.promptstudio.data.diagnostics.DiagnosticsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

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

    @Volatile
    private var interstitialAd: InterstitialAd? = null

    /**
     * Request consent information on each fresh app launch, then display any required form.
     * On network failure, only previously recorded UMP consent is respected via canRequestAds().
     */
    fun requestConsent(activity: Activity) {
        if (!consentRequestStarted.compareAndSet(false, true)) return
        try {
            val parameters = ConsentRequestParameters.Builder().build()
            consentInformation.requestConsentInfoUpdate(
                activity,
                parameters,
                {
                    try {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                            formError?.let {
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

    fun recordBannerFailure(errorCode: Int) {
        diagnostics.record(
            DiagnosticSeverity.DEBUG, "ads", "banner", "load_failed",
            "Banner ad failed to load (code " + errorCode + "). Core app functionality remains available."
        )
    }

    /**
     * Called only following a successful, completed prompt run while the studio destination is visible.
     * An absent/unloaded ad is skipped; an ad is never queued to appear later unexpectedly.
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
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                diagnostics.record(DiagnosticSeverity.INFO, "ads", "interstitial", "shown", "Interstitial shown after a completed prompt run.")
            }

            override fun onAdDismissedFullScreenContent() {
                diagnostics.record(DiagnosticSeverity.INFO, "ads", "interstitial", "dismissed", "Interstitial dismissed; returning control to the studio.")
                loadInterstitialIfNeeded()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                diagnostics.record(
                    DiagnosticSeverity.WARNING, "ads", "interstitial", "show_failed",
                    "Interstitial failed to show (code " + error.code + "); returning to the studio."
                )
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
            // Do not display a previously preloaded ad after the user withdraws consent.
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
        try {
            MobileAds.initialize(application) {
                mainHandler.post {
                    mutableAdsReady.value = true
                    diagnostics.record(DiagnosticSeverity.INFO, "ads", "sdk", "initialized", "Google Mobile Ads SDK initialized after the UMP consent check.")
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
    }

    private fun loadInterstitialIfNeeded() {
        if (!mutableAdsReady.value || !mutableCanRequestAds.value || interstitialAd != null ||
            !interstitialLoadStarted.compareAndSet(false, true)
        ) return

        try {
            InterstitialAd.load(
                application,
                AdMobConfig.interstitialUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
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
    const val RELEASE_APP_ID = "ca-app-pub-4783826505860771~3225002433"
    const val RELEASE_BANNER_ID = "ca-app-pub-4783826505860771/3927535003"
    const val RELEASE_INTERSTITIAL_ID = "ca-app-pub-4783826505860771/4941233773"

    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    val bannerUnitId: String
        get() = if (com.vibesync.promptstudio.BuildConfig.DEBUG) TEST_BANNER_ID else RELEASE_BANNER_ID

    val interstitialUnitId: String
        get() = if (com.vibesync.promptstudio.BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else RELEASE_INTERSTITIAL_ID
}

object AdFrequencyPolicy {
    fun shouldShowInterstitial(successfulRuns: Int, elapsedSinceLastShowMillis: Long): Boolean =
        successfulRuns >= 3 && elapsedSinceLastShowMillis >= 10L * 60L * 1000L
}
