package com.d2m.app.messaging.call.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.d2m.app.data.network.ApiClient
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.SoundEffects
import com.d2m.app.messaging.call.CallManager
import com.d2m.app.messaging.call.CallPhase
import com.d2m.app.messaging.call.PendingCallAccept
import com.d2m.app.ui.components.D2MErrorBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    val apiClient: ApiClient = koinInject()
    val view by callManager.view.collectAsState()
    val streams by callManager.streams.collectAsState()
    val error by callManager.errors.collectAsState()
    var minimized by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val requestPermissions = rememberCallPermissionLauncher { granted ->
        if (!granted) scope.launch { callManager.decline() }
    }

    // Rendered unconditionally, before the `view == null` early-return below
    // -- a call-start/accept failure tears the call down (phase -> IDLE, so
    // `view` goes null) in the same breath it sets the error, which used to
    // mean the error was set but nothing was ever left mounted to show it.
    // That's precisely why "calls not being sent" read as total silence
    // instead of a visible failure.
    CallErrorToast(error = error, onDismiss = { callManager.clearError() })

    // The incoming-call notification's Accept action opens the app via a
    // direct Activity-launch PendingIntent carrying just the callId (see
    // PendingCallAccept.kt's doc comment for why it can't do the actual
    // accept() work itself, from a BroadcastReceiver). MainActivity.kt reads
    // that extra and publishes it here; this effect is deliberately placed
    // ABOVE the `view == null` early-return below, since it needs to keep
    // running (waiting for CallManager to actually reflect this exact,
    // still-incoming call) even on the very first composition, where `view`
    // may still be null for a moment (e.g. the FCM handler's own decrypt/
    // live-drain race hadn't resolved yet when the Activity opened).
    //
    // PendingCallAccept.clear() MUST run LAST, not first: this effect is
    // keyed on pendingAcceptCallId itself (read via collectAsState()), so
    // clearing it early flips that key to null mid-block, which recomposes
    // and makes Compose cancel THIS running coroutine (to launch a fresh
    // LaunchedEffect(null) that immediately no-ops) -- cancelling it while
    // it's in the middle of ensureConnected()/accept(). That surfaced live
    // as "Could not answer call: ..." (a CancellationException caught by
    // accept()'s catch-all, with a message to the effect of the coroutine
    // having left its own scope) even though everything else about Accept
    // was otherwise working. Clearing only after accept() has already
    // returned means the cancellation lands on an already-completed job,
    // which is a harmless no-op.
    val pendingAcceptCallId by PendingCallAccept.pendingCallId.collectAsState()
    LaunchedEffect(pendingAcceptCallId) {
        val id = pendingAcceptCallId ?: return@LaunchedEffect
        val matched = withTimeoutOrNull(8_000) {
            callManager.view.first { it != null && it.callId == id && it.phase == CallPhase.INCOMING }
        }
        if (matched != null) {
            runCatching { messagingRepo.ensureConnected(apiClient.client) }
            runCatching { callManager.accept() }
        }
        PendingCallAccept.clear()
    }

    LaunchedEffect(view) {
        if (view == null) minimized = false
    }
    val currentView = view ?: return

    val peerName = messagingRepo.peerDisplayName(currentView.peerUsername)

    LaunchedEffect(currentView.callId, currentView.phase) {
        if (currentView.phase == CallPhase.INCOMING) requestPermissions()
    }

    // The real looping ringtone (SoundEffects.playCallTone/stopCallTone)
    // used to be triggered from HERE, gated on isForeground, on the
    // assumption that the background CallStyle notification
    // (push/LocalNotificationBridge.android.kt) would provide its own real
    // looping ring while backgrounded. Confirmed directly that assumption
    // was wrong -- NotificationCompat.CallStyle + a ringtone-usage
    // AudioAttributes channel does NOT actually loop like a real phone call
    // without a full Telecom ConnectionService integration; it just plays
    // the configured sound once, same as any other notification. The
    // trigger now lives in installLocalNotificationBridge's own
    // callManager.view collector instead -- a single, process-wide source
    // that plays/stops regardless of foreground state (covering this
    // Composable never even existing yet, e.g. a cold FCM-triggered
    // process), so there's nothing left to do here.
    DisposableEffect(Unit) {
        onDispose { SoundEffects.stopCallTone() }
    }

    if (currentView.phase == CallPhase.INCOMING) {
        IncomingCallToast(
            peerName = peerName,
            isVideo = currentView.media == "video",
            onAccept = { scope.launch { callManager.accept() } },
            onDecline = { callManager.decline() },
        )
    } else if (minimized) {
        // AnimatedVisibility(visible = true) always renders its content
        // immediately (only exit transitions are deferred) -- so this still
        // shows the bar right away, it just also plays the slide/fade-in the
        // first frame `minimized` flips true instead of popping in.
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(150)),
        ) {
            MinimizedCallBar(peerName = peerName, onExpand = { minimized = false }, onHangup = { callManager.hangup() })
        }
    } else {
        Dialog(
            onDismissRequest = { minimized = true },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false),
        ) {
            CallOverlay(callManager = callManager, view = currentView, streams = streams, peerName = peerName, onMinimize = { minimized = true })
        }
    }
}

/** Transient failure banner for call setup/mid-call errors (CallManager.errors) -- same slide-down toast convention as InAppNotificationLayer.kt for visual consistency across the app's two "something happened, here's a banner" surfaces. Auto-dismisses after a few seconds; tap to dismiss early. */
@Composable
private fun CallErrorToast(error: String?, onDismiss: () -> Unit) {
    LaunchedEffect(error) {
        if (error != null) {
            delay(4_500)
            onDismiss()
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = error != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp, start = 10.dp, end = 10.dp),
        ) {
            if (error != null) {
                D2MErrorBanner(message = error, modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss))
            }
        }
    }
}

@Composable
private fun IncomingCallToast(peerName: String, isVideo: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    Dialog(onDismissRequest = onDecline, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PulsingAvatar(initial = peerName.take(1).uppercase(), size = 88.dp)
                Text(peerName, style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.padding(top = 16.dp))
                Text(if (isVideo) "Incoming video call" else "Incoming audio call", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))

                Row(modifier = Modifier.padding(top = 48.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    RoundActionButton(icon = Icons.Filled.CallEnd, background = Color(0xFFE53935), onClick = onDecline, label = "Decline")
                    RoundActionButton(icon = if (isVideo) Icons.Filled.Videocam else Icons.Filled.Call, background = Color(0xFF43A047), onClick = onAccept, label = "Accept")
                }
            }
        }
    }
}

/**
 * A breathing ring behind an initial-letter avatar -- the standard "actively
 * trying to reach someone" visual language every phone call UI uses (Don
 * Norman: feedback for a state -- ringing, connecting -- that otherwise has
 * nothing moving on screen to signal the app hasn't just frozen). Reused for
 * both the incoming-call toast and CallOverlay's audio-only ringing/
 * connecting state.
 */
@Composable
fun PulsingAvatar(initial: String, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 88.dp) {
    val infinite = rememberInfiniteTransition(label = "callPulse")
    val ringScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(animation = tween(1100), repeatMode = RepeatMode.Restart),
        label = "pulseScale",
    )
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(1100), repeatMode = RepeatMode.Restart),
        label = "pulseAlpha",
    )
    Box(modifier = modifier.size(size * 1.4f), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { scaleX = ringScale; scaleY = ringScale; alpha = ringAlpha }
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Box(modifier = Modifier.size(size).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
            Text(initial, style = MaterialTheme.typography.headlineMedium, color = Color.White)
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
