package com.d2m.app.messaging.call

import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.d2m.app.messaging.IceServer
import com.d2m.app.messaging.protocol.IceCandidateData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Real Android WebRTC binding using `io.getstream:stream-webrtc-android`
 * (GetStream's Maven Central republish of Google's official prebuilt WebRTC
 * AAR under the same `org.webrtc.*` package/API -- Google itself stopped
 * publishing `org.webrtc:google-webrtc` to Maven Central, this is the
 * standard current replacement). Unified Plan throughout (addTrack-driven
 * transceivers, no manual SDP munging).
 *
 * Least certain part of this file: the exact `PeerConnection.Observer`
 * method set and `IceServer.builder(...)` overloads are written against
 * this SDK's long-stable, widely-documented API shape from memory -- this
 * has NOT been compiled (no Android toolchain in this sandbox, same
 * constraint as the rest of this project). If Android Studio reports a
 * signature mismatch here, it's almost certainly a small one (a renamed
 * observer callback or builder overload), not a design problem -- send the
 * error and it's a quick fix.
 */
/** Set by D2MApplication.onCreate(), same pattern as AndroidSettingsContextHolder in data/session/Settings.android.kt -- keeps commonMain platform-agnostic (AppModule.kt only ever calls the expect createWebRtcEngine() factory, never this class directly). */
object AndroidWebRtcContextHolder {
    lateinit var appContext: Context
}

actual fun createWebRtcEngine(): WebRtcEngine = AndroidWebRtcEngine(AndroidWebRtcContextHolder.appContext)

class AndroidWebRtcEngine(private val appContext: Context) : WebRtcEngine {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var eglBase: EglBase? = null
    private var factory: PeerConnectionFactory? = null
    private val pcObservers = mutableMapOf<PeerConnection, AndroidPcObserver>()
    private val cameraCapturers = mutableMapOf<VideoTrack, CameraBundle>()

    private data class CameraBundle(val capturer: CameraVideoCapturer, val source: VideoSource, val helper: SurfaceTextureHelper)

    private fun ensureFactory(): PeerConnectionFactory {
        factory?.let { return it }
        val egl = EglBase.create().also { eglBase = it }
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext).createInitializationOptions(),
        )
        val f = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(egl.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl.eglBaseContext))
            .createPeerConnectionFactory()
        factory = f
        return f
    }

    fun eglContext(): EglBase.Context? = eglBase?.eglBaseContext

    override suspend fun createPeerConnection(iceServers: List<IceServer>, observer: PeerConnectionObserver): Any {
        val f = ensureFactory()
        val rtcIceServers = iceServers.mapNotNull { s ->
            if (s.urls.isEmpty()) return@mapNotNull null
            // builder(List<String>) sets urls directly -- the single-String
            // builder(String) + chained .setUrls() combo doesn't exist on
            // this SDK's IceServer.Builder (compile error reported against
            // the first version of this file); this List overload is the
            // one that's actually there.
            val b: PeerConnection.IceServer.Builder = PeerConnection.IceServer.builder(s.urls)
            s.username?.let { b.setUsername(it) }
            s.credential?.let { b.setPassword(it) }
            b.createIceServer()
        }
        val rtcConfig = PeerConnection.RTCConfiguration(rtcIceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val bridged = AndroidPcObserver(observer, engineScope)
        val pc = f.createPeerConnection(rtcConfig, bridged) ?: error("PeerConnectionFactory.createPeerConnection returned null")
        pcObservers[pc] = bridged
        return pc
    }

    override suspend fun acquireMicrophone(): Any {
        val f = ensureFactory()
        val source = f.createAudioSource(MediaConstraints())
        return f.createAudioTrack("mic-${System.nanoTime()}", source)
    }

    override suspend fun acquireCamera(facing: CameraFacing): Any {
        val f = ensureFactory()
        val egl = eglBase ?: error("EGL context not initialized")
        val enumerator = Camera2Enumerator(appContext)
        val wantFront = facing == CameraFacing.FRONT
        val deviceName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) == wantFront }
            ?: enumerator.deviceNames.firstOrNull()
            ?: error("No camera available")
        val capturer = enumerator.createCapturer(deviceName, null) as CameraVideoCapturer
        val helper = SurfaceTextureHelper.create("CaptureThread-${System.nanoTime()}", egl.eglBaseContext)
        val source = f.createVideoSource(false)
        capturer.initialize(helper, appContext, source.capturerObserver)
        capturer.startCapture(1280, 720, 30)
        val track = f.createVideoTrack("camera-${System.nanoTime()}", source)
        cameraCapturers[track] = CameraBundle(capturer, source, helper)
        return track
    }

    override fun setTrackEnabled(track: Any, enabled: Boolean) {
        when (track) {
            is AudioTrack -> track.setEnabled(enabled)
            is VideoTrack -> track.setEnabled(enabled)
        }
    }

    override fun stopTrack(track: Any) {
        when (track) {
            is AudioTrack -> track.dispose()
            is VideoTrack -> {
                val bundle = cameraCapturers.remove(track)
                if (bundle != null) {
                    runCatching { bundle.capturer.stopCapture() }
                    bundle.capturer.dispose()
                    bundle.source.dispose()
                    bundle.helper.dispose()
                }
                track.dispose()
            }
        }
    }

    override fun addAudioTrack(pc: Any, track: Any, streamId: String): Any =
        (pc as PeerConnection).addTrack(track as AudioTrack, listOf(streamId))

    override fun addVideoTrack(pc: Any, track: Any, streamId: String): Any =
        (pc as PeerConnection).addTrack(track as VideoTrack, listOf(streamId))

    override fun replaceVideoTrack(sender: Any, track: Any?) {
        (sender as RtpSender).setTrack(track as? VideoTrack, false)
    }

    override suspend fun createOffer(pc: Any, iceRestart: Boolean): String = suspendCancellableCoroutine { cont ->
        val constraints = MediaConstraints().apply {
            if (iceRestart) mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
        }
        (pc as PeerConnection).createOffer(createSdpObserver(cont), constraints)
    }

    override suspend fun createAnswer(pc: Any): String = suspendCancellableCoroutine { cont ->
        (pc as PeerConnection).createAnswer(createSdpObserver(cont), MediaConstraints())
    }

    override suspend fun setLocalDescription(pc: Any, type: String, sdp: String): Unit = suspendCancellableCoroutine { cont ->
        val desc = SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp)
        (pc as PeerConnection).setLocalDescription(setSdpObserver(cont), desc)
    }

    override suspend fun setRemoteDescription(pc: Any, type: String, sdp: String): Unit = suspendCancellableCoroutine { cont ->
        val desc = SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp)
        (pc as PeerConnection).setRemoteDescription(setSdpObserver(cont), desc)
    }

    override suspend fun addIceCandidate(pc: Any, candidate: IceCandidateData) {
        (pc as PeerConnection).addIceCandidate(
            IceCandidate(candidate.sdpMid.orEmpty(), candidate.sdpMLineIndex ?: 0, candidate.candidate),
        )
    }

    override suspend fun waitForIceGatheringComplete(pc: Any, timeoutMs: Long) {
        val p = pc as PeerConnection
        if (p.iceGatheringState() == PeerConnection.IceGatheringState.COMPLETE) return
        val deferred = pcObservers[p]?.iceGatheringComplete ?: return
        withTimeoutOrNull(timeoutMs) { deferred.await() }
    }

    override fun localDescriptionSdp(pc: Any): String? = (pc as PeerConnection).localDescription?.description

    override fun isSignalingStable(pc: Any): Boolean = (pc as PeerConnection).signalingState() == PeerConnection.SignalingState.STABLE

    override fun closePeerConnection(pc: Any) {
        val p = pc as PeerConnection
        pcObservers.remove(p)
        runCatching { p.close() }
        runCatching { p.dispose() }
    }

    // SdpObserver has one interface for both create and set callbacks; each
    // of these two helpers only resumes on the pair it's actually used for,
    // the other pair is unreachable no-ops (still must be implemented --
    // SdpObserver isn't split into two interfaces upstream).
    private fun createSdpObserver(cont: kotlin.coroutines.Continuation<String>) = object : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) { cont.resume(desc?.description.orEmpty()) }
        override fun onCreateFailure(error: String?) { cont.resumeWithException(Exception(error ?: "createOffer/createAnswer failed")) }
        override fun onSetSuccess() {}
        override fun onSetFailure(error: String?) {}
    }

    private fun setSdpObserver(cont: kotlin.coroutines.Continuation<Unit>) = object : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {}
        override fun onCreateFailure(error: String?) {}
        override fun onSetSuccess() { cont.resume(Unit) }
        override fun onSetFailure(error: String?) { cont.resumeWithException(Exception(error ?: "setLocalDescription/setRemoteDescription failed")) }
    }
}

/** Bridges org.webrtc.PeerConnection.Observer (fires on WebRTC's own signaling thread) onto `observer`'s methods via `scope.launch` -- see PeerConnectionObserver's threading-contract doc comment in WebRtcEngine.kt. */
private class AndroidPcObserver(private val observer: PeerConnectionObserver, private val scope: CoroutineScope) : PeerConnection.Observer {
    val iceGatheringComplete = CompletableDeferred<Unit>()
    private val trackRoleByMsid = mutableMapOf<String, String>()

    override fun onIceCandidate(candidate: IceCandidate) {
        scope.launch {
            observer.onIceCandidate(IceCandidateData(candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex))
        }
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        scope.launch {
            when (newState) {
                PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> observer.onConnected()
                PeerConnection.IceConnectionState.FAILED -> observer.onFailed()
                PeerConnection.IceConnectionState.CLOSED -> observer.onClosed()
                else -> Unit
            }
        }
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
        if (newState == PeerConnection.IceGatheringState.COMPLETE) iceGatheringComplete.complete(Unit)
    }

    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        val track = receiver?.track() ?: return
        val msid = mediaStreams?.firstOrNull()?.id ?: "camera"
        scope.launch {
            when (track) {
                is AudioTrack -> observer.onRemoteAudioTrackAdded()
                is VideoTrack -> observer.onRemoteVideoTrackAdded(msid, track)
            }
        }
    }

    override fun onRemoveStream(stream: MediaStream?) {
        val id = stream?.id ?: return
        scope.launch { observer.onRemoteTrackRemoved(id) }
    }

    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
    override fun onIceConnectionReceivingChange(receiving: Boolean) {}
    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
    override fun onAddStream(stream: MediaStream?) {}
    override fun onDataChannel(channel: org.webrtc.DataChannel?) {}
    override fun onRenegotiationNeeded() {}
}

@Composable
actual fun VideoRendererView(track: Any?, mirror: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val engine = remember { org.koin.core.context.GlobalContext.get().get<WebRtcEngine>() as? AndroidWebRtcEngine }
    val initialized = remember { androidx.compose.runtime.mutableStateOf(false) }
    val renderer = remember {
        SurfaceViewRenderer(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    DisposableEffect(Unit) {
        val eglContext = engine?.eglContext()
        if (eglContext != null && !initialized.value) {
            renderer.init(eglContext, null)
            initialized.value = true
        }
        onDispose { if (initialized.value) runCatching { renderer.release() } }
    }

    DisposableEffect(track, mirror) {
        val videoTrack = track as? VideoTrack
        renderer.setMirror(mirror)
        videoTrack?.addSink(renderer)
        onDispose { videoTrack?.removeSink(renderer) }
    }

    AndroidView(factory = { renderer }, modifier = modifier)
}
