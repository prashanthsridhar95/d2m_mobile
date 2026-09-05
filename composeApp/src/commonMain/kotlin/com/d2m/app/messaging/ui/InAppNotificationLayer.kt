package com.d2m.app.messaging.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.d2m.app.AppForegroundState
import com.d2m.app.messaging.InboxNotification
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.SoundEffects
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/*
 * Retheme pass: the avatar placeholder was a fixed teal-to-cream gradient
 * (#DCEEEA -> #FBEAD2), left over from the old per-flow palettes and the
 * one place in this app with a colour baked in rather than read from the
 * theme. It's now the same warm sunken surface a missing photograph gets
 * everywhere else (see ProfilePhoto.kt), so an avatar with no photo and a
 * card with no photo read as the same material. A composable accessor
 * rather than a top-level val, since a CompositionLocal can only be read
 * inside composition.
 */
@Composable
private fun avatarPlaceholder() = d2m.surfaceSunken


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
        messagingRepo.inboxNotifications.collect { event ->
            current = event
            // "Why notification sounds are not working like how it's
            // working on web?" -- web plays playMsgTone() in the exact same
            // spot it sets incomingNotification (useMessaging.js), which
            // mobile never had at all. Gated on isForeground (this
            // LaunchedEffect itself keeps collecting even while backgrounded)
            // so this doesn't double up with the background system
            // notification's own channel sound for the same event.
            if (AppForegroundState.isForeground.value) SoundEffects.playMsgTone()
        }
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
                // WindowInsets.statusBars gives the real system-bar height on
                // this device/orientation (edge-to-edge is enabled, so Compose
                // draws under the status bar by default -- a fixed dp offset
                // undershoots on taller status bars, e.g. devices with a
                // notch/punch-hole/dynamic island equivalent, and the banner
                // rendered underneath the system clock/icons). windowInsetsPadding
                // applies that real inset as top padding; the extra 10.dp is
                // just breathing room below the status bar, same as before.
                Surface(
                    shape = RoundedCornerShape(D2MRadius.lg),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 10.dp, start = 10.dp, end = 10.dp)
                        .clickable {
                            onOpenPeer(event.peerUsername)
                            current = null
                        },
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(avatarPlaceholder(), RoundedCornerShape(12.dp)))
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
