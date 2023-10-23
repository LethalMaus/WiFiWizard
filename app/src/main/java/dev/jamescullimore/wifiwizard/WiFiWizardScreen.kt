package dev.jamescullimore.wifiwizard

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.jamescullimore.wifiwizard.ui.EnterPasswordScreen
import dev.jamescullimore.wifiwizard.ui.WiFiListScreen

enum class WiFiWizardScreen(@StringRes val title: Int, val route: String) {
    WiFiList(title = R.string.app_name, route = "WiFiList"),
    ConnectToWiFi(title = R.string.connect_to_wifi, route = "ConnectToWiFi/{ssid}");

    companion object {
        fun getScreenByRoute(route: String): WiFiWizardScreen {
            return when (route) {
                WiFiList.route -> WiFiList
                ConnectToWiFi.name,
                ConnectToWiFi.route -> ConnectToWiFi
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
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(currentScreen.title)) },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        }
    )
}

@Composable
fun WiFiWizardApp(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = WiFiWizardScreen.getScreenByRoute(
        backStackEntry?.destination?.route ?: WiFiWizardScreen.WiFiList.route
    )
    val context = LocalContext.current

    fun openSettings() {
        try {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (ex: RuntimeException) {
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
        }
    }
}