package com.d2m.app.ui.screens.parent

import androidx.compose.foundation.layout.Arrangement
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
import com.d2m.app.ui.components.D2MProfileTabsPanel
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
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

    D2MTheme(flow = if (identity.role == D2MRole.CHILD) D2MFlow.CHILD else D2MFlow.PARENT) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            // A quiet text link, not a maroon GHOST button -- the comps
            // open this screen on a breadcrumb row, and the first thing on
            // the page should be the person, not a control.
            LinkText(strings.back, onClick = onBack, modifier = Modifier.padding(bottom = 4.dp))

            when {
                // The shape of what's coming, not a word -- the header
                // block, then the photograph, then the panel.
                loading -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    D2MSkeleton(width = 110.dp, height = 11.dp)
                    D2MSkeleton(width = 220.dp, height = 26.dp)
                    D2MSkeleton(width = 160.dp, height = 14.dp)
                    D2MSkeleton(height = 260.dp, modifier = Modifier.padding(top = 6.dp))
                }
                error != null -> D2MErrorBanner(error!!)
                candidate != null -> {
                    val c = candidate!!
                    /*
                     * Header, per the Profile-detail comp: the registration
                     * id in tracked small caps, the name in the display
                     * serif on its own line, then one meta line.
                     *
                     * Name + age fold into one line -- "Ramesh, 34" --
                     * mirroring both web's port target and the pattern
                     * DiscoveryScreen.kt already uses for the same pairing;
                     * age used to sit in the meta line below instead
                     * (redundant with itself once folded in here, so it's
                     * dropped from that line, not duplicated).
                     *
                     * ScoreBadge (a raw "x/10" numeric tag) is gone from the
                     * header entirely, replaced by CompatibilityCard below
                     * the photo -- see that composable's own doc comment
                     * for why a word beats a fraction here.
                     */
                    RefNoText(c.candidateId)
                    PersonName(
                        c.candidateName + (c.age?.let { ", $it" } ?: ""),
                        Modifier.padding(top = 4.dp),
                    )
                    val meta = listOfNotNull(
                        c.sect?.let { s -> listOfNotNull(c.gothram, s).joinToString(", ") }
                            ?: c.gothram,
                        c.nativity ?: c.city,
                    )
                    if (meta.isNotEmpty()) {
                        MetaText(meta.joinToString(" · "), Modifier.padding(top = 5.dp))
                    }

                    ProfilePhoto(
                        photoUrl = c.photoUrl?.let(apiClient::resolveMediaUrl),
                        contentDescription = strings.photographOf(c.candidateName),
                        ratio = 1f,
                        caption = if (c.photoUrl == null) strings.noPhotographOnFile else null,
                        glyphSize = 52.dp,
                        shape = RoundedCornerShape(D2MRadius.lg),
                        modifier = Modifier.padding(top = 16.dp),
                    )

                    CompatibilityCard(
                        level = c.compositeScore?.let { overallRating(it) },
                        breakdown = listOf(
                            strings.astrology to astrologicalCompatibility(c.scores),
                            strings.preferences to preferenceCompatibility(c.scores),
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )

                    if (c.doshaFlags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        ) {
                            c.doshaFlags.forEach { D2MBadge(it, D2MBadgeTone.WARNING) }
                        }
                    }

                    trustSummary?.badgeExplanations?.takeIf { it.isNotEmpty() }?.let { badges ->
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                badges.forEach { D2MBadge(it.badge, D2MBadgeTone.NEUTRAL) }
                            }
                            // Only when the badge set actually includes a
                            // real, match-visible vouch (familyVouched) --
                            // the other badges (verification, endorsements)
                            // have no individual-identity click-through at
                            // all, and listPublicVouches would just come
                            // back empty for them.
                            if (trustSummary?.familyVouched == true) {
                                LinkText(
                                    strings.seeWhoVouched,
                                    onClick = {
                                        vouchersDialogOpen = true
                                        if (vouchers == null) {
                                            vouchersError = null
                                        }
                                    },
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }

                    D2MProfileTabsPanel(
                        candidateId = candidateId,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        showAboutMe = identity.role == D2MRole.CHILD,
                    )

                    messageParentError?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }
                    shareError?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (identity.role == D2MRole.PARENT) {
                            D2MButton(
                                text = if (actionInFlight) strings.suggesting else strings.suggestToChild,
                                enabled = !actionInFlight,
                                onClick = {
                                    val sponsorId = identity.sponsorId ?: return@D2MButton
                                    scope.launch {
                                        actionInFlight = true
                                        runCatching { suggestionsRepo.suggestToChild(sponsorId, candidateId) }
                                        actionInFlight = false
                                    }
                                },
                            )
                            // Parent-to-parent only (no request/accept gate, see
                            // messageParent()'s own doc comment above). Parent-to-
                            // child messaging deliberately isn't offered here yet --
                            // it needs the consent-unlock gate, which isn't built on
                            // mobile (see ParentMessagesScreen.kt's doc comment).
                            D2MButton(
                                text = if (messageParentBusy) strings.opening else strings.messageTheirParent,
                                variant = D2MButtonVariant.OUTLINE,
                                enabled = !messageParentBusy,
                                onClick = { messageParent() },
                            )
                            D2MButton(
                                text = when (shareState) {
                                    "busy" -> strings.creatingLink
                                    "copied" -> strings.linkCopied
                                    else -> strings.shareThisProfile
                                },
                                variant = D2MButtonVariant.OUTLINE,
                                enabled = shareState != "busy",
                                onClick = { shareProfile() },
                            )
                        } else {
                            listOf("accept" to strings.sendRequest, "snooze" to strings.snooze, "reject" to strings.pass).forEach { (action, label) ->
                                D2MButton(
                                    text = label,
                                    variant = if (action == "accept") D2MButtonVariant.SOLID else D2MButtonVariant.OUTLINE,
                                    enabled = !actionInFlight,
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
