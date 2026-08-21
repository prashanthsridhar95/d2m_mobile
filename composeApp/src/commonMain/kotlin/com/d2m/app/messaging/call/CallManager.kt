package com.d2m.app.messaging.call

import com.d2m.app.messaging.CallLogInfo
import com.d2m.app.messaging.ChatMessage
import com.d2m.app.messaging.MessageStatus
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.protocol.CallSignal
import com.d2m.app.messaging.protocol.IceCandidateData
import com.d2m.app.messaging.protocol.SdpDescription
import com.d2m.app.messaging.protocol.VideoMeta
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Direct Kotlin port of d2m_web/src/lib/messaging/callManager.ts's state
 * machine -- same phases, same perfect-negotiation renegotiation handling,
 * same 30s ring timeout, same call-log-on-teardown behavior -- driven
 * through WebRtcEngine.kt (native WebRTC object model) instead of the
 * browser DOM WebRTC API the original was written against. Deliberately
 * NOT ported from the web version: screen sharing (getDisplayMedia has no
 * direct mobile equivalent -- would need MediaProjection on Android and a
 * ReplayKit broadcast extension on iOS, both substantial separate features)
 * and the canvas-based "mirror what we SEND" trick (see
 * VideoRendererView's doc comment in WebRtcEngine.kt -- mirroring here is
 * local-preview-only). Device enumeration/explicit mic-switching UI is also
 * not ported (mobile's front/back `flipCamera()` covers the mobile-relevant
 * case; explicit device pickers are much more a desktop-multi-webcam
 * concern).
 *
 * Signaling travels over MessagingRepository.sendCallSignal (the same
 * Signal-encrypted channel as chat, tagged ephemeral) -- this class
 * registers itself as messagingRepository's callSignalHandler /
 * sessionResetHandler at construction time (see AppModule.kt's DI wiring),
 * mirroring how useMessaging.js wires `new CallManager({ send, ... })`
 * through its own WS/crypto plumbing.
 */
enum class CallPhase { IDLE, CALLING, INCOMING, CONNECTING, IN_CALL, ENDED }

data class CallView(
    val callId: String,
    val peerUsername: String,
    val incoming: Boolean,
    val media: String, // "audio" | "video"
    val phase: CallPhase,
    val ringing: Boolean,
    val micOn: Boolean,
    val remoteMicOn: Boolean,
    val camOn: Boolean,
    val hasRemoteVideo: Boolean,
    val remoteVideoCapable: Boolean,
    val startedAt: Long?,
    val cameraFacing: CameraFacing,
)

data class CallStreams(val localVideoTrack: Any? = null, val remoteVideoTrack: Any? = null)

enum class CallEndReason { ENDED, MISSED, NO_ANSWER, FAILED, BUSY, DECLINED }
data class CallEndInfo(val peerUsername: String, val incoming: Boolean, val media: String, val connected: Boolean, val durationSec: Int, val reason: CallEndReason)

private const val RING_TIMEOUT_MS = 30_000L
private val EMPTY_STREAMS = CallStreams()

class CallManager(
    private val messagingRepository: MessagingRepository,
    private val engine: WebRtcEngine,
) {
    /** Safety net matching MessagingRepository's own -- see that class's `exceptionHandler` doc comment. hangup()/decline()/mid-call-state updates below fire signals from bare `scope.launch { }` blocks with no local try/catch (call signaling is best-effort), so anything that still slips through needs somewhere to land besides "crash the app." */
    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        println("CallManager: uncaught coroutine exception (recovered, not fatal): $e")
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)

    private val _view = MutableStateFlow<CallView?>(null)
    val view: StateFlow<CallView?> = _view.asStateFlow()
    private val _streams = MutableStateFlow(EMPTY_STREAMS)
    val streams: StateFlow<CallStreams> = _streams.asStateFlow()
    private val _errors = MutableStateFlow<String?>(null)
    val errors: StateFlow<String?> = _errors.asStateFlow()

    /**
     * [errors] was previously never rendered anywhere and never cleared --
     * "Could not start call: ..." was set on genuine failures but the UI had
     * no idea it existed, so a failed call just silently vanished with zero
     * explanation (exactly the "calls not being sent" symptom this was
     * reported alongside). CallLayer.kt now observes [errors] and calls this
     * once it's finished showing the message.
     */
    fun clearError() {
        _errors.value = null
    }

    private var pc: Any? = null
    private var audioTrack: Any? = null
    private var cameraTrack: Any? = null
    private var audioSender: Any? = null
    private var camSender: Any? = null

    private val candidateQueue = mutableListOf<IceCandidateData>()
    private var pendingOfferSdp: String? = null
    private var ringJob: Job? = null

    private var callId = ""
    private var peer = ""
    private var incoming = false
    private var media = "audio"
    private var phase = CallPhase.IDLE
    private var ringing = false
    private var micOn = true
    private var remoteMicOn = true
    private var camOn = false
    private var facing = CameraFacing.FRONT
    private var hasRemoteVideo = false
    private var remoteVideoCapable = false
    private var remoteRoles = mutableMapOf<String, String>() // msid -> role, always "camera" in this build
    private var startedAt: Long? = null
    private var endFailed = false
    private var iceRestarted = false
    private var makingOffer = false
    private var polite = false
    private var remoteVideoTrack: Any? = null

    init {
        messagingRepository.callSignalHandler = { from, signal -> handleSignal(from, signal) }
        messagingRepository.sessionResetHandler = { from -> resendInviteIfMidRing(from) }
    }

    val active: Boolean get() = phase != CallPhase.IDLE && phase != CallPhase.ENDED

    private fun emit() {
        _view.value = if (phase == CallPhase.IDLE || phase == CallPhase.ENDED) null else CallView(
            callId = callId, peerUsername = peer, incoming = incoming, media = media, phase = phase,
            ringing = ringing, micOn = micOn, remoteMicOn = remoteMicOn, camOn = camOn,
            hasRemoteVideo = hasRemoteVideo, remoteVideoCapable = remoteVideoCapable,
            startedAt = startedAt, cameraFacing = facing,
        )
    }

    private fun emitStreams() {
        _streams.value = CallStreams(localVideoTrack = cameraTrack, remoteVideoTrack = remoteVideoTrack)
    }

    private fun videoMeta(): VideoMeta {
        val roles = mutableMapOf<String, String>()
        if (camSender != null) roles["camera"] = "camera"
        val sending = if (cameraTrack != null && camOn) listOf("camera") else emptyList()
        return VideoMeta(roles = roles, sending = sending)
    }

    private fun applyRemoteVideo(v: VideoMeta?) {
        if (v == null) return
        remoteRoles.putAll(v.roles)
        remoteVideoCapable = remoteVideoCapable || v.roles.containsValue("camera")
        hasRemoteVideo = v.sending.contains("camera") && remoteVideoTrack != null
        emit()
    }

    private fun applyRemoteMic(mic: Boolean?) {
        if (mic == null || mic == remoteMicOn) return
        remoteMicOn = mic
        emit()
    }

    private suspend fun newPeerConnection(): Any {
        val me = messagingRepository.currentUsername().orEmpty()
        polite = me < peer
        val iceServers = messagingRepository.fetchIceServers().ifEmpty {
            _errors.value = "Could not fetch TURN credentials, call may fail to connect"
            listOf(
                com.d2m.app.messaging.IceServer(listOf("stun:stun.cloudflare.com:3478")),
                com.d2m.app.messaging.IceServer(listOf("stun:stun.l.google.com:19302")),
            )
        }
        remoteRoles = mutableMapOf()
        remoteVideoTrack = null
        val observer = object : PeerConnectionObserver {
            override fun onIceCandidate(candidate: IceCandidateData) {
                scope.launch { messagingRepository.sendCallSignal(peer, CallSignal.Ice(callId, candidate)) }
            }
            override fun onConnected() {
                println("CallManager: peer connection connected (callId=$callId, peer=$peer)")
                if (phase != CallPhase.IN_CALL) {
                    phase = CallPhase.IN_CALL
                    startedAt = Clock.System.now().toEpochMilliseconds()
                    clearRing()
                    emit()
                }
            }
            override fun onFailed() {
                println("CallManager: peer connection failed (callId=$callId, peer=$peer), attempting ICE restart")
                scope.launch { tryIceRestart() }
            }
            override fun onClosed() {
                println("CallManager: peer connection closed (callId=$callId, peer=$peer)")
                teardown()
            }
            override fun onRemoteAudioTrackAdded() { /* audio plays automatically once attached by the engine's actual */ }
            override fun onRemoteVideoTrackAdded(msid: String, track: Any) {
                remoteVideoTrack = track
                val role = remoteRoles[msid] ?: "camera"
                if (role == "camera") remoteVideoCapable = true
                recomputeRemote()
                emitStreams()
            }
            override fun onRemoteTrackRemoved(msid: String) {
                remoteVideoTrack = null
                recomputeRemote()
                emitStreams()
            }
        }
        val newPc = engine.createPeerConnection(iceServers, observer)
        pc = newPc
        emitStreams()
        return newPc
    }

    private fun recomputeRemote() {
        hasRemoteVideo = remoteVideoTrack != null
        emit()
    }

    private suspend fun tryIceRestart() {
        val currentPc = pc ?: return
        if (incoming) return // callee waits for the caller's restart offer
        if (iceRestarted) {
            hangup()
            return
        }
        iceRestarted = true
        try {
            val offer = engine.createOffer(currentPc, iceRestart = true)
            engine.setLocalDescription(currentPc, "offer", offer)
            engine.waitForIceGatheringComplete(currentPc)
            if (pc !== currentPc) return
            val sdp = engine.localDescriptionSdp(currentPc) ?: return
            messagingRepository.sendCallSignal(peer, CallSignal.Offer(callId, SdpDescription("offer", sdp), video = videoMeta(), mic = micOn))
        } catch (_: Throwable) {
            hangup()
        }
    }

    private suspend fun acquireLocal(video: Boolean) {
        audioTrack = engine.acquireMicrophone()
        micOn = true
        if (video) {
            cameraTrack = engine.acquireCamera(facing)
            camOn = true
        }
        emitStreams()
    }

    // ---------- Outgoing ----------
    suspend fun startCall(peerUsername: String, media: String) {
        if (active) return
        reset()
        callId = "${Clock.System.now().toEpochMilliseconds()}-${randomSuffix()}"
        peer = peerUsername
        incoming = false
        this.media = media
        phase = CallPhase.CALLING
        emit()
        println("CallManager.startCall: calling $peerUsername (media=$media, callId=$callId)")

        try {
            val myPc = newPeerConnection()
            acquireLocal(media == "video")
            if (pc !== myPc) return
            audioTrack?.let { audioSender = engine.addAudioTrack(myPc, it, "camera") }
            cameraTrack?.let { camSender = engine.addVideoTrack(myPc, it, "camera") }

            val offer = engine.createOffer(myPc)
            engine.setLocalDescription(myPc, "offer", offer)
            engine.waitForIceGatheringComplete(myPc)
            if (pc !== myPc) return
            val sdp = engine.localDescriptionSdp(myPc) ?: offer
            val sent = messagingRepository.sendCallSignal(peer, CallSignal.Invite(callId, media, SdpDescription("offer", sdp), videoMeta(), micOn))
            if (!sent) error("couldn't reach $peer -- check your connection")
            println("CallManager.startCall: invite sent to $peer (callId=$callId)")

            ringJob = scope.launch {
                delay(RING_TIMEOUT_MS)
                if (phase == CallPhase.CALLING) {
                    println("CallManager.startCall: ring timeout (${RING_TIMEOUT_MS}ms) for $peer, hanging up (callId=$callId)")
                    hangup()
                }
            }
            emit()
        } catch (e: Throwable) {
            println("CallManager.startCall: failed to start call to $peerUsername: ${e.message ?: e::class.simpleName}")
            _errors.value = "Could not start call: ${e.message}"
            endFailed = true
            teardown()
        }
    }

    // ---------- Incoming ----------
    suspend fun accept() {
        if (phase != CallPhase.INCOMING) return
        val offerSdp = pendingOfferSdp ?: return
        phase = CallPhase.CONNECTING
        emit()
        println("CallManager.accept: accepting call from $peer (callId=$callId)")
        try {
            val myPc = newPeerConnection()
            acquireLocal(media == "video")
            if (pc !== myPc) return
            audioTrack?.let { audioSender = engine.addAudioTrack(myPc, it, "camera") }
            cameraTrack?.let { camSender = engine.addVideoTrack(myPc, it, "camera") }
            engine.setRemoteDescription(myPc, "offer", offerSdp)
            drainCandidates(myPc)
            val answer = engine.createAnswer(myPc)
            engine.setLocalDescription(myPc, "answer", answer)
            engine.waitForIceGatheringComplete(myPc)
            if (pc !== myPc) return
            val sdp = engine.localDescriptionSdp(myPc) ?: answer
            val sent = messagingRepository.sendCallSignal(peer, CallSignal.Accept(callId, SdpDescription("answer", sdp), videoMeta(), micOn))
            if (!sent) error("couldn't reach $peer -- check your connection")
            println("CallManager.accept: accept sent to $peer (callId=$callId)")
            pendingOfferSdp = null
        } catch (e: Throwable) {
            println("CallManager.accept: failed to accept call from $peer: ${e.message ?: e::class.simpleName}")
            _errors.value = "Could not answer call: ${e.message}"
            endFailed = true
            teardown()
        }
    }

    private fun resendInviteIfMidRing(fromPeer: String) {
        val myPc = pc ?: return
        if (phase == CallPhase.CALLING && !incoming && peer == fromPeer) {
            val sdp = engine.localDescriptionSdp(myPc) ?: return
            scope.launch { messagingRepository.sendCallSignal(peer, CallSignal.Invite(callId, media, SdpDescription("offer", sdp), videoMeta(), micOn)) }
        }
    }

    fun decline() {
        if (phase != CallPhase.INCOMING) return
        println("CallManager.decline: declining call from $peer (callId=$callId)")
        scope.launch { messagingRepository.sendCallSignal(peer, CallSignal.Decline(callId, "declined")) }
        teardown()
    }

    fun hangup() {
        if (!active) return
        println("CallManager.hangup: hanging up on $peer (callId=$callId, phase=$phase)")
        scope.launch { messagingRepository.sendCallSignal(peer, CallSignal.Hangup(callId)) }
        teardown()
    }

    // ---------- In-call controls ----------
    fun toggleMic() {
        val track = audioTrack ?: return
        micOn = !micOn
        engine.setTrackEnabled(track, micOn)
        scope.launch { sendMediaState() }
        emit()
    }

    suspend fun toggleCamera() {
        if (cameraTrack == null) {
            upgradeToVideo()
            return
        }
        camOn = !camOn
        engine.setTrackEnabled(cameraTrack!!, camOn)
        sendMediaState()
        emit()
    }

    suspend fun upgradeToVideo() {
        val myPc = pc ?: return
        if (cameraTrack != null) return
        try {
            cameraTrack = engine.acquireCamera(facing)
            camSender = engine.addVideoTrack(myPc, cameraTrack!!, "camera")
            media = "video"
            camOn = true
            emitStreams()
            renegotiate("upgrade")
            emit()
        } catch (e: Throwable) {
            _errors.value = "Could not start video: ${e.message}"
        }
    }

    suspend fun flipCamera() {
        if (pc == null || cameraTrack == null) return
        facing = if (facing == CameraFacing.FRONT) CameraFacing.BACK else CameraFacing.FRONT
        try {
            val newTrack = engine.acquireCamera(facing)
            engine.stopTrack(cameraTrack!!)
            cameraTrack = newTrack
            camOn = true
            emitStreams()
            camSender?.let { engine.replaceVideoTrack(it, newTrack) }
            sendMediaState()
            emit()
        } catch (e: Throwable) {
            _errors.value = "Could not switch camera: ${e.message}"
        }
    }

    private suspend fun sendMediaState() {
        if (peer.isEmpty() || callId.isEmpty()) return
        messagingRepository.sendCallSignal(peer, CallSignal.MediaState(callId, videoMeta(), micOn))
    }

    // ---------- Signaling in ----------
    suspend fun handleSignal(from: String, signal: CallSignal) {
        println("CallManager.handleSignal: ${signal::class.simpleName} from $from (currentCallId=$callId)")
        if (signal is CallSignal.Invite) {
            if (active) {
                if (signal.callId != callId) {
                    println("CallManager.handleSignal: busy, declining new invite ${signal.callId} from $from while on call $callId")
                    messagingRepository.sendCallSignal(from, CallSignal.Decline(signal.callId, "busy"))
                }
                return
            }
            reset()
            callId = signal.callId
            peer = from
            incoming = true
            media = signal.media
            phase = CallPhase.INCOMING
            pendingOfferSdp = signal.sdp.sdp
            remoteMicOn = signal.mic ?: true
            applyRemoteVideo(signal.video)
            messagingRepository.sendCallSignal(from, CallSignal.Ringing(callId))
            emit()
            return
        }

        if (signal.callId != callId) return

        when (signal) {
            is CallSignal.Ringing -> if (phase == CallPhase.CALLING) { ringing = true; emit() }
            is CallSignal.Accept -> {
                applyRemoteMic(signal.mic)
                applyRemoteVideo(signal.video)
                pc?.let { p -> engine.setRemoteDescription(p, "answer", signal.sdp.sdp.orEmpty()); drainCandidates(p) }
            }
            is CallSignal.Decline -> teardown(if (signal.reason == "busy") CallEndReason.BUSY else CallEndReason.DECLINED)
            is CallSignal.Ice -> addCandidate(signal.candidate)
            is CallSignal.Offer -> onRenegotiationOffer(signal.sdp.sdp.orEmpty(), signal.note, signal.video, signal.mic)
            is CallSignal.Answer -> {
                applyRemoteMic(signal.mic)
                applyRemoteVideo(signal.video)
                pc?.let { p -> if (!engine.isSignalingStable(p)) engine.setRemoteDescription(p, "answer", signal.sdp.sdp.orEmpty()) }
            }
            is CallSignal.MediaState -> {
                applyRemoteMic(signal.mic)
                applyRemoteVideo(signal.video)
            }
            is CallSignal.Hangup -> teardown()
            is CallSignal.Invite -> Unit // handled above
        }
    }

    private suspend fun renegotiate(note: String) {
        val myPc = pc ?: return
        try {
            makingOffer = true
            val offer = engine.createOffer(myPc)
            engine.setLocalDescription(myPc, "offer", offer)
            engine.waitForIceGatheringComplete(myPc)
            if (pc !== myPc) return
            val sdp = engine.localDescriptionSdp(myPc) ?: offer
            messagingRepository.sendCallSignal(peer, CallSignal.Offer(callId, SdpDescription("offer", sdp), note, videoMeta(), micOn))
        } finally {
            makingOffer = false
        }
    }

    private suspend fun onRenegotiationOffer(sdp: String, note: String?, video: VideoMeta?, mic: Boolean?) {
        val myPc = pc ?: return
        applyRemoteMic(mic)
        applyRemoteVideo(video)
        val offerCollision = makingOffer || !engine.isSignalingStable(myPc)
        if (offerCollision && !polite) return
        if (offerCollision) engine.setLocalDescription(myPc, "rollback", "")
        engine.setRemoteDescription(myPc, "offer", sdp)
        drainCandidates(myPc)
        if (note == "upgrade" || note == "screen-on") media = "video"
        val answer = engine.createAnswer(myPc)
        engine.setLocalDescription(myPc, "answer", answer)
        engine.waitForIceGatheringComplete(myPc)
        if (pc !== myPc) return
        val answerSdp = engine.localDescriptionSdp(myPc) ?: answer
        messagingRepository.sendCallSignal(peer, CallSignal.Answer(callId, SdpDescription("answer", answerSdp), videoMeta(), micOn))
        recomputeRemote()
        emit()
    }

    // ---------- helpers ----------
    private suspend fun addCandidate(c: IceCandidateData) {
        val myPc = pc
        if (myPc != null) {
            runCatching { engine.addIceCandidate(myPc, c) }
        } else {
            candidateQueue.add(c)
        }
    }

    private suspend fun drainCandidates(myPc: Any) {
        val q = candidateQueue.toList()
        candidateQueue.clear()
        for (c in q) runCatching { engine.addIceCandidate(myPc, c) }
    }

    private fun clearRing() {
        ringJob?.cancel()
        ringJob = null
    }

    private fun reset() {
        pc = null
        audioTrack = null
        cameraTrack = null
        audioSender = null
        camSender = null
        remoteVideoTrack = null
        remoteRoles = mutableMapOf()
        candidateQueue.clear()
        pendingOfferSdp = null
        clearRing()
        callId = ""
        peer = ""
        incoming = false
        media = "audio"
        ringing = false
        micOn = true
        remoteMicOn = true
        facing = CameraFacing.FRONT
        camOn = false
        hasRemoteVideo = false
        remoteVideoCapable = false
        startedAt = null
        makingOffer = false
        endFailed = false
        iceRestarted = false
    }

    private fun teardown(reasonOverride: CallEndReason? = null) {
        println("CallManager.teardown: callId=$callId, peer=$peer, phase=$phase, reasonOverride=$reasonOverride")
        var info: CallEndInfo? = null
        if (peer.isNotEmpty()) {
            val connected = startedAt != null
            val reason = reasonOverride ?: when {
                connected -> CallEndReason.ENDED
                endFailed -> CallEndReason.FAILED
                incoming -> CallEndReason.MISSED
                else -> CallEndReason.NO_ANSWER
            }
            info = CallEndInfo(
                peerUsername = peer, incoming = incoming, media = media, connected = connected,
                durationSec = startedAt?.let { ((Clock.System.now().toEpochMilliseconds() - it) / 1000).toInt() } ?: 0,
                reason = reason,
            )
        }

        clearRing()
        // Close/dispose the PeerConnection FIRST, THEN the local tracks --
        // this order matters and getting it backwards was the actual cause
        // of "app crashes after ending a call": native WebRTC throws
        // IllegalStateException out of MediaStreamTrack.dispose() if the
        // track is disposed while still referenced by an active RtpSender
        // on a live PeerConnection. Closing the PeerConnection first drops
        // its senders' references to these tracks, so disposing them right
        // after is safe. Every step is independently wrapped in runCatching
        // so one failure can't abort the rest of teardown() partway through
        // (which is exactly what the old unwrapped `engine.stopTrack(it)`
        // calls did -- an exception there skipped phase = ENDED, emit(),
        // and reset() entirely, leaving the call stuck mid-teardown).
        pc?.let { runCatching { engine.closePeerConnection(it) } }
        audioTrack?.let { runCatching { engine.stopTrack(it) } }
        cameraTrack?.let { runCatching { engine.stopTrack(it) } }
        phase = CallPhase.ENDED
        emit()
        _streams.value = EMPTY_STREAMS
        val peerAtEnd = peer
        reset()
        phase = CallPhase.IDLE
        if (info != null) logCallEnded(peerAtEnd, info)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun logCallEnded(peerUsername: String, info: CallEndInfo) {
        val me = messagingRepository.currentUsername() ?: return
        val reasonLabel = when (info.reason) {
            CallEndReason.ENDED -> "${if (info.media == "video") "Video" else "Voice"} call · ${info.durationSec / 60}:${(info.durationSec % 60).toString().padStart(2, '0')}"
            CallEndReason.BUSY, CallEndReason.FAILED -> "Call failed"
            CallEndReason.DECLINED -> "Call declined"
            CallEndReason.MISSED -> "Missed call"
            CallEndReason.NO_ANSWER -> "No answer"
        }
        messagingRepository.appendCallLogMessage(
            peerUsername,
            ChatMessage(
                id = Uuid.random().toString(),
                fromUsername = if (info.incoming) peerUsername else me,
                toUsername = if (info.incoming) me else peerUsername,
                text = reasonLabel,
                sentAt = Clock.System.now().toEpochMilliseconds(),
                isMine = !info.incoming,
                status = MessageStatus.DELIVERED,
                callLog = CallLogInfo(info.media, info.reason.name.lowercase().replace("_", "-"), info.durationSec, info.incoming),
            ),
        )
    }

    private fun randomSuffix(): String = (0..1).joinToString("") { (('a'..'z') + ('0'..'9')).random().toString() } +
        (0..3).joinToString("") { (('a'..'z') + ('0'..'9')).random().toString() }
}
