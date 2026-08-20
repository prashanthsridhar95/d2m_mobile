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
 * onMessageReceived now actually posts a real notification (previously a
 * pure no-op -- see this class's git history/README.md's prior Phase 3
 * status note) using the same channels/builder as
 * LocalNotificationBridge.kt's foreground-process path, reported directly
 * as point 3 of the notification request ("Notifications will be received?
 * If not, handle that").
 *
 * BUT -- and this is the actual gap, stated plainly rather than papered
 * over -- this method only fires when the backend actually sends a real
 * FCM push, and it doesn't today: PushTokenRegistrar.kt's doc comment and
 * the backend's own notification_service.dispatch() confirm every push
 * attempt server-side currently just logs a push_result of "stubbed" or
 * "no_device_token" and returns, never calling FCM/APNs at all. So this
 * client-side fix makes real push work the moment that backend send is
 * implemented, with zero further mobile-side changes -- but until then,
 * killed-app / long-backgrounded delivery still won't happen. The payload
 * shape below (falls back through RemoteMessage.notification, then a
 * couple of likely `data` keys) is a best-effort guess at what a real send
 * might look like, since nothing server-side defines that contract yet --
 * revisit once it does.
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
        val title = message.notification?.title ?: message.data["title"] ?: "New message"
        val body = message.notification?.body ?: message.data["body"] ?: return
        val isCall = message.data["type"] == "call"
        val channel = if (isCall) NOTIFICATION_CHANNEL_CALLS else NOTIFICATION_CHANNEL_MESSAGES
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        postD2mNotification(applicationContext, notificationManager, channel, System.currentTimeMillis().toInt(), title, body)
    }
}
