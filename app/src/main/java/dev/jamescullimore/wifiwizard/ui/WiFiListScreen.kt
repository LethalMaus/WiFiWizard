package dev.jamescullimore.wifiwizard.ui

import android.Manifest
import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WiFiListScreen(
    onWiFiClicked: (String) -> Unit,
    onEnterManuallyClicked: () -> Unit
) {
    val wifiManager = LocalContext.current.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val networks = remember { mutableStateListOf<String>() }

    LaunchedEffect(permissionState) {
        if (permissionState.status.isGranted) {
            networks.clear()
            wifiManager.scanResults.distinctBy { it.SSID }.forEach {
                if (it.SSID.isNotBlank()) networks.add(it.SSID)
            }
        } else {
            permissionState.launchPermissionRequest()
        }
    }

    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (list, enterManuallyButton) = createRefs()


        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(list) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                    bottom.linkTo(enterManuallyButton.top)
                    height = Dimension.fillToConstraints
                },
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(networks) { network ->
                WifiNetworkItem(network, onWiFiClicked)
            }
        }

        Button(
            modifier = Modifier
                .padding(8.dp)
                .constrainAs(enterManuallyButton) {
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                },
            onClick = { onEnterManuallyClicked.invoke() }) {
            Text(text = "Enter manually")
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