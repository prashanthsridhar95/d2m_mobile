import SwiftUI
import UserNotifications
import ComposeApp

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.keyboard) // Compose manages its own keyboard insets
        }
    }
}

/// Handles every OS-level push/call callback Kotlin can't receive directly:
/// the APNs device token, PushKit setup, and message-notification actions
/// (Reply + Mark as read). See d2m_mobile's "full iOS call/push parity" pass
/// -- the same content Android gets via D2MFirebaseMessagingService.kt +
/// LocalNotificationBridge.kt + CallActionReceiver/MessageReplyReceiver/
/// MarkReadReceiver.kt, just split across Swift (what only Apple's own
/// frameworks can do -- PushKit/CallKit registration, UNNotificationCenter)
/// and Kotlin (everything else -- IosPushBridge.kt, IosCallKitBridge.kt,
/// both commonMain/iosMain and reachable from here because ComposeApp.framework
/// is linked into this target).
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // MUST run before anything else below -- PushKit can launch this
        // process in the background (a VoIP push arriving while the app is
        // fully killed) well before SwiftUI ever builds a view, and
        // IosCallKitBridge.shared.setup() below already reaches into Koin
        // singletons. See KoinBootstrap.kt's own doc comment for why this
        // mirrors Android's Application.onCreate() guarantee.
        KoinBootstrap.shared.ensureStarted()

        // Registers PKPushRegistry (VoIP calls) + CXProvider (CallKit) --
        // see IosCallKitBridge.kt's doc comment for why this single call
        // covers both; nothing else in this file needs to touch PushKit or
        // CallKit directly.
        IosCallKitBridge.shared.setup()

        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().setNotificationCategories(Self.notificationCategories)

        return true
    }

    /// Regular alert-push token (chat messages + every other VALID_TYPES
    /// event) -- NOT the same token PKPushRegistry issues for VoIP calls,
    /// see push_service.py's routing comment for why these are two
    /// separate DeviceToken rows ("apns" vs "apns_voip").
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let tokenHex = deviceToken.map { String(format: "%02x", $0) }.joined()
        Task {
            do {
                try await IosPushBridge.shared.registerPushToken(platform: "apns", tokenHex: tokenHex)
            } catch {
                print("registerPushToken(apns) failed: \(error)")
            }
        }
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("APNs registration failed: \(error)")
    }

    // MARK: - UNUserNotificationCenterDelegate

    /// Foregrounded behavior -- Android's equivalent is
    /// InAppNotificationLayer.kt (a Compose-drawn banner) for calls/chat
    /// while the app is open; here we just let the system banner show too
    /// (.banner + .sound), same "never silently swallow a notification just
    /// because the app happens to be open" principle.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    /// Reply / Mark as read actions on a chat_message notification --
    /// mirrors MessageReplyReceiver.kt / MarkReadReceiver.kt. `sender` here
    /// is the raw messaging username (see push_service.py's send_apns_alert
    /// payload -- `data["sender"]`, a flat top-level key alongside `aps`),
    /// exactly what IosPushBridge.replyToMessage/markMessageRead expect --
    /// same "peerUsername, not peerD2mId" reasoning as the Android
    /// receivers' own doc comments.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        guard let sender = userInfo["sender"] as? String else {
            completionHandler()
            return
        }

        switch response.actionIdentifier {
        case Self.replyActionId:
            guard let textResponse = response as? UNTextInputNotificationResponse,
                  !textResponse.userText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                completionHandler()
                return
            }
            Task {
                try? await IosPushBridge.shared.replyToMessage(peerUsername: sender, text: textResponse.userText)
                completionHandler()
            }
        case Self.markReadActionId:
            guard let messageId = userInfo["messageId"] as? String else {
                completionHandler()
                return
            }
            Task {
                try? await IosPushBridge.shared.markMessageRead(peerUsername: sender, messageId: messageId)
                completionHandler()
            }
        default:
            // A plain tap (no specific action) -- nothing extra to do here,
            // the app just opens normally; ChatPane/CallLayer already read
            // MessagingRepository's own state once the UI composes.
            completionHandler()
        }
    }

    // MARK: - Category/action registration

    private static let replyActionId = "D2M_REPLY_ACTION"
    private static let markReadActionId = "D2M_MARK_READ_ACTION"

    private static var notificationCategories: Set<UNNotificationCategory> {
        let reply = UNTextInputNotificationAction(
            identifier: replyActionId,
            title: "Reply",
            options: [],
            textInputButtonTitle: "Send",
            textInputPlaceholder: "Message"
        )
        let markRead = UNNotificationAction(
            identifier: markReadActionId,
            title: "Mark as read",
            options: []
        )
        // Identifier MUST match push_service.py's send_apns_alert
        // (`aps.category = "D2M_MESSAGE"`) exactly, or iOS silently shows
        // the notification with no actions at all instead of erroring.
        let messageCategory = UNNotificationCategory(
            identifier: "D2M_MESSAGE",
            actions: [reply, markRead],
            intentIdentifiers: [],
            options: []
        )
        return [messageCategory]
    }
}
