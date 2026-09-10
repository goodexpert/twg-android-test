package nz.co.warehouseandroidtest.kmp.feature.qrscanner

import nz.co.warehouseandroidtest.kmp.ui.UiEffect
import nz.co.warehouseandroidtest.kmp.ui.UiIntent
import nz.co.warehouseandroidtest.kmp.ui.UiState

/**
 * [isHandlingResult] is what keeps a decoder from opening the details screen twice. A camera
 * reader delivers the same code on every frame it stays in view, so the first accepted result
 * closes the gate until the screen re-arms it.
 *
 * [isFlashlightOn] mirrors the torch state the screen hands to the underlying camera preview.
 * Kept here rather than in the reader so a configuration change does not silently drop it.
 */
data class QrScannerUiState(
    val isHandlingResult: Boolean = false,
    val isFlashlightOn: Boolean = false,
) : UiState

sealed interface QrScannerIntent : UiIntent {
    /** [value] is the raw decoded payload, which this app expects to be a product id. */
    data class CodeScanned(val value: String) : QrScannerIntent

    /** Sent when the screen comes back to the foreground, so scanning can start again. */
    data object ScanningResumed : QrScannerIntent

    /**
     * Sent by the reader when it can't decode a frame or when the camera surface itself
     * errors out. [message] is the reader's own description and is surfaced via
     * [QrScannerEffect.ShowToast] without further translation.
     */
    data class ScanFailed(val message: String) : QrScannerIntent
}

sealed interface QrScannerEffect : UiEffect {
    /**
     * Emitted only for a payload the view model accepted, so the screen never navigates on a
     * blank scan or on a repeat of the code it is already acting on. [route] is one of the
     * typed destinations from `ui/Routes.kt` and is handed straight to `NavController.navigate`
     * — parsing which route to open is the view model's job, not the screen's.
     */
    data class OpenRoute(val route: Any) : QrScannerEffect

    /**
     * Requests a transient snackbar be shown. Used both for reader errors coming through
     * [QrScannerIntent.ScanFailed] and for the view model's own validation feedback, e.g. a
     * deep-link payload that carries no `productId`. The screen collects this into its
     * `SnackbarHostState`.
     */
    data class ShowToast(val message: String) : QrScannerEffect
}
