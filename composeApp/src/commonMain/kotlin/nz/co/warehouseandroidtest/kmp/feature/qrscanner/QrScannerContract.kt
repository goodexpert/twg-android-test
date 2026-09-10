package nz.co.warehouseandroidtest.kmp.feature.qrscanner

import nz.co.warehouseandroidtest.kmp.ui.UiEffect
import nz.co.warehouseandroidtest.kmp.ui.UiIntent
import nz.co.warehouseandroidtest.kmp.ui.UiState

/**
 * [isHandlingResult] is what keeps a decoder from opening the details screen twice. A camera
 * reader delivers the same code on every frame it stays in view, so the first accepted result
 * closes the gate until the screen re-arms it.
 */
data class QrScannerUiState(
    val isHandlingResult: Boolean = false,
) : UiState

sealed interface QrScannerIntent : UiIntent {
    /** [value] is the raw decoded payload, which this app expects to be a product id. */
    data class CodeScanned(val value: String) : QrScannerIntent

    /** Sent when the screen comes back to the foreground, so scanning can start again. */
    data object ScanningResumed : QrScannerIntent
}

sealed interface QrScannerEffect : UiEffect {
    /**
     * Emitted only for a payload the view model accepted, so the screen never navigates on a
     * blank scan or on a repeat of the code it is already acting on.
     */
    data class OpenProductDetails(val productId: String) : QrScannerEffect
}
