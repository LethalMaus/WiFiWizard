package dev.jamescullimore.wifiwizard.ui

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.common.Barcode
import dev.jamescullimore.wifiwizard.data.ConnectionState
import dev.jamescullimore.wifiwizard.data.SecurityType
import dev.jamescullimore.wifiwizard.data.WiFiWizardUiState
import dev.jamescullimore.wifiwizard.util.QrCodeAnalyzer
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen (
    onSuccess: () -> Unit,
    onGoToSettings: () -> Unit,
) {
    val viewModel: WiFiWizardViewModel = viewModel()
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var password by remember { mutableStateOf("") }
    var ssid by remember { mutableStateOf("") }
    var security by remember { mutableStateOf(SecurityType.UNKNOWN) }

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
            onConnect = { viewModel.connectToWiFi(wifiManager, connectivityManager, ssid, password, security) },
            onSuggest = { viewModel.suggestWiFi(wifiManager, ssid, password, security) },
            onDismissRequest = { showSelectConnectionTypeDialog.value = false }
        )
    }

    QrScannerLayout(
        connecting = uiState.connectionState == ConnectionState.CONNECTING,
        onAnalyze = {
            ssid = it.ssid ?: ""
            password = it.password ?: ""
            security = when (it.encryptionType) {
                Barcode.WiFi.TYPE_OPEN -> SecurityType.OPEN
                Barcode.WiFi.TYPE_WEP -> SecurityType.WEP
                Barcode.WiFi.TYPE_WPA -> SecurityType.WPA
                else -> SecurityType.UNKNOWN
            }
            viewModel.setConnectionState(ConnectionState.QR_SCAN)
        }
    )
}

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
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
            val viewModel = remember { CameraPreviewViewModel() }
            CameraPreviewContent(
                viewModel,
                onAnalyze,
                Modifier.constrainAs(camera) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
            )
        }
    }
}

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraPreviewContent(
    viewModel: CameraPreviewViewModel,
    onAnalyze: (Barcode.WiFi) -> Unit,
    modifier: Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(lifecycleOwner, onAnalyze) {
        viewModel.bindToCamera(context.applicationContext, lifecycleOwner, onAnalyze)
    }

    var autofocusRequest by remember { mutableStateOf(UUID.randomUUID() to Offset.Unspecified) }
    val currentOnTapToFocus by rememberUpdatedState(viewModel::tapToFocus)
    val autofocusRequestId = autofocusRequest.first
    val showAutofocusIndicator = autofocusRequest.second.isSpecified

    if (showAutofocusIndicator) {
        LaunchedEffect(autofocusRequestId) {
            delay(1000)
            autofocusRequest = autofocusRequestId to Offset.Unspecified
        }
    }

    surfaceRequest?.let { request ->
        val coordinateTransformer = remember { MutableCoordinateTransformer() }
        CameraXViewfinder(
            surfaceRequest = request,
            coordinateTransformer = coordinateTransformer,
            modifier = modifier.pointerInput(coordinateTransformer) {
                detectTapGestures { tapCoords ->
                    with(coordinateTransformer) {
                        currentOnTapToFocus(tapCoords.transform())
                    }
                    autofocusRequest = UUID.randomUUID() to tapCoords
                }
            }
        )
    }
}

@ExperimentalCamera2Interop
class CameraPreviewViewModel : ViewModel() {
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest
    private var surfaceMeteringPointFactory: SurfaceOrientedMeteringPointFactory? = null
    private var cameraControl: CameraControl? = null

    private val cameraPreviewUseCase = CameraPreview.Builder()
        .build().apply {
            setSurfaceProvider { newSurfaceRequest ->
                _surfaceRequest.update { newSurfaceRequest }
                surfaceMeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                    newSurfaceRequest.resolution.width.toFloat(),
                    newSurfaceRequest.resolution.height.toFloat()
                )
            }
        }

    suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner, onAnalyze: (Barcode.WiFi) -> Unit) {
        val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)

        val cameraExecutor = Executors.newSingleThreadExecutor()
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().apply {
                setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrCode ->
                    qrCode.wifi?.let { wifi -> onAnalyze(wifi) }
                })
            }

        val camera = processCameraProvider.bindToLifecycle(
            lifecycleOwner,
            DEFAULT_BACK_CAMERA,
            cameraPreviewUseCase,
            imageAnalysis
        )
        cameraControl = camera.cameraControl

        try {
            awaitCancellation()
        } finally {
            processCameraProvider.unbindAll()
            cameraControl = null
            cameraExecutor.shutdown()
        }
    }

    fun tapToFocus(tapCoords: Offset) {
        val point = surfaceMeteringPointFactory?.createPoint(tapCoords.x, tapCoords.y)
        if (point != null) {
            val meteringAction = FocusMeteringAction.Builder(point).build()
            cameraControl?.startFocusAndMetering(meteringAction)
        }
    }
}