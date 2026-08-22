package com.d2m.app.push

import android.app.NotificationManager
import android.content.Context
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Registered in AndroidManifest.xml. onNewToken fires whenever FCM (re)issues
 * a token -- forwards it to PushTokenRegistrar the same way the initial
 * token fetch in PlatformPushInitializer.android.kt does.
 *
 * onMessageReceived posts a real notification -- point 3 of the original
 * notification request ("Notifications will be received? If not, handle
 * that") and, now that both halves are actually wired, the direct answer to
 * "even when I'm not on the app, I should be receiving notifications
 * regarding calls & messages."
 *
 * Both halves needed to be real for that to be true, and previously neither
 * was:
 *  - Server-side: d2m_core_engine's push_service.py now does a real FCM send
 *    (firebase_admin) whenever D2M_FCM_SERVICE_ACCOUNT_JSON is configured on
 *    the deployed backend -- confirmed via direct inspection, this is NOT a
 *    stub. It sends the message DATA-ONLY (no `notification=` block) and at
 *    android priority "high" specifically so it always reaches
 *    onMessageReceived below -- a message carrying a `notification` block
 *    gets auto-displayed by the OS itself as a generic tray notification
 *    whenever the app is backgrounded/killed, WITHOUT ever calling
 *    onMessageReceived, which would have silently skipped all the channel/
 *    ringtone/action logic below for exactly the "not on the app" case this
 *    exists for.
 *  - Client-side (this file + the Gradle wiring in composeApp/build.gradle.kts):
 *    needs the google-services plugin applied AND a real google-services.json
 *    from an actual Firebase project dropped into composeApp/ -- without
 *    either, FirebaseMessaging.getInstance() in PlatformPushInitializer.android.kt
 *    never mints a real token, so no DeviceToken row is ever registered
 *    server-side and every push attempt resolves to "no_device_token"
 *    regardless of whether the backend send above is configured. See
 *    README.md's push section for the exact setup steps -- this is the one
 *    piece that genuinely needs a person with access to a Firebase console,
 *    not more code.
 *
 * Payload shape, confirmed against the actual server code (messaging-framework's
 * ws.ts triggerPush call sites + d2m_core_engine's push_service.py TITLES/
 * BODIES): `type` is "chat_message" or "incoming_call", `title`/`body` are
 * the pre-baked generic copy ("New message" / "Someone is calling you" --
 * deliberately generic, not a real preview or caller name: the messaging
 * relay server is E2E and never decrypts anything, so it only ever knows
 * `from` (a raw username, see `data["from"]`), not a display name or a
 * callId. That means a push-triggered incoming-call notification can invite
 * the user to open the app, but can't wire real Answer/Decline actions the
 * way the live in-app CallManager-driven notification (LocalNotificationBridge.kt's
 * postIncomingCallNotification, used while the process is alive with an open
 * socket) can -- there's no callId here to act on. Tapping either
 * notification just opens MainActivity, where the live WebSocket connects
 * and, if the call/message is still live, the real in-app UI takes over.
 */
class D2MFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                org.koin.core.context.GlobalContext.get().get<PushTokenRegistrar>().registerCurrentToken("fcm", token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"]
        // Diagnostic breadcrumb for "notifications not received when app is
        // not live" reports -- this line running at all (check via `adb
        // logcat` while the app is killed) confirms the message reached
        // this device and this callback fired; if a future report says
        // nothing arrives, but this line never appears in logcat either,
        // that rules out everything downstream (channel/permission/
        // battery-optimization) and points back at FCM delivery itself
        // (token validity, server-side send, or OS-level throttling before
        // this process ever wakes) rather than anything in this method.
        println("D2MFirebaseMessagingService.onMessageReceived: type=$type from=${message.data["from"]} priority=${message.priority} originalPriority=${message.originalPriority}")
        val title = message.data["title"] ?: message.notification?.title ?: "New notification"
        val body = message.data["body"] ?: message.notification?.body ?: "You have a new notification."
        val channel = if (type == "incoming_call") NOTIFICATION_CHANNEL_CALLS else NOTIFICATION_CHANNEL_MESSAGES
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        postD2mNotification(applicationContext, notificationManager, channel, System.currentTimeMillis().toInt(), title, body)
    }
}
