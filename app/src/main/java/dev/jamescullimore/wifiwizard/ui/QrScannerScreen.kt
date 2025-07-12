package dev.jamescullimore.wifiwizard.ui

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.common.Barcode
import dev.jamescullimore.wifiwizard.data.ConnectionState
import dev.jamescullimore.wifiwizard.data.WiFiWizardUiState
import dev.jamescullimore.wifiwizard.util.QrCodeAnalyzer
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun QrScannerScreen (
    onSuccess: () -> Unit,
    onGoToSettings: () -> Unit,
) {
    val viewModel: WiFiWizardViewModel = viewModel()
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var password by remember { mutableStateOf("") }
    var ssid by remember { mutableStateOf("") }

    val showConnectionFailedDialog = remember { mutableStateOf(false) }
    val showConnectionSuccessDialog = remember { mutableStateOf(false) }
    val showConnectionSuggestedDialog = remember { mutableStateOf(false) }
    val showSelectConnectionTypeDialog = remember { mutableStateOf(false) }

    val wifiManager = LocalContext.current.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val connectivityManager = ContextCompat.getSystemService(LocalContext.current, ConnectivityManager::class.java)
    val uiState: WiFiWizardUiState by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!permissionState.status.isGranted) {
                    permissionState.launchPermissionRequest()
                }
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
            !showSelectConnectionTypeDialog.value && it == ConnectionState.QR_SCAN -> showSelectConnectionTypeDialog.value = true
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
        showSelectConnectionTypeDialog.value -> SelectConnectionTypeDialog(
            onConnect = { viewModel.connectToWiFi(wifiManager, connectivityManager, ssid, password) },
            onSuggest = { viewModel.suggestWiFi(wifiManager, ssid, password) },
            onDismissRequest = { showSelectConnectionTypeDialog.value = false }
        )
    }

    QrScannerLayout(
        connecting = uiState.connectionState == ConnectionState.CONNECTING,
        onAnalyze = {
            ssid = it.ssid ?: ""
            password = it.password ?: ""
            viewModel.setConnectionState(ConnectionState.QR_SCAN)
        }
    )
}

@Composable
fun QrScannerLayout(
    connecting: Boolean,
    onAnalyze: (Barcode.WiFi) -> Unit
) {
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (loader, camera) = createRefs()

        if (connecting) {
            CircularProgressIndicator(
                modifier = Modifier.constrainAs(loader) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
            )
        } else {
            AndroidView(
                { context ->
                    val cameraExecutor = Executors.newSingleThreadExecutor()
                    val previewView = PreviewView(context).also {
                        it.scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                        val imageCapture = ImageCapture.Builder().build()

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, QrCodeAnalyzer(
                                    onAnalyze = { qrCode ->
                                        qrCode.wifi?.apply { onAnalyze(this) }
                                    }
                                ))
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                context as ComponentActivity,
                                cameraSelector,
                                preview,
                                imageCapture,
                                imageAnalyzer
                            )
                        } catch (exc: Exception) {
                            Log.d("DEBUG", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(context))
                    previewView
                },
                modifier = Modifier.constrainAs(camera) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
            )
        }
    }
}