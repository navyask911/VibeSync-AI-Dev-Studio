package com.vibesync.promptstudio.data.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectivityMonitorTest {
    @Test
    fun connectivityTransitionDistinguishesOfflineConnectingAndValidatedInternet() {
        assertEquals(ConnectivityStatus.OFFLINE, ConnectivityStatus.fromCapabilities(false, false, false))
        assertEquals(ConnectivityStatus.OFFLINE, ConnectivityStatus.fromCapabilities(true, false, false))
        assertEquals(ConnectivityStatus.CONNECTING, ConnectivityStatus.fromCapabilities(true, true, false))
        assertEquals(ConnectivityStatus.ONLINE, ConnectivityStatus.fromCapabilities(true, true, true))
    }
}
