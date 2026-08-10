package com.d2m.app.push

import platform.UIKit.UIApplication
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Requests notification permission and registers for remote notifications.
 * The actual APNs device token only arrives via
 * UIApplicationDelegate.application(_:didRegisterForRemoteNotificationsWithDeviceToken:)
 * on the Swift/iosApp side (no Kotlin-reachable callback for this without
 * more AppDelegate wiring than fits this pass) -- see iosApp/README.md for
 * the hand-off point: that delegate method needs to call back into
 * PushTokenRegistrar.registerCurrentToken("apns", tokenHex) once implemented.
 */
actual class PlatformPushInitializer {
    actual fun initialize() {
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound,
        ) { granted, _ ->
            if (granted) {
                platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                    UIApplication.sharedApplication.registerForRemoteNotifications()
                }
            }
        }
    }
}
