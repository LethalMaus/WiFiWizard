package dev.jamescullimore.wifiwizard.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.net.wifi.WifiManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WiFiListScreen(
    onWiFiClicked: (String) -> Unit,
    onEnterManuallyClicked: () -> Unit,
    onScanQrCodeClicked: () -> Unit
) {
    val context = LocalContext.current
    val wifiManager = LocalContext.current.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val networks = remember { mutableStateListOf<String>() }
    val warningMessage = remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (permissionState.status.isGranted) {
                    val isWifiEnabled = wifiManager.isWifiEnabled
                    val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    warningMessage.value = when {
                        !isWifiEnabled && !isLocationEnabled -> "Please enable both WiFi and Location to scan for networks."
                        !isWifiEnabled -> "Please enable WiFi to scan for networks."
                        !isLocationEnabled -> "Please enable Location to scan for networks."
                        else -> null
                    }
                    networks.clear()
                    wifiManager.scanResults.distinctBy { it.SSID }.forEach {
                        if (it.SSID.isNotBlank()) networks.add(it.SSID)
                    }
                } else {
                    permissionState.launchPermissionRequest()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (list, enterManuallyButton, scanQrButton, banner) = createRefs()

        warningMessage.value?.let { message ->
            Text(
                text = message,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(8.dp)
                    .constrainAs(createRef()) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(list) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                    bottom.linkTo(banner.top)
                    height = Dimension.fillToConstraints
                },
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(networks) { network ->
                WifiNetworkItem(network, onWiFiClicked)
            }
        }

        AdmobBanner(modifier = Modifier
            .fillMaxWidth()
            .constrainAs(banner) {
                bottom.linkTo(enterManuallyButton.top)
            })

        Button(
            modifier = Modifier
                .padding(8.dp)
                .constrainAs(enterManuallyButton) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(scanQrButton.start)
                },
            onClick = { onEnterManuallyClicked.invoke() }) {
            Text(text = "Enter manually")
        }

        Button(
            modifier = Modifier
                .padding(8.dp)
                .constrainAs(scanQrButton) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(enterManuallyButton.end)
                    end.linkTo(parent.end)
                },
            onClick = { onScanQrCodeClicked.invoke() }) {
            Text(text = "Scan QR Code")
        }
    }
}

@Composable
fun WifiNetworkItem(ssid: String, onWiFiClicked: (String) -> Unit) {
    Card(modifier = Modifier.padding(4.dp)) {
        Text(
            text = ssid,
            modifier = Modifier
                .clickable {
                    onWiFiClicked(ssid)
                }
                .padding(16.dp)
        )
    }
}