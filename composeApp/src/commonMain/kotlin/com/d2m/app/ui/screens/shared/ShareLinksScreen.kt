package com.d2m.app.ui.screens.shared

import androidx.compose.foundation.layout.Arrangement
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
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            PageTitle(strings.pageTitle)
            Text(
                strings.createIntro,
                style = MaterialTheme.typography.bodySmall,
                color = mutedText(0.55f),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )

            when {
                notClaimed -> D2MEmptyState(strings.waitingOnChildTitle, strings.waitingOnChildBody)
                loading -> Text(strings.loading, color = mutedText(0.55f))
                else -> {
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
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                strings.fieldsSelectedLabel(customFields.trueFieldCount()),
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedText(0.55f),
                            )
                            D2MButton(text = strings.chooseFieldsCta, onClick = { pickerOpen = true })
                        }
                    }
                    D2MButton(
                        text = if (creating) strings.linksCreating else strings.linksCreateCta,
                        enabled = !creating && primaryId != null,
                        modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
                        onClick = {
                            val id = primaryId ?: return@D2MButton
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

                    error?.let { D2MErrorBanner(it, modifier = Modifier.padding(bottom = 12.dp)) }

                    if (links.isNullOrEmpty()) {
                        D2MEmptyState(strings.linksEmptyTitle, strings.linksEmptyBody)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(links!!) { link ->
                                ShareLinkRow(
                                    link = link,
                                    busy = busyLinkId == link.id,
                                    onCopy = { clipboard.setText(AnnotatedString(publicShareUrl(apiClient, link.code))) },
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

    LaunchedEffect(customFields) { fieldsCache.save(customFields) }
}

@Composable
private fun ShareLinkRow(link: ShareLinkOut, busy: Boolean, onCopy: () -> Unit, onToggle: () -> Unit, onExtend: () -> Unit) {
    val strings = LocalStrings.current.shareLinks
    var menuOpen by remember { mutableStateOf(false) }

    D2MCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (link.isActive) strings.linkStatusActive else strings.linkStatusOff,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (link.isActive) MaterialTheme.colorScheme.primary else mutedText(0.45f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (busy) Text("…", color = mutedText(0.45f))
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = strings.linkActions)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(strings.copyLink) }, onClick = { menuOpen = false; onCopy() })
                        DropdownMenuItem(text = { Text(if (link.isActive) strings.turnOff else strings.turnOn) }, onClick = { menuOpen = false; onToggle() })
                        DropdownMenuItem(text = { Text(strings.extendCta) }, onClick = { menuOpen = false; onExtend() })
                    }
                }
            }
            Text(
                when (link.detailLevel) {
                    "full" -> strings.detailLevelFullShort
                    "minimal" -> strings.detailLevelMinimal
                    else -> strings.detailLevelCustomCount(link.customFields?.trueFieldCount() ?: 0)
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(strings.linkExpires(link.expiresAt.take(10)), style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f))
            Text(
                strings.linkViewCount(link.viewCount) + (link.lastViewedAt?.let { strings.linkLastViewedSuffix(it) } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = mutedText(0.45f),
            )
        }
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
