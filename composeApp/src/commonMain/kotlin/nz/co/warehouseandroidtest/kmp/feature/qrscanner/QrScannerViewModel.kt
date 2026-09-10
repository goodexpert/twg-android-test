package nz.co.warehouseandroidtest.kmp.feature.qrscanner

import nz.co.warehouseandroidtest.kmp.deeplink.DEEP_LINK_SCHEME
import nz.co.warehouseandroidtest.kmp.deeplink.parseDeepLink
import nz.co.warehouseandroidtest.kmp.ui.BaseViewModel
import nz.co.warehouseandroidtest.kmp.ui.ProductDetailsRoute

/**
 * Turns decoded payloads into a single typed navigation.
 *
 * Two payload shapes are accepted, in this order:
 *   1. A URI on our own scheme (`$DEEP_LINK_SCHEME://…`) — handed to `parseDeepLink`, which
 *      is the same gate the platform intent bridges use. This is what a QR code produced by
 *      another install of the app looks like, and it lets a shared link round-trip through
 *      the scanner without the scanner knowing which hosts are supported.
 *   2. Anything else — treated as a bare product id, matching the legacy barcode reader.
 *
 * Reader errors arrive through [QrScannerIntent.ScanFailed] and are surfaced as a snackbar
 * via [QrScannerEffect.ShowToast] rather than crashing the screen. A payload that looks like
 * one of our URIs but fails to parse (unknown host, missing required parameter) produces
 * the same toast.
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

        val route = if (rawValue.startsWith("$DEEP_LINK_SCHEME://")) {
            parseDeepLink(rawValue)
        } else {
            ProductDetailsRoute(rawValue)
        }

        if (route == null) {
            sendEffect(QrScannerEffect.ShowToast("Invalid QR payload"))
            return
        }

        setState { copy(isHandlingResult = true) }
        sendEffect(QrScannerEffect.ShowToast(value))
        sendEffect(QrScannerEffect.OpenRoute(route))
    }
}
