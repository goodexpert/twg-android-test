package nz.co.warehouseandroidtest.kmp.feature.qrscanner

import nz.co.warehouseandroidtest.kmp.ui.BaseViewModel

/**
 * Turns decoded payloads into a single navigation to the details screen.
 *
 * Two payload shapes are accepted, in this order:
 *   1. A `productDetails` deep link (`$DEEP_LINK_PREFIX?productId=<id>`) — the id is pulled
 *      out and used. This is what a QR code produced by another install of the app looks
 *      like, and it lets a shared code round-trip through the scanner.
 *   2. Anything else — used as the product id directly, matching the legacy barcode reader.
 *
 * Reader errors arrive through [QrScannerIntent.ScanFailed] and are surfaced as a snackbar
 * via [QrScannerEffect.ShowToast] rather than crashing the screen. A payload that parses as
 * a deep link but carries no `productId` is treated as invalid and produces the same toast.
 */
class QrScannerViewModel :
    BaseViewModel<QrScannerUiState, QrScannerIntent, QrScannerEffect>(QrScannerUiState()) {

    override fun handleIntent(intent: QrScannerIntent) {
        when (intent) {
            is QrScannerIntent.CodeScanned -> handleScan(intent.value)

            QrScannerIntent.ScanningResumed -> setState { copy(isHandlingResult = false) }

            is QrScannerIntent.ScanFailed -> {
                sendEffect(QrScannerEffect.ShowToast(intent.message))
            }
        }
    }

    private fun handleScan(value: String) {
        // A reader that keeps firing while the code is still in frame must not stack up
        // navigations to the same product.
        if (currentState.isHandlingResult) return

        val rawValue = value.trim()
        if (rawValue.isEmpty()) return

        // A payload can arrive as either the bare product id (legacy barcodes) or as one of
        // our own share URLs. Only the latter needs unwrapping; anything else is passed
        // through, so a valid barcode still routes even if the scheme changes later.
        val productId = if (rawValue.startsWith(DEEP_LINK_PREFIX)) {
            rawValue.substringAfter(PARAM_PRODUCT_ID, "").trim()
        } else {
            rawValue
        }

        if (productId.isEmpty()) {
            sendEffect(QrScannerEffect.ShowToast("Invalid QR payload"))
            return
        }

        setState { copy(isHandlingResult = true) }
        sendEffect(QrScannerEffect.ShowToast(value))
        sendEffect(QrScannerEffect.OpenProductDetails(productId))
    }

    private companion object {
        const val DEEP_LINK_PREFIX = "nz.co.thewarehousegroup.app.kmp://productDetails"
        const val PARAM_PRODUCT_ID = "productId="
    }
}
