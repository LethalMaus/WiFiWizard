package dev.jamescullimore.wifiwizard.data

import android.net.ConnectivityManager

data class WiFiWizardUiState (
    var connectionState: ConnectionState? = null,
    var callback: ConnectivityManager.NetworkCallback? = null
)

enum class ConnectionState {
    CONNECTING,
    CONNECTED,
    SUGGESTED,
    FAILED,
    QR_SCAN,
    NONE;
}