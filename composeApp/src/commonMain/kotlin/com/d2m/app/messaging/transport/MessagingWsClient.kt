package com.d2m.app.messaging.transport

import com.d2m.app.messaging.protocol.ClientToServer
import com.d2m.app.messaging.protocol.ServerToClient
import com.d2m.app.messaging.protocol.messagingProtocolJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlin.time.Clock

/**
 * Direct port of messaging-framework/packages/client-web/src/wsClient.ts:
 * same URL shape (`{WS_BASE}/ws?user=...`), same 15s heartbeat ping, same
 * 1500ms auto-reconnect delay, same "don't stack sockets" guard. The web
 * version's document.visibilitychange/online/focus "reconnect the instant
 * the app wakes up" listeners have no direct multiplatform equivalent
 * (that's an Android/iOS lifecycle concern) -- left as a platform-actual
 * follow-up rather than faked here; the plain 1500ms retry loop still
 * reconnects on its own within a couple seconds either way.
 */
/** WebSocket close: the device id on the upgrade is not registered (anymore). */
const val WS_CLOSE_UNKNOWN_DEVICE = 4004
/** WebSocket close: this device was removed from the account or evicted by the per-account device cap. */
const val WS_CLOSE_DEVICE_REMOVED = 4005

class MessagingWsClient {
    private var session: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession? = null
    private var job: Job? = null
    private var heartbeatJob: Job? = null
    private var closedByUs = false
    private var skipNextDelay = false
    private var currentSessionJob: Job? = null
    private var lastRxAt = 0L
    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var onEvent: (ServerToClient) -> Unit = {}
    var onOpen: () -> Unit = {}

    /**
     * The server closed the socket with a code that reconnecting can never fix (see [WS_CLOSE_UNKNOWN_DEVICE],
     * [WS_CLOSE_DEVICE_REMOVED]). The reconnect loop has stopped; the owner shows a message and decides what to do. There is
     * deliberately NO automatic re-register here: devices evicting each other in a loop is worse than one stuck device.
     */
    var onTerminalClose: (code: Int) -> Unit = {}
    private var halted = false

    /**
     * True only while an actual session is live. [send] silently no-ops
     * (never throws) when this is false -- see that function's doc comment
     * for why that used to be invisible ("messages not sent... totally
     * silent" was reported while `start()` itself was succeeding, i.e. the
     * failure was happening here, past the point anything else could catch
     * it). MessagingRepository observes this to fail sends loudly instead of
     * leaving them at "Sending..." forever with nothing to explain why.
     */
    private val _isConnected = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isConnected: kotlinx.coroutines.flow.StateFlow<Boolean> = _isConnected

    /**
     * [deviceId] says which of the account's devices this socket is (sent as `?device=` on the upgrade, which the server
     * accepts like the auth frame's `deviceId`); null = legacy device 1.
     */
    fun connect(wsBaseUrl: String, username: String, client: HttpClient, deviceId: Int? = null) {
        closedByUs = false
        halted = false
        if (job?.isActive == true) return

        job = scope.launch {
            while (isActive && !closedByUs && !halted) {
                try {
                    val url = "$wsBaseUrl/ws?user=${encodeQueryParam(username)}" + if (deviceId != null) "&device=$deviceId" else ""
                    println("MessagingWsClient: connecting to $url")
                    // The whole live session runs in its own child job so
                    // reconnectNow()/the liveness watchdog can tear down a
                    // half-open socket (phone slept, network switched) that
                    // the OS never told us is dead -- previously such a
                    // zombie session kept isConnected=true forever, so
                    // pushes/ensureConnected() believed they were online
                    // while the server had already given up on us and was
                    // sending notifications instead of messages/call invites.
                    val sessionJob = scope.launch {
                        val s = client.webSocketSession(urlString = url)
                        session = s
                        lastRxAt = nowMs()
                        _isConnected.value = true
                        println("MessagingWsClient: connected (user=$username)")
                        onOpen()
                        heartbeatJob = launch {
                            while (isActive) {
                                delay(15_000)
                                val pingAt = nowMs()
                                val sent = send(ClientToServer.Ping)
                                if (!sent) println("MessagingWsClient: heartbeat ping failed to send")
                                delay(10_000)
                                if (lastRxAt < pingAt) {
                                    println("MessagingWsClient: no traffic since ping – socket is dead, reconnecting")
                                    this@launch.cancel()
                                    currentSessionJob?.cancel()
                                }
                            }
                        }
                        try {
                            for (frame in s.incoming) {
                                lastRxAt = nowMs()
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    val event = runCatching {
                                        messagingProtocolJson.decodeFromString(ServerToClient.serializer(), text)
                                    }.getOrElse { e ->
                                        println("MessagingWsClient: failed to decode incoming frame: ${e.message ?: e::class.simpleName} – raw=$text")
                                        null
                                    }
                                    if (event != null) {
                                        println("MessagingWsClient: received ${event::class.simpleName}")
                                        onEvent(event)
                                    }
                                }
                            }
                            val closeCode = runCatching { s.closeReason.await()?.code?.toInt() }.getOrNull()
                            if (closeCode == WS_CLOSE_UNKNOWN_DEVICE || closeCode == WS_CLOSE_DEVICE_REMOVED) {
                                println("MessagingWsClient: server closed the socket with terminal code $closeCode, not reconnecting")
                                halted = true
                                onTerminalClose(closeCode)
                            } else {
                                println("MessagingWsClient: socket closed (code=$closeCode), will reconnect")
                            }
                        } catch (e: ClosedReceiveChannelException) {
                            println("MessagingWsClient: socket closed (${e.message ?: "normal close"}), will reconnect")
                        } finally {
                            heartbeatJob?.cancel()
                            runCatching { s.cancel() }
                        }
                    }
                    currentSessionJob = sessionJob
                    sessionJob.join()
                } catch (e: Throwable) {
                    println("MessagingWsClient: connection attempt failed: ${e.message ?: e::class.simpleName}, will retry")
                }
                session = null
                currentSessionJob = null
                _isConnected.value = false
                if (closedByUs || halted) break
                if (skipNextDelay) skipNextDelay = false else delay(1_500)
            }
            println("MessagingWsClient: reconnect loop exited (closedByUs=$closedByUs)")
        }
    }

    /**
     * Drops whatever session exists (live or silently dead) and reconnects
     * immediately, without the usual 1.5s backoff. Call when the app returns
     * to the foreground or a push arrives -- the two moments a socket that
     * "looks" connected is most likely a zombie. Safe to call when no
     * session exists yet (the connect loop is already retrying).
     */
    fun reconnectNow() {
        if (closedByUs || halted || job?.isActive != true) return
        println("MessagingWsClient: reconnectNow()")
        skipNextDelay = true
        currentSessionJob?.cancel()
        _isConnected.value = false
    }

    /** Returns false (never throws) if there's no live session to send on, or if the send itself fails -- see [isConnected]'s doc comment for why callers that care about delivery (MessagingRepository's send paths) need to check this instead of assuming a lack of exception means it went out. */
    suspend fun send(event: ClientToServer): Boolean {
        val s = session ?: run {
            println("MessagingWsClient: send(${event::class.simpleName}) dropped – not connected")
            return false
        }
        val text = messagingProtocolJson.encodeToString(ClientToServer.serializer(), event)
        val result = runCatching { s.send(Frame.Text(text)) }
        if (result.isFailure) {
            println("MessagingWsClient: send(${event::class.simpleName}) failed – ${result.exceptionOrNull()?.message ?: result.exceptionOrNull()?.let { it::class.simpleName }}")
        }
        return result.isSuccess
    }

    fun disconnect() {
        println("MessagingWsClient: disconnect() called")
        closedByUs = true
        heartbeatJob?.cancel()
        scope.launch { runCatching { session?.close() } }
        job?.cancel()
        _isConnected.value = false
    }
}

expect fun encodeQueryParam(value: String): String
