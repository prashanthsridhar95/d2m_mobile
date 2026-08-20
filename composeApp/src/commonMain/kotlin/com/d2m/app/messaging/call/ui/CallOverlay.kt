package com.d2m.app.messaging.call.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.d2m.app.messaging.call.CallManager
import com.d2m.app.messaging.call.CallPhase
import com.d2m.app.messaging.call.CallStreams
import com.d2m.app.messaging.call.CallView
import com.d2m.app.messaging.call.VideoRendererView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen in-call UI -- mirrors d2m_web's CallOverlay.jsx (mute/camera/
 * flip/hangup controls, local PIP + remote video) minus screen-share (not
 * ported, see CallManager.kt's doc comment) and device-picker menus (mobile
 * covers the equivalent case with flipCamera). Shown by CallLayer.kt for
 * CALLING/CONNECTING/IN_CALL phases; INCOMING gets its own
 * IncomingCallToast instead (accept/decline, not in-call controls).
 */
@Composable
fun CallOverlay(callManager: CallManager, view: CallView, streams: CallStreams, peerName: String, onMinimize: () -> Unit) {
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (view.hasRemoteVideo) {
            VideoRendererView(track = streams.remoteVideoTrack, mirror = false, modifier = Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(peerName, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text(statusLabel(view), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                    if (!view.remoteMicOn) {
                        Spacer(Modifier.height(8.dp))
                        Text("${peerName} is muted", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // Top bar: name/status + minimize.
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(peerName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(statusLabel(view), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
            }
            IconButton(onClick = onMinimize) {
                Icon(Icons.Filled.Close, contentDescription = "Minimize", tint = Color.White)
            }
        }

        // Local self-view PIP.
        if (view.camOn && streams.localVideoTrack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 16.dp)
                    .width(100.dp).height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray),
            ) {
                VideoRendererView(track = streams.localVideoTrack, mirror = view.cameraFacing == com.d2m.app.messaging.call.CameraFacing.FRONT, modifier = Modifier.fillMaxSize())
            }
        }

        // Controls.
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp, alignment = Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CallControlButton(
                icon = if (view.micOn) Icons.Filled.Mic else Icons.Filled.MicOff,
                label = if (view.micOn) "Mute" else "Unmute",
                onClick = { callManager.toggleMic() },
            )
            CallControlButton(
                icon = if (view.camOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                label = if (view.camOn) "Stop video" else "Start video",
                onClick = { scope.launch { callManager.toggleCamera() } },
            )
            if (view.camOn) {
                CallControlButton(icon = Icons.Filled.Cameraswitch, label = "Flip", onClick = { scope.launch { callManager.flipCamera() } })
            }
            CallControlButton(
                icon = Icons.Filled.CallEnd,
                label = "End",
                background = Color(0xFFE53935),
                onClick = { callManager.hangup() },
            )
        }
    }
}

@Composable
private fun statusLabel(view: CallView): String = when (view.phase) {
    CallPhase.CALLING -> if (view.ringing) "Ringing…" else "Calling…"
    CallPhase.CONNECTING -> "Connecting…"
    CallPhase.IN_CALL -> view.startedAt?.let { formatDuration(it) } ?: "In call"
    else -> ""
}

@Composable
private fun formatDuration(startedAtMs: Long): String {
    var now by remember { mutableStateOf(kotlinx.datetime.Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(startedAtMs) {
        while (true) {
            now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            delay(1000)
        }
    }
    val secs = ((now - startedAtMs) / 1000).coerceAtLeast(0)
    val m = secs / 60
    val s = secs % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

@Composable
private fun CallControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, background: Color = Color.White.copy(alpha = 0.15f)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(background),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onClick) {
                Icon(icon, contentDescription = label, tint = Color.White)
            }
        }
    }
}
