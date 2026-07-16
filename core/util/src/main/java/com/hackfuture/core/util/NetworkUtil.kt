package com.hackfuture.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 网络连接状态
 */
data class NetworkState(
    val isConnected: Boolean,
    val type: NetworkType = NetworkType.UNKNOWN,
) {
    val isUnavailable: Boolean get() = !isConnected
}

enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    UNKNOWN,
    NONE,
}

/**
 * 网络状态观察者
 */
class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * 观察网络连接状态变化，发射 [NetworkState]
     */
    fun observeNetworkState(): Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getCurrentNetworkState())
            }

            override fun onLost(network: Network) {
                trySend(NetworkState(false, NetworkType.NONE))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(getCurrentNetworkState())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // 发送初始状态
        trySend(getCurrentNetworkState())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * 获取当前网络状态（一次性快照）
     */
    fun getCurrentNetworkState(): NetworkState {
        val network = connectivityManager.activeNetwork ?: return NetworkState(false, NetworkType.NONE)
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkState(false, NetworkType.NONE)

        val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
            else -> NetworkType.UNKNOWN
        }

        return NetworkState(isConnected, type)
    }
}
