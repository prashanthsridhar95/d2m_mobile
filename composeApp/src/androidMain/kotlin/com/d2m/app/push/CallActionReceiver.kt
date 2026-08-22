package com.d2m.app.push

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.d2m.app.messaging.call.CallManager
import org.koin.core.context.GlobalContext

const val ACTION_ACCEPT_CALL = "com.d2m.app.action.ACCEPT_CALL"
const val ACTION_DECLINE_CALL = "com.d2m.app.action.DECLINE_CALL"
const val EXTRA_CALL_ID = "call_id"
const val EXTRA_NOTIFICATION_ID = "notification_id"
/** MainActivity intent extra carrying the callId to accept -- see PendingCallAccept.kt's doc comment for why Accept is a direct Activity-launch PendingIntent (LocalNotificationBridge.kt) instead of a broadcast through this receiver. */
const val EXTRA_ACCEPT_CALL_ID = "accept_call_id"

/**
 * Decline action on the incoming-call notification's
 * NotificationCompat.CallStyle (see LocalNotificationBridge.kt's
 * postIncomingCallNotification). Answer used to be handled here too, but
 * moved to a direct PendingIntent.getActivity targeting MainActivity (see
 * PendingCallAccept.kt) -- confirmed directly that a BroadcastReceiver
 * launching an Activity, even via PendingIntent.send(), does not reliably
 * inherit the "user just tapped this notification" background-activity-
 * start exemption on Android 10+. Decline never needs to open the app at
 * all, so it has no such reliability concern and stays a plain broadcast.
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

        if (intent.action == ACTION_DECLINE_CALL) {
            callManager.decline() // synchronous (fires its own coroutine internally) -- see CallManager.decline().
        }
    }
}
