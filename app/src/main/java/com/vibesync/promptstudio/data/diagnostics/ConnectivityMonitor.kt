package com.vibesync.promptstudio.data.diagnostics

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class ConnectivityStatus {
    OFFLINE, CONNECTING, ONLINE;

    companion object {
        fun fromCapabilities(hasActiveNetwork: Boolean, hasInternet: Boolean, validated: Boolean): ConnectivityStatus = when {
            !hasActiveNetwork || !hasInternet -> OFFLINE
            validated -> ONLINE
            else -> CONNECTING
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityMonitor(context: Context, private val diagnostics: DiagnosticsStore? = null) {
    private val appContext = context.applicationContext

    fun observe(): Flow<ConnectivityStatus> = callbackFlow {
        val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var lastSent: ConnectivityStatus? = null

        fun sendStatus(status: ConnectivityStatus) {
            if (lastSent != status) {
                lastSent = status
                diagnostics?.record(
                    DiagnosticSeverity.INFO,
                    "connectivity",
                    "network",
                    "state_changed",
                    "Internet connectivity state changed to " + status.name + "."
                )
            }
            trySend(status)
        }

        fun currentStatus(): ConnectivityStatus {
            val network = manager.activeNetwork
            val capabilities = network?.let { manager.getNetworkCapabilities(it) }
            return ConnectivityStatus.fromCapabilities(
                hasActiveNetwork = network != null,
                hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
                validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            )
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { sendStatus(currentStatus()) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                sendStatus(
                    ConnectivityStatus.fromCapabilities(
                        hasActiveNetwork = true,
                        hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                        validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    )
                )
            }
            override fun onLost(network: Network) { sendStatus(currentStatus()) }
        }

        try {
            manager.registerDefaultNetworkCallback(callback)
            sendStatus(currentStatus())
        } catch (error: Exception) {
            diagnostics?.record(
                DiagnosticSeverity.WARNING,
                "connectivity",
                "network",
                "callback_registration_failed",
                "Could not register the connectivity callback."
            )
            sendStatus(ConnectivityStatus.OFFLINE)
        }
        awaitClose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }
}
