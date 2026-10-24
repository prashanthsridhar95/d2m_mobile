package com.d2m.app.ui.screens.parent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.d2m.app.ui.components.D2MButtonSize
import com.d2m.app.ui.components.DataRow
import com.d2m.app.ui.theme.d2m
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.data.model.VouchOut
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.domain.repository.ShareLinksRepository
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.domain.repository.TrustRepository
import com.d2m.app.messaging.ChatUiState
import com.d2m.app.messaging.ParentContactsStore
import com.d2m.app.messaging.d2mIdToMessagingUsername
import com.d2m.app.ui.components.CompatibilityCard
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.FeaturedBadge
import com.d2m.app.ui.components.D2MProfileTabsPanel
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
import com.d2m.app.ui.components.VerifiedBadge
import com.d2m.app.ui.components.D2MSkeleton
import com.d2m.app.ui.components.LinkText
import com.d2m.app.ui.components.MetaText
import com.d2m.app.ui.components.PersonName
import com.d2m.app.ui.components.ProfilePhoto
import com.d2m.app.ui.components.RefNoText
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.components.astrologicalCompatibility
import com.d2m.app.ui.components.overallRating
import com.d2m.app.ui.components.preferenceCompatibility
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors screens/parent/ProfileDetailScreen.jsx -- shared by both parent
 * and child shells despite living in the "parent" screens package (matches
 * the web app's own file layout, kept for the same reason: the component
 * really is shared, moving it wouldn't change what it does). Role-aware
 * action row: parent gets Suggest to child, child gets Accept/Snooze/Pass.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileDetailScreen(candidateId: String, onBack: () -> Unit, onOpenMessages: () -> Unit = {}) {
    val identityStore: IdentityStore = koinInject()
    val suggestionsRepo: SuggestionsRepository = koinInject()
    val identityRepo: IdentityRepository = koinInject()
    val shareLinksRepo: ShareLinksRepository = koinInject()
    val trustRepo: TrustRepository = koinInject()
    val contactsStore: ParentContactsStore = koinInject()
    val chatUiState: ChatUiState = koinInject()
    val apiClient: ApiClient = koinInject()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val strings = LocalStrings.current.profileDetail

    var candidate by remember { mutableStateOf<SuggestionOut?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionInFlight by remember { mutableStateOf(false) }
    var messageParentBusy by remember { mutableStateOf(false) }
    var messageParentError by remember { mutableStateOf<String?>(null) }
    // "Share profile - from other users (people who are discovering) --
    // only parent mode (Default link share to be generated for all
    // profiles -- use that here)" (reported directly). Always "full" --
    // the only detail level a caller sharing a profile they don't own is
    // allowed to create (app/routers/identity.py::create_share_link).
    var shareState by remember { mutableStateOf("idle") } // idle | busy | copied | error
    var shareError by remember { mutableStateOf<String?>(null) }

    // Open, no request/accept gate -- mirrors ProfileDetailScreen.jsx's
    // handleMessageParent exactly, see that file's own comment: "there is no
    // match policy in parent to parent... when a parent sees a profile &
    // thinks it's a viable profile, they can initiate a conversation with
    // the child's parent, they dont need a request/accept flow." candidateId
    // here is the CHILD's own D2M id (this screen's whole subject), never
    // their Sponsor's -- resolved via GET /primaries/{id}/sponsor first,
    // same two-step lookup web does, since the candidate payload never
    // carries the Sponsor's id directly.
    fun messageParent() {
        val mySponsorId = identity.sponsorId ?: return
        scope.launch {
            messageParentBusy = true
            messageParentError = null
            try {
                val sponsor = identityRepo.getPrimarySponsor(candidateId)
                val sponsorProfile = identityRepo.getSponsorProfile(sponsor.sponsorId)
                val myUsername = d2mIdToMessagingUsername(mySponsorId)
                val peerUsername = d2mIdToMessagingUsername(sponsor.sponsorId)
                // name is ALWAYS the Sponsor's OWN name (never the candidate's) --
                // see ParentContactsStore.kt's doc comment for why that distinction
                // matters (a real reported bug on web when it was seeded wrong).
                contactsStore.registerContact(
                    myUsername = myUsername,
                    peerUsername = peerUsername,
                    peerD2mId = sponsor.sponsorId,
                    name = sponsorProfile.name,
                    kind = "parent",
                    childId = candidateId,
                    childName = sponsorProfile.childName,
                )
                chatUiState.requestOpenPeer(peerUsername)
                onOpenMessages()
            } catch (e: Exception) {
                messageParentError = friendlyError(e, strings.errStartConversation)
            } finally {
                messageParentBusy = false
            }
        }
    }

    fun shareProfile() {
        scope.launch {
            shareState = "busy"
            shareError = null
            try {
                val link = shareLinksRepo.createShareLink(candidateId, "full")
                clipboard.setText(AnnotatedString("${apiClient.baseUrl.trimEnd('/')}/${link.code}"))
                shareState = "copied"
                delay(2000)
                if (shareState == "copied") shareState = "idle"
            } catch (e: Exception) {
                shareState = "error"
                shareError = friendlyError(e, strings.errCreateShareLink)
            }
        }
    }

    val viewerPrimaryId = identity.primaryId ?: identity.childPrimaryId
    // "Preview profile" (reported directly: "I want it to be exactly the
    // same" as viewing any other profile) reuses this exact screen rather
    // than a lookalike -- Settings' "Preview profile" row navigates here
    // with candidateId == the viewer's own id, same route every other
    // "open this candidate" call site uses. A handful of actions below
    // (Accept/Snooze/Pass, Suggest to child, Message, a real compatibility
    // score) don't mean anything pointed at your own profile and are
    // hidden when this is true; everything else (photo, name/badges, bio
    // tabs, chart, vouches you've RECEIVED) renders exactly as it would
    // for anyone else.
    val isSelf = viewerPrimaryId != null && candidateId == viewerPrimaryId

    LaunchedEffect(candidateId, viewerPrimaryId) {
        if (viewerPrimaryId == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            candidate = suggestionsRepo.getCandidate(viewerPrimaryId, candidateId)
        } catch (e: Exception) {
            error = friendlyError(e, strings.errLoadProfile)
        } finally {
            loading = false
        }
    }

    // "In each profile, vouch data to be shown -- on clicking, get
    // information of the people who have vouched" (reported directly).
    // Viewer-agnostic (respects the profile owner's own
    // show_match_trust_signals toggle server-side), so this loads
    // unconditionally, independent of the candidate-card fetch above.
    var trustSummary by remember(candidateId) { mutableStateOf<com.d2m.app.data.model.TrustSummaryOut?>(null) }
    var vouchersDialogOpen by remember { mutableStateOf(false) }
    var vouchers by remember { mutableStateOf<List<VouchOut>?>(null) }
    var vouchersError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(candidateId) {
        trustSummary = try {
            trustRepo.getPublicTrustSummary(candidateId)
        } catch (e: Exception) {
            null
        }
    }

    LaunchedEffect(vouchersDialogOpen) {
        if (!vouchersDialogOpen || vouchers != null) return@LaunchedEffect
        try {
            vouchers = trustRepo.listPublicVouches(candidateId)
        } catch (e: Exception) {
            vouchersError = friendlyError(e, strings.errLoadVouchers)
        }
    }

    // Optimistic shortlist toggle for the sticky bar's heart. Seeded from the
    // fetched candidate; rolls back if the call fails.
    var shortlistedOverride by remember(candidateId) { mutableStateOf<Boolean?>(null) }
    fun toggleShortlist(current: Boolean) {
        val pid = identity.childPrimaryId ?: viewerPrimaryId ?: return
        val now = !current
        shortlistedOverride = now
        scope.launch {
            try {
                if (now) suggestionsRepo.addToShortlist(pid, candidateId) else suggestionsRepo.removeFromShortlist(pid, candidateId)
            } catch (_: Exception) {
                shortlistedOverride = current
            }
        }
    }
    var suggestedDone by remember(candidateId) { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    val shared = LocalStrings.current.sharedComponents

    D2MTheme(flow = if (identity.role == D2MRole.CHILD) D2MFlow.CHILD else D2MFlow.PARENT) {
        Box(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
                when {
                    // Same silhouette as the loaded page: full-bleed photo block,
                    // then the cards.
                    loading -> Column {
                        Box(Modifier.fillMaxWidth().aspectRatio(4f / 5f).background(d2m.surfaceSunken))
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            D2MSkeleton(height = 110.dp, radius = D2MRadius.lg)
                            D2MSkeleton(height = 140.dp, radius = D2MRadius.lg)
                        }
                    }

                    error != null -> Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 64.dp)) { D2MErrorBanner(error!!) }

                    candidate != null -> {
                        val c = candidate!!
                        // ---- full-bleed collapsing hero --------------------
                        Box(Modifier.fillMaxWidth().clipToBounds()) {
                            ProfilePhoto(
                                photoUrl = c.photoUrl?.let(apiClient::resolveMediaUrl),
                                contentDescription = strings.photographOf(c.candidateName),
                                ratio = 4f / 5f,
                                caption = if (c.photoUrl == null) strings.noPhotographOnFile else null,
                                glyphSize = 64.dp,
                                // parallax + fade as the sheet of cards scrolls over it; the
                                // scroll offset is read in the layer block, not in composition
                                modifier = Modifier.graphicsLayer {
                                    translationY = scroll.value * 0.5f
                                    alpha = (1f - scroll.value / (size.height * 1.2f)).coerceIn(0f, 1f)
                                },
                            )
                            Column(
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD91A120C))))
                                    .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 16.dp),
                            ) {
                                c.shortId?.let { RefNoText(it) }      // registration number, never the internal id
                                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        c.candidateName + (c.age?.let { ", $it" } ?: ""),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 2,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (c.isFoundingMember) FeaturedBadge(modifier = Modifier.padding(start = 8.dp))
                                    if (c.isVerified) VerifiedBadge(modifier = Modifier.padding(start = 6.dp))
                                }
                                val meta = listOfNotNull(
                                    c.sect?.let { s -> listOfNotNull(c.gothram, s).joinToString(", ") } ?: c.gothram,
                                    c.nativity ?: c.city,
                                )
                                if (meta.isNotEmpty()) {
                                    Text(
                                        meta.joinToString(" · "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.88f),
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }

                        Column(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (isSelf) 24.dp else 112.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            // A self-pair still gets a real, non-null `scores`
                            // object back (every field 0) -- `!isSelf` keeps it on
                            // the "not yet compared" fallback instead of a real-
                            // looking "Low compatibility" against yourself.
                            CompatibilityCard(
                                level = if (!isSelf) c.compositeScore?.let { overallRating(it) } else null,
                                breakdown = if (!isSelf) listOf(
                                    strings.astrology to astrologicalCompatibility(c.scores),
                                    strings.preferences to preferenceCompatibility(c.scores),
                                ) else emptyList(),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            val badges = trustSummary?.badgeExplanations?.takeIf { it.isNotEmpty() }
                            if (c.doshaFlags.isNotEmpty() || badges != null) {
                                D2MCard(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (c.doshaFlags.isNotEmpty()) {
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                c.doshaFlags.forEach { D2MBadge(it, D2MBadgeTone.WARNING) }
                                            }
                                        }
                                        if (badges != null) {
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                badges.forEach { D2MBadge(it.badge, D2MBadgeTone.NEUTRAL) }
                                            }
                                            // Only when the badge set includes a real, match-visible
                                            // vouch (familyVouched); other badges have no
                                            // individual-identity click-through.
                                            if (trustSummary?.familyVouched == true) {
                                                LinkText(
                                                    strings.seeWhoVouched,
                                                    onClick = {
                                                        vouchersDialogOpen = true
                                                        if (vouchers == null) vouchersError = null
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            val facts = buildList {
                                listOfNotNull(c.gothram, c.sect).joinToString(", ")
                                    .takeIf { it.isNotEmpty() }?.let { add(shared.matchRowGothram to it) }
                                c.moonNakshatra?.let { n -> add(shared.matchRowStar to (c.moonPada?.let { "$n, pada $it" } ?: n)) }
                                c.occupationTitle?.let { add(shared.matchRowWork to it) }
                                c.employer?.takeIf { it.isNotBlank() }?.let { add(shared.matchRowOrganisation to it) }
                            }
                            if (facts.isNotEmpty()) {
                                D2MCard(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp)) { facts.forEach { (l, v) -> DataRow(l, v) } }
                                }
                            }

                            D2MProfileTabsPanel(
                                candidateId = candidateId,
                                modifier = Modifier.fillMaxWidth(),
                                showAboutMe = identity.role == D2MRole.CHILD,
                                viewerPrimaryId = viewerPrimaryId,
                            )
                        }
                    }
                }
            }

            // ---- floating back / share over the photo ----------------------
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FloatingCircleButton(Icons.AutoMirrored.Filled.ArrowBack, strings.back, onClick = onBack)
                // Share is parent-only and meaningless pointed at your own profile
                // (same "Share my profile" row exists in Settings).
                if (candidate != null && !isSelf && identity.role == D2MRole.PARENT) {
                    FloatingCircleButton(
                        icon = if (shareState == "copied") Icons.Filled.Check else Icons.Filled.Share,
                        contentDescription = when (shareState) {
                            "busy" -> strings.creatingLink
                            "copied" -> strings.linkCopied
                            else -> strings.shareThisProfile
                        },
                        enabled = shareState != "busy",
                        onClick = { shareProfile() },
                    )
                }
            }

            // ---- sticky action bar -----------------------------------------
            // Suggest to child / Message their parent / Share, or Accept /
            // Snooze / Pass -- none mean anything pointed at your own
            // profile, so hidden entirely for a self-preview.
            val cand = candidate
            if (cand != null && !isSelf) {
                val shortlisted = shortlistedOverride ?: cand.isShortlisted
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, d2m.border))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    messageParentError?.let { D2MErrorBanner(it) }
                    shareError?.let { D2MErrorBanner(it) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (identity.role == D2MRole.PARENT) {
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(D2MRadius.md))
                                    .border(BorderStroke(1.5.dp, d2m.gold), RoundedCornerShape(D2MRadius.md))
                                    .background(if (shortlisted) d2m.gold.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        toggleShortlist(shortlisted)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (shortlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (shortlisted) shared.removeFromShortlist else shared.addToShortlist,
                                    tint = if (shortlisted) d2m.accent else d2m.goldStrong,
                                )
                            }
                            // Parent-to-parent only (no request/accept gate, see
                            // messageParent()'s doc comment). Parent-to-child messaging
                            // deliberately isn't offered here yet -- it needs the
                            // consent-unlock gate, which isn't built on mobile.
                            D2MButton(
                                text = if (messageParentBusy) strings.opening else strings.messageTheirParent,
                                variant = D2MButtonVariant.OUTLINE,
                                size = D2MButtonSize.LG,
                                enabled = !messageParentBusy,
                                modifier = Modifier.weight(1f),
                                onClick = { messageParent() },
                            )
                            D2MButton(
                                text = when {
                                    actionInFlight -> strings.suggesting
                                    suggestedDone -> strings.suggested
                                    else -> strings.suggestToChild
                                },
                                size = D2MButtonSize.LG,
                                enabled = !actionInFlight && !suggestedDone,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val sponsorId = identity.sponsorId ?: return@D2MButton
                                    scope.launch {
                                        actionInFlight = true
                                        runCatching { suggestionsRepo.suggestToChild(sponsorId, candidateId) }
                                            .onSuccess { suggestedDone = true }
                                        actionInFlight = false
                                    }
                                },
                            )
                        } else {
                            listOf("accept" to strings.sendRequest, "snooze" to strings.snooze, "reject" to strings.pass).forEach { (action, label) ->
                                D2MButton(
                                    text = label,
                                    variant = if (action == "accept") D2MButtonVariant.SOLID else D2MButtonVariant.OUTLINE,
                                    size = D2MButtonSize.LG,
                                    enabled = !actionInFlight,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val pid = viewerPrimaryId ?: return@D2MButton
                                        scope.launch {
                                            actionInFlight = true
                                            runCatching { suggestionsRepo.act(pid, candidateId, action) }
                                            actionInFlight = false
                                            onBack()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (vouchersDialogOpen) {
            Dialog(onDismissRequest = { vouchersDialogOpen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                D2MCard(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(strings.whoVouchedFor(candidate?.candidateName ?: strings.thisProfile), style = MaterialTheme.typography.titleMedium)
                        vouchersError?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }
                        Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(top = 14.dp)) {
                            when {
                                vouchers == null && vouchersError == null -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    repeat(2) {
                                        D2MCard(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                D2MSkeleton(width = 140.dp, height = 13.dp)
                                                D2MSkeleton(width = 90.dp, height = 11.dp)
                                            }
                                        }
                                    }
                                }
                                vouchers?.isEmpty() == true -> Text(strings.noMatchVisibleVouches, color = mutedText(0.55f))
                                else -> vouchers?.forEach { v ->
                                    D2MCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(v.voucherName ?: strings.aFamilyMember, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                v.claimScopes.joinToString(", ") { it.lowercase().replace('_', ' ').replaceFirstChar { c -> c.uppercase() } },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = mutedText(0.55f),
                                                modifier = Modifier.padding(top = 4.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        D2MButton(
                            text = strings.close,
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                            onClick = { vouchersDialogOpen = false },
                        )
                    }
                }
            }
        }
    }
}

/** Round, scrim-backed icon button that stays legible over any photograph; 48dp touch target. */
@Composable
private fun FloatingCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(percent = 50)).background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
                Icon(icon, contentDescription = contentDescription, tint = Color.White)
            }
        }
    }
}
