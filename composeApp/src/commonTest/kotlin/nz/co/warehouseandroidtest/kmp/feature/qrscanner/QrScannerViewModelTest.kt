package nz.co.warehouseandroidtest.kmp.feature.qrscanner

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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

        val effect = viewModel.effects.first()
        assertEquals(QrScannerEffect.OpenProductDetails("9400000000001"), effect)
    }

    @Test
    fun trimsThePayloadBeforeHandingItOn() = runTest {
        // A decoder can hand back trailing whitespace or a newline; the id must not carry it
        // into the route.
        val viewModel = QrScannerViewModel()

        viewModel.onIntent(QrScannerIntent.CodeScanned("  9400000000001\n"))

        assertEquals(
            QrScannerEffect.OpenProductDetails("9400000000001"),
            viewModel.effects.first(),
        )
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
        assertEquals(QrScannerEffect.OpenProductDetails("first"), viewModel.effects.first())
    }

    @Test
    fun scansAgainAfterTheScreenComesBack() = runTest {
        val viewModel = QrScannerViewModel()
        viewModel.onIntent(QrScannerIntent.CodeScanned("first"))
        assertEquals(QrScannerEffect.OpenProductDetails("first"), viewModel.effects.first())

        viewModel.onIntent(QrScannerIntent.ScanningResumed)
        viewModel.onIntent(QrScannerIntent.CodeScanned("second"))

        assertEquals(QrScannerEffect.OpenProductDetails("second"), viewModel.effects.first())
    }
}
