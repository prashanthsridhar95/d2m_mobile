package com.d2m.app.push

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.d2m.app.AppForegroundState
import com.d2m.app.MainActivity
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.SoundEffects
import com.d2m.app.messaging.call.CallManager
import com.d2m.app.messaging.call.CallPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

// Versioned ids ("_v2", not "d2m_messages") -- reported directly:
// "Notification ringtones not working." Android notification channels are
// immutable after first creation: NotificationManager.createNotificationChannel
// is a documented no-op for any settings (sound/importance/vibration) on a
// channel id that already exists on the device. These two channels were
// first created (by an earlier build, before per-channel sound was wired up
// -- see task history) with no explicit sound, so on any device that already
// had the app installed, the setSound() calls below silently never took
// effect -- the channel already existed with "default" (often silent/no
// custom ringtone) settings baked in, and re-running createNotificationChannel
// with the same id can't change that. Bumping the id forces Android to treat
// this as a brand-new channel, so the real sound config actually applies.
// The stale old-id channels are explicitly deleted right below so they don't
// linger as dead duplicate entries in the user's system notification
// settings.
const val NOTIFICATION_CHANNEL_MESSAGES = "d2m_messages_v2"
// v3: bumped again -- "getting notification tone AND the call ringtone,
// should only get call ringtone." v2's channel sound (the ringtone URI,
// played once like any other notification sound) was layered underneath the
// real looping ring SoundEffects.playCallTone() now provides (see this
// file's callManager.view collector below) -- the fix is to make this
// channel SILENT (no setSound call at all) so the loop is the only audio,
// but per Android's channel-immutability rule (see the comment below) that
// requires yet another id bump, not just changing what v2 already has.
const val NOTIFICATION_CHANNEL_CALLS = "d2m_calls_v3"
private const val LEGACY_NOTIFICATION_CHANNEL_MESSAGES = "d2m_messages"
private const val LEGACY_NOTIFICATION_CHANNEL_CALLS = "d2m_calls"
private const val LEGACY_NOTIFICATION_CHANNEL_CALLS_V2 = "d2m_calls_v2"

/**
 * Answers point 3 ("Notifications will be received? If not, handle that")
 * and point 5 ("notif should be received in watch as well") of the
 * notification request, for the case the app process is still alive in the
 * background. D2MFirebaseMessagingService.onMessageReceived was previously
 * a no-op (see its own doc comment, and PushTokenRegistrar.kt) -- nothing
 * ever showed a notification for anything, foreground or background.
 *
 * Since then, also answers a few follow-up UX reports directly:
 *  - "Ringtone, message tone not included" -- the messages channel gets an
 *    explicit default notification sound (previously implicit/unset). The
 *    calls channel originally tried AudioAttributes.USAGE_NOTIFICATION_
 *    RINGTONE for this too, on the assumption that would loop like a real
 *    incoming call -- confirmed directly (by testing) that a notification
 *    channel's own sound, even with ringtone-usage audio attributes, only
 *    ever plays ONCE per notification post, same as any other channel; true
 *    looping requires registering a Telecom ConnectionService (not done
 *    here). The calls channel is now deliberately silent (no setSound at
 *    all) -- the real loop is SoundEffects.playCallTone()/stopCallTone(),
 *    triggered below off callManager.view's phase, independent of whether
 *    the channel's own sound would have doubled up with it.
 *  - "When receiving a call through notif, quick actions not shown. same
 *    for messages" -- postIncomingCallNotification below uses
 *    NotificationCompat.CallStyle with real Answer/Decline actions
 *    (CallActionReceiver.kt), and postMessageNotification uses
 *    NotificationCompat.MessagingStyle with an inline quick-reply action
 *    (MessageReplyReceiver.kt) -- both work without opening the app.
 *  - "When receiving a call through notif, quick actions not shown. same
 *    for messages" -- postIncomingCallNotification below uses
 *    NotificationCompat.CallStyle with real Answer/Decline actions
 *    (CallActionReceiver.kt), and postMessageNotification uses
 *    NotificationCompat.MessagingStyle with an inline quick-reply action
 *    (MessageReplyReceiver.kt) -- both work without opening the app.
 *
 * IMPORTANT scope boundary, stated plainly: this rides MessagingRepository's
 * live WebSocket connection, which Android keeps open for a while after the
 * app is backgrounded but not indefinitely, and never once the OS kills the
 * process. For that "fully killed, or never opened this cold boot" case,
 * see D2MFirebaseMessagingService.kt instead -- real FCM push (backend send
 * via d2m_core_engine's push_service.py, client token registration via
 * PlatformPushInitializer.android.kt + the google-services Gradle plugin)
 * is what covers it; that file's doc comment has the current, accurate
 * status of both halves (this comment previously described the backend
 * send as still stubbed, which is now out of date -- it's real, gated only
 * behind D2M_FCM_SERVICE_ACCOUNT_JSON being configured on the deployed
 * server, same as the client half needs its own google-services.json).
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

    // Safe no-op if the legacy channel was never created (fresh install) --
    // only matters for upgrades from a build that used the un-versioned ids.
    runCatching { notificationManager.deleteNotificationChannel(LEGACY_NOTIFICATION_CHANNEL_MESSAGES) }
    runCatching { notificationManager.deleteNotificationChannel(LEGACY_NOTIFICATION_CHANNEL_CALLS) }
    runCatching { notificationManager.deleteNotificationChannel(LEGACY_NOTIFICATION_CHANNEL_CALLS_V2) }

    val messageAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    val messageSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    notificationManager.createNotificationChannel(
        NotificationChannel(NOTIFICATION_CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            runCatching { setSound(messageSoundUri, messageAttributes) }
        },
    )
    // Deliberately NO setSound() here -- see NOTIFICATION_CHANNEL_CALLS's own
    // comment above. SoundEffects.playCallTone() (this file's
    // callManager.view collector below) is the one real, looping ring;
    // giving this channel its own sound too just plays a second, competing
    // "ding" once on top of it, which is the "getting notification tone AND
    // the call ringtone" bug. Vibration stays on -- that's still a real,
    // useful, non-audio incoming-call cue, no reason to drop it.
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
            // Mirrors this same flag into AppForegroundState so commonMain UI
            // (CallLayer.kt/ChatPane.kt/InAppNotificationLayer.kt's in-app
            // sound-effects triggers) can read it too -- see that object's
            // own doc comment on why exactly one of "in-app tone" or "OS
            // notification sound" should ever fire for a given event.
            AppForegroundState.set(true)
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivities--
            if (startedActivities <= 0) {
                isForeground = false
                AppForegroundState.set(false)
            }
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
            postMessageNotification(
                app,
                notificationManager,
                nextNotificationId++,
                event.peerUsername,
                messagingRepo.peerDisplayName(event.peerUsername),
                event.preview,
                event.messageId,
            )
        }
    }

    // Incoming calls -- CallLayer.kt already shows a full-screen in-app
    // ring UI while foregrounded, so this only fires while backgrounded too.
    // Notification id is derived from callId (not the shared counter) so it
    // can be looked up again below to cancel it the moment the call stops
    // being INCOMING for any other reason (answered/declined via some other
    // path, caller hung up before we responded, it timed out) -- otherwise a
    // stale "Incoming call" notification with dead Answer/Decline buttons
    // could sit there ringing after the call itself is long gone.
    scope.launch {
        val messagingRepo = koin.get<MessagingRepository>()
        val callManager = koin.get<CallManager>()
        var lastNotifiedCallId: String? = null
        callManager.view.collect { view ->
            // Real looping ringtone -- NOT the CallStyle notification's own
            // channel sound below. Confirmed directly: NotificationCompat.
            // CallStyle + a ringtone-usage AudioAttributes channel does NOT
            // actually loop like a real phone call without registering a
            // Telecom ConnectionService (a much bigger integration, not
            // attempted here) -- it just plays the configured sound once,
            // like any other notification. SoundEffects.playCallTone() is
            // the SAME MediaPlayer-backed real loop CallLayer.kt used to
            // trigger itself (foreground-only, on the wrong assumption that
            // the background notification's sound alone would loop) -- moved
            // here so it plays for an incoming call regardless of whether
            // the app is foregrounded, backgrounded, or was cold-started
            // purely to handle this push (D2MFirebaseMessagingService.kt).
            // Unconditional on isForeground, unlike the notification post
            // below -- this is now the ONE source of ring audio either way.
            if (view != null && view.phase == CallPhase.INCOMING) {
                SoundEffects.playCallTone()
            } else {
                SoundEffects.stopCallTone()
            }

            // Dedup bookkeeping (lastNotifiedCallId) is tracked independent of
            // isForeground now -- previously it was only ever SET inside the
            // `!isForeground` branch, so if isForeground happened to be
            // (wrongly, or just briefly) true the moment a call's view first
            // went INCOMING, this call's id was never recorded at all. That
            // was a real, separate bug from the isForeground check itself:
            // "calls after [the first one] not showing call notif, just
            // ringtone heard" -- SoundEffects.playCallTone() above doesn't
            // care about any of this bookkeeping, so the ring still played,
            // but the notification silently never got its chance.
            if (view != null && view.phase == CallPhase.INCOMING) {
                if (view.callId != lastNotifiedCallId) {
                    lastNotifiedCallId = view.callId
                    if (!isForeground) {
                        postIncomingCallNotification(
                            app,
                            notificationManager,
                            view.callId,
                            messagingRepo.peerDisplayName(view.peerUsername),
                            isVideo = view.media == "video",
                        )
                    }
                }
            } else if (lastNotifiedCallId != null) {
                notificationManager.cancel(lastNotifiedCallId.hashCode())
                lastNotifiedCallId = null
            }
        }
    }
}

/**
 * NotificationCompat.CallStyle.forIncomingCall -- renders as a real
 * full-bleed incoming-call notification on Android 12+ (large avatar,
 * prominent Answer/Decline) and degrades to a normal high-priority
 * notification with the same two actions on older versions.
 *
 * Answer is a PendingIntent.getActivity targeting MainActivity DIRECTLY
 * (carrying EXTRA_ACCEPT_CALL_ID), NOT a CallActionReceiver broadcast like
 * Decline below -- confirmed directly that "Accept still doesn't open the
 * app" persisted even after moving CallActionReceiver's own launch to
 * PendingIntent.send(): a BroadcastReceiver launching an Activity, even via
 * a fresh PendingIntent, does not reliably inherit the "user just tapped
 * this notification" background-activity-start exemption on Android 10+.
 * The only launch style guaranteed to work is the same one the
 * notification's own body/full-screen tap already uses successfully:
 * targeting the Activity directly. See PendingCallAccept.kt (commonMain)
 * and MainActivity.kt for where that extra is picked up and the actual
 * accept() work happens once the app is genuinely open. Decline doesn't
 * need to open the app at all, so it stays a plain broadcast
 * (CallActionReceiver) with no launch-reliability concerns.
 *
 * setFullScreenIntent additionally asks to show this over the lock screen --
 * wrapped in runCatching since Android 14+ can silently ignore it until the
 * user grants "Settings > Apps > D2M > Full screen notifications" (see the
 * USE_FULL_SCREEN_INTENT permission's comment in AndroidManifest.xml); the
 * heads-up notification with working actions still shows either way.
 */
fun postIncomingCallNotification(context: Context, notificationManager: NotificationManager, callId: String, callerName: String, isVideo: Boolean = false) {
    val notificationId = callId.hashCode()
    val fullScreenIntent = PendingIntent.getActivity(
        context,
        notificationId,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(EXTRA_ACCEPT_CALL_ID, callId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val acceptIntent = PendingIntent.getActivity(
        context,
        notificationId * 31 + ACTION_ACCEPT_CALL.hashCode(),
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(EXTRA_ACCEPT_CALL_ID, callId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val declineIntent = PendingIntent.getBroadcast(
        context,
        notificationId * 31 + ACTION_DECLINE_CALL.hashCode(),
        Intent(context, CallActionReceiver::class.java).apply {
            action = ACTION_DECLINE_CALL
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val caller = Person.Builder().setName(callerName).build()
    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_CALLS)
        .setSmallIcon(context.applicationInfo.icon)
        .setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, declineIntent, acceptIntent))
        .setContentTitle(callerName)
        .setContentText(if (isVideo) "Incoming video call" else "Incoming audio call")
        .setContentIntent(fullScreenIntent)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setOngoing(true)
        .setAutoCancel(false)
        .setLocalOnly(false) // deliberately -- mirrors to a paired Wear OS watch (point 5).
        .apply { if (canShowFullScreenIntent(context)) setFullScreenIntent(fullScreenIntent, true) }
        .build()
    runCatching { notificationManager.notify(notificationId, notification) }
}

private fun canShowFullScreenIntent(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 34) return true // permission auto-granted pre-Android 14
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return runCatching { notificationManager.canUseFullScreenIntent() }.getOrDefault(false)
}

/**
 * NotificationCompat.MessagingStyle + an inline RemoteInput reply action --
 * lets a reply go out (MessageReplyReceiver) without opening the app, same
 * as any modern messaging app's notification.
 *
 * [messageId] is optional (older/malformed push payloads may not carry
 * one -- see D2MFirebaseMessagingService.kt's handleChatMessagePush): when
 * present, a second "Mark as read" action (MarkReadReceiver) is shown
 * alongside Reply, sending a real read receipt and clearing this peer's
 * unread badge without opening the app.
 */
fun postMessageNotification(
    context: Context,
    notificationManager: NotificationManager,
    notificationId: Int,
    peerUsername: String,
    senderName: String,
    preview: String,
    messageId: String? = null,
) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentIntent = PendingIntent.getActivity(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val remoteInput = RemoteInput.Builder(KEY_QUICK_REPLY_INPUT).setLabel("Reply").build()
    val replyIntent = PendingIntent.getBroadcast(
        context,
        notificationId,
        Intent(context, MessageReplyReceiver::class.java).apply {
            action = ACTION_QUICK_REPLY
            putExtra(EXTRA_PEER_USERNAME, peerUsername)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        },
        // Mutable -- required for the platform to attach the RemoteInput's
        // typed text onto this PendingIntent when the user submits it.
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
    val replyAction = NotificationCompat.Action.Builder(0, "Reply", replyIntent)
        .addRemoteInput(remoteInput)
        .setAllowGeneratedReplies(true)
        .build()

    val markReadAction = messageId?.let { id ->
        val markReadIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 31 + ACTION_MARK_READ.hashCode(),
            Intent(context, MarkReadReceiver::class.java).apply {
                action = ACTION_MARK_READ
                putExtra(EXTRA_PEER_USERNAME, peerUsername)
                putExtra(EXTRA_MESSAGE_ID, id)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        NotificationCompat.Action.Builder(0, "Mark as read", markReadIntent).build()
    }

    val sender = Person.Builder().setName(senderName).build()
    val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_MESSAGES)
        .setSmallIcon(context.applicationInfo.icon)
        .setStyle(
            NotificationCompat.MessagingStyle(sender)
                .addMessage(preview, System.currentTimeMillis(), sender),
        )
        .setContentIntent(contentIntent)
        .addAction(replyAction)
        .setAutoCancel(true)
        .setLocalOnly(false) // deliberately -- mirrors to a paired Wear OS watch (point 5).
    markReadAction?.let { builder.addAction(it) }
    runCatching { notificationManager.notify(notificationId, builder.build()) }
}

/**
 * Shared by D2MFirebaseMessagingService.onMessageReceived once the backend
 * actually sends a real push, for anything that doesn't warrant the
 * call/message styling above. Opens MainActivity on tap -- there's no deep
 * link yet for "open this exact thread" (see MatchesScreen.kt/ChatUiState.kt),
 * so this lands on whatever screen the app resumes to rather than the exact
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
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(context.applicationInfo.icon)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setLocalOnly(false) // deliberately -- lets this bridge to a paired Wear OS watch automatically (point 5).
        .build()
    runCatching { notificationManager.notify(notificationId, notification) }
}
