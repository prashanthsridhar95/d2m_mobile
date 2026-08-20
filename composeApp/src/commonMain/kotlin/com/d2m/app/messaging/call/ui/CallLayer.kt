package com.d2m.app.messaging.call.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.call.CallManager
import com.d2m.app.messaging.call.CallPhase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Global call dispatcher -- mounted once at the App.kt shell level (mirrors
 * d2m_web's CallLayer.jsx, mounted above the router so a call survives
 * screen navigation). Renders nothing while idle; an IncomingCallToast on
 * CallPhase.INCOMING; a full CallOverlay for CALLING/CONNECTING/IN_CALL
 * (unless minimized, then MinimizedCallBar instead).
 */
@Composable
fun CallLayer() {
    val callManager: CallManager = koinInject()
    val messagingRepo: MessagingRepository = koinInject()
    val view by callManager.view.collectAsState()
    val streams by callManager.streams.collectAsState()
    var minimized by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val requestPermissions = rememberCallPermissionLauncher { granted ->
        if (!granted) scope.launch { callManager.decline() }
    }

    LaunchedEffect(view) {
        if (view == null) minimized = false
    }
    val currentView = view ?: return

    val peerName = messagingRepo.peerDisplayName(currentView.peerUsername)

    LaunchedEffect(currentView.callId, currentView.phase) {
        if (currentView.phase == CallPhase.INCOMING) requestPermissions()
    }

    when (currentView.phase) {
        CallPhase.INCOMING -> IncomingCallToast(
            peerName = peerName,
            isVideo = currentView.media == "video",
            onAccept = { scope.launch { callManager.accept() } },
            onDecline = { callManager.decline() },
        )
        else -> if (minimized) {
            MinimizedCallBar(peerName = peerName, onExpand = { minimized = false }, onHangup = { callManager.hangup() })
        } else {
            Dialog(
                onDismissRequest = { minimized = true },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false),
            ) {
                CallOverlay(callManager = callManager, view = currentView, streams = streams, peerName = peerName, onMinimize = { minimized = true })
            }
        }
    }
}

@Composable
private fun IncomingCallToast(peerName: String, isVideo: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    Dialog(onDismissRequest = onDecline, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(88.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Text(peerName.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = Color.White)
                }
                Text(peerName, style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.padding(top = 16.dp))
                Text(if (isVideo) "Incoming video call" else "Incoming call", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))

                Row(modifier = Modifier.padding(top = 48.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    RoundActionButton(icon = Icons.Filled.CallEnd, background = Color(0xFFE53935), onClick = onDecline, label = "Decline")
                    RoundActionButton(icon = if (isVideo) Icons.Filled.Videocam else Icons.Filled.Call, background = Color(0xFF43A047), onClick = onAccept, label = "Accept")
                }
            }
        }
    }
}

@Composable
private fun RoundActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, background: Color, onClick: () -> Unit, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(64.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
            IconButton(onClick = onClick) { Icon(icon, contentDescription = label, tint = Color.White) }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun MinimizedCallBar(peerName: String, onExpand: () -> Unit, onHangup: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.BottomCenter) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand),
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("In call with $peerName", style = MaterialTheme.typography.labelLarge, color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = onHangup) { Icon(Icons.Filled.CallEnd, contentDescription = "Hang up", tint = Color.White) }
            }
        }
    }
}
