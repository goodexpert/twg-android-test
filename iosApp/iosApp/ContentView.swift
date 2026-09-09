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
        ComposeView()
            .ignoresSafeArea(.keyboard)
    }
}
