package com.d2m.app.ui.screens.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.NotificationPreferenceIn
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.Identity
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.domain.repository.NotificationsRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.components.ProfileThumb
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors screens/shared/SettingsScreen.jsx: role-branching -- child gets a
 * tabbed Preferences/Others layout (Preferences moved into
 * ChildProfileDialogScreen here, same "Preferences tab reuses the same
 * fields" split web already made), parent gets a single notifications+
 * logout page (no equivalent basic-data/filters concept for sponsors).
 */
@Composable
fun SettingsScreen(onLogout: () -> Unit, onOpenShareLinks: () -> Unit) {
    val identityStore: IdentityStore = koinInject()
    val notificationsRepo: NotificationsRepository = koinInject()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    var muted by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf("immediate") }

    val accountId = identity.sponsorId ?: identity.primaryId
    LaunchedEffect(accountId) {
        if (accountId == null) return@LaunchedEffect
        runCatching { notificationsRepo.getPreferences(accountId) }.onSuccess {
            muted = it.muted
            frequency = it.frequency
        }
    }

    val flow = if (identity.role == D2MRole.CHILD) D2MFlow.CHILD else D2MFlow.PARENT
    D2MTheme(flow = flow) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PageTitle("Settings")

            AccountHeader(identity = identity, accountId = accountId)
            HorizontalDivider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Mute notifications", fontWeight = FontWeight.Bold)
                    Text("Turn off all push and in-app alerts", color = mutedText(0.55f), style = MaterialTheme.typography.labelMedium)
                }
                Switch(
                    checked = muted,
                    onCheckedChange = { checked ->
                        muted = checked
                        if (accountId != null) {
                            scope.launch { runCatching { notificationsRepo.setPreferences(accountId, NotificationPreferenceIn(muted = checked)) } }
                        }
                    },
                )
            }

            // Child role only -- the parent role reaches this via its own
            // "Sharing" bottom tab (AppScaffold.kt's D2MTab.ParentSharing),
            // but the child tab bar has no free slot (Settings itself was
            // deliberately pulled off it already), so this is the child
            // side's only entry point, same placement as web's
            // SettingsScreen.jsx "Share my profile" button.
            if (identity.role == D2MRole.CHILD) {
                D2MButton(text = "Share my profile", variant = D2MButtonVariant.OUTLINE, onClick = onOpenShareLinks)
            }

            D2MButton(text = "Log out", variant = D2MButtonVariant.OUTLINE, onClick = {
                identityStore.clear()
                onLogout()
            })
        }
    }
}

// The SaaS-standard "who am I" header -- photo/name/contact/account id
// above everything else -- so Settings opens by confirming whose account
// this is before it gets into preferences ("I want it to be how saas
// products have it as. It should have a header section with user image,
// name, email address, account id. Then comes the other settings",
// reported directly). Mirrors d2m_web's SettingsScreen.jsx AccountHeader.
// Fetches the account's own profile once per screen open (name/
// contactInfo aren't part of IdentityStore's own Identity -- that only
// ever holds role + raw ids, see data/session/IdentityStore.kt) rather
// than duplicating another screen's fetch. Sponsors have no photo of
// their own in this app (only a linked child's Primary profile does), so
// the parent role always falls back to ProfileThumb's own PersonGlyph;
// the child role fetches its first photo (if any) the same way
// ChildHomeScreen's own profile row does.
@Composable
private fun AccountHeader(identity: Identity, accountId: String?) {
    val identityRepo: IdentityRepository = koinInject()
    var name by remember { mutableStateOf<String?>(null) }
    var contactInfo by remember { mutableStateOf<String?>(null) }
    var photoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(accountId, identity.role) {
        val pid = accountId ?: return@LaunchedEffect
        if (identity.role == D2MRole.PARENT) {
            runCatching { identityRepo.getSponsorProfile(pid) }.onSuccess {
                name = it.name
                contactInfo = it.contactInfo
            }
        } else {
            runCatching { identityRepo.getPrimaryProfile(pid) }.onSuccess {
                name = it.name
                contactInfo = it.contactInfo
            }
            runCatching { identityRepo.getPhotos(pid) }.onSuccess { photos ->
                photoUrl = photos.firstOrNull()?.url
            }
        }
    }

    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ProfileThumb(photoUrl = photoUrl, contentDescription = name, size = 52.dp, shape = CircleShape)
        Column {
            Text(name ?: "…", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            val contact = contactInfo
            if (!contact.isNullOrBlank()) {
                Text(contact, style = MaterialTheme.typography.bodySmall, color = mutedText(0.55f))
            }
            if (accountId != null) {
                Text(
                    if (copied) "Copied" else accountId,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (copied) d2m.accentStrong else mutedText(0.45f),
                    modifier = Modifier.padding(top = 4.dp).clickable {
                        clipboard.setText(AnnotatedString(accountId))
                        copied = true
                        scope.launch {
                            delay(1500)
                            copied = false
                        }
                    },
                )
            }
        }
    }
}
