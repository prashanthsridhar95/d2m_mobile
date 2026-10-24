package com.d2m.app.ui.screens.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.draw.clip
import com.d2m.app.data.model.ShareLinkViewerOut
import com.d2m.app.ui.components.D2MDivider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.ui.text.font.FontWeight
import com.d2m.app.ui.components.D2MActionButton
import com.d2m.app.ui.components.D2MActionEmphasis
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
import com.d2m.app.ui.components.D2MSkeleton
import com.d2m.app.ui.components.rememberHaptics
import com.d2m.app.ui.theme.d2m
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.d2m.app.data.model.CustomFieldSelection
import com.d2m.app.data.model.ShareLinkOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.DashboardRepository
import com.d2m.app.domain.repository.ShareLinkFieldsCache
import com.d2m.app.domain.repository.ShareLinksRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MCheckboxGroup
import com.d2m.app.ui.components.D2MCheckboxRow
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MSelectField
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.strings.ShareLinksStrings
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val EXTEND_HOURS = 168 // +7 days -- one plain "extend" action, same as ShareLinksScreen.jsx

/** Mirrors screens/shared/ShareLinksScreen.jsx -- create/list/toggle/extend
 *  public profile share links, mounted for both roles (the parent
 *  "Sharing" tab -- AppScaffold.kt's D2MTab.ParentSharing -- and a child-
 *  side entry point in Settings, since the child tab bar has no free
 *  slot). Create/manage only: the actual public landing page a link's
 *  recipient opens is a web-only route this app never renders itself. */
@Composable
fun ShareLinksScreen() {
    val identityStore: IdentityStore = koinInject()
    val shareLinksRepo: ShareLinksRepository = koinInject()
    val dashboardRepo: DashboardRepository = koinInject()
    val apiClient: ApiClient = koinInject()
    val fieldsCache: ShareLinkFieldsCache = koinInject()
    val identity by identityStore.identity.collectAsState()
    val strings = LocalStrings.current.shareLinks
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var links by remember { mutableStateOf<List<ShareLinkOut>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var notClaimed by remember { mutableStateOf(false) }
    var detailLevel by remember { mutableStateOf("full") }
    // "The setting should be stored in cache", reported directly -- seeded
    // from ShareLinkFieldsCache once at first composition, same as
    // ShareLinksScreen.jsx's loadCustomFields().
    var customFields by remember { mutableStateOf(fieldsCache.load() ?: CustomFieldSelection()) }
    var pickerOpen by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var busyLinkId by remember { mutableStateOf<String?>(null) }
    var viewersLink by remember { mutableStateOf<ShareLinkOut?>(null) }

    // Child role already has its own primary_id directly on identity. The
    // parent role's linked child id lives under childPrimaryId instead --
    // NOT identity.primaryId, which is only ever set for the child role
    // (see IdentityStore.kt's doc comment; ParentHomeScreen.kt/
    // ProfileDetailScreen.kt/ParentBrowseScreen.kt all branch the same
    // way). Unlike those screens, this one resolves childPrimaryId itself
    // via GET /sponsors/{id}/child (DashboardRepository.getSponsorChild,
    // already wired for other purposes) when it isn't cached yet -- the
    // same lookup web's ChildIdGate.jsx does -- since nothing else in this
    // app currently populates it (IdentityStore.updateChildPrimaryId has
    // no other caller today), and this screen needs a real id to work at
    // all, not just to skip a section when one's missing.
    val primaryId = if (identity.role == D2MRole.CHILD) identity.primaryId else identity.childPrimaryId

    LaunchedEffect(identity.role, identity.sponsorId, primaryId) {
        if (identity.role == D2MRole.PARENT && primaryId == null) {
            val sponsorId = identity.sponsorId
            if (sponsorId != null) {
                runCatching { dashboardRepo.getSponsorChild(sponsorId) }
                    .onSuccess { identityStore.updateChildPrimaryId(it.primaryId) }
                    .onFailure { notClaimed = true; loading = false }
            }
            return@LaunchedEffect
        }
        if (primaryId == null) return@LaunchedEffect
        notClaimed = false
        loading = true
        error = null
        try {
            links = shareLinksRepo.listShareLinks(primaryId)
        } catch (e: Exception) {
            error = friendlyError(e, strings.linksLoadError)
        } finally {
            loading = false
        }
    }

    val flow = if (identity.role == D2MRole.CHILD) D2MFlow.CHILD else D2MFlow.PARENT
    D2MTheme(flow = flow) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column {
                    PageTitle(strings.pageTitle)
                    Text(
                        strings.createIntro,
                        style = MaterialTheme.typography.bodyMedium,
                        color = d2m.meta,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            when {
                notClaimed -> item(key = "unclaimed") { D2MEmptyState(strings.waitingOnChildTitle, strings.waitingOnChildBody) }
                loading -> {
                    item(key = "sk-create") { D2MSkeleton(height = 190.dp, radius = 14.dp) }
                    items(2, key = { "sk-$it" }) { D2MSkeleton(height = 130.dp, radius = 14.dp) }
                }
                else -> {
                    item(key = "create") {
                        D2MCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                D2MSelectField(
                                    label = strings.linksShowLabel,
                                    value = detailLevel,
                                    options = listOf("full", "minimal", "custom"),
                                    onValueChange = { detailLevel = it },
                                    optionLabel = {
                                        when (it) {
                                            "full" -> strings.detailLevelFull
                                            "minimal" -> strings.detailLevelMinimal
                                            else -> strings.detailLevelCustom
                                        }
                                    },
                                )
                                if (detailLevel == "custom") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            strings.fieldsSelectedLabel(customFields.trueFieldCount()),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = d2m.meta,
                                            modifier = Modifier.weight(1f),
                                        )
                                        D2MActionButton(
                                            strings.chooseFieldsCta,
                                            onClick = { pickerOpen = true },
                                            emphasis = D2MActionEmphasis.SECONDARY,
                                            height = 44.dp,
                                        )
                                    }
                                }
                                D2MActionButton(
                                    text = if (creating) strings.linksCreating else strings.linksCreateCta,
                                    leadingIcon = Icons.Filled.Link,
                                    loading = creating,
                                    enabled = primaryId != null,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        val id = primaryId ?: return@D2MActionButton
                                        creating = true
                                        error = null
                                        scope.launch {
                                            try {
                                                shareLinksRepo.createShareLink(
                                                    id, detailLevel,
                                                    customFields = if (detailLevel == "custom") customFields else null,
                                                )
                                                links = shareLinksRepo.listShareLinks(id, forceRefresh = true)
                                            } catch (e: Exception) {
                                                error = friendlyError(e, strings.linksCreateError)
                                            } finally {
                                                creating = false
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }

                    error?.let { msg -> item(key = "error") { D2MErrorBanner(msg) } }

                    if (links.isNullOrEmpty()) {
                        item(key = "empty") { D2MEmptyState(strings.linksEmptyTitle, strings.linksEmptyBody) }
                    } else {
                        items(links!!, key = { it.id }) { link ->
                            ShareLinkRow(
                                link = link,
                                busy = busyLinkId == link.id,
                                onCopy = { clipboard.setText(AnnotatedString(publicShareUrl(apiClient, link.code))) },
                                onSeeViewers = { viewersLink = link },
                                onToggle = {
                                    val id = primaryId ?: return@ShareLinkRow
                                    busyLinkId = link.id
                                    scope.launch {
                                        runCatching { shareLinksRepo.updateShareLink(id, link.id, isActive = !link.isActive) }
                                        links = shareLinksRepo.listShareLinks(id, forceRefresh = true)
                                        busyLinkId = null
                                    }
                                },
                                onExtend = {
                                    val id = primaryId ?: return@ShareLinkRow
                                    busyLinkId = link.id
                                    scope.launch {
                                        runCatching { shareLinksRepo.updateShareLink(id, link.id, extendHours = EXTEND_HOURS) }
                                        links = shareLinksRepo.listShareLinks(id, forceRefresh = true)
                                        busyLinkId = null
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        // "Custom should open a dialog box & show the list", reported
        // directly -- the checklist used to inline-reveal under the
        // template selector; it now lives in a real Dialog instead, same
        // "the setting should be stored in cache" request satisfied by
        // fieldsCache.load()/.save() above/below rather than by this
        // dialog itself (closing it never loses a toggle either way,
        // since `customFields` lives in the parent's own state).
        if (pickerOpen) {
            Dialog(onDismissRequest = { pickerOpen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                D2MCard(modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(24.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(strings.pickerTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings.pickerSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedText(0.55f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                        )
                        Column(modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                            CustomFieldPicker(selection = customFields, onChange = { customFields = it })
                        }
                        D2MButton(
                            text = strings.done,
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                            onClick = { pickerOpen = false },
                        )
                    }
                }
            }
        }
    }

    viewersLink?.let { link ->
        val pid = primaryId
        if (pid != null) ViewersSheet(primaryId = pid, link = link, onDismiss = { viewersLink = null })
    }

    LaunchedEffect(customFields) { fieldsCache.save(customFields) }
}

@Composable
private fun ShareLinkRow(link: ShareLinkOut, busy: Boolean, onCopy: () -> Unit, onSeeViewers: () -> Unit, onToggle: () -> Unit, onExtend: () -> Unit) {
    val strings = LocalStrings.current.shareLinks
    val haptics = rememberHaptics()
    var menuOpen by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1800); copied = false } }

    // ISO dates compare correctly as plain strings; a link past its expiry date is not "Active" whatever the flag says.
    val expired = link.expiresAt.take(10) < kotlin.time.Clock.System.now().toString().take(10)
    val (statusText, statusTone) = when {
        !link.isActive -> strings.linkStatusOff to D2MBadgeTone.NEUTRAL
        expired -> strings.linkStatusExpired to D2MBadgeTone.WARNING
        else -> strings.linkStatusActive to D2MBadgeTone.SUCCESS
    }

    D2MCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        when (link.detailLevel) {
                            "full" -> strings.detailLevelFullShort
                            "minimal" -> strings.detailLevelMinimal
                            else -> strings.detailLevelCustomCount(link.customFields?.trueFieldCount() ?: 0)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(strings.linkExpires(link.expiresAt.take(10)), style = MaterialTheme.typography.bodySmall, color = d2m.meta)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .clickable(enabled = link.viewCount > 0, role = androidx.compose.ui.semantics.Role.Button, onClick = onSeeViewers),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = d2m.meta, modifier = Modifier.size(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                strings.linkViewCount(link.viewCount) + (link.lastViewedAt?.let { strings.linkLastViewedSuffix(it.take(10)) } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = d2m.meta,
                            )
                            if (link.viewCount > 0) {
                                Text(strings.seeViewers, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = d2m.accent)
                            }
                        }
                    }
                }
                D2MBadge(statusText, statusTone)
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = strings.linkActions)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(if (link.isActive) strings.turnOff else strings.turnOn) }, onClick = { menuOpen = false; onToggle() })
                        DropdownMenuItem(text = { Text(strings.extendCta) }, onClick = { menuOpen = false; onExtend() })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                D2MActionButton(
                    text = if (copied) strings.linkCopied else strings.copyLink,
                    leadingIcon = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    onClick = { onCopy(); haptics.success(); copied = true },
                    emphasis = D2MActionEmphasis.SECONDARY,
                    done = copied,
                    height = 44.dp,
                    modifier = Modifier.weight(1f),
                )
                if (expired || !link.isActive) {
                    D2MActionButton(
                        text = if (expired) strings.extendCta else strings.turnOn,
                        onClick = if (expired) onExtend else onToggle,
                        loading = busy,
                        height = 44.dp,
                        modifier = Modifier.weight(1f),
                    )
                } else if (busy) {
                    D2MActionButton(text = "…", onClick = {}, enabled = false, emphasis = D2MActionEmphasis.TERTIARY, height = 44.dp)
                }
            }
        }
    }
}

/**
 * "List of users accessed the link should be able to be seen by the creator of the link" (web: ViewersModal in
 * ShareLinksScreen.jsx). One entry per real visitor, grouped server-side, not one per visit.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ViewersSheet(primaryId: String, link: ShareLinkOut, onDismiss: () -> Unit) {
    val strings = LocalStrings.current.shareLinks
    val repo: ShareLinksRepository = koinInject()
    var viewers by remember(link.id) { mutableStateOf<List<ShareLinkViewerOut>?>(null) }
    var error by remember(link.id) { mutableStateOf<String?>(null) }
    var tick by remember(link.id) { mutableStateOf(0) }

    LaunchedEffect(link.id, tick) {
        viewers = null
        error = null
        try {
            viewers = repo.listViewers(primaryId, link.id)
        } catch (e: Exception) {
            error = friendlyError(e, strings.viewersLoadError)
        }
    }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = d2m.textPrimary,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(strings.viewersTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                strings.linkViewCount(link.viewCount),
                style = MaterialTheme.typography.bodyMedium,
                color = d2m.meta,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
            val list = viewers
            when {
                error != null -> D2MErrorBanner(error!!, onRetry = { tick++ })
                list == null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) { D2MSkeleton(height = 72.dp, radius = 12.dp) }
                }
                list.isEmpty() -> D2MEmptyState(strings.viewersEmpty, "")
                else -> LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(list.size, key = { list[it].viewerPrimaryId ?: "${list[it].name}-${list[it].phone}-$it" }) { i ->
                        ViewerRow(list[i], last = i == list.lastIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerRow(v: ShareLinkViewerOut, last: Boolean) {
    val strings = LocalStrings.current.shareLinks
    val name = v.name?.takeIf { it.isNotBlank() } ?: strings.viewerUnknown
    val verified = v.identityAssurance == "authenticated_account"
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(d2m.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(name.first().uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = d2m.accentStrong)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(
                    if (v.isAnonymous) (v.phone ?: strings.viewerNoPhone) else strings.viewerMember,
                    style = MaterialTheme.typography.bodyMedium,
                    color = d2m.meta,
                )
                D2MBadge(
                    if (verified) strings.viewerVerified else strings.viewerNotVerified,
                    if (verified) D2MBadgeTone.SUCCESS else D2MBadgeTone.NEUTRAL,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
                Text(
                    strings.viewerFirstLast(v.firstViewedAt.take(10), v.lastViewedAt.take(10)),
                    style = MaterialTheme.typography.bodySmall,
                    color = d2m.meta,
                )
            }
            Text(strings.viewerVisits(v.visitCount), style = MaterialTheme.typography.labelMedium, color = d2m.meta)
        }
        if (!last) D2MDivider()
    }
}

/** The "custom" template's field checklist -- one D2MCheckboxRow per
 *  CustomFieldSelection field, grouped the same way ShareLinksScreen.jsx's
 *  own FIELD_GROUPS does (Basic & personal / Education & career / Family),
 *  plus a standalone Horoscope toggle and a photo-count field. Reuses
 *  D2MCheckboxGroup/D2MCheckboxRow (already used elsewhere, e.g.
 *  FilterSheet.kt) rather than a new low-level checkbox primitive --
 *  a mechanical mirror of the web picker, not a re-design. */
@Composable
private fun CustomFieldPicker(selection: CustomFieldSelection, onChange: (CustomFieldSelection) -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current.shareLinks
    Column(modifier.fillMaxWidth()) {
        D2MCheckboxGroup(label = strings.groupBasicPersonal, first = true) {
            D2MCheckboxRow(strings.fieldHeight, selection.heightCm, { onChange(selection.copy(heightCm = it)) })
            D2MCheckboxRow(strings.fieldComplexion, selection.complexion, { onChange(selection.copy(complexion = it)) })
            D2MCheckboxRow(strings.fieldBodyType, selection.bodyType, { onChange(selection.copy(bodyType = it)) })
            D2MCheckboxRow(strings.fieldMotherTongue, selection.motherTongue, { onChange(selection.copy(motherTongue = it)) })
            D2MCheckboxRow(strings.fieldOtherLanguages, selection.otherLanguages, { onChange(selection.copy(otherLanguages = it)) })
            D2MCheckboxRow(strings.fieldReligion, selection.religion, { onChange(selection.copy(religion = it)) })
            D2MCheckboxRow(strings.fieldCommunity, selection.casteCommunity, { onChange(selection.copy(casteCommunity = it)) })
            D2MCheckboxRow(strings.fieldSect, selection.sect, { onChange(selection.copy(sect = it)) })
            D2MCheckboxRow(strings.fieldGothram, selection.gothram, { onChange(selection.copy(gothram = it)) })
            D2MCheckboxRow(strings.fieldCitizenship, selection.citizenshipStatus, { onChange(selection.copy(citizenshipStatus = it)) })
        }
        D2MCheckboxGroup(label = strings.groupEducationCareer) {
            D2MCheckboxRow(strings.fieldEducation, selection.highestEducation, { onChange(selection.copy(highestEducation = it)) })
            D2MCheckboxRow(strings.fieldInstitution, selection.institution, { onChange(selection.copy(institution = it)) })
            D2MCheckboxRow(strings.fieldOccupation, selection.occupationTitle, { onChange(selection.copy(occupationTitle = it)) })
            D2MCheckboxRow(strings.fieldEmployer, selection.employer, { onChange(selection.copy(employer = it)) })
            D2MCheckboxRow(strings.fieldEmploymentSector, selection.employmentSector, { onChange(selection.copy(employmentSector = it)) })
            D2MCheckboxRow(strings.fieldFinancialStatus, selection.financialStatus, { onChange(selection.copy(financialStatus = it)) })
        }
        D2MCheckboxGroup(label = strings.groupFamily) {
            D2MCheckboxRow(strings.fieldElderBrothers, selection.elderBrothersCount, { onChange(selection.copy(elderBrothersCount = it)) })
            D2MCheckboxRow(strings.fieldYoungerBrothers, selection.youngerBrothersCount, { onChange(selection.copy(youngerBrothersCount = it)) })
            D2MCheckboxRow(strings.fieldElderSisters, selection.elderSistersCount, { onChange(selection.copy(elderSistersCount = it)) })
            D2MCheckboxRow(strings.fieldYoungerSisters, selection.youngerSistersCount, { onChange(selection.copy(youngerSistersCount = it)) })
            D2MCheckboxRow(strings.fieldNativePlace, selection.nativity, { onChange(selection.copy(nativity = it)) })
            D2MCheckboxRow(strings.fieldFamilyType, selection.familyType, { onChange(selection.copy(familyType = it)) })
            D2MCheckboxRow(strings.fieldFamilyValues, selection.familyValues, { onChange(selection.copy(familyValues = it)) })
        }
        D2MCheckboxGroup(label = strings.fieldHoroscope) {
            D2MCheckboxRow(strings.fieldHoroscopeDetail, selection.horoscope, { onChange(selection.copy(horoscope = it)) })
        }
        D2MTextField(
            label = strings.fieldPhotoCount,
            value = selection.photoCount.toString(),
            onValueChange = { text -> onChange(selection.copy(photoCount = text.toIntOrNull()?.coerceAtLeast(1) ?: 1)) },
            keyboardType = KeyboardType.Number,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** Points at the BACKEND's own short, root-level preview/unfurl page
 *  (app/routers/links.py's root_router, GET /{code} -- "Url shouldnt be
 *  so long", reported directly), not directly at the web app's /p/{code}
 *  SPA route -- this is the URL that actually gets pasted into WhatsApp/
 *  etc, and a client-rendered SPA route has no HTML for a link-preview
 *  crawler to scrape. That endpoint serves real Open Graph tags server-
 *  side, then redirects a human visitor into the real SPA page a beat
 *  later. Building this off apiClient.baseUrl directly (rather than
 *  guessing a separate web-app domain, which this app has no configured
 *  value for at all) is simpler AND more correct than an earlier version
 *  of this function that tried to derive a "web origin" by string-
 *  swapping api.* for app.* in the API domain -- that was a fragile guess
 *  this fix makes unnecessary. */
private fun publicShareUrl(apiClient: ApiClient, code: String): String =
    "${apiClient.baseUrl.trimEnd('/')}/$code"
