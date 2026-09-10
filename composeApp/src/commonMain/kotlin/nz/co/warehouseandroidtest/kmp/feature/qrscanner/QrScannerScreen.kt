package nz.co.warehouseandroidtest.kmp.feature.qrscanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/** Stands in for a decoded scan until the camera reader is ported. */
private const val SAMPLE_PRODUCT_ID = "R2436546"

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
    onOpenProductDetails: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: QrScannerViewModel = viewModel { QrScannerViewModel() }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is QrScannerEffect.OpenProductDetails -> onOpenProductDetails(effect.productId)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "The camera preview lands here once the barcode reader is ported.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = {
                    viewModel.onIntent(QrScannerIntent.CodeScanned(SAMPLE_PRODUCT_ID))
                },
                enabled = !state.isHandlingResult,
            ) {
                Text("Simulate scan")
            }
            Text(
                text = "Opens the details screen with product id $SAMPLE_PRODUCT_ID.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
