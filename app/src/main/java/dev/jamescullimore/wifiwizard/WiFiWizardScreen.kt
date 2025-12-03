package dev.jamescullimore.wifiwizard

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dev.jamescullimore.wifiwizard.ui.EnterPasswordScreen
import dev.jamescullimore.wifiwizard.ui.QrScannerScreen
import dev.jamescullimore.wifiwizard.ui.WiFiListScreen
import dev.jamescullimore.wifiwizard.util.RewardedAdLoader
import androidx.core.net.toUri


enum class WiFiWizardScreen(@StringRes val title: Int, val route: String) {
    WiFiList(title = R.string.app_name, route = "WiFiList"),
    ConnectToWiFi(title = R.string.connect_to_wifi, route = "ConnectToWiFi/{ssid}"),
    ScanQrCode(title = R.string.scan_qr_code, route = "ScanQrCode");

    companion object {
        fun getScreenByRoute(route: String): WiFiWizardScreen {
            return when (route) {
                WiFiList.route -> WiFiList
                ConnectToWiFi.name,
                ConnectToWiFi.route,
                -> ConnectToWiFi
                ScanQrCode.route -> ScanQrCode
                else -> throw IllegalArgumentException("Unknown WiFiWizardScreen route: $route")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiWizardAppBar(
    currentScreen: WiFiWizardScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    RewardedAdLoader.loadRewardedAd(context)

    TopAppBar(
        title = { Text(stringResource(currentScreen.title)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            scrolledContainerColor = Color.Unspecified,
            navigationIconContentColor = Color.Unspecified,
            titleContentColor = Color.Unspecified,
            actionIconContentColor = Color.Unspecified
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(text = {
                    Text(text = "Open Source Libraries")
                }, onClick = {
                    val intent = Intent(context, OssLicensesMenuActivity::class.java)
                    context.startActivity(intent)
                })
                DropdownMenuItem(text = {
                    Text(text = "Source Code")
                }, onClick = {
                    val browserIntent = Intent(Intent.ACTION_VIEW,
                        "https://github.com/LethalMaus/WiFiWizard".toUri())
                    context.startActivity(browserIntent)
                })
                DropdownMenuItem(text = {
                    Text(text = "StackOverflow")
                }, onClick = {
                    val browserIntent = Intent(Intent.ACTION_VIEW,
                        "https://stackoverflow.com/questions/63124728/connect-to-wifi-in-android-q-programmatically/65327716#65327716".toUri())
                    context.startActivity(browserIntent)
                })
                DropdownMenuItem(text = {
                    Text(text = "Article")
                }, onClick = {
                    val browserIntent = Intent(Intent.ACTION_VIEW,
                        "https://levelup.gitconnected.com/wifi-wizardry-a-developers-guide-to-android-network-magic-4f0f81c612d3".toUri())
                    context.startActivity(browserIntent)
                })
                DropdownMenuItem(text = {
                    Text(text = "Watch Advert")
                }, onClick = {
                    RewardedAdLoader.showRewardedAd(context as MainActivity)
                })
            }
        }
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun WiFiWizardApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = WiFiWizardScreen.getScreenByRoute(
        backStackEntry?.destination?.route ?: WiFiWizardScreen.WiFiList.route
    )
    val context = LocalContext.current

    fun openSettings() {
        try {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (_: RuntimeException) {
            Toast.makeText(context, R.string.cant_open_wifi_settings, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            WiFiWizardAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = WiFiWizardScreen.WiFiList.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = WiFiWizardScreen.WiFiList.route) {
                WiFiListScreen(
                    onWiFiClicked = {
                        if (it.isNotEmpty()) {
                            navController.navigate(WiFiWizardScreen.ConnectToWiFi.route.replace("{ssid}", it))
                        }
                    },
                    onEnterManuallyClicked = {
                        navController.navigate(WiFiWizardScreen.ConnectToWiFi.name)
                    },
                    onScanQrCodeClicked = {
                        navController.navigate(WiFiWizardScreen.ScanQrCode.name)
                    }
                )
            }
            composable(
                route = WiFiWizardScreen.ConnectToWiFi.name,
            ) {
                EnterPasswordScreen(
                    onSuccess = {
                        navController.navigate(WiFiWizardScreen.WiFiList.route)
                    },
                    onGoToSettings = { openSettings() }
                )
            }
            composable(
                route = WiFiWizardScreen.ConnectToWiFi.route,
                arguments = listOf(navArgument("ssid") {type = NavType.StringType})
            ) {
                EnterPasswordScreen(
                    chosenSsid = it.arguments?.getString("ssid")!!,
                    onSuccess = {
                        navController.navigate(WiFiWizardScreen.WiFiList.route)
                    },
                    onGoToSettings = { openSettings() }
                )
            }
            composable(
                route = WiFiWizardScreen.ScanQrCode.route,
            ) {
                QrScannerScreen(
                    onSuccess = {
                        navController.navigate(WiFiWizardScreen.WiFiList.route)
                    },
                    onGoToSettings = { openSettings() }
                )
            }
        }
    }
}