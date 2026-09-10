package nz.co.warehouseandroidtest.kmp.feature.qrscanner

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import nz.co.warehouseandroidtest.kmp.ui.ProductDetailsRoute
import nz.co.warehouseandroidtest.kmp.ui.ProductListRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QrScannerViewModelTest {

    @Test
    fun startsReadyToScan() {
        assertFalse(QrScannerViewModel().state.value.isHandlingResult)
    }

    @Test
    fun opensProductDetailsWithTheScannedPayload() = runTest {
        val viewModel = QrScannerViewModel()

        viewModel.onIntent(QrScannerIntent.CodeScanned("9400000000001"))

        // A successful scan emits a debug toast with the raw payload before the navigation
        // effect. The toast carries the untrimmed value; the navigation carries the route.
        assertEquals(
            listOf(
                QrScannerEffect.ShowToast("9400000000001"),
                QrScannerEffect.OpenRoute(ProductDetailsRoute("9400000000001")),
            ),
            viewModel.effects.take(2).toList(),
        )
    }

    @Test
    fun trimsThePayloadBeforeHandingItOn() = runTest {
        // A decoder can hand back trailing whitespace or a newline; the id must not carry it
        // into the route.
        val viewModel = QrScannerViewModel()

        viewModel.onIntent(QrScannerIntent.CodeScanned("  9400000000001\n"))

        assertEquals(
            listOf(
                QrScannerEffect.ShowToast("  9400000000001\n"),
                QrScannerEffect.OpenRoute(ProductDetailsRoute("9400000000001")),
            ),
            viewModel.effects.take(2).toList(),
        )
    }

    @Test
    fun unwrapsAProductDetailsDeepLinkIntoTheRoute() = runTest {
        val viewModel = QrScannerViewModel()

        viewModel.onIntent(
            QrScannerIntent.CodeScanned(
                "nz.co.thewarehousegroup.app.kmp://productDetails?productId=R42"
            )
        )

        assertEquals(
            QrScannerEffect.OpenRoute(ProductDetailsRoute("R42")),
            viewModel.effects.take(2).toList().last(),
        )
    }

    @Test
    fun unwrapsAProductListDeepLinkIntoTheRoute() = runTest {
        // A scanner scan of a productList link now routes to the list screen — parsing lives
        // in `deeplink/DeepLinks.kt`, so the scanner does not need to know the host.
        val viewModel = QrScannerViewModel()

        viewModel.onIntent(
            QrScannerIntent.CodeScanned(
                "nz.co.thewarehousegroup.app.kmp://productList?query=drill"
            )
        )

        assertEquals(
            QrScannerEffect.OpenRoute(ProductListRoute("drill")),
            viewModel.effects.take(2).toList().last(),
        )
    }

    @Test
    fun surfacesAToastForOurSchemeThatFailsToParse() = runTest {
        // Our scheme but a host we don't recognise — treat as invalid rather than falling
        // through to a bogus bare-id route.
        val viewModel = QrScannerViewModel()

        viewModel.onIntent(
            QrScannerIntent.CodeScanned("nz.co.thewarehousegroup.app.kmp://unknown")
        )

        assertEquals(
            QrScannerEffect.ShowToast("Invalid QR payload"),
            viewModel.effects.take(1).toList().first(),
        )
        // The gate stays open so the next frame can retry.
        assertFalse(viewModel.state.value.isHandlingResult)
    }

    @Test
    fun ignoresABlankPayload() {
        val viewModel = QrScannerViewModel()

        viewModel.onIntent(QrScannerIntent.CodeScanned("   "))

        // Nothing was accepted, so the screen is still armed for the next frame.
        assertFalse(viewModel.state.value.isHandlingResult)
    }

    @Test
    fun ignoresFurtherScansWhileTheFirstIsBeingHandled() = runTest {
        // A camera reader repeats the same code on every frame it stays in view.
        val viewModel = QrScannerViewModel()

        viewModel.onIntent(QrScannerIntent.CodeScanned("first"))
        viewModel.onIntent(QrScannerIntent.CodeScanned("second"))

        assertTrue(viewModel.state.value.isHandlingResult)
        // Only the first scan's toast + navigation come through; the second scan is dropped
        // because the gate closed after the first one.
        assertEquals(
            listOf(
                QrScannerEffect.ShowToast("first"),
                QrScannerEffect.OpenRoute(ProductDetailsRoute("first")),
            ),
            viewModel.effects.take(2).toList(),
        )
    }

    @Test
    fun scansAgainAfterTheScreenComesBack() = runTest {
        val viewModel = QrScannerViewModel()
        viewModel.onIntent(QrScannerIntent.CodeScanned("first"))
        assertEquals(
            listOf(
                QrScannerEffect.ShowToast("first"),
                QrScannerEffect.OpenRoute(ProductDetailsRoute("first")),
            ),
            viewModel.effects.take(2).toList(),
        )

        viewModel.onIntent(QrScannerIntent.ScanningResumed)
        viewModel.onIntent(QrScannerIntent.CodeScanned("second"))

        assertEquals(
            listOf(
                QrScannerEffect.ShowToast("second"),
                QrScannerEffect.OpenRoute(ProductDetailsRoute("second")),
            ),
            viewModel.effects.take(2).toList(),
        )
    }
}
