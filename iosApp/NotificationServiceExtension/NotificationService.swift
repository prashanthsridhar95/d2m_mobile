import UserNotifications
import ComposeApp

/// New Xcode target, not wired up automatically -- see iosApp/README.md's
/// "Add the Notification Service Extension target" step. This is what
/// turns a generic "New message" alert push into the real decrypted
/// sender name + preview, mirroring
/// D2MFirebaseMessagingService.handleChatMessagePush on Android -- except
/// here it runs in its OWN OS process (Apple's Notification Service
/// Extension model), not the main app, which is why:
///   1. It has to start its own Koin instance (KoinBootstrap.shared) --
///      the main app's Koin instance lives in a different process entirely.
///   2. It can only see the same MessagingRepository/Signal session state
///      as the main app because both targets share an App Group container
///      -- see SqlDriverFactory.ios.kt/Settings.ios.kt's own doc comments.
///      Without that App Group set up in Xcode, decryptChatMessagePush
///      below will simply fail every time (no session to decrypt against)
///      and this always falls back to the generic push_service.py title/
///      body copy -- never a crash, never a blank notification.
///
/// Apple gives this process a strict ~30s budget (serviceExtensionTimeWillExpire
/// fires as a warning before the OS kills it outright) -- IosPushBridge.
/// decryptChatMessagePush's own 8s internal timeout leaves comfortable
/// headroom under that.
class NotificationService: UNNotificationServiceExtension {
    var contentHandler: ((UNNotificationContent) -> Void)?
    var bestAttemptContent: UNMutableNotificationContent?

    override func didReceive(
        _ request: UNNotificationRequest,
        withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void
    ) {
        self.contentHandler = contentHandler
        let attempt = (request.content.mutableCopy() as? UNMutableNotificationContent) ?? UNMutableNotificationContent()
        self.bestAttemptContent = attempt

        KoinBootstrap.shared.ensureStarted()

        let userInfo = request.content.userInfo
        guard let sender = userInfo["sender"] as? String else {
            contentHandler(attempt)
            return
        }
        let messageId = userInfo["messageId"] as? String
        let ciphertextType = userInfo["ciphertextType"] as? String
        let ciphertext = userInfo["ciphertext"] as? String
        let sentAt = (userInfo["sentAt"] as? String).flatMap { Int64($0) }

        Task {
            do {
                let result = try await IosPushBridge.shared.decryptChatMessagePush(
                    sender: sender,
                    messageId: messageId,
                    ciphertextType: ciphertextType,
                    ciphertext: ciphertext,
                    sentAt: sentAt.map { KotlinLong(value: $0) }
                )
                if let result = result {
                    attempt.title = result.senderDisplayName
                    if let preview = result.preview {
                        attempt.body = preview
                    }
                }
            } catch {
                // Decrypt failed for any reason -- attempt already holds
                // push_service.py's generic aps.alert title/body copy
                // unchanged, exactly the same "never a broken/blank
                // notification" fallback D2MFirebaseMessagingService.kt
                // follows on Android.
            }
            contentHandler(attempt)
        }
    }

    /// Apple's hard deadline is close -- deliver whatever we have (the
    /// original generic copy, if decrypt hadn't finished yet) rather than
    /// let the OS silently drop the notification entirely.
    override func serviceExtensionTimeWillExpire() {
        if let handler = contentHandler, let attempt = bestAttemptContent {
            handler(attempt)
        }
    }
}
