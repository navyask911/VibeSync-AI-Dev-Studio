package com.vibesync.promptstudio.data.diagnostics

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observes only validated internet connectivity. It does not inspect, persist, or transmit
 * network traffic, destinations, prompts, or user activity.
 */
class NetworkMonitor(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val mutableOnline = MutableStateFlow(readOnlineState())
    val isOnline: StateFlow<Boolean> = mutableOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            mutableOnline.value = readOnlineState(network)
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            mutableOnline.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        override fun onLost(network: Network) {
            mutableOnline.value = readOnlineState()
        }
    }

    init {
        try {
            manager.registerDefaultNetworkCallback(callback)
            mutableOnline.value = readOnlineState()
        } catch (_: RuntimeException) {
            mutableOnline.value = false
        }
    }

    private fun readOnlineState(network: Network? = manager.activeNetwork): Boolean {
        val capabilities = network?.let { manager.getNetworkCapabilities(it) } ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun close() {
        runCatching { manager.unregisterNetworkCallback(callback) }
    }
}
