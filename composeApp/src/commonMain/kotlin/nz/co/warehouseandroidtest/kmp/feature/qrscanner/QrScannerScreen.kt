package nz.co.warehouseandroidtest.kmp.feature.qrscanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import nz.co.warehouseandroidtest.kmp.core.permission.PermissionStatus
import nz.co.warehouseandroidtest.kmp.core.permission.PermissionType
import nz.co.warehouseandroidtest.kmp.core.permission.rememberPermissionManager
import qrscanner.CameraLens
import qrscanner.QrScanner

/**
 * Camera preview and the scan result handling. Ports `nz.co.warehouseandroidtest
 * .BarScanActivity`, minus the decoding: zxing-library is Android-only and survives on a
 * JCenter mirror alone, so the reader itself is a separate piece of work.
 *
 * Until then the preview area is a placeholder with a button that feeds a fixed payload
 * through the same [QrScannerIntent.CodeScanned] path a decoder will use, so the route to the
 * details screen is real and testable now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onNavigate: (Any) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: QrScannerViewModel = viewModel { QrScannerViewModel() }
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var permissionStatus by remember { mutableStateOf(PermissionStatus.NOT_DETERMINED) }
    val permissionManager = rememberPermissionManager(PermissionType.CAMERA) { status ->
        permissionStatus = status
    }

    LaunchedEffect(Unit) {
        permissionStatus = permissionManager.getStatus()
        if (permissionStatus == PermissionStatus.NOT_DETERMINED) {
            permissionManager.requestPermission()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is QrScannerEffect.OpenRoute -> onNavigate(effect.route)
                is QrScannerEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    // NavHost disposes this destination on the way to the details screen and composes it again
    // on the way back, while the view model survives on the back stack entry. Re-arming here is
    // what lets a second scan through after the user returns.
    LaunchedEffect(Unit) {
        viewModel.onIntent(QrScannerIntent.ScanningResumed)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Scan barcode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (permissionStatus == PermissionStatus.GRANTED || permissionStatus == PermissionStatus.NOT_DETERMINED) {
                QrScanner(
                    modifier = Modifier.fillMaxSize(),
                    flashlightOn = state.isFlashlightOn,
                    cameraLens = CameraLens.Back,
                    openImagePicker = false,
                    onCompletion = { barcodeRawText ->
                        viewModel.onIntent(QrScannerIntent.CodeScanned(barcodeRawText))
                    },
                    onFailure = { errorString ->
                        viewModel.onIntent(QrScannerIntent.ScanFailed(errorString))
                    },
                    imagePickerHandler = { false },
                )
            } else {
                CameraPermissionDeniedView(
                    onRequestPermission = { permissionManager.requestPermission() },
                    onOpenSettings = { permissionManager.openSystemSettings() },
                )
            }
        }
    }
}

@Composable
fun CameraPermissionDeniedView(
    onRequestPermission: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Camera permission required",
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Camera access is required to scan QR codes.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
        ) {
            Text("Try again")
        }

        if (onOpenSettings != null) {
            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onOpenSettings,
            ) {
                Text("Open settings")
            }
        }
    }
}
