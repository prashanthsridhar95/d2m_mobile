package com.d2m.app.ui.screens.parent

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme

/**
 * Mirrors screens/parent/ParentMessagesScreen.jsx (sponsor-to-sponsor
 * contact after consent is granted). Real-time E2E chat is Phase 6 (see
 * plan §6/§8 and messaging/README.md) -- until then this surfaces as an
 * honest "not yet" state rather than a fake static conversation, matching
 * how d2m_web's own MatchesScreen looked before its messaging integration
 * landed (two hardcoded bubbles + a banner, per D2M_Messaging_Integration_
 * Plan.md's own description of that prior state) -- we're not repeating
 * that placeholder-that-looks-real pattern here.
 */
@Composable
fun ParentMessagesScreen() {
    D2MTheme(flow = D2MFlow.PARENT) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            D2MEmptyState(
                title = "Messaging is coming in a later release",
                subtitle = "Real-time encrypted chat (Phase 6) isn't wired up yet in this build. The match state machine -- requests, Serious Mode, consent -- all works today.",
            )
        }
    }
}
