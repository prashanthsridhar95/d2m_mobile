package com.d2m.app.ui.screens.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.ConnectionOut
import com.d2m.app.data.model.EndorsementOut
import com.d2m.app.data.model.ExternalReferenceOut
import com.d2m.app.data.model.RecommendationOut
import com.d2m.app.data.model.VouchOut
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.DashboardRepository
import com.d2m.app.domain.repository.TrustRepository
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MCheckboxRow
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MSelectField
import com.d2m.app.ui.components.D2MSkeleton
import com.d2m.app.ui.components.D2MTabs
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.components.StepUpController
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val VOUCH_CLAIM_SCOPES = listOf("IDENTITY", "FAMILY_DETAILS", "PROFILE_ACCURACY")
private val ENDORSEMENT_CLAIM_SCOPES = listOf("EDUCATION", "OCCUPATION", "PROFILE_ACCURACY", "KNOWN_PERSONALLY")
private val RECOMMENDATION_CONFIRMED_SCOPES = listOf("IDENTITY", "EDUCATION", "OCCUPATION", "PROFILE_ACCURACY", "KNOWN_PERSONALLY")
private val VISIBILITY_OPTIONS = listOf("PRIVATE", "MATCHES_ONLY")
private val CONNECTION_TYPES = listOf("SIBLING", "RELATIVE", "FRIEND", "COLLEAGUE", "COMMUNITY_REFERENCE")
private const val EXTERNAL_REFERENCE_EXPIRES_HOURS = 72

private fun humanize(s: String): String = s.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun statusTone(status: String): D2MBadgeTone = when {
    status.contains("APPROVED") || status == "ACTIVE" -> D2MBadgeTone.SUCCESS
    status.contains("PENDING") -> D2MBadgeTone.WARNING
    status.contains("DECLIN") || status.contains("REVOK") -> D2MBadgeTone.DANGER
    else -> D2MBadgeTone.NEUTRAL
}

// Shared loading placeholder for every list in this screen (vouches
// received/given, connections, endorsements, recommendations, external
// references) -- previews the same "card, a couple text lines" shape
// each list's real rows already render in. Replaces the plain
// Text("Loading…") every section used to show for its list content.
@Composable
private fun TrustListSkeleton(count: Int = 2) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) {
            D2MCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    D2MSkeleton(width = 140.dp, height = 13.dp)
                    D2MSkeleton(width = 90.dp, height = 11.dp)
                }
            }
        }
    }
}

/**
 * WedLock trust subsystem (app/routers/trust.py): vouches, trusted
 * connections, endorsements (Round 1), plus recommendations and the
 * authenticated side of external references (Round 2), as five tabs.
 * Mirrors ShareLinksScreen.kt's list/create-form shape -- the closest
 * existing template for "list what exists, offer a small create form
 * above it".
 *
 * External references' referee-facing OTP-verify/submit step is
 * unauthenticated on WedLock's own side and is a web-only public page
 * (d2m_web builds it) -- this screen only ever creates/lists/approves/
 * declines/hides/withdraws a reference, never renders the referee's own
 * form.
 *
 * No free bottom-tab slot for either role (AppScaffold.kt's D2MTab already
 * has 4 each), so this is reached from Settings for both roles (see
 * SettingsScreen.kt's "Trust & vouches" button) rather than getting its own
 * D2MTab the way ShareLinksScreen did on the parent side.
 *
 * Action buttons (approve/decline/withdraw/revoke) are shown whenever an
 * item's status makes the action plausible, without trying to determine
 * client-side whether THIS account is the one allowed to take it --
 * WedLock's own authorization decides that server-side (see
 * trust_bridge_service.py's doc comment: "D2M doesn't duplicate that
 * check"), so the wrong actor simply gets a real error back, surfaced via
 * the same D2MErrorBanner every other screen uses.
 */
@Composable
fun TrustScreen() {
    val identityStore: IdentityStore = koinInject()
    val trustRepo: TrustRepository = koinInject()
    val dashboardRepo: DashboardRepository = koinInject()
    val stepUpController: StepUpController = koinInject()
    val identity by identityStore.identity.collectAsState()

    var notClaimed by remember { mutableStateOf(false) }
    var tabIndex by remember { mutableStateOf(0) }

    // Same childPrimaryId resolution as ShareLinksScreen.kt -- see that
    // file's doc comment. Vouches/endorsements are profile-scoped
    // (primary_id in the URL); connections are account-level and need no
    // primary_id at all, so that tab works even before this resolves.
    val primaryId = if (identity.role == D2MRole.CHILD) identity.primaryId else identity.childPrimaryId

    LaunchedEffect(identity.role, identity.sponsorId, primaryId) {
        if (identity.role == D2MRole.PARENT && primaryId == null) {
            val sponsorId = identity.sponsorId
            if (sponsorId != null) {
                runCatching { dashboardRepo.getSponsorChild(sponsorId) }
                    .onSuccess { identityStore.updateChildPrimaryId(it.primaryId) }
                    .onFailure { notClaimed = true }
            }
        }
    }

    val flow = if (identity.role == D2MRole.CHILD) D2MFlow.CHILD else D2MFlow.PARENT
    D2MTheme(flow = flow) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            PageTitle("Trust & vouches")
            Text(
                "Vouches, trusted connections, and endorsements from WedLock's identity network -- people who know this family can back up details on this profile.",
                style = MaterialTheme.typography.bodySmall,
                color = mutedText(0.55f),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )

            D2MTabs(
                titles = listOf("Vouches", "Connections", "Endorsements", "Recommendations", "References"),
                selectedIndex = tabIndex,
                onSelect = { tabIndex = it },
            )

            Column(modifier = Modifier.padding(top = 16.dp)) {
                when (tabIndex) {
                    0 -> VouchesSection(primaryId, notClaimed, identity.role, trustRepo, stepUpController)
                    1 -> ConnectionsSection(trustRepo, stepUpController)
                    2 -> EndorsementsSection(primaryId, notClaimed, identity.role, trustRepo, stepUpController)
                    3 -> RecommendationsSection(primaryId, notClaimed, identity.role, trustRepo, stepUpController)
                    else -> ExternalReferencesSection(primaryId, notClaimed, identity.role, trustRepo, stepUpController)
                }
            }
        }
    }
}

// -- Vouches ------------------------------------------------------------

@Composable
private fun VouchesSection(
    primaryId: String?,
    notClaimed: Boolean,
    role: D2MRole?,
    trustRepo: TrustRepository,
    stepUpController: StepUpController,
) {
    val scope = rememberCoroutineScope()
    var vouches by remember { mutableStateOf<List<VouchOut>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var selectedScopes by remember { mutableStateOf(setOf<String>()) }
    var visibility by remember { mutableStateOf("PRIVATE") }
    var creating by remember { mutableStateOf(false) }

    // Received (profile-scoped, GET /profiles/{id}/vouches) vs given
    // (account-scoped, GET /vouches/given) -- two different queries, both
    // load independently, same split as d2m_web's TrustScreen.jsx.
    var vouchTab by remember { mutableStateOf(0) } // 0 = received, 1 = given
    var givenVouches by remember { mutableStateOf<List<VouchOut>?>(null) }
    var givenLoading by remember { mutableStateOf(true) }
    var givenError by remember { mutableStateOf<String?>(null) }

    fun reloadGiven() {
        scope.launch {
            givenLoading = true
            givenError = null
            try {
                givenVouches = trustRepo.listGivenVouches(forceRefresh = true)
            } catch (e: Exception) {
                givenError = friendlyError(e, "Couldn't load vouches you've given.")
            } finally {
                givenLoading = false
            }
        }
    }

    fun reload() {
        val id = primaryId ?: return
        scope.launch {
            loading = true
            error = null
            try {
                vouches = trustRepo.listVouches(id, forceRefresh = true)
            } catch (e: Exception) {
                error = friendlyError(e, "Couldn't load vouches.")
            } finally {
                loading = false
            }
        }
        reloadGiven()
    }

    LaunchedEffect(primaryId) {
        val id = primaryId
        if (id == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            vouches = trustRepo.listVouches(id)
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load vouches.")
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        givenLoading = true
        givenError = null
        try {
            givenVouches = trustRepo.listGivenVouches()
        } catch (e: Exception) {
            givenError = friendlyError(e, "Couldn't load vouches you've given.")
        } finally {
            givenLoading = false
        }
    }

    fun act(vouchId: String, action: suspend (String, String) -> VouchOut) {
        if (primaryId == null) return
        busyId = vouchId
        scope.launch {
            val token = stepUpController.confirmStepUp()
            if (token == null) {
                busyId = null
                return@launch
            }
            try {
                action(vouchId, token)
                reload()
            } catch (e: Exception) {
                error = friendlyError(e, "That didn't go through.")
            } finally {
                busyId = null
            }
        }
    }

    when {
        notClaimed -> D2MEmptyState("Waiting on your child", "Once they claim your invite, you'll be able to manage vouches from here.")
        primaryId == null && role == D2MRole.PARENT -> Text("Loading…", color = mutedText(0.55f))
        else -> {
            D2MCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ask someone to vouch for this profile", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Choose what they're confirming, then send them the request outside the app -- they'll approve it from their own WedLock account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedText(0.55f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                    )
                    VOUCH_CLAIM_SCOPES.forEach { s ->
                        D2MCheckboxRow(
                            label = humanize(s),
                            checked = s in selectedScopes,
                            onCheckedChange = { checked ->
                                selectedScopes = if (checked) selectedScopes + s else selectedScopes - s
                            },
                        )
                    }
                    D2MSelectField(
                        label = "Visibility",
                        value = visibility,
                        options = VISIBILITY_OPTIONS,
                        onValueChange = { visibility = it },
                        optionLabel = ::humanize,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    D2MButton(
                        text = if (creating) "Creating…" else "Create vouch",
                        enabled = !creating && selectedScopes.isNotEmpty() && primaryId != null,
                        modifier = Modifier.padding(top = 12.dp),
                        onClick = {
                            val id = primaryId ?: return@D2MButton
                            creating = true
                            error = null
                            scope.launch {
                                val token = stepUpController.confirmStepUp()
                                if (token == null) {
                                    creating = false
                                    return@launch
                                }
                                try {
                                    trustRepo.createVouch(id, selectedScopes.toList(), visibility, token)
                                    selectedScopes = emptySet()
                                    reload()
                                } catch (e: Exception) {
                                    error = friendlyError(e, "Couldn't create a vouch.")
                                } finally {
                                    creating = false
                                }
                            }
                        },
                    )
                }
            }

            D2MTabs(
                titles = listOf("Received", "Given"),
                selectedIndex = vouchTab,
                onSelect = { vouchTab = it },
                modifier = Modifier.padding(top = 16.dp),
            )

            if (vouchTab == 0) {
                error?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    when {
                        loading -> TrustListSkeleton()
                        vouches.isNullOrEmpty() -> D2MEmptyState("No vouches yet", "Vouches you request or receive will appear here.")
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(vouches!!) { v ->
                                D2MCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            D2MBadge(v.status, statusTone(v.status))
                                            v.visibility?.let { Text(humanize(it), style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f)) }
                                        }
                                        Text(
                                            v.claimScopes.joinToString(", ") { humanize(it) },
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(top = 6.dp),
                                        )
                                        v.createdAt?.let { Text("Created $it", style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f), modifier = Modifier.padding(top = 4.dp)) }

                                        val busy = busyId == v.id
                                        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (v.status.contains("PENDING")) {
                                                D2MButton(
                                                    text = "Approve", size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                    enabled = !busy,
                                                    onClick = { act(v.id) { id2, token -> trustRepo.approveVouch(primaryId!!, id2, token) } },
                                                )
                                                D2MButton(
                                                    text = "Decline", variant = D2MButtonVariant.OUTLINE, size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                    enabled = !busy,
                                                    onClick = { act(v.id) { id2, token -> trustRepo.declineVouch(primaryId!!, id2, token) } },
                                                )
                                            }
                                            if (v.status.contains("PENDING") || v.status == "APPROVED") {
                                                D2MButton(
                                                    text = "Withdraw", variant = D2MButtonVariant.OUTLINE, size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                    enabled = !busy,
                                                    onClick = { act(v.id) { id2, token -> trustRepo.withdrawVouch(primaryId!!, id2, token) } },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                givenError?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    when {
                        givenLoading -> TrustListSkeleton()
                        givenVouches.isNullOrEmpty() -> D2MEmptyState("You haven't vouched for anyone yet", "Vouches you give, on any profile, will appear here.")
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(givenVouches!!) { v ->
                                D2MCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                v.subjectName ?: "Profile no longer available",
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            D2MBadge(v.status, statusTone(v.status))
                                        }
                                        Text(
                                            v.claimScopes.joinToString(", ") { humanize(it) },
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(top = 6.dp),
                                        )
                                        v.createdAt?.let { Text("Created $it", style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f), modifier = Modifier.padding(top = 4.dp)) }

                                        val busy = busyId == v.id
                                        if (v.status.contains("PENDING") || v.status == "APPROVED") {
                                            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                                D2MButton(
                                                    text = "Withdraw", variant = D2MButtonVariant.OUTLINE, size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                    enabled = !busy,
                                                    onClick = {
                                                        busyId = v.id
                                                        scope.launch {
                                                            val token = stepUpController.confirmStepUp()
                                                            if (token == null) {
                                                                busyId = null
                                                                return@launch
                                                            }
                                                            try {
                                                                trustRepo.withdrawVouch(v.subjectPrimaryId ?: "", v.id, token)
                                                                reloadGiven()
                                                            } catch (e: Exception) {
                                                                givenError = friendlyError(e, "That didn't go through.")
                                                            } finally {
                                                                busyId = null
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
                    }
                }
            }
        }
    }
}

// -- Trusted connections --------------------------------------------------

@Composable
private fun ConnectionsSection(trustRepo: TrustRepository, stepUpController: StepUpController) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var connections by remember { mutableStateOf<List<ConnectionOut>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var connectionType by remember { mutableStateOf(CONNECTION_TYPES.first()) }
    var inviting by remember { mutableStateOf(false) }
    var lastInviteToken by remember { mutableStateOf<String?>(null) }
    var acceptToken by remember { mutableStateOf("") }
    var accepting by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                connections = trustRepo.listConnections(forceRefresh = true)
            } catch (e: Exception) {
                error = friendlyError(e, "Couldn't load trusted connections.")
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            connections = trustRepo.listConnections()
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load trusted connections.")
        } finally {
            loading = false
        }
    }

    D2MCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Invite a trusted connection", style = MaterialTheme.typography.titleSmall)
            Text(
                "Creates a one-time invite link to share outside the app. Whoever accepts it can later endorse specific details on this profile.",
                style = MaterialTheme.typography.bodySmall,
                color = mutedText(0.55f),
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            D2MSelectField(
                label = "Relationship",
                value = connectionType,
                options = CONNECTION_TYPES,
                onValueChange = { connectionType = it },
                optionLabel = ::humanize,
            )
            D2MButton(
                text = if (inviting) "Creating…" else "Create invite",
                enabled = !inviting,
                modifier = Modifier.padding(top = 12.dp),
                onClick = {
                    inviting = true
                    error = null
                    scope.launch {
                        val token = stepUpController.confirmStepUp()
                        if (token == null) {
                            inviting = false
                            return@launch
                        }
                        try {
                            val invite = trustRepo.createConnectionInvite(connectionType, 72, token)
                            lastInviteToken = invite.inviteToken
                            reload()
                        } catch (e: Exception) {
                            error = friendlyError(e, "Couldn't create an invite.")
                        } finally {
                            inviting = false
                        }
                    }
                },
            )
            lastInviteToken?.let { inv ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Invite ready -- share it once", style = MaterialTheme.typography.bodySmall, color = mutedText(0.55f))
                    D2MButton(
                        text = "Copy",
                        size = com.d2m.app.ui.components.D2MButtonSize.SM,
                        variant = D2MButtonVariant.OUTLINE,
                        onClick = { clipboard.setText(AnnotatedString(inv)) },
                    )
                }
            }
        }
    }

    D2MCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Accept an invite", style = MaterialTheme.typography.titleSmall)
            D2MTextField(
                label = "Invite code",
                value = acceptToken,
                onValueChange = { acceptToken = it },
                placeholder = "Paste the invite someone shared with you",
                modifier = Modifier.padding(top = 8.dp),
            )
            D2MButton(
                text = if (accepting) "Accepting…" else "Accept",
                enabled = !accepting && acceptToken.isNotBlank(),
                modifier = Modifier.padding(top = 12.dp),
                onClick = {
                    accepting = true
                    error = null
                    scope.launch {
                        val token = stepUpController.confirmStepUp()
                        if (token == null) {
                            accepting = false
                            return@launch
                        }
                        try {
                            trustRepo.acceptConnectionInvite(acceptToken, token)
                            acceptToken = ""
                            reload()
                        } catch (e: Exception) {
                            error = friendlyError(e, "Couldn't accept that invite.")
                        } finally {
                            accepting = false
                        }
                    }
                },
            )
        }
    }

    error?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        when {
            loading -> TrustListSkeleton()
            connections.isNullOrEmpty() -> D2MEmptyState("No trusted connections yet", "Connections you invite or accept will appear here.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(connections!!) { c ->
                    D2MCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                D2MBadge(c.status, statusTone(c.status))
                                Text(humanize(c.connectionType), style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f))
                            }
                            c.expiresAt?.let { Text("Expires $it", style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f), modifier = Modifier.padding(top = 6.dp)) }

                            val busy = busyId == c.id
                            if (c.status == "ACTIVE" || c.status == "PENDING") {
                                D2MButton(
                                    text = "Revoke",
                                    variant = D2MButtonVariant.OUTLINE,
                                    size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                    enabled = !busy,
                                    modifier = Modifier.padding(top = 8.dp),
                                    onClick = {
                                        busyId = c.id
                                        scope.launch {
                                            val token = stepUpController.confirmStepUp()
                                            if (token == null) {
                                                busyId = null
                                                return@launch
                                            }
                                            try {
                                                trustRepo.revokeConnection(c.id, token)
                                                reload()
                                            } catch (e: Exception) {
                                                error = friendlyError(e, "Couldn't revoke that connection.")
                                            } finally {
                                                busyId = null
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
    }
}

// -- Endorsements -----------------------------------------------------

@Composable
private fun EndorsementsSection(
    primaryId: String?,
    notClaimed: Boolean,
    role: D2MRole?,
    trustRepo: TrustRepository,
    stepUpController: StepUpController,
) {
    val scope = rememberCoroutineScope()
    var endorsements by remember { mutableStateOf<List<EndorsementOut>?>(null) }
    var connections by remember { mutableStateOf<List<ConnectionOut>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var selectedConnectionId by remember { mutableStateOf("") }
    var claimScope by remember { mutableStateOf(ENDORSEMENT_CLAIM_SCOPES.first()) }
    var visibility by remember { mutableStateOf("PRIVATE") }
    var creating by remember { mutableStateOf(false) }

    fun reload() {
        val id = primaryId ?: return
        scope.launch {
            loading = true
            error = null
            try {
                endorsements = trustRepo.listEndorsements(id, forceRefresh = true)
            } catch (e: Exception) {
                error = friendlyError(e, "Couldn't load endorsements.")
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(primaryId) {
        val id = primaryId
        if (id == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            endorsements = trustRepo.listEndorsements(id)
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load endorsements.")
        } finally {
            loading = false
        }
        runCatching { connections = trustRepo.listConnections().filter { it.status == "ACTIVE" } }
    }

    when {
        notClaimed -> D2MEmptyState("Waiting on your child", "Once they claim your invite, you'll be able to manage endorsements from here.")
        primaryId == null && role == D2MRole.PARENT -> Text("Loading…", color = mutedText(0.55f))
        else -> {
            D2MCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Request an endorsement", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Built on an active trusted connection -- add one from the Connections tab first if the list below is empty.",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedText(0.55f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                    )
                    if (connections.isNullOrEmpty()) {
                        Text("No active trusted connections yet.", style = MaterialTheme.typography.bodySmall, color = mutedText(0.55f))
                    } else {
                        D2MSelectField(
                            label = "Trusted connection",
                            value = selectedConnectionId,
                            options = connections!!.map { it.id },
                            onValueChange = { selectedConnectionId = it },
                            optionLabel = { id -> connections!!.firstOrNull { it.id == id }?.let { humanize(it.connectionType) } ?: id },
                            placeholderWhenEmpty = "Choose one",
                        )
                        D2MSelectField(
                            label = "What are they confirming",
                            value = claimScope,
                            options = ENDORSEMENT_CLAIM_SCOPES,
                            onValueChange = { claimScope = it },
                            optionLabel = ::humanize,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        D2MSelectField(
                            label = "Visibility",
                            value = visibility,
                            options = VISIBILITY_OPTIONS,
                            onValueChange = { visibility = it },
                            optionLabel = ::humanize,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        D2MButton(
                            text = if (creating) "Creating…" else "Request endorsement",
                            enabled = !creating && selectedConnectionId.isNotEmpty() && primaryId != null,
                            modifier = Modifier.padding(top = 12.dp),
                            onClick = {
                                val id = primaryId ?: return@D2MButton
                                creating = true
                                error = null
                                scope.launch {
                                    val token = stepUpController.confirmStepUp()
                                    if (token == null) {
                                        creating = false
                                        return@launch
                                    }
                                    try {
                                        trustRepo.createEndorsement(id, selectedConnectionId, claimScope, visibility, token)
                                        reload()
                                    } catch (e: Exception) {
                                        error = friendlyError(e, "Couldn't request an endorsement.")
                                    } finally {
                                        creating = false
                                    }
                                }
                            },
                        )
                    }
                }
            }

            error?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }

            Column(modifier = Modifier.padding(top = 16.dp)) {
                when {
                    loading -> TrustListSkeleton()
                    endorsements.isNullOrEmpty() -> D2MEmptyState("No endorsements yet", "Endorsements you request or receive will appear here.")
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(endorsements!!) { e ->
                            D2MCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        D2MBadge(e.status, statusTone(e.status))
                                        e.visibility?.let { Text(humanize(it), style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f)) }
                                    }
                                    Text(humanize(e.claimScope), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                                    e.createdAt?.let { Text("Created $it", style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f), modifier = Modifier.padding(top = 4.dp)) }

                                    val busy = busyId == e.id
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (e.status.contains("PENDING")) {
                                            D2MButton(
                                                text = "Approve", size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                enabled = !busy,
                                                onClick = {
                                                    busyId = e.id
                                                    scope.launch {
                                                        val token = stepUpController.confirmStepUp()
                                                        if (token == null) { busyId = null; return@launch }
                                                        try { trustRepo.approveEndorsement(primaryId!!, e.id, token); reload() }
                                                        catch (ex: Exception) { error = friendlyError(ex, "That didn't go through.") }
                                                        finally { busyId = null }
                                                    }
                                                },
                                            )
                                            D2MButton(
                                                text = "Decline", variant = D2MButtonVariant.OUTLINE, size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                enabled = !busy,
                                                onClick = {
                                                    busyId = e.id
                                                    scope.launch {
                                                        val token = stepUpController.confirmStepUp()
                                                        if (token == null) { busyId = null; return@launch }
                                                        try { trustRepo.declineEndorsement(primaryId!!, e.id, token); reload() }
                                                        catch (ex: Exception) { error = friendlyError(ex, "That didn't go through.") }
                                                        finally { busyId = null }
                                                    }
                                                },
                                            )
                                        }
                                        if (e.status.contains("PENDING") || e.status == "APPROVED") {
                                            D2MButton(
                                                text = "Withdraw", variant = D2MButtonVariant.OUTLINE, size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                enabled = !busy,
                                                onClick = {
                                                    busyId = e.id
                                                    scope.launch {
                                                        val token = stepUpController.confirmStepUp()
                                                        if (token == null) { busyId = null; return@launch }
                                                        try { trustRepo.withdrawEndorsement(primaryId!!, e.id, token); reload() }
                                                        catch (ex: Exception) { error = friendlyError(ex, "That didn't go through.") }
                                                        finally { busyId = null }
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -- Recommendations (Round 2) -------------------------------------------

/**
 * Two-sided flow, same "request -> author acts -> owner decides" shape as
 * EndorsementsSection above: the profile owner requests one against an
 * ACTIVE trusted connection (DRAFT), the OTHER party on that connection
 * submits the actual text/scopes/known-since-year (-> PENDING_OWNER_APPROVAL),
 * then the owner approves/hides/withdraws it. As with every other section
 * here, action buttons are shown whenever an item's status makes the
 * action plausible without trying to determine client-side which account
 * is the requester vs. the author -- WedLock's own authorization decides
 * that server-side, same reasoning as this file's top doc comment.
 *
 * The submit step (author-side) is the one action here that needs its own
 * small form (confirmed scopes + free-text + optional year) rather than a
 * single button -- shown inline under the DRAFT card it belongs to,
 * mirroring how the picker-in-a-card pattern already works for the create
 * forms above rather than introducing a new dialog just for this.
 */
@Composable
private fun RecommendationsSection(
    primaryId: String?,
    notClaimed: Boolean,
    role: D2MRole?,
    trustRepo: TrustRepository,
    stepUpController: StepUpController,
) {
    val scope = rememberCoroutineScope()
    var recommendations by remember { mutableStateOf<List<RecommendationOut>?>(null) }
    var connections by remember { mutableStateOf<List<ConnectionOut>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var selectedConnectionId by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("PRIVATE") }
    var creating by remember { mutableStateOf(false) }

    // Inline submit form -- only one recommendation's form open at a time.
    var submittingId by remember { mutableStateOf<String?>(null) }
    var submitScopes by remember { mutableStateOf(setOf<String>()) }
    var submitText by remember { mutableStateOf("") }
    var submitYear by remember { mutableStateOf("") }

    fun reload() {
        val id = primaryId ?: return
        scope.launch {
            loading = true
            error = null
            try {
                recommendations = trustRepo.listRecommendations(id, forceRefresh = true)
            } catch (e: Exception) {
                error = friendlyError(e, "Couldn't load recommendations.")
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(primaryId) {
        val id = primaryId
        if (id == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            recommendations = trustRepo.listRecommendations(id)
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load recommendations.")
        } finally {
            loading = false
        }
        runCatching { connections = trustRepo.listConnections().filter { it.status == "ACTIVE" } }
    }

    fun openSubmitForm(recommendationId: String) {
        submittingId = recommendationId
        submitScopes = emptySet()
        submitText = ""
        submitYear = ""
    }

    when {
        notClaimed -> D2MEmptyState("Waiting on your child", "Once they claim your invite, you'll be able to manage recommendations from here.")
        primaryId == null && role == D2MRole.PARENT -> Text("Loading…", color = mutedText(0.55f))
        else -> {
            D2MCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Request a recommendation", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Built on an active trusted connection -- add one from the Connections tab first if the list below is empty. They'll write it themselves once you send the request.",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedText(0.55f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                    )
                    if (connections.isNullOrEmpty()) {
                        Text("No active trusted connections yet.", style = MaterialTheme.typography.bodySmall, color = mutedText(0.55f))
                    } else {
                        D2MSelectField(
                            label = "Trusted connection",
                            value = selectedConnectionId,
                            options = connections!!.map { it.id },
                            onValueChange = { selectedConnectionId = it },
                            optionLabel = { id -> connections!!.firstOrNull { it.id == id }?.let { humanize(it.connectionType) } ?: id },
                            placeholderWhenEmpty = "Choose one",
                        )
                        D2MSelectField(
                            label = "Visibility",
                            value = visibility,
                            options = VISIBILITY_OPTIONS,
                            onValueChange = { visibility = it },
                            optionLabel = ::humanize,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        D2MButton(
                            text = if (creating) "Requesting…" else "Request recommendation",
                            enabled = !creating && selectedConnectionId.isNotEmpty() && primaryId != null,
                            modifier = Modifier.padding(top = 12.dp),
                            onClick = {
                                val id = primaryId ?: return@D2MButton
                                creating = true
                                error = null
                                scope.launch {
                                    val token = stepUpController.confirmStepUp()
                                    if (token == null) {
                                        creating = false
                                        return@launch
                                    }
                                    try {
                                        trustRepo.requestRecommendation(id, selectedConnectionId, visibility, token)
                                        reload()
                                    } catch (e: Exception) {
                                        error = friendlyError(e, "Couldn't request a recommendation.")
                                    } finally {
                                        creating = false
                                    }
                                }
                            },
                        )
                    }
                }
            }

            error?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }

            Column(modifier = Modifier.padding(top = 16.dp)) {
                when {
                    loading -> TrustListSkeleton()
                    recommendations.isNullOrEmpty() -> D2MEmptyState("No recommendations yet", "Recommendations you request or receive will appear here.")
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recommendations!!) { r ->
                            D2MCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        D2MBadge(r.status, statusTone(r.status))
                                        r.visibility?.let { Text(humanize(it), style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f)) }
                                    }
                                    r.relationshipType?.let { Text(humanize(it), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
                                    r.recommendationText?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
                                    r.confirmedScopes?.takeIf { it.isNotEmpty() }?.let {
                                        Text(it.joinToString(", ") { s -> humanize(s) }, style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f), modifier = Modifier.padding(top = 4.dp))
                                    }
                                    r.createdAt?.let { Text("Created $it", style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f), modifier = Modifier.padding(top = 4.dp)) }

                                    val busy = busyId == r.id
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (r.status == "DRAFT") {
                                            D2MButton(
                                                text = "Submit", size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                enabled = !busy,
                                                onClick = { if (submittingId == r.id) submittingId = null else openSubmitForm(r.id) },
                                            )
                                        }
                                        if (r.status == "PENDING_OWNER_APPROVAL") {
                                            D2MButton(
                                                text = "Approve", size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                enabled = !busy,
                                                onClick = {
                                                    busyId = r.id
                                                    scope.launch {
                                                        val token = stepUpController.confirmStepUp()
                                                        if (token == null) { busyId = null; return@launch }
                                                        try { trustRepo.approveRecommendation(primaryId!!, r.id, token); reload() }
                                                        catch (ex: Exception) { error = friendlyError(ex, "That didn't go through.") }
                                                        finally { busyId = null }
                                                    }
                                                },
                                            )
                                        }
                                        if (r.status == "PENDING_OWNER_APPROVAL" || r.status == "APPROVED") {
                                            D2MButton(
                                                text = "Hide", variant = D2MButtonVariant.OUTLINE, size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                enabled = !busy,
                                                onClick = {
                                                    busyId = r.id
                                                    scope.launch {
                                                        val token = stepUpController.confirmStepUp()
                                                        if (token == null) { busyId = null; return@launch }
                                                        try { trustRepo.hideRecommendation(primaryId!!, r.id, token); reload() }
                                                        catch (ex: Exception) { error = friendlyError(ex, "That didn't go through.") }
                                                        finally { busyId = null }
                                                    }
                                                },
                                            )
                                        }
                                        if (r.status == "DRAFT" || r.status == "PENDING_OWNER_APPROVAL" || r.status == "APPROVED") {
                                            D2MButton(
                                                text = "Withdraw", variant = D2MButtonVariant.OUTLINE, size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                enabled = !busy,
                                                onClick = {
                                                    busyId = r.id
                                                    scope.launch {
                                                        val token = stepUpController.confirmStepUp()
                                                        if (token == null) { busyId = null; return@launch }
                                                        try { trustRepo.withdrawRecommendation(primaryId!!, r.id, token); reload() }
                                                        catch (ex: Exception) { error = friendlyError(ex, "That didn't go through.") }
                                                        finally { busyId = null }
                                                    }
                                                },
                                            )
                                        }
                                    }

                                    if (submittingId == r.id) {
                                        Column(modifier = Modifier.padding(top = 12.dp)) {
                                            Text("What are you confirming?", style = MaterialTheme.typography.labelMedium)
                                            RECOMMENDATION_CONFIRMED_SCOPES.forEach { s ->
                                                D2MCheckboxRow(
                                                    label = humanize(s),
                                                    checked = s in submitScopes,
                                                    onCheckedChange = { checked ->
                                                        submitScopes = if (checked) submitScopes + s else submitScopes - s
                                                    },
                                                )
                                            }
                                            D2MTextField(
                                                label = "Your recommendation",
                                                value = submitText,
                                                onValueChange = { submitText = it },
                                                placeholder = "20-500 characters",
                                                singleLine = false,
                                                minLines = 3,
                                                modifier = Modifier.padding(top = 4.dp),
                                            )
                                            D2MTextField(
                                                label = "Known since year",
                                                value = submitYear,
                                                onValueChange = { text -> submitYear = text.filter { it.isDigit() }.take(4) },
                                                placeholder = "Optional, e.g. 2015",
                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                                optional = true,
                                                modifier = Modifier.padding(top = 8.dp),
                                            )
                                            val canSend = submitScopes.isNotEmpty() && submitText.trim().length >= 20
                                            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                D2MButton(
                                                    text = if (busy) "Sending…" else "Send",
                                                    size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                    enabled = !busy && canSend,
                                                    onClick = {
                                                        busyId = r.id
                                                        scope.launch {
                                                            val token = stepUpController.confirmStepUp()
                                                            if (token == null) { busyId = null; return@launch }
                                                            try {
                                                                trustRepo.submitRecommendation(
                                                                    primaryId!!, r.id, submitScopes.toList(), submitText.trim(),
                                                                    submitYear.toIntOrNull(), token,
                                                                )
                                                                submittingId = null
                                                                reload()
                                                            } catch (ex: Exception) {
                                                                error = friendlyError(ex, "Couldn't submit that recommendation.")
                                                            } finally {
                                                                busyId = null
                                                            }
                                                        }
                                                    },
                                                )
                                                D2MButton(
                                                    text = "Cancel", variant = D2MButtonVariant.OUTLINE, size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                    enabled = !busy,
                                                    onClick = { submittingId = null },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -- External references (Round 2, authenticated side only) --------------

/**
 * Create/list/approve/decline/hide/withdraw an external reference. The
 * referee-facing OTP-verify/submit step this ultimately leads to is
 * unauthenticated on WedLock's own side and lives on a web-only public
 * page (d2m_web builds it) -- this app never renders it. The one-time
 * inviteToken WedLock returns on create (live-verified: absent on every
 * later list call for the same reference, same as ConnectionOut's) is
 * shown with a copy button rather than a constructed share link -- this
 * app has no configured web-app origin to build `/reference/{token}`
 * against (see publicShareUrl's doc comment in ShareLinksScreen.kt for
 * why an earlier attempt at guessing one was scrapped), and no native
 * share-sheet plumbing exists yet either (see HandoffScreen.kt's doc
 * comment) -- so a plain copy button, exactly what ConnectionsSection's
 * own invite-token affordance above already does, is the simplest
 * correct thing here.
 */
@Composable
private fun ExternalReferencesSection(
    primaryId: String?,
    notClaimed: Boolean,
    role: D2MRole?,
    trustRepo: TrustRepository,
    stepUpController: StepUpController,
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var references by remember { mutableStateOf<List<ExternalReferenceOut>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var contact by remember { mutableStateOf("") }
    var relationshipType by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var lastInviteToken by remember { mutableStateOf<String?>(null) }

    fun reload() {
        val id = primaryId ?: return
        scope.launch {
            loading = true
            error = null
            try {
                references = trustRepo.listExternalReferences(id, forceRefresh = true)
            } catch (e: Exception) {
                error = friendlyError(e, "Couldn't load external references.")
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(primaryId) {
        val id = primaryId
        if (id == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            references = trustRepo.listExternalReferences(id)
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load external references.")
        } finally {
            loading = false
        }
    }

    when {
        notClaimed -> D2MEmptyState("Waiting on your child", "Once they claim your invite, you'll be able to manage external references from here.")
        primaryId == null && role == D2MRole.PARENT -> Text("Loading…", color = mutedText(0.55f))
        else -> {
            D2MCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Invite an external reference", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Someone outside your trusted connections -- a colleague, family friend, anyone who knows you. They'll get a link to confirm a few details about you, valid for $EXTERNAL_REFERENCE_EXPIRES_HOURS hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedText(0.55f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                    )
                    D2MTextField(
                        label = "Their email or phone",
                        value = contact,
                        onValueChange = { contact = it },
                        placeholder = "email@example.com or +91...",
                    )
                    D2MTextField(
                        label = "How do you know them",
                        value = relationshipType,
                        onValueChange = { relationshipType = it },
                        placeholder = "e.g. colleague, family friend",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    D2MButton(
                        text = if (creating) "Sending…" else "Send invite",
                        enabled = !creating && contact.isNotBlank() && relationshipType.trim().length >= 2 && primaryId != null,
                        modifier = Modifier.padding(top = 12.dp),
                        onClick = {
                            val id = primaryId ?: return@D2MButton
                            creating = true
                            error = null
                            scope.launch {
                                val token = stepUpController.confirmStepUp()
                                if (token == null) {
                                    creating = false
                                    return@launch
                                }
                                try {
                                    val ref = trustRepo.createExternalReference(id, contact, relationshipType.trim(), EXTERNAL_REFERENCE_EXPIRES_HOURS, token)
                                    lastInviteToken = ref.inviteToken
                                    contact = ""
                                    relationshipType = ""
                                    reload()
                                } catch (e: Exception) {
                                    error = friendlyError(e, "Couldn't send that invite.")
                                } finally {
                                    creating = false
                                }
                            }
                        },
                    )
                    lastInviteToken?.let { inv ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Invite ready -- share it once", style = MaterialTheme.typography.bodySmall, color = mutedText(0.55f))
                            D2MButton(
                                text = "Copy",
                                size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                variant = D2MButtonVariant.OUTLINE,
                                onClick = { clipboard.setText(AnnotatedString(inv)) },
                            )
                        }
                    }
                }
            }

            error?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }

            Column(modifier = Modifier.padding(top = 16.dp)) {
                when {
                    loading -> TrustListSkeleton()
                    references.isNullOrEmpty() -> D2MEmptyState("No external references yet", "References you invite will appear here.")
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(references!!) { r ->
                            D2MCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        D2MBadge(r.status, statusTone(r.status))
                                        Text(humanize(r.relationshipType), style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f))
                                    }
                                    r.recommendationText?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
                                    r.confirmedScopes?.takeIf { it.isNotEmpty() }?.let {
                                        Text(it.joinToString(", ") { s -> humanize(s) }, style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f), modifier = Modifier.padding(top = 4.dp))
                                    }
                                    r.expiresAt?.let { Text("Expires $it", style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f), modifier = Modifier.padding(top = 4.dp)) }

                                    val busy = busyId == r.id
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (r.status == "PENDING_OWNER_APPROVAL") {
                                            D2MButton(
                                                text = "Approve", size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                enabled = !busy,
                                                onClick = {
                                                    busyId = r.id
                                                    scope.launch {
                                                        val token = stepUpController.confirmStepUp()
                                                        if (token == null) { busyId = null; return@launch }
                                                        try { trustRepo.approveExternalReference(primaryId!!, r.id, token); reload() }
                                                        catch (ex: Exception) { error = friendlyError(ex, "That didn't go through.") }
                                                        finally { busyId = null }
                                                    }
                                                },
                                            )
                                            D2MButton(
                                                text = "Decline", variant = D2MButtonVariant.OUTLINE, size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                enabled = !busy,
                                                onClick = {
                                                    busyId = r.id
                                                    scope.launch {
                                                        val token = stepUpController.confirmStepUp()
                                                        if (token == null) { busyId = null; return@launch }
                                                        try { trustRepo.declineExternalReference(primaryId!!, r.id, token); reload() }
                                                        catch (ex: Exception) { error = friendlyError(ex, "That didn't go through.") }
                                                        finally { busyId = null }
                                                    }
                                                },
                                            )
                                        }
                                        if (r.status == "APPROVED") {
                                            D2MButton(
                                                text = "Hide", variant = D2MButtonVariant.OUTLINE, size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                enabled = !busy,
                                                onClick = {
                                                    busyId = r.id
                                                    scope.launch {
                                                        val token = stepUpController.confirmStepUp()
                                                        if (token == null) { busyId = null; return@launch }
                                                        try { trustRepo.hideExternalReference(primaryId!!, r.id, token); reload() }
                                                        catch (ex: Exception) { error = friendlyError(ex, "That didn't go through.") }
                                                        finally { busyId = null }
                                                    }
                                                },
                                            )
                                        }
                                        if (r.status == "PENDING_SUBMISSION" || r.status == "PENDING_OWNER_APPROVAL" || r.status == "APPROVED") {
                                            D2MButton(
                                                text = "Withdraw", variant = D2MButtonVariant.OUTLINE, size = com.d2m.app.ui.components.D2MButtonSize.SM,
                                                enabled = !busy,
                                                onClick = {
                                                    busyId = r.id
                                                    scope.launch {
                                                        val token = stepUpController.confirmStepUp()
                                                        if (token == null) { busyId = null; return@launch }
                                                        try { trustRepo.withdrawExternalReference(primaryId!!, r.id, token); reload() }
                                                        catch (ex: Exception) { error = friendlyError(ex, "That didn't go through.") }
                                                        finally { busyId = null }
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
