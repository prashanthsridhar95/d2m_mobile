package com.d2m.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Registered in AndroidManifest.xml. onNewToken fires whenever FCM (re)issues
 * a token -- forwards it to PushTokenRegistrar the same way the initial
 * token fetch in PlatformPushInitializer.android.kt does. onMessageReceived
 * shows nothing yet (no notification-building/channel setup in this pass --
 * see README.md's Phase 3 status note); incoming pushes are dropped rather
 * than crashing, which is the correct default until the backend's real
 * send is wired up too (nothing sends a real push today, see plan §7).
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
        // TODO(plan Phase 3 follow-up): build a real notification once the
        // backend's push send is implemented and this starts receiving
        // real payloads to act on.
    }
}
