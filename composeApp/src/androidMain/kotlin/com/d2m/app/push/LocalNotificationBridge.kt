package com.d2m.app.push

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.d2m.app.MainActivity
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.call.CallManager
import com.d2m.app.messaging.call.CallPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

const val NOTIFICATION_CHANNEL_MESSAGES = "d2m_messages"
const val NOTIFICATION_CHANNEL_CALLS = "d2m_calls"

/**
 * Answers point 3 ("Notifications will be received? If not, handle that")
 * and point 5 ("notif should be received in watch as well") of the
 * notification request, for the case the app process is still alive in the
 * background. D2MFirebaseMessagingService.onMessageReceived was previously
 * a no-op (see its own doc comment, and PushTokenRegistrar.kt) -- nothing
 * ever showed a notification for anything, foreground or background.
 *
 * IMPORTANT scope boundary, stated plainly: this rides MessagingRepository's
 * live WebSocket connection, which Android keeps open for a while after the
 * app is backgrounded but not indefinitely, and never once the OS kills the
 * process. It is a real, working improvement over "nothing" today, but it
 * is NOT the same as true push. The backend's notification_service.dispatch()
 * only logs a push_result of "stubbed"/"no_device_token" -- there is no
 * real FCM/APNs send implemented server-side yet. Once that lands and
 * starts reaching D2MFirebaseMessagingService.onMessageReceived (also
 * updated in this pass to post a real notification once it does), delivery
 * will work even with the app fully killed. That backend work is out of
 * scope for this mobile codebase.
 *
 * Notifications posted here are deliberately NOT setLocalOnly(true) --
 * Android's standard notification bridge mirrors any non-local-only
 * notification to a paired Wear OS watch automatically, so point 5's watch
 * delivery falls out of this for free on Android/Wear OS pairs. This does
 * NOT cover non-Wear-OS smartwatches, which would need a dedicated
 * companion integration (out of scope, no existing code for that here).
 *
 * Called once from D2MApplication.onCreate(), after startKoin -- same
 * "fire once at process start" shape as PlatformPushInitializer, just
 * Android-only so it lives directly in androidMain rather than as an
 * expect/actual (there is nothing meaningful to do on iOS here yet: no
 * Xcode project exists to run it in, see WebRtcEngine.ios.kt).
 */
fun installLocalNotificationBridge(app: Application) {
    val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(
        NotificationChannel(NOTIFICATION_CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH),
    )
    notificationManager.createNotificationChannel(
        NotificationChannel(NOTIFICATION_CHANNEL_CALLS, "Calls", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
        },
    )

    var startedActivities = 0
    var isForeground = true
    app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            startedActivities++
            isForeground = true
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivities--
            if (startedActivities <= 0) isForeground = false
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    })

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val koin = GlobalContext.get()
    var nextNotificationId = 1000

    // New messages, any peer that isn't the open conversation -- same
    // stream the in-app banner uses (messaging/ui/InAppNotificationLayer.kt),
    // but only acted on here while backgrounded, so the two never double up.
    scope.launch {
        val messagingRepo = koin.get<MessagingRepository>()
        messagingRepo.inboxNotifications.collect { event ->
            if (isForeground) return@collect
            postD2mNotification(
                app,
                notificationManager,
                NOTIFICATION_CHANNEL_MESSAGES,
                nextNotificationId++,
                messagingRepo.peerDisplayName(event.peerUsername),
                event.preview,
            )
        }
    }

    // Incoming calls -- CallLayer.kt already shows a full-screen in-app
    // ring UI while foregrounded, so this only fires while backgrounded too.
    scope.launch {
        val messagingRepo = koin.get<MessagingRepository>()
        val callManager = koin.get<CallManager>()
        var lastNotifiedCallId: String? = null
        callManager.view.collect { view ->
            if (view != null && view.phase == CallPhase.INCOMING && !isForeground && view.callId != lastNotifiedCallId) {
                lastNotifiedCallId = view.callId
                postD2mNotification(
                    app,
                    notificationManager,
                    NOTIFICATION_CHANNEL_CALLS,
                    nextNotificationId++,
                    "Incoming call",
                    messagingRepo.peerDisplayName(view.peerUsername),
                )
            }
        }
    }
}

/**
 * Shared by installLocalNotificationBridge above and
 * D2MFirebaseMessagingService.onMessageReceived once the backend actually
 * sends a real push. Opens MainActivity on tap -- there's no deep link yet
 * for "open this exact thread" (see MatchesScreen.kt/ChatUiState.kt), so
 * this lands on whatever screen the app resumes to rather than the exact
 * conversation; acceptable for now, a smaller follow-up if it matters later.
 */
fun postD2mNotification(context: Context, notificationManager: NotificationManager, channelId: String, notificationId: Int, title: String, text: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    // Uses the app's own launcher icon rather than a dedicated monochrome
    // notification-icon asset (none exists in this repo) -- fine
    // functionally, just not the crisp single-color icon Android prefers
    // in the status bar; a real icon asset is a design follow-up.
    val notification = Notification.Builder(context, channelId)
        .setSmallIcon(context.applicationInfo.icon)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setLocalOnly(false) // deliberately -- lets this bridge to a paired Wear OS watch automatically (point 5).
        .build()
    runCatching { notificationManager.notify(notificationId, notification) }
}
