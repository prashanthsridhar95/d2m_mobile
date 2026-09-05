package com.d2m.app.push

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.d2m.app.data.network.ApiClient
import com.d2m.app.messaging.MessagingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

const val ACTION_MARK_READ = "com.d2m.app.action.MARK_READ"
const val EXTRA_MESSAGE_ID = "message_id"

/**
 * "Mark as read" quick action on a message notification (requested
 * alongside real content and Reply -- see postMessageNotification's second
 * action in LocalNotificationBridge.kt). Same cold-process concern, same
 * fix, as MessageReplyReceiver.kt's reply action: this can fire in a
 * freshly-spawned process where MessagingRepository.start() never ran
 * (D2MFirebaseMessagingService woke it just to handle a push), so
 * ensureConnected() runs first -- markMessageRead's ReceiptRead send would
 * otherwise silently no-op against a dead/nonexistent socket.
 *
 * Not exported (AndroidManifest.xml) -- only this app's own PendingIntent
 * fires it, same as CallActionReceiver/MessageReplyReceiver.
 */
class MarkReadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_READ) return
        val peerUsername = intent.getStringExtra(EXTRA_PEER_USERNAME) ?: return
        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val koin = GlobalContext.get()
        val messagingRepo = koin.get<MessagingRepository>()
        val apiClient = koin.get<ApiClient>()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { messagingRepo.ensureConnected(apiClient.client) }
            runCatching { messagingRepo.markMessageRead(peerUsername, messageId) }
            if (notificationId != -1) {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
            }
            pending.finish()
        }
    }
}
