package com.d2m.app.push

import com.d2m.app.di.KoinBootstrap
import com.d2m.app.messaging.call.CallManager
import com.d2m.app.messaging.call.CallPhase
import com.d2m.app.messaging.call.WebRtcAudioSessionBridge
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import org.koin.mp.KoinPlatform
import platform.CallKit.CXAnswerCallAction
// CXHandleType and CXCallEndedReason are plain NS_ENUM(NSInteger, ...)
// declarations (not Objective-C classes), and CallKit binds these the same
// way klib dump-metadata proved for CXHandleType: as a bare `typealias X =
// Long` PLUS top-level `const val` constants -- NOT as a Kotlin enum class
// with nested cases. So these are bare top-level names, never
// `CXHandleType.CXHandleTypeGeneric` / `CXCallEndedReason.CXCallEndedReasonX`.
import platform.CallKit.CXCallEndedReasonFailed
import platform.CallKit.CXCallEndedReasonRemoteEnded
import platform.CallKit.CXCallUpdate
import platform.CallKit.CXEndCallAction
import platform.CallKit.CXHandle
import platform.CallKit.CXHandleTypeGeneric
import platform.CallKit.CXProvider
import platform.CallKit.CXProviderConfiguration
import platform.CallKit.CXProviderDelegateProtocol
import platform.CallKit.CXStartCallAction
import platform.CallKit.CXTransaction
import platform.CallKit.CXCallController
// CXProviderDelegateProtocol's didActivateAudioSession/didDeactivateAudioSession
// parameter type is NEITHER platform.AVFoundation.AVAudioSession NOR
// platform.AVFAudio.AVAudioSession -- confirmed directly via `klib
// dump-metadata` against the real CallKit platform klib: CallKit.h only
// forward-declares AVAudioSession (`@class AVAudioSession;`) without
// importing the framework that defines it, so Kotlin/Native's cinterop
// binds it as the opaque forward-declaration type objcnames.classes.AVAudioSession,
// distinct from either "real" AVAudioSession binding. This override never
// needs to call a real method on the parameter (WebRtcAudioSessionBridge's
// callbacks don't take it), so the opaque type is all that's needed here.
import objcnames.classes.AVAudioSession
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSUUID
import platform.PushKit.PKPushCredentials
import platform.PushKit.PKPushPayload
import platform.PushKit.PKPushRegistry
import platform.PushKit.PKPushRegistryDelegateProtocol
import platform.PushKit.PKPushTypeVoIP
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

/**
 * iOS's counterpart to LocalNotificationBridge.android.kt's incoming-call
 * handling PLUS D2MFirebaseMessagingService.handleIncomingCallPush -- there
 * is no Android-style "post a NotificationCompat.CallStyle notification and
 * loop a MediaPlayer ringtone" on iOS. Apple mandates a completely different
 * (and, once wired up, more capable -- native ringer, lock-screen full-
 * bleed UI, Bluetooth/CarPlay/Watch integration, Do Not Disturb "allow
 * calls from" rules all work automatically) mechanism for this exact use
 * case: PushKit delivers a VoIP push that wakes the app in the background
 * or from fully killed, and CallKit (CXProvider) is the ONLY sanctioned way
 * to show a ringing call UI in response -- there is no custom "look like a
 * call notification" option the way NotificationCompat.CallStyle is on
 * Android. Skipping CallKit and just showing a regular alert notification
 * for a VoIP push is explicitly against Apple's rules and risks the app
 * losing its VoIP background-wake entitlement entirely.
 *
 * `object` (process-wide singleton, same shape as LocalNotificationBridge's
 * installLocalNotificationBridge) -- [setup] must be called exactly once,
 * as early as possible in the app process's life (AppDelegate.
 * didFinishLaunchingWithOptions, BEFORE returning true) so PushKit can wake
 * this process for a VoIP push at all; registering the delegate late (e.g.
 * only once the user opens the call UI) misses the entire point.
 *
 * ARCHITECTURE NOTE on timing: Apple requires reportNewIncomingCallWithUUID
 * be called essentially immediately inside didReceiveIncomingPushWithPayload
 * (repeated/slow violations get the app's VoIP entitlement revoked) -- but
 * the REAL caller name and audio/video media type only become known after
 * this device decrypts the Signal-encrypted invite, which needs a live
 * socket round-trip (ensureConnected) that can take real time. The fix
 * (same one Apple's own docs recommend): report immediately with a
 * best-effort placeholder (the raw sender username, audio-only assumed),
 * THEN once IosPushBridge.handleIncomingCallPush resolves, upgrade the
 * already-visible call via CXProvider.reportCallWithUUID(_:updated:) with
 * the real name/hasVideo. The system call UI updates live in place --
 * nothing about this reads as broken/laggy to the person seeing it ring.
 *
 * KNOWN-RISKY FILE: written against CallKit/PushKit's Objective-C API
 * shapes from documentation/memory, not compiled or opened in Xcode (this
 * sandbox has no Apple toolchain at all -- see iosApp/README.md). The
 * selector -> Kotlin/Native parameter-name mapping for multi-argument
 * delegate methods (e.g. exactly how `pushRegistry:didReceiveIncomingPushWithPayload:forType:withCompletionHandler:`
 * turns into a Kotlin function signature) is the single most likely source
 * of a real compile error once this is actually opened in Xcode -- check
 * Xcode's own generated header / autocomplete against the method
 * signatures below first if it doesn't compile as-is.
 */
object IosCallKitBridge {
    private lateinit var provider: CXProvider
    private val callController = CXCallController()
    private val pushRegistry = PKPushRegistry(dispatch_get_main_queue())

    /** callId (CallManager's own string id) <-> the NSUUID CallKit needs -- CallManager was never designed to know about CallKit's UUID requirement, so this bridge owns the mapping instead of leaking it back into commonMain. */
    private val uuidByCallId = mutableMapOf<String, NSUUID>()
    private var outgoingReported = false

    fun setup() {
        KoinBootstrap.ensureStarted()

        val config = CXProviderConfiguration("D2M").apply {
            supportsVideo = true
            // NSUInteger-backed properties bridge to ULong in Kotlin/Native,
            // not Long -- confirmed directly ("Assignment type mismatch:
            // actual type is 'Long', but 'ULong' was expected").
            maximumCallGroups = 1UL
            maximumCallsPerCallGroup = 1UL
            supportedHandleTypes = setOf(CXHandleTypeGeneric)
        }
        provider = CXProvider(config)
        provider.setDelegate(providerDelegate, dispatch_get_main_queue())

        pushRegistry.delegate = pushRegistryDelegate
        pushRegistry.desiredPushTypes = setOf(PKPushTypeVoIP)

        // Auto-end the native call UI when CallManager tears down for a
        // reason OTHER than this bridge's own CXEndCallAction handler (the
        // caller hanging up first, a ring timeout, a signaling failure) --
        // without this, the system call screen would keep ringing forever
        // with no way to dismiss it short of the OS's own stale-call
        // timeout. Mirrors LocalNotificationBridge.android.kt's
        // `else if (lastNotifiedCallId != null) notificationManager.cancel(...)`
        // branch.
        CoroutineScope(Dispatchers.Default).launch {
            val callManager = KoinPlatform.getKoin().get<CallManager>()
            var lastActiveCallId: String? = null
            callManager.view.collect { view ->
                if (view != null && view.phase != CallPhase.ENDED) {
                    lastActiveCallId = view.callId
                    if (view.phase == CallPhase.CALLING && !view.incoming && !outgoingReported) {
                        reportOutgoingCallStarted(view.callId)
                    }
                    if (view.phase == CallPhase.IN_CALL && !view.incoming) {
                        reportOutgoingCallConnected(view.callId)
                    }
                } else if (lastActiveCallId != null) {
                    val id = lastActiveCallId!!
                    lastActiveCallId = null
                    outgoingReported = false
                    val uuid = uuidByCallId.remove(id)
                    if (uuid != null) {
                        provider.reportCallWithUUID(uuid, NSDate(), CXCallEndedReasonRemoteEnded)
                    }
                }
            }
        }
    }

    /** CallLayer.kt's outgoing-call path (CallManager.startCall) has no
     * CallKit involvement today -- this reports it retroactively so
     * Bluetooth/CarPlay/lock-screen controls work for outgoing calls too,
     * not just incoming ones. Best-effort: a failure here doesn't block the
     * call itself, it only means the system call UI won't show it. */
    private fun reportOutgoingCallStarted(callId: String) {
        outgoingReported = true
        val uuid = NSUUID()
        uuidByCallId[callId] = uuid
        val callManager = KoinPlatform.getKoin().get<CallManager>()
        val view = callManager.view.value ?: return
        val startAction = CXStartCallAction(uuid, CXHandle(CXHandleTypeGeneric, view.peerUsername))
        startAction.video = view.media == "video"
        callController.requestTransaction(CXTransaction(startAction)) { _: NSError? -> }
    }

    private fun reportOutgoingCallConnected(callId: String) {
        val uuid = uuidByCallId[callId] ?: return
        // CXProvider declares TWO reportOutgoingCallWithUUID: overloads
        // (startedConnectingAtDate: and connectedAtDate:) that both bridge to
        // (NSUUID, NSDate) -- confirmed directly against the real header,
        // which is why the unlabeled two-arg call was ambiguous. Naming the
        // date argument picks the "connected" overload, matching what this
        // call site actually means (CallPhase.IN_CALL).
        provider.reportOutgoingCallWithUUID(uuid, connectedAtDate = NSDate())
    }

    private val pushRegistryDelegate = object : NSObject(), PKPushRegistryDelegateProtocol {
        override fun pushRegistry(registry: PKPushRegistry, didUpdatePushCredentials: PKPushCredentials, forType: platform.PushKit.PKPushType) {
            val tokenHex = didUpdatePushCredentials.token.toHexString()
            CoroutineScope(Dispatchers.Default).launch {
                runCatching { IosPushBridge.registerPushToken("apns_voip", tokenHex) }
            }
        }

        override fun pushRegistry(registry: PKPushRegistry, didInvalidatePushTokenForType: platform.PushKit.PKPushType) {
            // Nothing registered server-side needs active invalidation here --
            // the next real call push attempt against a now-dead token will
            // surface as a normal APNs 410/BadDeviceToken, which
            // push_service.py already treats as "delete this DeviceToken
            // row" (see BadApnsDeviceToken). A new token (if the OS issues
            // one) arrives via didUpdatePushCredentials above regardless.
        }

        override fun pushRegistry(
            registry: PKPushRegistry,
            didReceiveIncomingPushWithPayload: PKPushPayload,
            forType: platform.PushKit.PKPushType,
            withCompletionHandler: () -> Unit,
        ) {
            KoinBootstrap.ensureStarted()
            val raw = didReceiveIncomingPushWithPayload.dictionaryPayload
            val data = mutableMapOf<String, String>()
            raw.forEach { (k, v) -> if (k != null && v != null) data[k.toString()] = v.toString() }

            val sender = data["sender"]
            val uuid = NSUUID()
            if (sender == null) {
                // Nothing usable at all -- still MUST report a call (Apple
                // requirement) or risk this app's VoIP entitlement, even
                // though we have zero identifying info for it. Reported as
                // already-ended immediately after so it doesn't sit there
                // ringing forever with no way for CallManager to ever
                // resolve it -- same "never a broken/dead notification"
                // principle Android's fallback paths follow.
                provider.reportNewIncomingCallWithUUID(uuid, CXCallUpdate().apply { localizedCallerName = "Unknown caller" }) { _ ->
                    provider.reportCallWithUUID(uuid, NSDate(), CXCallEndedReasonFailed)
                    withCompletionHandler()
                }
                return
            }

            val placeholderUpdate = CXCallUpdate().apply {
                remoteHandle = CXHandle(CXHandleTypeGeneric, sender)
                localizedCallerName = sender
                hasVideo = false
                supportsHolding = false
                supportsGrouping = false
                supportsUngrouping = false
                supportsDTMF = false
            }
            provider.reportNewIncomingCallWithUUID(uuid, placeholderUpdate) { error: NSError? ->
                if (error != null) {
                    withCompletionHandler()
                    return@reportNewIncomingCallWithUUID
                }
                uuidByCallId[sender] = uuid // provisional key, replaced by the real callId below once known

                CoroutineScope(Dispatchers.Default).launch {
                    val result = runCatching {
                        IosPushBridge.handleIncomingCallPush(
                            sender = sender,
                            messageId = data["messageId"],
                            ciphertextType = data["ciphertextType"],
                            ciphertext = data["ciphertext"],
                            sentAt = data["sentAt"]?.toLongOrNull(),
                        )
                    }.getOrNull()

                    uuidByCallId.remove(sender)
                    if (result != null) {
                        uuidByCallId[result.callId] = uuid
                        val update = CXCallUpdate().apply {
                            remoteHandle = CXHandle(CXHandleTypeGeneric, result.callerUsername)
                            localizedCallerName = result.callerDisplayName
                            hasVideo = result.isVideo
                            supportsHolding = false
                            supportsGrouping = false
                            supportsUngrouping = false
                            supportsDTMF = false
                        }
                        provider.reportCallWithUUID(uuid, update)
                    } else {
                        // Never resolved to a live CallManager invite (SDP
                        // too large for even the live-drain path within the
                        // timeout, decrypt failure, invite already gone
                        // stale) -- end the placeholder call rather than
                        // leaving it ringing with dead Answer/Decline
                        // buttons.
                        provider.reportCallWithUUID(uuid, NSDate(), CXCallEndedReasonFailed)
                    }
                    withCompletionHandler()
                }
            }
        }
    }

    private val providerDelegate = object : NSObject(), CXProviderDelegateProtocol {
        override fun providerDidReset(provider: CXProvider) {
            KoinPlatform.getKoin().get<CallManager>().hangup()
            uuidByCallId.clear()
        }

        override fun provider(provider: CXProvider, performAnswerCallAction: CXAnswerCallAction) {
            CoroutineScope(Dispatchers.Default).launch {
                runCatching { IosPushBridge.acceptCall() }
                performAnswerCallAction.fulfill()
            }
        }

        override fun provider(provider: CXProvider, performEndCallAction: CXEndCallAction) {
            runCatching { IosPushBridge.endCall() }
            performEndCallAction.fulfill()
        }

        override fun provider(provider: CXProvider, performStartCallAction: CXStartCallAction) {
            // CallManager.startCall() already ran (it's what triggered
            // reportOutgoingCallStarted above in the first place) -- this
            // just satisfies CallKit's own transaction contract. This is the
            // call just beginning to connect (not yet connected), so this
            // uses the OTHER reportOutgoingCallWithUUID: overload from
            // reportOutgoingCallConnected() above (startedConnectingAtDate:
            // vs. connectedAtDate:) -- the unlabeled two-arg call is
            // ambiguous between the two, confirmed directly against the
            // real header.
            provider.reportOutgoingCallWithUUID(performStartCallAction.callUUID, startedConnectingAtDate = NSDate())
            performStartCallAction.fulfill()
        }

        // didActivateAudioSession/didDeactivateAudioSession both take the
        // opaque objcnames.classes.AVAudioSession forward-declaration type
        // (see the import comment above), which requires opting in to
        // ExperimentalForeignApi. They also collide under Kotlin's own
        // overload erasure (same param types, different ObjC selectors) the
        // same way didAddStream/didRemoveStream did in WebRtcEngine.ios.kt,
        // so one of the pair needs @ObjCSignatureOverride to coexist.
        @OptIn(ExperimentalForeignApi::class)
        @ObjCSignatureOverride
        override fun provider(provider: CXProvider, didActivateAudioSession: AVAudioSession) {
            WebRtcAudioSessionBridge.audioSessionDidActivate()
        }

        @OptIn(ExperimentalForeignApi::class)
        @ObjCSignatureOverride
        override fun provider(provider: CXProvider, didDeactivateAudioSession: AVAudioSession) {
            WebRtcAudioSessionBridge.audioSessionDidDeactivate()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toHexString(): String {
    val ptr = this.bytes?.reinterpret<ByteVar>() ?: return ""
    return (0 until this.length.toInt()).joinToString("") { i ->
        (ptr[i].toInt() and 0xFF).toString(16).padStart(2, '0')
    }
}
