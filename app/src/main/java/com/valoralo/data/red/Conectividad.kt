package com.valoralo.data.red

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Whether the device has a network with internet (airplane mode → false). */
interface Conectividad {
    val conectado: StateFlow<Boolean>
}

@Singleton
class AndroidConectividad @Inject constructor(@ApplicationContext context: Context) : Conectividad {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val estado = MutableStateFlow(hayInternet(cm.activeNetwork))
    override val conectado: StateFlow<Boolean> = estado.asStateFlow()

    init {
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { estado.value = hayInternet(network) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                estado.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
            override fun onLost(network: Network) { estado.value = false }
        })
    }

    private fun hayInternet(red: Network?): Boolean =
        red != null && cm.getNetworkCapabilities(red)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

/** Message for actions that need the server. */
const val MENSAJE_SIN_CONEXION = "Sin conexión. Inténtalo cuando vuelvas a tener red."
