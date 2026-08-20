package com.d2m.app.messaging.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d2m.app.messaging.InboxNotification
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private val BannerAvatarGradient = Brush.linearGradient(listOf(Color(0xFFDCEEEA), Color(0xFFFBEAD2)))

/**
 * Instagram/iMessage-style in-app banner for incoming messages -- point 4 of
 * the notification request: "In case notifications are turned off, I want
 * instagram style in app notification to be received instead of system
 * notification when the user is in the app." Incoming calls already get
 * this treatment (CallLayer.kt's full-screen IncomingCallToast, which is the
 * correct in-app pattern for a call, not a small banner) -- this covers the
 * message half.
 *
 * Rides MessagingRepository.inboxNotifications directly (the same live
 * WebSocket stream ChatPane already renders from), so it works regardless
 * of whether real backend push ever gets wired up (see
 * push/LocalNotificationBridge.android.kt's doc comment on that gap) --
 * this is not push, it's "the app already knows, so tell the person while
 * they're looking at it." Suppressed automatically for whichever peer's
 * chat is currently open, since MessagingRepository only emits
 * inboxNotifications for a peer that ISN'T the active conversation (same
 * check the unread badge uses).
 *
 * Mounted once at the App.kt shell level, same pattern as CallLayer. Tap
 * opens that peer's thread via ChatUiState's pending-open mechanism;
 * auto-dismisses after a few seconds like every other "toast" pattern.
 */
@Composable
fun InAppNotificationLayer(onOpenPeer: (String) -> Unit) {
    val messagingRepo: MessagingRepository = koinInject()
    var current by remember { mutableStateOf<InboxNotification?>(null) }

    LaunchedEffect(Unit) {
        messagingRepo.inboxNotifications.collect { event -> current = event }
    }

    LaunchedEffect(current) {
        val shown = current ?: return@LaunchedEffect
        delay(4_500)
        if (current == shown) current = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val event = current
        AnimatedVisibility(
            visible = event != null,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            if (event != null) {
                // A fixed top offset rather than precise WindowInsets.statusBars()
                // -- keeps this composable free of platform-specific inset
                // wiring (the app enables edge-to-edge on Android; iOS has no
                // real target to verify against yet, see WebRtcEngine.ios.kt),
                // generous enough to clear the status bar on real devices.
                Surface(
                    shape = RoundedCornerShape(D2MRadius.lg),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 10.dp, end = 10.dp)
                        .clickable {
                            onOpenPeer(event.peerUsername)
                            current = null
                        },
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(BannerAvatarGradient, RoundedCornerShape(12.dp)))
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(
                                messagingRepo.peerDisplayName(event.peerUsername),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                event.preview,
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedText(0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { current = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
