package com.d2m.app.push

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.d2m.app.data.network.ApiClient
import com.d2m.app.messaging.MessagingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

const val ACTION_QUICK_REPLY = "com.d2m.app.action.QUICK_REPLY"
const val EXTRA_PEER_USERNAME = "peer_username"
const val KEY_QUICK_REPLY_INPUT = "key_quick_reply_input"

/**
 * Inline quick-reply on a message notification (point 5's other half: "same
 * for messages"). Not exported -- only this app's own PendingIntent fires it.
 *
 * Takes `peerUsername` (the messaging username, e.g. from
 * MessagingRepository.InboxNotification), NOT the d2m participant id that
 * MessagingRepository.sendText's `peerD2mId` parameter is normally given.
 * That's deliberate, not a mismatch: sendText's very first step is
 * `d2mIdToMessagingUsername(peerD2mId)`, and that transform
 * (strip hyphens + lowercase) is idempotent on a string that's already in
 * username form -- passing peerUsername straight through resolves back to
 * itself. Reconstructing the original hyphenated d2m id here would need a
 * reverse lookup MessagingRepository doesn't expose (and doesn't need to,
 * for this).
 *
 * Calls messagingRepo.ensureConnected() before sendText -- reported
 * directly: a reply typed straight from a message notification "stops at
 * the mobile itself... shows as failed" until the app is opened by hand.
 * Root cause: this receiver can fire either in a freshly-spawned process
 * (the app was fully killed -- nothing has ever called
 * MessagingRepository.start() in this process) or with a socket that's
 * gone silently stale in the background (Android's Doze/background network
 * limits, no server-side action needed to look "connected" from this
 * device's own stale view -- see MessagingWsClient.kt); either way,
 * sendText's wsClient.send() would fail immediately, and did. See
 * ensureConnected's own doc comment for the full reasoning; this just gives
 * it a bounded window (its default 6s) to actually establish a live socket
 * before attempting the send, rather than trying against a connection that
 * was never coming.
 */
class MessageReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_QUICK_REPLY) return
        val peerUsername = intent.getStringExtra(EXTRA_PEER_USERNAME) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_QUICK_REPLY_INPUT)
            ?.toString()
            ?.trim()
        if (replyText.isNullOrEmpty()) return

        val koin = GlobalContext.get()
        val messagingRepo = koin.get<MessagingRepository>()
        val apiClient = koin.get<ApiClient>()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { messagingRepo.ensureConnected(apiClient.client) }
            runCatching { messagingRepo.sendText(peerUsername, replyText) }
            if (notificationId != -1) {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
            }
            pending.finish()
        }
    }
}
