package com.d2m.app.messaging.call

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.d2m.app.messaging.IceServer
import com.d2m.app.messaging.protocol.IceCandidateData

/**
 * NOT IMPLEMENTED. Audio/video calling on iOS needs a real WebRTC binding
 * (the standard path is Google's `GoogleWebRTC` CocoaPod, wired through
 * Kotlin/Native's CocoaPods Gradle plugin -- `pod("GoogleWebRTC")` inside a
 * `cocoapods {}` block in composeApp/build.gradle.kts, which auto-generates
 * Kotlin bindings for the pod's Objective-C classes: RTCPeerConnectionFactory,
 * RTCPeerConnection, RTCAudioTrack/RTCVideoTrack, RTCCameraVideoCapturer,
 * RTCMTLVideoView, etc. -- the same object model AndroidWebRtcEngine already
 * implements against `org.webrtc`, since both bindings wrap the same
 * libwebrtc core almost 1:1).
 *
 * Deliberately not wired up in this pass: `iosApp/` has no `.xcodeproj` yet
 * (see iosApp/README.md -- this sandbox has no Apple toolchain to generate
 * or verify one, and none has been created in Xcode yet either), so there is
 * no CocoaPods-integrated Xcode project for a Podfile/pod install step to
 * target. Bolting a `cocoapods {}` block onto the shared Gradle module's iOS
 * framework config now -- unverifiable here, and touching the same
 * framework-export config every other iOS build output depends on -- risked
 * breaking iOS buildability entirely for a platform this project isn't
 * building on yet (every screenshot/build report in this project's history
 * has been from Android Studio). Once an Xcode project exists: add the
 * cocoapods plugin + Podfile + `pod("GoogleWebRTC")`, then port
 * WebRtcEngine.android.kt's method bodies to the equivalent RTCXxx Kotlin
 * bindings -- CallManager.kt and everything else in messaging/ needs zero
 * changes, this file is the entire remaining seam.
 *
 * Every method below throws so a call attempt fails loudly (CallManager
 * catches this and surfaces it via CallManager.errors) instead of silently
 * doing nothing.
 */
private fun notImplemented(): Nothing =
    throw NotImplementedError("Calling isn't wired up on iOS yet -- see messaging/call/WebRtcEngine.ios.kt")

actual fun createWebRtcEngine(): WebRtcEngine = object : WebRtcEngine {
    override suspend fun createPeerConnection(iceServers: List<IceServer>, observer: PeerConnectionObserver): Any = notImplemented()
    override suspend fun acquireMicrophone(): Any = notImplemented()
    override suspend fun acquireCamera(facing: CameraFacing): Any = notImplemented()
    override fun setTrackEnabled(track: Any, enabled: Boolean) = notImplemented()
    override fun stopTrack(track: Any) = notImplemented()
    override fun addAudioTrack(pc: Any, track: Any, streamId: String): Any = notImplemented()
    override fun addVideoTrack(pc: Any, track: Any, streamId: String): Any = notImplemented()
    override fun replaceVideoTrack(sender: Any, track: Any?) = notImplemented()
    override suspend fun createOffer(pc: Any, iceRestart: Boolean): String = notImplemented()
    override suspend fun createAnswer(pc: Any): String = notImplemented()
    override suspend fun setLocalDescription(pc: Any, type: String, sdp: String) = notImplemented()
    override suspend fun setRemoteDescription(pc: Any, type: String, sdp: String) = notImplemented()
    override suspend fun addIceCandidate(pc: Any, candidate: IceCandidateData) = notImplemented()
    override suspend fun waitForIceGatheringComplete(pc: Any, timeoutMs: Long) = notImplemented()
    override fun localDescriptionSdp(pc: Any): String? = notImplemented()
    override fun isSignalingStable(pc: Any): Boolean = notImplemented()
    override fun closePeerConnection(pc: Any) = notImplemented()
}

@Composable
actual fun VideoRendererView(track: Any?, mirror: Boolean, modifier: Modifier) {
    // Placeholder until the real RTCMTLVideoView binding lands (see this
    // file's doc comment) -- an empty box rather than a crash, so the call
    // UI (CallOverlay.kt) still renders its controls/labels correctly on iOS
    // even though no video frame is ever shown.
    Box(modifier = modifier)
}
