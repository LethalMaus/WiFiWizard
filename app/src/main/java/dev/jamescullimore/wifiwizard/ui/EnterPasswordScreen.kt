package dev.jamescullimore.wifiwizard.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jamescullimore.wifiwizard.data.ConnectionState
import dev.jamescullimore.wifiwizard.data.WiFiWizardUiState

@Composable
fun EnterPasswordScreen(
    chosenSsid: String = "",
    onSuccess: () -> Unit,
    onGoToSettings: () -> Unit,
) {
    val viewModel: WiFiWizardViewModel = viewModel()
    var ssid by remember { mutableStateOf(chosenSsid) }
    val showConnectionFailedDialog = remember { mutableStateOf(false) }
    val showConnectionSuccessDialog = remember { mutableStateOf(false) }
    val showConnectionSuggestedDialog = remember { mutableStateOf(false) }

    val wifiManager = LocalContext.current.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val connectivityManager = ContextCompat.getSystemService(LocalContext.current, ConnectivityManager::class.java)
    val uiState: WiFiWizardUiState by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.isConnected(ssid, wifiManager, connectivityManager)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    uiState.connectionState?.let {
        when {
            !showConnectionSuccessDialog.value && it == ConnectionState.CONNECTED -> showConnectionSuccessDialog.value = true
            !showConnectionSuggestedDialog.value && it == ConnectionState.SUGGESTED -> showConnectionSuggestedDialog.value = true
            !showConnectionFailedDialog.value && it == ConnectionState.FAILED -> showConnectionFailedDialog.value = true
            else -> { /* do nothing */ }
        }
    }

    when {
        showConnectionFailedDialog.value -> ConnectionFailedDialog(
            onConnectViaSettings = {
                showConnectionFailedDialog.value = false
                viewModel.setConnectionState(ConnectionState.NONE)
                onGoToSettings.invoke()
            },
            onDismissRequest = {
                showConnectionFailedDialog.value = false
                viewModel.setConnectionState(ConnectionState.NONE)
            }
        )
        showConnectionSuccessDialog.value -> ConnectionSuccessDialog(
            onDismissRequest = { onSuccess.invoke() }
        )
        showConnectionSuggestedDialog.value -> ConnectionSuggestedDialog(
            onDismissRequest = { onSuccess.invoke() }
        )
    }

    EnterPasswordLayout(
        viewModel = viewModel,
        connectionState = uiState.connectionState,
        ssid = ssid,
        onSsidChange = { ssid = it },
        wifiManager = wifiManager,
        connectivityManager = connectivityManager
    )
}

@Composable
fun EnterPasswordLayout(
    viewModel: WiFiWizardViewModel,
    connectionState: ConnectionState?,
    ssid: String,
    onSsidChange: (String) -> Unit,
    wifiManager: WifiManager,
    connectivityManager: ConnectivityManager?
) {
    var password by remember { mutableStateOf("") }

    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (loader, instructionText, ssidField, passwordField, connectButton, suggestButton) = createRefs()

        if (connectionState == ConnectionState.CONNECTING) {
            CircularProgressIndicator(
                modifier = Modifier.constrainAs(loader) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
            )
        }
        if (ssid.isNotBlank()) {
            Text(
                text = "Enter the password for $ssid",
                modifier = Modifier.constrainAs(instructionText) {
                    bottom.linkTo(passwordField.top, margin = 8.dp)
                    start.linkTo(passwordField.start)
                    end.linkTo(passwordField.end)
                    width = Dimension.fillToConstraints

                },
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = "Enter the SSID & password",
                modifier = Modifier.constrainAs(instructionText) {
                    bottom.linkTo(ssidField.top, margin = 8.dp)
                    start.linkTo(parent.start, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                }
            )
            OutlinedTextField(
                value = ssid,
                onValueChange = { onSsidChange(it) },
                label = { Text(text = "SSID") },
                modifier = Modifier
                    .constrainAs(ssidField) {
                        bottom.linkTo(passwordField.top, margin = 8.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                        end.linkTo(parent.end, margin = 16.dp)
                    }
                    .semantics {
                        contentDescription = "SSID TextField"
                    },
                enabled = connectionState != ConnectionState.CONNECTING,
            )
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Password") },
            modifier = Modifier
                .constrainAs(passwordField) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                    bottom.linkTo(parent.bottom)
                }
                .semantics {
                    contentDescription = "Password TextField"
                },
            enabled = connectionState != ConnectionState.CONNECTING
        )

        Button(
            modifier = Modifier
                .padding(8.dp)
                .constrainAs(connectButton) {
                    top.linkTo(passwordField.bottom)
                    end.linkTo(passwordField.end)
                },
            onClick = {
                viewModel.connectToWiFi(wifiManager, connectivityManager, ssid, password)
            },
            enabled = ssid.isNotBlank() && connectionState != ConnectionState.CONNECTING
        ) {
            Text(text = "Connect")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Button(
                modifier = Modifier
                    .padding(8.dp)
                    .constrainAs(suggestButton) {
                        top.linkTo(passwordField.bottom)
                        start.linkTo(passwordField.start)
                    },
                onClick = {
                    viewModel.suggestWiFi(wifiManager, ssid, password)
                },
                enabled = ssid.isNotBlank() && connectionState != ConnectionState.CONNECTING
            ) {
                Text(text = "Suggest")
            }
        }
    }
}