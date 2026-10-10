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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.d2m.app.data.model.NotificationPreferenceIn
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.Identity
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.domain.repository.NotificationsRepository
import com.d2m.app.domain.repository.TrustRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.LanguageSettingControl
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.components.ProfileThumb
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.strings.LocaleStore
import com.d2m.app.ui.strings.SettingsStrings
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
fun SettingsScreen(onLogout: () -> Unit, onOpenShareLinks: () -> Unit, onOpenTrust: () -> Unit) {
    val identityStore: IdentityStore = koinInject()
    val notificationsRepo: NotificationsRepository = koinInject()
    val identityRepo: IdentityRepository = koinInject()
    val trustRepo: TrustRepository = koinInject()
    val localeStore: LocaleStore = koinInject()
    val identity by identityStore.identity.collectAsState()
    val strings = LocalStrings.current.settings
    val scope = rememberCoroutineScope()

    var muted by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf("immediate") }
    var hideNameOverride by remember { mutableStateOf(false) }

    val accountId = identity.sponsorId ?: identity.primaryId
    // "Linking management of parent with child & vice versa" (reported
    // directly) -- a Sponsor's linked child is identity.childPrimaryId
    // (resolved and cached onto Identity the same way ParentBrowseScreen.kt
    // does for search); if nothing has populated it yet (a first-ever
    // visit landing directly on Settings), this section just doesn't
    // render rather than re-implementing that lookup here too.
    val linkedPrimaryId = if (identity.role == D2MRole.PARENT) identity.childPrimaryId else identity.primaryId
    LaunchedEffect(accountId) {
        if (accountId == null) return@LaunchedEffect
        runCatching { notificationsRepo.getPreferences(accountId) }.onSuccess {
            muted = it.muted
            frequency = it.frequency
        }
    }
    LaunchedEffect(linkedPrimaryId) {
        if (linkedPrimaryId == null) return@LaunchedEffect
        runCatching { identityRepo.getPrimaryProfile(linkedPrimaryId) }.onSuccess {
            hideNameOverride = it.hideNameOverride
        }
    }

    val flow = if (identity.role == D2MRole.CHILD) D2MFlow.CHILD else D2MFlow.PARENT
    D2MTheme(flow = flow) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PageTitle(strings.pageTitle)

            AccountHeader(identity = identity, accountId = accountId)
            HorizontalDivider()

            Column {
                Text(strings.languageTitle, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                LanguageSettingControl(localeStore = localeStore, hint = strings.languageHint)
            }
            HorizontalDivider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(strings.muteNotificationsTitle, fontWeight = FontWeight.Bold)
                    Text(strings.muteNotificationsSubtitle, color = mutedText(0.55f), style = MaterialTheme.typography.labelMedium)
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

            // "Hide name" (reported directly) -- by default a name
            // auto-masks to any viewer until a match exists, then auto-
            // unmasks (no setting needed for that half, see suggestion_
            // service._display_name on the backend). This switch is the
            // manual override: keeps it masked even after a match.
            if (linkedPrimaryId != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(strings.hideNameTitle, fontWeight = FontWeight.Bold)
                        Text(strings.hideNameSubtitle, color = mutedText(0.55f), style = MaterialTheme.typography.labelMedium)
                    }
                    Switch(
                        checked = hideNameOverride,
                        onCheckedChange = { checked ->
                            hideNameOverride = checked
                            scope.launch { runCatching { identityRepo.setHideNameOverride(linkedPrimaryId, checked) } }
                        },
                    )
                }
            }

            // Child role only -- the parent role reaches this via its own
            // "Sharing" bottom tab (AppScaffold.kt's D2MTab.ParentSharing),
            // but the child tab bar has no free slot (Settings itself was
            // deliberately pulled off it already), so this is the child
            // side's only entry point, same placement as web's
            // SettingsScreen.jsx "Share my profile" button.
            if (identity.role == D2MRole.CHILD) {
                D2MButton(text = strings.shareMyProfile, variant = D2MButtonVariant.OUTLINE, onClick = onOpenShareLinks)
            }

            // WedLock trust subsystem (vouches/trusted connections/
            // endorsements) -- neither role's bottom tab bar has a free
            // slot (both already have 4, see AppScaffold.kt's D2MTab), so
            // this is the entry point for both roles, same "Settings" home
            // "Share my profile" is already using for the child side.
            D2MButton(text = strings.trustAndVouches, variant = D2MButtonVariant.OUTLINE, onClick = onOpenTrust)

            if (linkedPrimaryId != null) {
                FamilyLinkSection(identity = identity, linkedPrimaryId = linkedPrimaryId, trustRepo = trustRepo)
            }

            D2MButton(text = strings.logOut, variant = D2MButtonVariant.OUTLINE, onClick = {
                identityStore.clear()
                onLogout()
            })
        }
    }
}

// "Linking management of parent with child & vice versa" (reported
// directly) -- not built on StepUpConfirmDialog.kt's existing pattern:
// that mints a pre-scoped X-Step-Up-Token via WedLock's generic
// reauthenticate(), which can't satisfy unlink_family's stricter,
// action-bound require_action_proof (confirmed by live testing against
// the real stack -- see trust_bridge_service.unlink_family's own
// docstring on the d2m_core_engine side); this sends the raw password
// straight through instead.
@Composable
private fun FamilyLinkSection(identity: Identity, linkedPrimaryId: String, trustRepo: TrustRepository) {
    val strings = LocalStrings.current.settings
    val scope = rememberCoroutineScope()
    var dialogOpen by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }

    HorizontalDivider()
    Column {
        Text(strings.familyLinkTitle, fontWeight = FontWeight.Bold)
        if (done) {
            Text(
                strings.familyLinkUnlinkedNotice,
                style = MaterialTheme.typography.bodySmall,
                color = mutedText(0.55f),
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            Text(
                if (identity.role == D2MRole.PARENT) strings.familyLinkUnlinkDescParent else strings.familyLinkUnlinkDescChild,
                style = MaterialTheme.typography.bodySmall,
                color = mutedText(0.55f),
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            D2MButton(
                text = strings.unlinkFamilyAccount,
                variant = D2MButtonVariant.OUTLINE,
                onClick = { password = ""; error = null; dialogOpen = true },
            )
        }
    }

    if (dialogOpen) {
        Dialog(onDismissRequest = { if (!busy) dialogOpen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            D2MCard(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(strings.confirmWithPassword, style = MaterialTheme.typography.titleMedium)
                    Text(
                        strings.unlinkModalSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedText(0.55f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                    )
                    D2MTextField(label = strings.passwordLabel, value = password, onValueChange = { password = it }, isPassword = true)
                    error?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        D2MButton(
                            text = strings.cancel, variant = D2MButtonVariant.OUTLINE, enabled = !busy,
                            modifier = Modifier.weight(1f),
                            onClick = { dialogOpen = false },
                        )
                        D2MButton(
                            text = if (busy) strings.unlinking else strings.unlink,
                            enabled = !busy && password.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                busy = true
                                error = null
                                scope.launch {
                                    try {
                                        trustRepo.unlinkFamily(linkedPrimaryId, password)
                                        done = true
                                        dialogOpen = false
                                    } catch (e: Exception) {
                                        error = friendlyError(e, strings.errUnlink)
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                        )
                    }
                }
            }
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
    val strings = LocalStrings.current.settings
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
                    if (copied) strings.copied else accountId,
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
