package nz.co.warehouseandroidtest.kmp.feature.qrscanner

import nz.co.warehouseandroidtest.kmp.ui.BaseViewModel

/**
 * Turns decoded payloads into a single navigation to the details screen.
 *
 * No dependencies yet: the camera is not ported, so nothing is fetched here. The decoding side
 * will hand its results in through [QrScannerIntent.CodeScanned] exactly as the placeholder
 * button does, which is the point of routing them through an intent rather than calling
 * navigation from the reader.
 */
class QrScannerViewModel :
    BaseViewModel<QrScannerUiState, QrScannerIntent, QrScannerEffect>(QrScannerUiState()) {

    override fun handleIntent(intent: QrScannerIntent) {
        when (intent) {
            is QrScannerIntent.CodeScanned -> handleScan(intent.value)

            QrScannerIntent.ScanningResumed -> setState { copy(isHandlingResult = false) }
        }
    }

    private fun handleScan(value: String) {
        // A reader that keeps firing while the code is still in frame must not stack up
        // navigations to the same product.
        if (currentState.isHandlingResult) return

        val productId = value.trim()
        if (productId.isEmpty()) return

        setState { copy(isHandlingResult = true) }
        sendEffect(QrScannerEffect.OpenProductDetails(productId))
    }
}
