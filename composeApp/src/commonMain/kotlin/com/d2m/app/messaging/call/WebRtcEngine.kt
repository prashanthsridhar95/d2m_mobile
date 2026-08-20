package com.d2m.app.messaging.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.d2m.app.messaging.IceServer
import com.d2m.app.messaging.protocol.IceCandidateData

enum class CameraFacing { FRONT, BACK }

/**
 * Abstracts the NATIVE WebRTC object model -- PeerConnection, RtpSender,
 * local/remote audio+video tracks -- which is what Android's `org.webrtc`
 * (via the `stream-webrtc-android` artifact, see WebRtcEngine.android.kt)
 * and iOS's GoogleWebRTC CocoaPod (see WebRtcEngine.ios.kt) both directly
 * expose: they're separate language bindings (Java/Kotlin, Objective-C)
 * generated from the exact same libwebrtc C++ core, so their APIs line up
 * almost 1:1 with each other -- unlike the *browser* DOM API
 * (RTCPeerConnection/MediaStream as exposed to JS) that
 * d2m_web/src/lib/messaging/callManager.ts was written against, which has
 * its own higher-level Promise-based shape. CallManager.kt is a port of
 * that same call state machine, driving both platforms through this one
 * seam instead.
 *
 * Track/sender/peer-connection handles below are deliberately typed `Any`
 * rather than separate `expect class` declarations per handle: CallManager
 * never inspects them, it only threads them back into this same interface's
 * other methods, so a marker-only expect/actual per handle type would be
 * pure ceremony for zero type safety benefit (the compiler can't check
 * cross-platform native WebRTC object identity anyway). Each platform
 * actual casts internally; a wrong cast surfaces immediately as a
 * ClassCastException at the call site, not a silent bug.
 *
 * Threading contract: CallManager is not internally synchronized (it's a
 * plain class with mutable `var` state, driven from one CoroutineScope on
 * Dispatchers.Default). Native WebRTC SDKs invoke their own callbacks on a
 * dedicated signaling/worker thread, not whatever dispatcher created the
 * PeerConnection -- WebRtcEngine actuals MUST hop every one of these
 * callbacks onto the same dispatcher CallManager runs on (e.g. wrap each
 * native callback registration in `scope.launch(Dispatchers.Default) { ... }`)
 * before invoking them here, rather than calling straight through from the
 * native thread. Both WebRtcEngine.android.kt and WebRtcEngine.ios.kt do
 * this at their observer-registration seam.
 */
interface PeerConnectionObserver {
    fun onIceCandidate(candidate: IceCandidateData)
    fun onConnected()
    fun onFailed()
    fun onClosed()
    fun onRemoteAudioTrackAdded()
    fun onRemoteVideoTrackAdded(msid: String, track: Any)
    fun onRemoteTrackRemoved(msid: String)
}

interface WebRtcEngine {
    suspend fun createPeerConnection(iceServers: List<IceServer>, observer: PeerConnectionObserver): Any

    suspend fun acquireMicrophone(): Any
    suspend fun acquireCamera(facing: CameraFacing): Any
    fun setTrackEnabled(track: Any, enabled: Boolean)
    fun stopTrack(track: Any)

    /** streamId tags the sender's MediaStream (msid) so the remote can classify inbound tracks via VideoMeta.roles -- always "camera" here (no screen-share support in this build). */
    fun addAudioTrack(pc: Any, track: Any, streamId: String): Any
    fun addVideoTrack(pc: Any, track: Any, streamId: String): Any
    fun replaceVideoTrack(sender: Any, track: Any?)

    suspend fun createOffer(pc: Any, iceRestart: Boolean = false): String
    suspend fun createAnswer(pc: Any): String
    suspend fun setLocalDescription(pc: Any, type: String, sdp: String)
    suspend fun setRemoteDescription(pc: Any, type: String, sdp: String)
    suspend fun addIceCandidate(pc: Any, candidate: IceCandidateData)
    suspend fun waitForIceGatheringComplete(pc: Any, timeoutMs: Long = 2500)
    fun localDescriptionSdp(pc: Any): String?
    fun isSignalingStable(pc: Any): Boolean
    fun closePeerConnection(pc: Any)
}

/** Platform factory -- see WebRtcEngine.android.kt (real, needs AndroidWebRtcContextHolder.appContext set by D2MApplication.onCreate) and WebRtcEngine.ios.kt (not implemented yet, see that file's doc comment) for the two actuals. */
expect fun createWebRtcEngine(): WebRtcEngine

/** Renders a local or remote video track handle (the same `Any` handles WebRtcEngine hands out) -- a tiny platform view (SurfaceViewRenderer on Android, RTCMTLVideoView on iOS) wrapped for Compose interop. `mirror` only affects the LOCAL preview's on-screen presentation (front-camera selfie-mirror UX); it is never applied to the outgoing wire track -- simpler than web's canvas-based mirror-on-the-wire trick and matches how native camera/phone call UIs normally behave. */
@Composable
expect fun VideoRendererView(track: Any?, mirror: Boolean, modifier: Modifier)
