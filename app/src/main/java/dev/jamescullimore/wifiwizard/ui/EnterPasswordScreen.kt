package dev.jamescullimore.wifiwizard.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
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
    var password by remember { mutableStateOf("") }
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

    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (loader, instructionText, ssidField, passwordField, connectButton, suggestButton) = createRefs()

        if (uiState.connectionState == ConnectionState.CONNECTING) {
            CircularProgressIndicator(
                modifier = Modifier.constrainAs(loader) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
            )
        }
        if (chosenSsid.isNotBlank()) {
            Text(
                text = "Enter the password for $ssid",
                modifier = Modifier.constrainAs(instructionText) {
                    bottom.linkTo(passwordField.top, margin = 8.dp)
                    start.linkTo(parent.start, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                }
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
                onValueChange = { ssid = it },
                label = { Text(text = "SSID") },
                modifier = Modifier.constrainAs(ssidField) {
                    bottom.linkTo(passwordField.top, margin = 8.dp)
                    start.linkTo(parent.start, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                }.semantics {
                    contentDescription = "SSID TextField"
                },
                enabled = uiState.connectionState != ConnectionState.CONNECTING,
            )
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Password") },
            modifier = Modifier.constrainAs(passwordField) {
                top.linkTo(parent.top)
                start.linkTo(parent.start, margin = 16.dp)
                end.linkTo(parent.end, margin = 16.dp)
                bottom.linkTo(parent.bottom)
            }.semantics {
                contentDescription = "Password TextField"
            },
            enabled = uiState.connectionState != ConnectionState.CONNECTING
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
            enabled = ssid.isNotBlank() && uiState.connectionState != ConnectionState.CONNECTING
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
                enabled = ssid.isNotBlank() && uiState.connectionState != ConnectionState.CONNECTING
            ) {
                Text(text = "Suggest")
            }
        }
    }
}

@Composable
fun ConnectionFailedDialog (
    onConnectViaSettings: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Default.Warning, contentDescription = "Warning")
        },
        title = {
            Text(text = "Connection Failed")
        },
        text = {
            Text(text = "Could not connect to the provided WiFi. Please make sure the password is correct and try again.")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConnectViaSettings()
                }
            ) {
                Text("Connect via Settings")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("OK")
            }
        }
    )
}

@Composable
fun ConnectionSuccessDialog (
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Default.Check, contentDescription = "Warning")
        },
        title = {
            Text(text = "Connection Successful")
        },
        text = {
            Text(text = "Your WiFi connection was successful.")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("OK")
            }
        }
    )
}

@Composable
fun ConnectionSuggestedDialog (
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Default.List, contentDescription = "Warning")
        },
        title = {
            Text(text = "WiFI Suggested")
        },
        text = {
            Text(text = "Your WiFi was suggested, check your settings to confirm if automatic connections will be made.")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("OK")
            }
        }
    )
}