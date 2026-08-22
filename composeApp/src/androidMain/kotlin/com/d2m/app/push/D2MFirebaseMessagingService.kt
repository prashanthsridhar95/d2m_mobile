package com.d2m.app.push

import android.app.NotificationManager
import android.content.Context
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.SeriousModeRepository
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.ParentContactsStore
import com.d2m.app.messaging.call.CallManager
import com.d2m.app.messaging.call.CallPhase
import com.d2m.app.messaging.protocol.ChatPayload
import com.d2m.app.messaging.protocol.MediaMeta
import com.d2m.app.messaging.protocol.MessageEnvelope
import com.d2m.app.messaging.resolveDisplayName
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.Koin

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
 * CONTEXTUAL content ("real sender/caller name, real message text, Reply +
 * Mark as read, and a real ringing call with working Accept/Decline" --
 * explicitly requested, in two passes, after the generic-copy version above
 * shipped): the relay server is E2E and never decrypts anything server-side,
 * so all of this has to be resolved/decrypted HERE, client-side, from what
 * the push payload carries:
 *  - `sender` (see `data["sender"]` -- NOT `from`: FCM reserves that key for
 *    its own protocol envelope and firebase_admin rejects any data payload
 *    containing it) is the raw messaging-framework username. [resolveDisplayName]
 *    turns that into a real name via ParentContactsStore (Parent flow, local)
 *    or SeriousModeRepository's thread list (Child flow, backend) -- see
 *    that file's own doc comment.
 *  - For `chat_message`, ws.ts's triggerPush call site now also carries the
 *    message's ciphertext (`messageId`/`ciphertextType`/`ciphertext`/
 *    `sentAt`) alongside the generic `title`/`body` fallback copy. This is
 *    still E2E-safe: the server is just as blind to it routed through FCM as
 *    it is routed through the WS relay itself. [MessagingRepository.
 *    decryptAndPersistIncoming] -- the SAME function the live socket path
 *    uses, not a separate reimplementation -- does the actual decrypt here,
 *    guarded by an id-based dedup check so this can never double-consume the
 *    Signal ratchet's message key if the live socket later redelivers the
 *    exact same envelope from the server's durable queue (see that
 *    function's own doc comment for the full reasoning). A successful
 *    decrypt also means the message is posted via [postMessageNotification]
 *    (MessagingStyle + a real, working inline Reply action --
 *    MessageReplyReceiver.kt already handles the "process was killed" case
 *    for the reply itself -- plus a Mark-as-read action, MarkReadReceiver.kt)
 *    instead of the plain [postD2mNotification].
 *  - For `incoming_call`, the SAME ciphertext-carrying mechanism now also
 *    covers the Invite itself (see ws.ts's message.send handler and
 *    messaging-framework's new pending_call_invites table/store.ts --
 *    unlike every other ephemeral call signal, callInvite-flagged envelopes
 *    ARE durably stashed, single-slot per recipient, specifically so a
 *    push-woken cold start has something to decrypt). [handleIncomingCallPush]
 *    below calls [MessagingRepository.ensureConnected] (a live socket is
 *    needed regardless -- CallManager.handleSignal's Invite branch sends a
 *    Ringing signal back to the caller immediately), then decrypts the
 *    Invite through the exact same [MessagingRepository.decryptAndPersistIncoming]
 *    path, which already dispatches a decoded ChatPayload.Call to
 *    CallManager's callSignalHandler (wired at CallManager's construction --
 *    see D2MApplication.onCreate() -> installLocalNotificationBridge ->
 *    koin.get<CallManager>(), which happens before this service can ever
 *    receive a message, even on a fully cold process start). Once
 *    CallManager.view reflects a live CallPhase.INCOMING for this callId,
 *    [postIncomingCallNotification] (real CallStyle, loops the ringtone on
 *    NOTIFICATION_CHANNEL_CALLS the same way an actual phone call does) is
 *    used instead of the plain fallback -- and CallActionReceiver's
 *    Answer/Decline buttons now work, since its guard
 *    (`callManager.view.value?.callId != callId`) is satisfied by a real
 *    Invite, not just a live-socket one.
 *  - If any of the above is missing or fails (older server build without
 *    the ciphertext fields, first-ever message from a brand-new contact,
 *    decrypt failure, invite already gone stale server-side, network hiccup
 *    resolving a Child-flow name), this falls back to the original generic
 *    `title`/`body` copy (messages) or a plain non-ringing notification with
 *    no action buttons (calls) -- never a blank or broken notification, and
 *    never buttons that would silently do nothing.
 *
 * Runs its decrypt/resolve work via a bounded (8s) [runBlocking] rather than
 * `CoroutineScope(...).launch` like onNewToken above, because the
 * notification actually needs the result (real name/preview/live call
 * state) before it can be posted -- unlike onNewToken, which is truly
 * fire-and-forget. FCM already invokes onMessageReceived off the main thread
 * and grants extra background execution time for high-priority data
 * messages (see push_service.py's AndroidConfig(priority="high")), so a
 * short bounded block here is the same trade-off MessageReplyReceiver.kt's
 * goAsync() makes for the same underlying reason (this device's crypto
 * session/local store/socket work has to happen somewhere before anything
 * downstream can use it).
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
        val sender = message.data["sender"]
        // Diagnostic breadcrumb for "notifications not received when app is
        // not live" reports -- this line running at all (check via `adb
        // logcat` while the app is killed) confirms the message reached
        // this device and this callback fired; if a future report says
        // nothing arrives, but this line never appears in logcat either,
        // that rules out everything downstream (channel/permission/
        // battery-optimization) and points back at FCM delivery itself
        // (token validity, server-side send, or OS-level throttling before
        // this process ever wakes) rather than anything in this method.
        println("D2MFirebaseMessagingService.onMessageReceived: type=$type sender=$sender priority=${message.priority} originalPriority=${message.originalPriority}")

        val fallbackTitle = message.data["title"] ?: message.notification?.title ?: "New notification"
        val fallbackBody = message.data["body"] ?: message.notification?.body ?: "You have a new notification."
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val koin = org.koin.core.context.GlobalContext.get()

        when (type) {
            "chat_message" -> handleChatMessagePush(message, sender, fallbackBody, koin, notificationManager)
            "incoming_call" -> handleIncomingCallPush(message, sender, koin, notificationManager)
            else -> postD2mNotification(applicationContext, notificationManager, NOTIFICATION_CHANNEL_MESSAGES, System.currentTimeMillis().toInt(), fallbackTitle, fallbackBody)
        }
    }

    private fun handleChatMessagePush(
        message: RemoteMessage,
        sender: String?,
        fallbackBody: String,
        koin: Koin,
        notificationManager: NotificationManager,
    ) {
        val notificationId = System.currentTimeMillis().toInt()
        if (sender == null) {
            // No sender id at all -- an even older/malformed payload than the
            // "no ciphertext fields" case below. Nothing to resolve or
            // decrypt; show the generic copy exactly like before this change.
            postD2mNotification(applicationContext, notificationManager, NOTIFICATION_CHANNEL_MESSAGES, notificationId, "New message", fallbackBody)
            return
        }

        val messagingRepo = koin.get<MessagingRepository>()
        val identityStore = koin.get<IdentityStore>()
        val parentContactsStore = koin.get<ParentContactsStore>()
        val seriousModeRepository = koin.get<SeriousModeRepository>()

        val messageId = message.data["messageId"]
        val ciphertextType = message.data["ciphertextType"]
        val ciphertext = message.data["ciphertext"]
        val sentAt = message.data["sentAt"]?.toLongOrNull()

        val result = runBlocking(Dispatchers.Default) {
            withTimeoutOrNull(8_000) {
                val myUsername = runCatching { messagingRepo.ensurePassiveIdentity() }
                    .onFailure { e -> println("D2MFirebaseMessagingService: ensurePassiveIdentity failed: ${e.message ?: e::class.simpleName}") }
                    .getOrNull()
                val decrypted = if (myUsername != null && messageId != null && ciphertextType != null && ciphertext != null && sentAt != null) {
                    val envelope = MessageEnvelope(messageId, sender, myUsername, ciphertextType, ciphertext, sentAt)
                    runCatching { messagingRepo.decryptAndPersistIncoming(envelope) }
                        .onFailure { e -> println("D2MFirebaseMessagingService: decryptAndPersistIncoming failed for $messageId: ${e.message ?: e::class.simpleName}") }
                        .getOrNull()
                } else {
                    println("D2MFirebaseMessagingService: chat_message push for $sender missing ciphertext fields (older server build?) -- falling back to generic body")
                    null
                }
                val preview = when (decrypted) {
                    is ChatPayload.Text -> decrypted.text
                    is ChatPayload.Media -> decrypted.caption?.takeIf { it.isNotBlank() } ?: mediaPreviewLabel(decrypted.media)
                    // Edit/Delete/Reaction/Call, a dedup-skip, or a decrypt
                    // failure -- none of these are worth a fabricated
                    // preview; fall back to the generic body below instead.
                    else -> null
                }
                val senderName = runCatching {
                    resolveDisplayName(identityStore, parentContactsStore, seriousModeRepository, sender, fallback = sender)
                }.getOrDefault(sender)
                // Seed MessagingRepository's own (otherwise cold-process-empty)
                // name cache -- see cachePeerDisplayName's doc comment. Only
                // worth it if resolution actually found something real, not
                // the bare fallback (no point caching "id == id").
                if (senderName != sender) messagingRepo.cachePeerDisplayName(sender, senderName)
                preview to senderName
            }
        }
        val (preview, senderName) = result ?: (null to sender)

        postMessageNotification(applicationContext, notificationManager, notificationId, sender, senderName, preview ?: fallbackBody, messageId)
    }

    /**
     * See this class's own doc comment for the full design. Short version:
     * decrypt the Invite (if the payload carries one) through the exact
     * same path a live socket delivery would use, which -- on success --
     * leaves CallManager.view holding a real CallPhase.INCOMING for this
     * call. Only then do we post the real ringing/Answer/Decline
     * notification; otherwise this falls back to the old plain one.
     */
    private fun handleIncomingCallPush(message: RemoteMessage, sender: String?, koin: Koin, notificationManager: NotificationManager) {
        val fallbackNotificationId = System.currentTimeMillis().toInt()
        if (sender == null) {
            postD2mNotification(applicationContext, notificationManager, NOTIFICATION_CHANNEL_CALLS, fallbackNotificationId, "Incoming call", "Someone is calling you")
            return
        }

        val identityStore = koin.get<IdentityStore>()
        val parentContactsStore = koin.get<ParentContactsStore>()
        val seriousModeRepository = koin.get<SeriousModeRepository>()
        val messagingRepo = koin.get<MessagingRepository>()
        val apiClient = koin.get<ApiClient>()
        val callManager = koin.get<CallManager>()

        val messageId = message.data["messageId"]
        val ciphertextType = message.data["ciphertextType"]
        val ciphertext = message.data["ciphertext"]
        val sentAt = message.data["sentAt"]?.toLongOrNull()

        val result = runBlocking(Dispatchers.Default) {
            withTimeoutOrNull(11_000) {
                val callerNameDeferred = async {
                    runCatching {
                        resolveDisplayName(identityStore, parentContactsStore, seriousModeRepository, sender, fallback = sender)
                    }.getOrDefault(sender)
                }

                // A live socket first: CallManager.handleSignal's Invite
                // branch sends a Ringing signal straight back to the caller
                // (CallManager.kt), and Accept/Decline need one too once the
                // user acts on the notification. Critically, this ALSO makes
                // the server replay this recipient's pending_call_invites row
                // over the socket the instant it connects (see ws.ts's
                // connection handler) -- completely independent of whether
                // the push payload itself carried the invite ciphertext,
                // which -- unlike a chat message -- an SDP offer very often
                // won't fit into (a real one here ran 4114 bytes, already
                // over FCM's 4KB cap on its own). That live delivery lands
                // asynchronously via MessagingRepository's own internal
                // handleEvent -> decryptAndPersistIncoming, on a DIFFERENT
                // coroutine than this one -- so the direct-decrypt block
                // below is a latency optimization when the payload DOES fit,
                // never the only path.
                runCatching { messagingRepo.ensureConnected(apiClient.client) }
                    .onFailure { e -> println("D2MFirebaseMessagingService: ensureConnected failed for incoming_call: ${e.message ?: e::class.simpleName}") }
                val myUsername = runCatching { messagingRepo.ensurePassiveIdentity() }.getOrNull()
                if (myUsername != null && messageId != null && ciphertextType != null && ciphertext != null && sentAt != null) {
                    val envelope = MessageEnvelope(messageId, sender, myUsername, ciphertextType, ciphertext, sentAt, ephemeral = true, callInvite = true)
                    runCatching { messagingRepo.decryptAndPersistIncoming(envelope) }
                        .onFailure { e -> println("D2MFirebaseMessagingService: incoming_call decrypt failed for $messageId: ${e.message ?: e::class.simpleName}") }
                } else {
                    println("D2MFirebaseMessagingService: incoming_call push for $sender missing ciphertext fields (SDP too large for FCM, older server build, or the invite already went stale) -- waiting on the live-socket drain instead")
                }

                // Whichever path actually got there first -- the direct
                // decrypt above (updates CallManager.view synchronously, so
                // this returns near-instantly) or the async live-drain
                // triggered by ensureConnected() -- wait for CallManager to
                // actually reflect it rather than checking .value once and
                // giving up. claimEnvelopeForProcessing (MessagingRepository)
                // guarantees only one of the two ever actually decrypts.
                val view = runCatching {
                    withTimeoutOrNull(4_000) {
                        callManager.view.first { it != null && it.phase == CallPhase.INCOMING && it.peerUsername == sender }
                    }
                }.getOrNull()

                val callerName = callerNameDeferred.await()
                // Seed MessagingRepository's own (otherwise cold-process-empty)
                // name cache -- CallLayer.kt reads messagingRepo.peerDisplayName()
                // directly, which is exactly what showed the raw id instead of
                // a name when the call view was reached straight from a
                // killed-app notification. See cachePeerDisplayName's doc
                // comment.
                if (callerName != sender) messagingRepo.cachePeerDisplayName(sender, callerName)
                view to callerName
            }
        }
        val (view, callerName) = result ?: (null to sender)

        if (view != null) {
            // Decrypt succeeded and CallManager now holds a real, live
            // Invite -- same state CallActionReceiver's guard checks, so
            // Accept/Decline actually work, and CallStyle on
            // NOTIFICATION_CHANNEL_CALLS (ringtone-usage audio, see
            // LocalNotificationBridge.kt) makes this actually ring/loop like
            // a real incoming call instead of a single notification sound.
            postIncomingCallNotification(applicationContext, notificationManager, view.callId, callerName, isVideo = view.media == "video")
        } else {
            // No usable invite -- real caller name if resolved, but a plain
            // notification: no ring loop, no buttons that would silently do
            // nothing (CallActionReceiver's guard would just reject them).
            postD2mNotification(applicationContext, notificationManager, NOTIFICATION_CHANNEL_CALLS, fallbackNotificationId, callerName, "Incoming call")
        }
    }
}

/** "📷 Photo" / "🎥 Video" / "🎤 Voice message" / "📎 Attachment" -- used when a decrypted chat_message push is a ChatPayload.Media with no (or blank) caption, so the notification still says something concrete instead of falling all the way back to the generic "You have a new message." */
private fun mediaPreviewLabel(media: MediaMeta): String = when (media.kind) {
    "image" -> "📷 Photo"
    "video" -> "🎥 Video"
    "audio" -> "🎤 Voice message"
    else -> "📎 Attachment"
}
