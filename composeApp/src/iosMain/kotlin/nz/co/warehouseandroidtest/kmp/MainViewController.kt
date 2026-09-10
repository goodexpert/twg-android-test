package nz.co.warehouseandroidtest.kmp

import androidx.compose.ui.window.ComposeUIViewController

/**
 * App-scoped, the iOS counterpart of `WarehouseApplication.container`. iOS needs no
 * initialisation call of its own — the Keychain-backed store requires no Context, and
 * everything in the container is lazy.
 */
private val appContainer = AppContainer()

/** Bridged into SwiftUI by `iosApp/iosApp/ContentView.swift`. */
@Suppress("FunctionName", "unused")
fun MainViewController() = ComposeUIViewController { App(appContainer) }

/**
 * Called from SwiftUI's `.onOpenURL` in `ContentView.swift`. Pushes the URL into the same
 * [nz.co.warehouseandroidtest.kmp.deeplink.DeepLinkHandler] that the App composable observes,
 * so warm and cold deep links share the exact same navigation path.
 */
@Suppress("unused")
fun handleIosDeepLink(url: String) {
    appContainer.deepLinkHandler.push(url)
}
