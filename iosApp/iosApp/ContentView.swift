import SwiftUI
import ComposeApp

/// Thin UIViewControllerRepresentable wrapping the Kotlin-side
/// MainViewController() (composeApp/src/iosMain/.../MainViewController.kt),
/// which itself starts Koin and hosts App() -- the same root composable
/// androidMain's MainActivity renders. This file, iOSApp.swift,
/// ScreenshotSecureContainerView.swift, and Info.plist are the only 4
/// pieces of Swift/plist source this app needs; everything else
/// (navigation, screens, networking, state) lives in commonMain and is
/// shared with Android.
///
/// The Compose view controller's own .view is reparented inside
/// ScreenshotSecureContainerView rather than added directly -- see that
/// file's doc comment for what this buys (real screenshot/recording
/// exclusion on device, android/security/ScreenCapture.kt's own comment
/// on why Apple gives apps no supported way to get this) and its real
/// risk (unofficial, can break silently on a future iOS release).
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let composeVC = MainViewControllerKt.MainViewController()
        let wrapper = UIViewController()

        let secureContainer = ScreenshotSecureContainerView(frame: .zero)
        secureContainer.translatesAutoresizingMaskIntoConstraints = false
        wrapper.view.addSubview(secureContainer)
        NSLayoutConstraint.activate([
            secureContainer.leadingAnchor.constraint(equalTo: wrapper.view.leadingAnchor),
            secureContainer.trailingAnchor.constraint(equalTo: wrapper.view.trailingAnchor),
            secureContainer.topAnchor.constraint(equalTo: wrapper.view.topAnchor),
            secureContainer.bottomAnchor.constraint(equalTo: wrapper.view.bottomAnchor),
        ])

        // Standard UIViewController containment (addChild/didMove) --
        // without it, composeVC never receives viewWillAppear/
        // viewDidAppear and Compose Multiplatform's own lifecycle-driven
        // setup (e.g. resuming its render loop) doesn't fire correctly.
        wrapper.addChild(composeVC)
        secureContainer.hideContent(composeVC.view)
        composeVC.didMove(toParent: wrapper)

        return wrapper
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
