package nz.co.warehouseandroidtest.kmp

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Android-only. The multiplatform preview annotation is deprecated in Compose Multiplatform
 * 1.10, and its androidx replacement does not resolve for the iOS targets, so previews live here.
 */
@Preview(showBackground = true)
@Composable
private fun AppPreview() {
    // Safe to construct: everything in the container is lazy, and a static preview does not run
    // the LaunchedEffect that would reach the network.
    App(AppContainer())
}
