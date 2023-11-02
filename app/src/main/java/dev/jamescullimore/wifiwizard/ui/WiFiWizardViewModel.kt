package dev.jamescullimore.wifiwizard.ui

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import dev.jamescullimore.wifiwizard.data.ConnectionState
import dev.jamescullimore.wifiwizard.data.WiFiWizardUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WiFiWizardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WiFiWizardUiState())
    val uiState: StateFlow<WiFiWizardUiState> = _uiState.asStateFlow()

    private fun setCallback(callback: ConnectivityManager.NetworkCallback) {
        _uiState.update {
            it.copy(
                callback = callback,
                connectionState = it.connectionState
            )
        }
    }

    fun setConnectionState(connectionState: ConnectionState) {
        _uiState.update {
            it.copy(
                callback = it.callback,
                connectionState = connectionState
            )
        }
    }

    private fun connectToNetworkId(wifiManager: WifiManager, it: Int) {
        val networkId = wifiManager.connectionInfo?.networkId ?: -1
        wifiManager.removeNetwork(networkId)
        wifiManager.saveConfiguration()
        wifiManager.disconnect()
        wifiManager.enableNetwork(it, true)
        if (wifiManager.reconnect()) {
            setConnectionState(ConnectionState.CONNECTED)
        } else {
            setConnectionState(ConnectionState.FAILED)
        }
    }

    private fun getNetworkCallback(connectivityManager: ConnectivityManager?): ConnectivityManager.NetworkCallback {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                super.onLost(network)
                uiState.value.callback?.let {
                    connectivityManager?.bindProcessToNetwork(null)
                    connectivityManager?.unregisterNetworkCallback(it)
                }
                setConnectionState(ConnectionState.FAILED)
            }

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                setConnectionState(ConnectionState.CONNECTED)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                setConnectionState(ConnectionState.FAILED)
            }
        }
        setCallback(callback)
        return callback
    }

    fun connectToWiFi(wifiManager: WifiManager, connectivityManager: ConnectivityManager?, ssid: String, password: String) {
        setConnectionState(ConnectionState.CONNECTING)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            wifiManager.let {
                val conf = WifiConfiguration()
                conf.SSID = "\"$ssid\""
                conf.preSharedKey = "\"${password}\""
                connectToNetworkId(wifiManager, it.addNetwork(conf))
            }
        } else {
            val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder().apply {
                setSsid(ssid)
                setWpa2Passphrase(password)
            }.build()

            val networkRequest = NetworkRequest.Builder().apply {
                addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                setNetworkSpecifier(wifiNetworkSpecifier)
            }.build()

            getNetworkCallback(connectivityManager).let {
                connectivityManager?.requestNetwork(networkRequest!!, it)
            }
        }
    }

    fun isConnected(ssid: String, wifiManager: WifiManager, connectivityManager: ConnectivityManager?) {
        return wifiManager.let { wm ->
            val wifi = connectivityManager?.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            val ssidMatch = wm.connectionInfo?.ssid?.replace("\"", "") == ssid
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && wifi?.isConnected == true) {
                setConnectionState(ConnectionState.CONNECTED)
            } else if (ssidMatch) {
                setConnectionState(ConnectionState.CONNECTED)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun suggestWiFi(wifiManager: WifiManager, ssid: String, password: String) {
        val suggestion: WifiNetworkSuggestion = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val suggestionsList: List<WifiNetworkSuggestion> = listOf(suggestion)

        wifiManager.addNetworkSuggestions(suggestionsList)
        setConnectionState(ConnectionState.SUGGESTED)
    }
}