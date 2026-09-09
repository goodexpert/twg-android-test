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
