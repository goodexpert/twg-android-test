import SwiftUI
import UIKit
import ComposeApp

/// Wraps the Compose Multiplatform hierarchy built by `MainViewController()` in
/// `composeApp/src/iosMain/kotlin/.../MainViewController.kt`.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // Ignore all safe areas so Compose owns inset handling through its own WindowInsets.
        // Ignoring only .keyboard let SwiftUI apply the top inset as well, which Compose's
        // Scaffold then applied again — the doubled top padding seen on iOS.
        ComposeView()
            .ignoresSafeArea()
    }
}
