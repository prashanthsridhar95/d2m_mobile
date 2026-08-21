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
class MessagingWsClient {
    private var session: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession? = null
    private var job: Job? = null
    private var heartbeatJob: Job? = null
    private var closedByUs = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var onEvent: (ServerToClient) -> Unit = {}
    var onOpen: () -> Unit = {}

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

    fun connect(wsBaseUrl: String, username: String, client: HttpClient) {
        closedByUs = false
        if (job?.isActive == true) return

        job = scope.launch {
            while (isActive && !closedByUs) {
                try {
                    val url = "$wsBaseUrl/ws?user=${encodeQueryParam(username)}"
                    println("MessagingWsClient: connecting to $url")
                    client.webSocketSession(urlString = url).let { s ->
                        session = s
                        _isConnected.value = true
                        println("MessagingWsClient: connected (user=$username)")
                        onOpen()
                        heartbeatJob = scope.launch {
                            while (isActive) {
                                delay(15_000)
                                val sent = send(ClientToServer.Ping)
                                if (!sent) println("MessagingWsClient: heartbeat ping failed to send")
                            }
                        }
                        try {
                            for (frame in s.incoming) {
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    val event = runCatching {
                                        messagingProtocolJson.decodeFromString(ServerToClient.serializer(), text)
                                    }.getOrElse { e ->
                                        println("MessagingWsClient: failed to decode incoming frame: ${e.message ?: e::class.simpleName} -- raw=$text")
                                        null
                                    }
                                    if (event != null) {
                                        println("MessagingWsClient: received ${event::class.simpleName}")
                                        onEvent(event)
                                    }
                                }
                            }
                        } catch (e: ClosedReceiveChannelException) {
                            println("MessagingWsClient: socket closed (${e.message ?: "normal close"}), will reconnect")
                        } finally {
                            heartbeatJob?.cancel()
                        }
                    }
                } catch (e: Throwable) {
                    println("MessagingWsClient: connection attempt failed: ${e.message ?: e::class.simpleName}, will retry")
                }
                session = null
                _isConnected.value = false
                if (closedByUs) break
                delay(1_500)
            }
            println("MessagingWsClient: reconnect loop exited (closedByUs=$closedByUs)")
        }
    }

    /** Returns false (never throws) if there's no live session to send on, or if the send itself fails -- see [isConnected]'s doc comment for why callers that care about delivery (MessagingRepository's send paths) need to check this instead of assuming a lack of exception means it went out. */
    suspend fun send(event: ClientToServer): Boolean {
        val s = session ?: run {
            println("MessagingWsClient: send(${event::class.simpleName}) dropped -- not connected")
            return false
        }
        val text = messagingProtocolJson.encodeToString(ClientToServer.serializer(), event)
        val result = runCatching { s.send(Frame.Text(text)) }
        if (result.isFailure) {
            println("MessagingWsClient: send(${event::class.simpleName}) failed -- ${result.exceptionOrNull()?.message ?: result.exceptionOrNull()?.let { it::class.simpleName }}")
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
