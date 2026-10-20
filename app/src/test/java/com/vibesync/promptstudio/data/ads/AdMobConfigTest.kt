package com.vibesync.promptstudio.data.ads

import com.vibesync.promptstudio.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdMobConfigTest {
    @Test
    fun releaseIdsAreExactlyTheConfiguredPublisherIds() {
        assertEquals("ca-app-pub-4783826505860771~3225002433", AdMobConfig.RELEASE_APP_ID)
        assertEquals("ca-app-pub-4783826505860771/3927535003", AdMobConfig.RELEASE_BANNER_ID)
        assertEquals("ca-app-pub-4783826505860771/4941233773", AdMobConfig.RELEASE_INTERSTITIAL_ID)
    }

    @Test
    fun debugVariantAlwaysUsesGoogleSampleAdUnits() {
        if (BuildConfig.DEBUG) {
            assertEquals(AdMobConfig.TEST_BANNER_ID, AdMobConfig.bannerUnitId)
            assertEquals(AdMobConfig.TEST_INTERSTITIAL_ID, AdMobConfig.interstitialUnitId)
        }
    }

    @Test
    fun interstitialFrequencyRequiresThreeSuccessfulRunsAndTenMinutes() {
        assertFalse(AdFrequencyPolicy.shouldShowInterstitial(1, 60L * 60L * 1000L))
        assertFalse(AdFrequencyPolicy.shouldShowInterstitial(2, 60L * 60L * 1000L))
        assertFalse(AdFrequencyPolicy.shouldShowInterstitial(3, 2L * 60L * 1000L))
        assertFalse(AdFrequencyPolicy.shouldShowInterstitial(3, -1L))
        assertTrue(AdFrequencyPolicy.shouldShowInterstitial(3, 10L * 60L * 1000L))
        assertTrue(AdFrequencyPolicy.shouldShowInterstitial(6, 60L * 60L * 1000L))
    }
}
