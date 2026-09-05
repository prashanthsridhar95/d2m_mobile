package com.d2m.app.push

import platform.UIKit.UIApplication
// registerForRemoteNotifications is a package-level cinterop extension
// function on UIApplication (confirmed via klib dump-metadata: declared as
// `fun platform/UIKit/UIApplication.registerForRemoteNotifications()`, not a
// true class member) -- importing the UIApplication class alone does not
// bring it into scope, same issue independently found in WebRtcEngine.ios.kt
// for timeIntervalSince1970/senderWithKind. Neither the parenthesized nor
// property form of `sharedApplication` mattered; this missing import was
// the actual cause both times.
import platform.UIKit.registerForRemoteNotifications
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
                    // klib dump-metadata on the real UIKit platform klib shows
                    // BOTH a companion `fun sharedApplication()` AND a real,
                    // separately-declared `val sharedApplication` property
                    // (matching UIKit.h's actual `@property(class, readonly)
                    // UIApplication *sharedApplication`). The earlier
                    // parenthesized call-form (`.sharedApplication()`) still
                    // left `registerForRemoteNotifications` unresolved on
                    // rebuild, so that guess was wrong -- using the property
                    // form instead, which is also the idiomatic Kotlin/Native
                    // form for an ObjC class property.
                    UIApplication.sharedApplication.registerForRemoteNotifications()
                }
            }
        }
    }
}
