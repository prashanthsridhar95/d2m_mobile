package com.d2m.app.push

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.d2m.app.MainActivity
import com.d2m.app.messaging.call.CallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

const val ACTION_ACCEPT_CALL = "com.d2m.app.action.ACCEPT_CALL"
const val ACTION_DECLINE_CALL = "com.d2m.app.action.DECLINE_CALL"
const val EXTRA_CALL_ID = "call_id"
const val EXTRA_NOTIFICATION_ID = "notification_id"

/**
 * Answer/Decline actions on the incoming-call notification's
 * NotificationCompat.CallStyle (see LocalNotificationBridge.kt's
 * postIncomingCallNotification) -- point 5 of the follow-up UX request:
 * "when receiving a call through notif, quick actions not shown."
 *
 * Not exported (AndroidManifest.xml) -- only this app's own PendingIntents
 * can fire it. `callManager.view.value?.callId != callId` guards against a
 * stale notification action firing after the call already ended or changed
 * (e.g. the notification is still showing from a call that was already
 * answered/declined via the in-app UI while this device was briefly
 * foregrounded, then backgrounded again).
 */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra(EXTRA_CALL_ID)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
        }

        val callManager = GlobalContext.get().get<CallManager>()
        if (callManager.view.value?.callId != callId) return

        when (intent.action) {
            ACTION_DECLINE_CALL -> callManager.decline() // synchronous (fires its own coroutine internally) -- see CallManager.decline().
            ACTION_ACCEPT_CALL -> {
                // accept() is suspend -- goAsync() keeps the receiver (and the
                // process) alive long enough for it to actually run; a bare
                // fire-and-forget launch risks the system tearing this
                // receiver down mid-handshake.
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    runCatching { callManager.accept() }
                    // Bring the app to the foreground so CallLayer's actual
                    // in-call UI (mute/camera/hangup controls) is visible --
                    // answering from a notification with no follow-up UI
                    // would leave the user staring at their home screen mid-call.
                    runCatching {
                        context.startActivity(
                            Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            },
                        )
                    }
                    pending.finish()
                }
            }
        }
    }
}
