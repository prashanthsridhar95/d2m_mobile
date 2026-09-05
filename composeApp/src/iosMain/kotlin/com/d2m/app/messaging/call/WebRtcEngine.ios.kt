package com.d2m.app.messaging.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.d2m.app.messaging.IceServer
import com.d2m.app.messaging.protocol.IceCandidateData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import platform.AVFoundation.AVCaptureDevice
// AVCaptureDevicePosition is a plain NS_ENUM(NSInteger, ...), same binding
// pattern confirmed via klib dump-metadata for CXHandleType: a bare
// `typealias AVCaptureDevicePosition = Long` plus top-level const vals
// (AVCaptureDevicePositionFront/Back/Unspecified), not a Kotlin enum class
// with nested cases.
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.position
import kotlinx.cinterop.useContents
import platform.Foundation.NSError
// timeIntervalSince1970 is a package-level cinterop extension property (same
// pattern as AVFoundation.position above, and confirmed by klib dump-metadata
// showing it's declared as `fun platform/Foundation/NSDate.timeIntervalSince1970`
// rather than a true member) -- simply importing the NSDate class does NOT
// bring this into scope, it needs its own import.
import platform.Foundation.timeIntervalSince1970
import cocoapods.webrtc.RTCAudioTrack
import cocoapods.webrtc.RTCCameraVideoCapturer
import cocoapods.webrtc.RTCConfiguration
import cocoapods.webrtc.RTCDataChannel
import cocoapods.webrtc.RTCDefaultVideoDecoderFactory
import cocoapods.webrtc.RTCDefaultVideoEncoderFactory
import cocoapods.webrtc.RTCIceCandidate
import cocoapods.webrtc.RTCIceConnectionState
import cocoapods.webrtc.RTCIceGatheringState
import cocoapods.webrtc.RTCIceServer
import cocoapods.webrtc.RTCMediaConstraints
import cocoapods.webrtc.RTCMediaStream
import cocoapods.webrtc.RTCMediaStreamTrack
import cocoapods.webrtc.RTCMTLVideoView
import cocoapods.webrtc.RTCPeerConnection
import cocoapods.webrtc.RTCPeerConnectionDelegateProtocol
import cocoapods.webrtc.RTCPeerConnectionFactory
import cocoapods.webrtc.RTCRtpReceiver
import cocoapods.webrtc.RTCRtpSender
import cocoapods.webrtc.RTCSdpType
import cocoapods.webrtc.RTCSessionDescription
import cocoapods.webrtc.RTCSignalingState
import cocoapods.webrtc.RTCVideoTrack
// senderWithKind is likewise a package-level cinterop extension function on
// RTCPeerConnection, not a true class member -- same import-scoping issue.
import cocoapods.webrtc.senderWithKind
import cocoapods.webrtc.RTCInitializeSSL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.cinterop.ObjCSignatureOverride

/**
 * Real iOS WebRTC binding using the `GoogleWebRTC` CocoaPod (see
 * composeApp/build.gradle.kts's `cocoapods { pod("GoogleWebRTC") }` block
 * and iosApp/Podfile) -- Kotlin/Native's CocoaPods Gradle plugin
 * auto-generates a `cocoapods.webrtc.*` Kotlin binding for the pod's
 * Objective-C classes, the same object model AndroidWebRtcEngine already
 * implements against `org.webrtc` (both wrap the same libwebrtc core almost
 * 1:1 -- this file is a close method-for-method port of
 * WebRtcEngine.android.kt).
 *
 * NOT COMPILED. This sandbox has no Apple toolchain (see iosApp/README.md),
 * so this is written from the GoogleWebRTC pod's long-stable, widely
 * documented Objective-C API shape from memory, same precedent
 * WebRtcEngine.android.kt's own doc comment already sets for
 * stream-webrtc-android. The two spots most likely to need a small fix in
 * Xcode: (1) `RTCRtpParameters`/`RTCRtpEncodingParameters`'s exact bitrate
 * property names/types across pod versions, and (2)
 * `RTCCameraVideoCapturer.startCaptureWithDevice:format:fps:`'s exact
 * signature (some pod versions only expose the completion-handler variant).
 *
 * CallKit integration: does NOT call AVAudioSession.setActive itself
 * anywhere in this file, deliberately -- see WebRtcAudioSessionBridge.kt's
 * doc comment. `RTCAudioSession.sharedInstance().useManualAudio = true` is
 * set once in [ensureFactory], and [WebRtcAudioSessionBridge]'s callbacks
 * (wired in `setup()` below, driven by IosCallKitBridge.kt's
 * CXProviderDelegate) are the only place audio session (de)activation
 * happens, via `RTCAudioSession.sharedInstance().audioSessionDidActivate/
 * audioSessionDidDeactivate`.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun createWebRtcEngine(): WebRtcEngine = IosWebRtcEngine.also { it.setup() }

@OptIn(ExperimentalForeignApi::class)
object IosWebRtcEngine : WebRtcEngine {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var factory: RTCPeerConnectionFactory? = null
    private val pcObservers = mutableMapOf<RTCPeerConnection, IosPcObserver>()
    private val cameraCapturers = mutableMapOf<RTCVideoTrack, RTCCameraVideoCapturer>()

    fun setup() {
        WebRtcAudioSessionBridge.onAudioSessionActivated = {
            // platform.AVFAudio, not platform.AVFoundation -- confirmed
            // directly against Xcode 26.5's SDK (AVAudioSession.h now lives
            // in its own AVFAudio.framework; Kotlin/Native's
            // platform.AVFoundation klib doesn't re-export it).
            runCatching { cocoapods.webrtc.RTCAudioSession.sharedInstance().audioSessionDidActivate(platform.AVFAudio.AVAudioSession.sharedInstance()) }
        }
        WebRtcAudioSessionBridge.onAudioSessionDeactivated = {
            runCatching { cocoapods.webrtc.RTCAudioSession.sharedInstance().audioSessionDidDeactivate(platform.AVFAudio.AVAudioSession.sharedInstance()) }
        }
    }

    private fun ensureFactory(): RTCPeerConnectionFactory {
        factory?.let { return it }
        RTCInitializeSSL()
        // CallKit owns the audio session lifecycle for a real phone-style
        // call -- see this file's own doc comment and
        // WebRtcAudioSessionBridge.kt. Without useManualAudio=true, WebRTC
        // activates/configures AVAudioSession itself the moment a track is
        // added, which fights with CallKit for the same hardware resource.
        runCatching {
            cocoapods.webrtc.RTCAudioSession.sharedInstance().useManualAudio = true
        }
        val encoderFactory = RTCDefaultVideoEncoderFactory()
        val decoderFactory = RTCDefaultVideoDecoderFactory()
        val f = RTCPeerConnectionFactory(encoderFactory = encoderFactory, decoderFactory = decoderFactory)
        factory = f
        return f
    }

    override suspend fun createPeerConnection(iceServers: List<IceServer>, observer: PeerConnectionObserver): Any {
        val f = ensureFactory()
        val rtcIceServers = iceServers.mapNotNull { s ->
            if (s.urls.isEmpty()) return@mapNotNull null
            RTCIceServer(uRLStrings = s.urls, username = s.username, credential = s.credential)
        }
        val config = RTCConfiguration().apply {
            // Bare `iceServers = rtcIceServers` here was resolving to this
            // FUNCTION's own `iceServers: List<IceServer>` parameter (a val,
            // and the app's own common IceServer type) rather than
            // RTCConfiguration's real, writable `iceServers: List<RTCIceServer>`
            // member -- confirmed directly by the compiler's own error
            // ("actual type is List<RTCIceServer>, but List<IceServer> was
            // expected" -- IceServer is never a WebRTC type, only this
            // file's own parameter name). Explicit `this.` forces resolution
            // to the RTCConfiguration receiver instead of the enclosing
            // function's parameter of the same name.
            this.iceServers = rtcIceServers
            sdpSemantics = cocoapods.webrtc.RTCSdpSemantics.RTCSdpSemanticsUnifiedPlan
        }
        val constraints = RTCMediaConstraints(mandatoryConstraints = emptyMap<Any?, Any?>(), optionalConstraints = emptyMap<Any?, Any?>())
        val bridged = IosPcObserver(observer, engineScope)
        val pc = f.peerConnectionWithConfiguration(config, constraints = constraints, delegate = bridged)
            ?: error("RTCPeerConnectionFactory.peerConnectionWithConfiguration returned null")
        pcObservers[pc] = bridged
        return pc
    }

    override suspend fun acquireMicrophone(): Any {
        val f = ensureFactory()
        val constraints = RTCMediaConstraints(mandatoryConstraints = emptyMap<Any?, Any?>(), optionalConstraints = emptyMap<Any?, Any?>())
        val source = f.audioSourceWithConstraints(constraints)
        return f.audioTrackWithSource(source, trackId = "mic-${platform.Foundation.NSDate().timeIntervalSince1970}")
    }

    override suspend fun acquireCamera(facing: CameraFacing): Any {
        val f = ensureFactory()
        val source = f.videoSource()
        val capturer = RTCCameraVideoCapturer(delegate = source)
        val wantPosition = if (facing == CameraFacing.FRONT) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack
        val devices = RTCCameraVideoCapturer.captureDevices().filterIsInstance<AVCaptureDevice>()
        val device = devices.firstOrNull { it.position == wantPosition } ?: devices.firstOrNull()
            ?: error("No camera available")
        val formats = RTCCameraVideoCapturer.supportedFormatsForDevice(device).filterIsInstance<platform.AVFoundation.AVCaptureDeviceFormat>()
        // Closest match to the 1280x720@30 Android targets -- picks the
        // smallest format whose dimensions are >= 1280x720 so quality
        // doesn't fall below Android's own floor; falls back to the
        // largest available format if the device can't do 720p at all
        // (older/front cameras on some devices top out lower).
        val chosen = formats.minByOrNull { fmt ->
            // CMVideoDimensions is a plain C struct, so a C function
            // returning it by value comes back as CValue<CMVideoDimensions>
            // in Kotlin/Native -- its fields aren't directly accessible on
            // that wrapper (confirmed: "Unresolved reference 'width'"), they
            // need .useContents { } to unwrap to the real struct instance.
            val (w, h) = platform.CoreMedia.CMVideoFormatDescriptionGetDimensions(fmt.formatDescription).useContents { width to height }
            val area = w.toLong() * h.toLong()
            if (w >= 1280 && h >= 720) area else Long.MAX_VALUE - area
        } ?: formats.lastOrNull() ?: error("Camera has no supported capture formats")

        capturer.startCaptureWithDevice(device, format = chosen, fps = 30)
        val track = f.videoTrackWithSource(source, trackId = "camera-${platform.Foundation.NSDate().timeIntervalSince1970}")
        cameraCapturers[track] = capturer
        return track
    }

    override fun setTrackEnabled(track: Any, enabled: Boolean) {
        (track as? RTCMediaStreamTrack)?.isEnabled = enabled
    }

    override fun stopTrack(track: Any) {
        when (track) {
            is RTCVideoTrack -> {
                val capturer = cameraCapturers.remove(track)
                capturer?.let { runCatching { it.stopCapture() } }
            }
            is RTCAudioTrack -> Unit // No explicit dispose call on RTCAudioTrack in this API -- released when the factory/source drop their last reference.
        }
    }

    override fun addAudioTrack(pc: Any, track: Any, streamId: String): Any {
        val sender = (pc as RTCPeerConnection).senderWithKind("audio", streamId = streamId)
        sender.track = track as RTCAudioTrack
        runCatching {
            val params = sender.parameters
            params.encodings.filterIsInstance<cocoapods.webrtc.RTCRtpEncodingParameters>().forEach { it.maxBitrateBps = platform.Foundation.NSNumber(int = AUDIO_MAX_BITRATE_BPS) }
            sender.parameters = params
        }
        return sender
    }

    override fun addVideoTrack(pc: Any, track: Any, streamId: String): Any {
        val sender = (pc as RTCPeerConnection).senderWithKind("video", streamId = streamId)
        sender.track = track as RTCVideoTrack
        runCatching {
            val params = sender.parameters
            params.encodings.filterIsInstance<cocoapods.webrtc.RTCRtpEncodingParameters>().forEach {
                // maxBitrateBps/minBitrateBps are `NSNumber *` (nullable
                // boxed number) in the real header -- Kotlin/Native doesn't
                // auto-box Int to NSNumber on assignment (confirmed:
                // "actual type is Int, but NSNumber? was expected"), needs
                // the explicit NSNumber(int = ...) constructor (bound from
                // ObjC's -initWithInt:).
                it.maxBitrateBps = platform.Foundation.NSNumber(int = VIDEO_MAX_BITRATE_BPS)
                it.minBitrateBps = platform.Foundation.NSNumber(int = VIDEO_MIN_BITRATE_BPS)
            }
            sender.parameters = params
        }
        return sender
    }

    override fun replaceVideoTrack(sender: Any, track: Any?) {
        (sender as RTCRtpSender).track = track as? RTCVideoTrack
    }

    override suspend fun createOffer(pc: Any, iceRestart: Boolean): String = suspendCancellableCoroutine { cont ->
        val constraints = RTCMediaConstraints(
            mandatoryConstraints = if (iceRestart) mapOf("IceRestart" to "true") else emptyMap<Any?, Any?>(),
            optionalConstraints = emptyMap<Any?, Any?>(),
        )
        (pc as RTCPeerConnection).offerForConstraints(constraints) { desc: RTCSessionDescription?, error: NSError? ->
            if (error != null || desc == null) {
                cont.resumeWithException(Exception(error?.localizedDescription ?: "createOffer failed"))
            } else {
                cont.resume(desc.sdp)
            }
        }
    }

    override suspend fun createAnswer(pc: Any): String = suspendCancellableCoroutine { cont ->
        val constraints = RTCMediaConstraints(mandatoryConstraints = emptyMap<Any?, Any?>(), optionalConstraints = emptyMap<Any?, Any?>())
        (pc as RTCPeerConnection).answerForConstraints(constraints) { desc: RTCSessionDescription?, error: NSError? ->
            if (error != null || desc == null) {
                cont.resumeWithException(Exception(error?.localizedDescription ?: "createAnswer failed"))
            } else {
                cont.resume(desc.sdp)
            }
        }
    }

    override suspend fun setLocalDescription(pc: Any, type: String, sdp: String): Unit = suspendCancellableCoroutine { cont ->
        val desc = RTCSessionDescription(type = sdpTypeFromString(type), sdp = sdp)
        (pc as RTCPeerConnection).setLocalDescription(desc) { error: NSError? ->
            if (error != null) cont.resumeWithException(Exception(error.localizedDescription)) else cont.resume(Unit)
        }
    }

    override suspend fun setRemoteDescription(pc: Any, type: String, sdp: String): Unit = suspendCancellableCoroutine { cont ->
        val desc = RTCSessionDescription(type = sdpTypeFromString(type), sdp = sdp)
        (pc as RTCPeerConnection).setRemoteDescription(desc) { error: NSError? ->
            if (error != null) cont.resumeWithException(Exception(error.localizedDescription)) else cont.resume(Unit)
        }
    }

    override suspend fun addIceCandidate(pc: Any, candidate: IceCandidateData) {
        val c = RTCIceCandidate(sdp = candidate.candidate, sdpMLineIndex = (candidate.sdpMLineIndex ?: 0), sdpMid = candidate.sdpMid)
        (pc as RTCPeerConnection).addIceCandidate(c)
    }

    override suspend fun waitForIceGatheringComplete(pc: Any, timeoutMs: Long) {
        val p = pc as RTCPeerConnection
        if (p.iceGatheringState == RTCIceGatheringState.RTCIceGatheringStateComplete) return
        val deferred = pcObservers[p]?.iceGatheringComplete ?: return
        withTimeoutOrNull(timeoutMs) { deferred.await() }
    }

    override fun localDescriptionSdp(pc: Any): String? = (pc as RTCPeerConnection).localDescription?.sdp

    override fun isSignalingStable(pc: Any): Boolean = (pc as RTCPeerConnection).signalingState == RTCSignalingState.RTCSignalingStateStable

    override fun closePeerConnection(pc: Any) {
        val p = pc as RTCPeerConnection
        pcObservers.remove(p)
        runCatching { p.close() }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun sdpTypeFromString(type: String): RTCSdpType = when (type) {
    "offer" -> RTCSdpType.RTCSdpTypeOffer
    "answer" -> RTCSdpType.RTCSdpTypeAnswer
    "pranswer" -> RTCSdpType.RTCSdpTypePrAnswer
    // No RTCSdpTypeRollback in this pod's RTCSdpType -- confirmed directly
    // against the real installed header (RTCSessionDescription.h): only
    // Offer/PrAnswer/Answer exist. This app's own signaling protocol never
    // actually sends "rollback" as a type string; falls through to the
    // Unknown-SDP-type error below like any other unexpected value would.
    else -> error("Unknown SDP type: $type")
}

/** Bridges cocoapods.webrtc.RTCPeerConnectionDelegate (fires on WebRTC's own signaling/callback thread/queue) onto `observer`'s methods via `scope.launch` -- same threading-contract reasoning as AndroidPcObserver in WebRtcEngine.android.kt. */
@OptIn(ExperimentalForeignApi::class)
private class IosPcObserver(
    private val observer: PeerConnectionObserver,
    private val scope: CoroutineScope,
) : platform.darwin.NSObject(), RTCPeerConnectionDelegateProtocol {
    val iceGatheringComplete = CompletableDeferred<Unit>()

    // RTCRtpReceiver has no streamIds property (confirmed directly against
    // the real RTCRtpReceiver.h header -- only receiverId/parameters/track/
    // delegate exist), and didRemoveReceiver: doesn't hand back the streams
    // list the way didAddReceiver:streams: does. Track the mapping ourselves
    // from the add side so removal can still report the right msid.
    private val receiverStreamIds = mutableMapOf<RTCRtpReceiver, String>()

    override fun peerConnection(peerConnection: RTCPeerConnection, didGenerateIceCandidate: RTCIceCandidate) {
        scope.launch {
            observer.onIceCandidate(IceCandidateData(didGenerateIceCandidate.sdp, didGenerateIceCandidate.sdpMid, didGenerateIceCandidate.sdpMLineIndex))
        }
    }

    override fun peerConnection(peerConnection: RTCPeerConnection, didChangeIceConnectionState: RTCIceConnectionState) {
        scope.launch {
            when (didChangeIceConnectionState) {
                RTCIceConnectionState.RTCIceConnectionStateConnected, RTCIceConnectionState.RTCIceConnectionStateCompleted -> observer.onConnected()
                RTCIceConnectionState.RTCIceConnectionStateFailed -> observer.onFailed()
                RTCIceConnectionState.RTCIceConnectionStateClosed -> observer.onClosed()
                else -> Unit
            }
        }
    }

    override fun peerConnection(peerConnection: RTCPeerConnection, didChangeIceGatheringState: RTCIceGatheringState) {
        if (didChangeIceGatheringState == RTCIceGatheringState.RTCIceGatheringStateComplete) iceGatheringComplete.complete(Unit)
    }

    override fun peerConnection(peerConnection: RTCPeerConnection, didAddReceiver: RTCRtpReceiver, streams: List<*>) {
        val track = didAddReceiver.track ?: return
        val msid = (streams.firstOrNull() as? RTCMediaStream)?.streamId ?: "camera"
        receiverStreamIds[didAddReceiver] = msid
        scope.launch {
            when (track) {
                is RTCAudioTrack -> observer.onRemoteAudioTrackAdded()
                is RTCVideoTrack -> observer.onRemoteVideoTrackAdded(msid, track)
            }
        }
    }

    override fun peerConnection(peerConnection: RTCPeerConnection, didRemoveReceiver: RTCRtpReceiver) {
        val msid = receiverStreamIds.remove(didRemoveReceiver) ?: return
        scope.launch { observer.onRemoteTrackRemoved(msid) }
    }

    override fun peerConnection(peerConnection: RTCPeerConnection, didChangeSignalingState: RTCSignalingState) {}
    // peerConnection:didAddStream: and peerConnection:didRemoveStream: are
    // two distinct required ObjC selectors that both bridge to the identical
    // Kotlin signature (RTCPeerConnection, RTCMediaStream) -> Unit -- without
    // @ObjCSignatureOverride, Kotlin sees this as one function declared
    // twice ("Conflicting overloads", confirmed directly). Annotating only
    // one member of the pair was NOT sufficient in practice -- both need it.
    @ObjCSignatureOverride
    override fun peerConnection(peerConnection: RTCPeerConnection, didAddStream: RTCMediaStream) {}
    @ObjCSignatureOverride
    override fun peerConnection(peerConnection: RTCPeerConnection, didRemoveStream: RTCMediaStream) {}
    override fun peerConnectionShouldNegotiate(peerConnection: RTCPeerConnection) {}
    override fun peerConnection(peerConnection: RTCPeerConnection, didRemoveIceCandidates: List<*>) {}
    override fun peerConnection(peerConnection: RTCPeerConnection, didOpenDataChannel: RTCDataChannel) {}
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoRendererView(track: Any?, mirror: Boolean, modifier: Modifier) {
    val videoView = remember { RTCMTLVideoView() }

    DisposableEffect(track) {
        val videoTrack = track as? RTCVideoTrack
        videoTrack?.addRenderer(videoView)
        onDispose { videoTrack?.removeRenderer(videoView) }
    }

    DisposableEffect(mirror) {
        // RTCMTLVideoView doesn't expose a direct "mirror" flag the way
        // Android's SurfaceViewRenderer.setMirror does -- flipping it via a
        // CATransform3D on the view's own layer is the standard workaround
        // (used by several open-source CallKit+WebRTC iOS apps). Purely a
        // local-preview presentation flip, same as Android -- never applied
        // to the outgoing wire track either way (see this function's own
        // commonMain doc comment).
        videoView.layer.setAffineTransform(
            // CGAffineTransformIdentity's own binding type turned out not to
            // unify with CGAffineTransformMakeScale's CValue<CGAffineTransform>
            // return type (the if/else fell back to a common type of `Any`,
            // confirmed directly: "actual type is Any, but
            // CValue<CGAffineTransform> was expected"). Scale(1,1) is the
            // identity transform mathematically, and guarantees the exact
            // same concrete type as the `if` branch, sidestepping whatever
            // CGAffineTransformIdentity's real type is.
            platform.CoreGraphics.CGAffineTransformMakeScale(if (mirror) -1.0 else 1.0, 1.0),
        )
        onDispose {}
    }

    UIKitView(
        factory = { videoView },
        modifier = modifier,
    )
}

private const val VIDEO_MAX_BITRATE_BPS = 2_500_000
private const val VIDEO_MIN_BITRATE_BPS = 200_000
private const val AUDIO_MAX_BITRATE_BPS = 64_000
