import SwiftUI
import ComposeApp

/// Thin UIViewControllerRepresentable wrapping the Kotlin-side
/// MainViewController() (composeApp/src/iosMain/.../MainViewController.kt),
/// which itself starts Koin and hosts App() -- the same root composable
/// androidMain's MainActivity renders. This file, iOSApp.swift, and
/// Info.plist are the only 3 pieces of Swift/plist source this app needs;
/// everything else (navigation, screens, networking, state) lives in
/// commonMain and is shared with Android.
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
