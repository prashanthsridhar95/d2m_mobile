import SwiftUI

@main
struct iOSApp: App {
    // Handles the two push-registration callbacks Kotlin can't receive
    // directly (see PlatformPushInitializer.ios.kt's doc comment): the APNs
    // device token, and incoming remote notifications while foregrounded.
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.keyboard) // Compose manages its own keyboard insets
        }
    }
}

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let tokenHex = deviceToken.map { String(format: "%02x", $0) }.joined()
        // Hand-off point noted in PlatformPushInitializer.ios.kt: wire this
        // into PushTokenRegistrar.registerCurrentToken("apns", tokenHex) via
        // a small Kotlin bridge function once push is exercised end-to-end
        // (real push send is explicitly deferred backend-side, per the
        // mobile plan's Phase 3 scope note -- this callback is here so the
        // token capture itself isn't a missing piece when that lands).
        print("APNs device token registered: \(tokenHex)")
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("APNs registration failed: \(error)")
    }
}
