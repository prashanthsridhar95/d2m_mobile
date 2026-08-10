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

    fun connect(wsBaseUrl: String, username: String, client: HttpClient) {
        closedByUs = false
        if (job?.isActive == true) return

        job = scope.launch {
            while (isActive && !closedByUs) {
                try {
                    val url = "$wsBaseUrl/ws?user=${encodeQueryParam(username)}"
                    client.webSocketSession(urlString = url).let { s ->
                        session = s
                        onOpen()
                        heartbeatJob = scope.launch {
                            while (isActive) {
                                delay(15_000)
                                send(ClientToServer.Ping)
                            }
                        }
                        try {
                            for (frame in s.incoming) {
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    val event = runCatching {
                                        messagingProtocolJson.decodeFromString(ServerToClient.serializer(), text)
                                    }.getOrNull()
                                    if (event != null) onEvent(event)
                                }
                            }
                        } catch (_: ClosedReceiveChannelException) {
                            // normal close, fall through to reconnect below
                        } finally {
                            heartbeatJob?.cancel()
                        }
                    }
                } catch (_: Throwable) {
                    // connection failed or dropped -- fall through to the reconnect delay below
                }
                session = null
                if (closedByUs) break
                delay(1_500)
            }
        }
    }

    suspend fun send(event: ClientToServer) {
        val s = session ?: return
        val text = messagingProtocolJson.encodeToString(ClientToServer.serializer(), event)
        runCatching { s.send(Frame.Text(text)) }
    }

    fun disconnect() {
        closedByUs = true
        heartbeatJob?.cancel()
        scope.launch { runCatching { session?.close() } }
        job?.cancel()
    }
}

expect fun encodeQueryParam(value: String): String
