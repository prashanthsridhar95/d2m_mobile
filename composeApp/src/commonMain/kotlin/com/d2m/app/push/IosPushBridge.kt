package com.d2m.app.push

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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform

/**
 * The iOS counterpart to D2MFirebaseMessagingService.kt's onMessageReceived
 * handling -- same decrypt-real-content-from-a-push-payload logic, ported
 * here as plain commonMain functions instead of living inside an
 * Android-only FirebaseMessagingService subclass, because on iOS the two
 * places that need this logic (PushKitManager.ios.kt for calls, running in
 * the main app process; NotificationService.swift for messages, running in
 * a separate Notification Service Extension process) are both native
 * Swift/Kotlin-Native call sites, not one single OS-provided service class
 * the way FCM gives Android.
 *
 * `object` (not a class) so Swift can reach every function as
 * `IosPushBridge.shared.xxx(...)` without any DI/constructor wiring on the
 * Swift side -- internally still goes through Koin, via org.koin.mp.KoinPlatform
 * (NOT org.koin.core.context.GlobalContext, which every Android push receiver
 * in this codebase uses -- confirmed directly via the real koin-core klib
 * that GlobalContext is a JVM/Android-only extra, absent from Kotlin/Native's
 * public API). Koin MUST already be
 * started in whichever process calls these -- see
 * KoinBootstrap.ensureStarted() (commonMain) for the entry point both
 * AppDelegate (main app process, PushKit-woken or not) and
 * NotificationService.swift (extension process) call before touching
 * anything here.
 *
 * Every suspend function below compiles to a completion-handler-based
 * Objective-C method automatically (Kotlin/Native's standard suspend-fun
 * ObjC export) -- callable from Swift either with a trailing closure or
 * `await` if the Xcode project's Swift concurrency setting supports it.
 */
object IosPushBridge {

    /** Plain data carrier for a decrypted chat_message push -- see decryptChatMessagePush. */
    class DecryptedMessagePush(
        val senderUsername: String,
        val senderDisplayName: String,
        val preview: String?,
    )

    /** Plain data carrier for a resolved incoming_call push -- see handleIncomingCallPush. */
    class IncomingCallPush(
        val callId: String,
        val callerUsername: String,
        val callerDisplayName: String,
        val isVideo: Boolean,
    )

    /**
     * Mirrors D2MFirebaseMessagingService.handleChatMessagePush. Called from
     * NotificationService.swift's didReceive(_:withContentHandler:) once
     * that extension has parsed the raw APNs payload dictionary into these
     * plain string/long arguments (userInfo["sender"], userInfo["messageId"],
     * etc. -- see push_service.py's send_apns_alert for the exact payload
     * shape, `aps` aside). Returns null if the payload was missing fields,
     * the sender is unknown, or decryption failed for any reason -- the
     * extension falls back to the generic `aps.alert` title/body Apple
     * already delivered in that case, exactly like postD2mNotification does
     * on Android.
     */
    suspend fun decryptChatMessagePush(
        sender: String,
        messageId: String?,
        ciphertextType: String?,
        ciphertext: String?,
        sentAt: Long?,
    ): DecryptedMessagePush? = withTimeoutOrNull(8_000) {
        val koin = KoinPlatform.getKoin()
        val messagingRepo = koin.get<MessagingRepository>()
        val identityStore = koin.get<IdentityStore>()
        val parentContactsStore = koin.get<ParentContactsStore>()
        val seriousModeRepository = koin.get<SeriousModeRepository>()

        val myUsername = runCatching { messagingRepo.ensurePassiveIdentity() }.getOrNull()
        val decrypted = if (myUsername != null && messageId != null && ciphertextType != null && ciphertext != null && sentAt != null) {
            val envelope = MessageEnvelope(messageId, sender, myUsername, ciphertextType, ciphertext, sentAt)
            runCatching { messagingRepo.decryptAndPersistIncoming(envelope) }.getOrNull()
        } else {
            null
        }
        val preview = when (decrypted) {
            is ChatPayload.Text -> decrypted.text
            is ChatPayload.Media -> decrypted.caption?.takeIf { it.isNotBlank() } ?: mediaPreviewLabel(decrypted.media)
            else -> null
        }
        val senderName = runCatching {
            resolveDisplayName(identityStore, parentContactsStore, seriousModeRepository, sender, fallback = sender)
        }.getOrDefault(sender)
        if (senderName != sender) messagingRepo.cachePeerDisplayName(sender, senderName)
        DecryptedMessagePush(senderUsername = sender, senderDisplayName = senderName, preview = preview)
    }

    /**
     * Mirrors D2MFirebaseMessagingService.handleIncomingCallPush, minus the
     * notification-posting tail (PushKitManager.ios.kt does that itself via
     * CXProvider, not a UNNotificationRequest). Called from the main app
     * process's PKPushRegistryDelegate as soon as a VoIP push wakes it.
     *
     * Unlike Android, there's no separate "wait 4s for CallManager.view"
     * step needed as an afterthought here -- iOS's own contract (report a
     * call practically immediately) means the caller of this function
     * should already have called CXProvider.reportNewIncomingCall with a
     * best-effort placeholder BEFORE awaiting this, then call
     * CXProvider.reportCall(with:updated:) once this returns with the real
     * name/media -- see PushKitManager.ios.kt's own doc comment for why
     * that ordering, not this function, is what satisfies Apple's timing
     * requirement.
     */
    suspend fun handleIncomingCallPush(
        sender: String,
        messageId: String?,
        ciphertextType: String?,
        ciphertext: String?,
        sentAt: Long?,
    ): IncomingCallPush? = withTimeoutOrNull(11_000) {
        val koin = KoinPlatform.getKoin()
        val messagingRepo = koin.get<MessagingRepository>()
        val identityStore = koin.get<IdentityStore>()
        val parentContactsStore = koin.get<ParentContactsStore>()
        val seriousModeRepository = koin.get<SeriousModeRepository>()
        val apiClient = koin.get<ApiClient>()
        val callManager = koin.get<CallManager>()

        coroutineScope {
            val callerNameDeferred = async {
                runCatching {
                    resolveDisplayName(identityStore, parentContactsStore, seriousModeRepository, sender, fallback = sender)
                }.getOrDefault(sender)
            }

            runCatching { messagingRepo.ensureConnected(apiClient.client) }
            val myUsername = runCatching { messagingRepo.ensurePassiveIdentity() }.getOrNull()
            if (myUsername != null && messageId != null && ciphertextType != null && ciphertext != null && sentAt != null) {
                val envelope = MessageEnvelope(messageId, sender, myUsername, ciphertextType, ciphertext, sentAt, ephemeral = true, callInvite = true)
                runCatching { messagingRepo.decryptAndPersistIncoming(envelope) }
            }

            val view = runCatching {
                withTimeoutOrNull(4_000) {
                    callManager.view.first { it != null && it.phase == CallPhase.INCOMING && it.peerUsername == sender }
                }
            }.getOrNull()

            val callerName = callerNameDeferred.await()
            if (callerName != sender) messagingRepo.cachePeerDisplayName(sender, callerName)

            view?.let {
                IncomingCallPush(callId = it.callId, callerUsername = sender, callerDisplayName = callerName, isVideo = it.media == "video")
            }
        }
    }

    /** Reply notification action (D2MFirebaseMessagingService's
     * MessageReplyReceiver.kt equivalent) -- ensureConnected first, same
     * "cold process / stale background socket" reasoning as that receiver's
     * own doc comment. */
    suspend fun replyToMessage(peerUsername: String, text: String) {
        val koin = KoinPlatform.getKoin()
        val messagingRepo = koin.get<MessagingRepository>()
        val apiClient = koin.get<ApiClient>()
        runCatching { messagingRepo.ensureConnected(apiClient.client) }
        runCatching { messagingRepo.sendText(peerUsername, text) }
    }

    /** Mark-as-read notification action (MarkReadReceiver.kt equivalent). */
    suspend fun markMessageRead(peerUsername: String, messageId: String) {
        val koin = KoinPlatform.getKoin()
        val messagingRepo = koin.get<MessagingRepository>()
        val apiClient = koin.get<ApiClient>()
        runCatching { messagingRepo.ensureConnected(apiClient.client) }
        runCatching { messagingRepo.markMessageRead(peerUsername, messageId) }
    }

    /** CXProviderDelegate's CXAnswerCallAction handler calls this (see
     * PushKitManager.ios.kt) -- same ensureConnected-then-accept ordering
     * CallLayer.kt's PendingCallAccept effect uses on the in-app path. */
    suspend fun acceptCall() {
        val koin = KoinPlatform.getKoin()
        val messagingRepo = koin.get<MessagingRepository>()
        val apiClient = koin.get<ApiClient>()
        val callManager = koin.get<CallManager>()
        runCatching { messagingRepo.ensureConnected(apiClient.client) }
        runCatching { callManager.accept() }
    }

    /** CXProviderDelegate's CXEndCallAction handler -- covers both "decline
     * a still-ringing call" and "hang up a connected one" since CallKit
     * only exposes one End action for both; CallManager itself already
     * routes correctly based on its current phase. */
    fun endCall() {
        val callManager = KoinPlatform.getKoin().get<CallManager>()
        if (callManager.view.value?.phase == CallPhase.INCOMING) {
            callManager.decline()
        } else {
            callManager.hangup()
        }
    }

    /** Registers a freshly-issued platform push token -- "apns" (regular
     * remote notification token, from
     * UIApplication.registerForRemoteNotifications) or "apns_voip" (PushKit
     * token, from PKPushRegistry.pushTokenForType(.voIP)). See
     * push_service.py's _attempt_push routing for why these are two
     * separate DeviceToken rows, not one. */
    suspend fun registerPushToken(platform: String, tokenHex: String) {
        val registrar = KoinPlatform.getKoin().get<PushTokenRegistrar>()
        registrar.registerCurrentToken(platform, tokenHex)
    }
}

private fun mediaPreviewLabel(media: MediaMeta): String = when (media.kind) {
    "image" -> "📷 Photo"
    "video" -> "🎥 Video"
    "audio" -> "🎤 Voice message"
    else -> "📎 Attachment"
}
