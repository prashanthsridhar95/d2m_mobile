package com.d2m.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import org.koin.compose.koinInject

/**
 * Mirrors screens/parent/HandoffScreen.jsx. Reads from OnboardingResultHolder
 * (this app's equivalent of the web app's router-state hand-off) -- a direct
 * deep-link/relaunch onto this screen with nothing in the holder shows the
 * same "no invite to show, start setup" fallback the web version has, since
 * nothing here is meant to persist across a process restart either.
 *
 * The native share sheet replaces web's copy-link/WhatsApp-button pair --
 * see plan §4: a strictly better mobile-native choice, since the OS share
 * sheet already includes WhatsApp, Messages, email, etc. Wiring the actual
 * platform share intent is a small expect/actual (see platform/ShareSheet.kt)
 * left as a follow-up alongside the other platform actuals in this build.
 */
@Composable
fun HandoffScreen(onBrowseProfiles: () -> Unit) {
    val resultHolder: OnboardingResultHolder = koinInject()
    val handoff by resultHolder.current.collectAsState()

    D2MTheme(flow = D2MFlow.PARENT) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            val h = handoff
            if (h == null) {
                D2MEmptyState("No invite to show", "Start setup again to generate a new one.")
            } else {
                Text("Sent to ${h.childName}.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "A private claim link is ready to share -- theirs to open whenever they're ready. Expires ${h.inviteExpiresAt}.",
                    color = mutedText(0.55f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                )
                D2MButton(text = "Share invite", onClick = { /* platform share sheet -- see doc comment */ })
                D2MButton(text = "Browse profiles", variant = com.d2m.app.ui.components.D2MButtonVariant.OUTLINE, onClick = onBrowseProfiles, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
