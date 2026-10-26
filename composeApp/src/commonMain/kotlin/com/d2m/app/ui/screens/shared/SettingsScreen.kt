package com.d2m.app.ui.screens.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.components.ProfileThumb
import com.d2m.app.ui.components.SettingsCard
import com.d2m.app.ui.components.SettingsDivider
import com.d2m.app.ui.components.SettingsRow
import com.d2m.app.ui.components.SettingsSection
import com.d2m.app.ui.components.SettingsSwitchRow
import com.d2m.app.ui.components.FeaturedBadge
import com.d2m.app.ui.components.VerifiedBadge
import com.d2m.app.ui.components.bounceClickable
import com.d2m.app.ui.components.rememberHaptics
import com.d2m.app.ui.strings.AppLocale
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.strings.LocaleStore
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors screens/shared/SettingsScreen.jsx, restyled mobile-first: a profile
 * header card, then grouped inset sections (Profile & sharing, Trust & safety,
 * Preferences, Family) and a standalone Log out row. Role gating is unchanged:
 * child-only "Share my profile"; hide-name / verification / family-link /
 * preview need a linked primary profile.
 */
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onOpenShareLinks: () -> Unit,
    onOpenTrust: () -> Unit,
    onOpenVerification: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenProfilePreview: (String) -> Unit,
) {
    val identityStore: IdentityStore = koinInject()
    val notificationsRepo: NotificationsRepository = koinInject()
    val identityRepo: IdentityRepository = koinInject()
    val trustRepo: TrustRepository = koinInject()
    val localeStore: LocaleStore = koinInject()
    val identity by identityStore.identity.collectAsState()
    val locale by localeStore.locale.collectAsState()
    val strings = LocalStrings.current.settings
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    var muted by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf("immediate") }
    var hideNameOverride by remember { mutableStateOf(false) }
    var primaryGender by remember { mutableStateOf("") }
    var primaryVerified by remember { mutableStateOf(false) }
    var verificationStatus by remember { mutableStateOf<String?>(null) }
    var languageSheet by remember { mutableStateOf(false) }
    var logoutConfirm by remember { mutableStateOf(false) }

    val accountId = identity.sponsorId ?: identity.primaryId
    // A Sponsor's linked child is identity.childPrimaryId (resolved and cached
    // onto Identity the same way ParentBrowseScreen.kt does for search); if
    // nothing has populated it yet, the linked-profile rows just don't render.
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
            primaryGender = it.gender
            primaryVerified = it.isVerified
        }
        verificationStatus = runCatching { identityRepo.getIdentityVerification(linkedPrimaryId)?.status }.getOrNull()
    }

    val flow = if (identity.role == D2MRole.CHILD) D2MFlow.CHILD else D2MFlow.PARENT
    D2MTheme(flow = flow) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            PageTitle(strings.pageTitle, modifier = Modifier.padding(start = 4.dp))

            AccountHeader(identity = identity, accountId = accountId, verified = identity.role == D2MRole.CHILD && primaryVerified, onOpenProfile = onOpenProfile)

            // Both roles: the parent's former Sharing tab now lives here too.
            SettingsSection(strings.sectionProfile) {
                if (linkedPrimaryId != null) {
                    SettingsRow(
                        icon = Icons.Filled.Visibility,
                        title = strings.previewProfile,
                        subtitle = strings.previewProfileHint,
                        onClick = { onOpenProfilePreview(linkedPrimaryId) },
                    )
                }
                if (linkedPrimaryId != null) SettingsDivider()
                SettingsRow(
                    icon = Icons.Filled.Share,
                    title = strings.shareMyProfile,
                    subtitle = strings.shareMyProfileSubtitle,
                    onClick = onOpenShareLinks,
                )
            }

            SettingsSection(strings.sectionTrust) {
                SettingsRow(
                    icon = Icons.Filled.Shield,
                    title = strings.trustAndVouches,
                    subtitle = strings.trustSubtitle,
                    onClick = onOpenTrust,
                )
                // Verification -- both roles (a parent can submit on the child's behalf).
                if (linkedPrimaryId != null) {
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Filled.VerifiedUser,
                        title = strings.verification,
                        subtitle = if (primaryVerified || verificationStatus == "approved") null else strings.verificationSubtitle,
                        onClick = onOpenVerification,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val (label, tone) = when {
                                    primaryVerified || verificationStatus == "approved" -> strings.statusVerified to D2MBadgeTone.SUCCESS
                                    verificationStatus == "pending" -> strings.statusInReview to D2MBadgeTone.WARNING
                                    verificationStatus == "rejected" -> strings.statusRejected to D2MBadgeTone.DANGER
                                    else -> strings.statusNotStarted to D2MBadgeTone.NEUTRAL
                                }
                                D2MBadge(label, tone)
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = mutedText(0.45f))
                            }
                        },
                    )
                }
            }

            SettingsSection(strings.sectionPreferences) {
                SettingsRow(
                    icon = Icons.Filled.Language,
                    title = strings.languageTitle,
                    onClick = { languageSheet = true },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(locale.nativeName, style = MaterialTheme.typography.bodyMedium, color = mutedText(0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = mutedText(0.45f))
                        }
                    },
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon = Icons.Filled.NotificationsOff,
                    title = strings.muteNotificationsTitle,
                    subtitle = strings.muteNotificationsSubtitle,
                    checked = muted,
                    onCheckedChange = { checked ->
                        muted = checked
                        if (accountId != null) {
                            scope.launch { runCatching { notificationsRepo.setPreferences(accountId, NotificationPreferenceIn(muted = checked)) } }
                        }
                    },
                )
                // "Hide name": a name auto-masks until a match exists, then unmasks; this
                // is the manual override that keeps it masked even after a match.
                // Same gate as web: the child's own control, shown only for a female profile.
                if (linkedPrimaryId != null && identity.role == D2MRole.CHILD && primaryGender.equals("female", ignoreCase = true)) {
                    SettingsDivider()
                    SettingsSwitchRow(
                        icon = Icons.Filled.VisibilityOff,
                        title = strings.hideNameTitle,
                        subtitle = strings.hideNameSubtitle,
                        checked = hideNameOverride,
                        onCheckedChange = { checked ->
                            hideNameOverride = checked
                            scope.launch { runCatching { identityRepo.setHideNameOverride(linkedPrimaryId, checked) } }
                        },
                    )
                }
            }

            if (linkedPrimaryId != null) {
                FamilyLinkSection(identity = identity, linkedPrimaryId = linkedPrimaryId, trustRepo = trustRepo)
            }

            SettingsCard {
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = strings.logOut,
                    destructive = true,
                    trailing = null,
                    onClick = { logoutConfirm = true },
                )
            }

            Text(
                "D2M",
                style = MaterialTheme.typography.labelMedium,
                color = mutedText(0.35f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            Spacer(Modifier.height(24.dp))
        }

        if (languageSheet) LanguageSheet(localeStore = localeStore, hint = strings.languageHint, title = strings.languageTitle, onDismiss = { languageSheet = false })

        if (logoutConfirm) {
            AlertDialog(
                onDismissRequest = { logoutConfirm = false },
                title = { Text(strings.logOutConfirmTitle) },
                text = { Text(strings.logOutConfirmBody) },
                confirmButton = {
                    TextButton(onClick = {
                        logoutConfirm = false
                        haptics.medium()
                        identityStore.clear()
                        onLogout()
                    }) { Text(strings.logOut, color = com.d2m.app.ui.theme.LocalD2MStatusPalette.current.danger.fg) }
                },
                dismissButton = { TextButton(onClick = { logoutConfirm = false }) { Text(strings.cancel) } },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSheet(localeStore: LocaleStore, hint: String, title: String, onDismiss: () -> Unit) {
    val locale by localeStore.locale.collectAsState()
    val haptics = rememberHaptics()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = d2m.textPrimary,
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            SettingsCard {
                AppLocale.entries.forEachIndexed { i, l ->
                    if (i > 0) SettingsDivider(inset = 16.dp)
                    Row(
                        Modifier.fillMaxWidth().bounceClickable(
                            onClick = { haptics.light(); localeStore.setLocale(l); onDismiss() },
                            pressedScale = 0.985f, enforceMinimumTarget = false,
                        ).padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            l.nativeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (l == locale) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (l == locale) Icon(Icons.Filled.Check, contentDescription = null, tint = d2m.accent)
                    }
                }
            }
            Text(hint, style = MaterialTheme.typography.labelMedium, color = mutedText(0.55f), modifier = Modifier.padding(start = 4.dp, top = 12.dp))
        }
    }
}

// "Linking management of parent with child & vice versa" -- not built on
// StepUpConfirmDialog.kt's pattern: that mints a pre-scoped X-Step-Up-Token
// which can't satisfy unlink_family's stricter, action-bound
// require_action_proof (see trust_bridge_service.unlink_family on the
// d2m_core_engine side); this sends the raw password straight through.
@Composable
private fun FamilyLinkSection(identity: Identity, linkedPrimaryId: String, trustRepo: TrustRepository) {
    val strings = LocalStrings.current.settings
    val scope = rememberCoroutineScope()
    var dialogOpen by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }

    SettingsSection(strings.sectionFamily) {
        if (done) {
            Text(
                strings.familyLinkUnlinkedNotice,
                style = MaterialTheme.typography.bodySmall,
                color = mutedText(0.55f),
                modifier = Modifier.padding(16.dp),
            )
        } else {
            SettingsRow(
                icon = Icons.Filled.LinkOff,
                title = strings.unlinkFamilyAccount,
                subtitle = if (identity.role == D2MRole.PARENT) strings.familyLinkUnlinkDescParent else strings.familyLinkUnlinkDescChild,
                destructive = true,
                trailing = null,
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

@Composable
private fun AccountHeader(identity: Identity, accountId: String?, verified: Boolean, onOpenProfile: () -> Unit) {
    val strings = LocalStrings.current.settings
    val identityRepo: IdentityRepository = koinInject()
    var name by remember { mutableStateOf<String?>(null) }
    var contactInfo by remember { mutableStateOf<String?>(null) }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var founding by remember { mutableStateOf(false) }

    // Name/contact aren't part of IdentityStore's Identity (role + raw ids only), so fetch once per open.
    // Sponsors have no photo of their own; the child role uses its first photo.
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
                founding = it.isFoundingMember
            }
            runCatching { identityRepo.getPhotos(pid) }.onSuccess { photos ->
                photoUrl = photos.firstOrNull()?.url
            }
        }
    }

    val displayName = name?.takeIf { it.isNotBlank() } ?: strings.yourAccount
    val roleLabel = if (identity.role == D2MRole.PARENT) strings.roleParent else strings.roleChild

    SettingsCard(Modifier.bounceClickable(onClick = onOpenProfile, pressedScale = 0.985f, enforceMinimumTarget = false)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            ProfileThumb(
                photoUrl = photoUrl,
                contentDescription = displayName,
                size = 72.dp,
                shape = CircleShape,
                fallback = if (name.isNullOrBlank()) null else ({
                    Text(
                        displayName.first().uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = d2m.accentStrong,
                    )
                }),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, style = MaterialTheme.typography.titleLarge, color = d2m.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                    Text(roleLabel, style = MaterialTheme.typography.bodyMedium, color = mutedText(0.55f))
                    if (founding) FeaturedBadge(size = 16.dp)
                    if (verified) VerifiedBadge(size = 16.dp)
                }
                val contact = contactInfo
                if (!contact.isNullOrBlank()) {
                    Text(contact, style = MaterialTheme.typography.bodySmall, color = mutedText(0.45f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = mutedText(0.45f))
        }
    }
}
