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
            // REVERTED: tried plain .ignoresSafeArea() on the theory that
            // App.kt's `contentWindowInsets = WindowInsets.systemBars`
            // double-applying insets on top of SwiftUI's own safe-area
            // constraint was clipping the render surface to ~60% of the
            // screen height. Confirmed directly that this made things
            // strictly worse -- the app went fully black (process still
            // alive per `launchctl list`, so not a crash; Compose's own
            // inset/size calculation broke instead). Back to the original
            // .ignoresSafeArea(.keyboard) known-working (if undersized)
            // state -- the "app only fills ~60% of the screen" sizing issue
            // needs a different root cause than this.
            .ignoresSafeArea(.keyboard)
    }
}
