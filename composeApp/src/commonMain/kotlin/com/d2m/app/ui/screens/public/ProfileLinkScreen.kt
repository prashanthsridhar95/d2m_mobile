package com.d2m.app.ui.screens.public

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.PublicProfileChartOut
import com.d2m.app.data.model.PublicProfileLinkOut
import com.d2m.app.data.model.PublicVouchOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.ApiError
import com.d2m.app.data.network.createPublicLinkPhotoImageLoader
import com.d2m.app.data.network.PublicLinkViewerGate
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.LeadKeyCache
import com.d2m.app.domain.repository.PublicLinksRepository
import com.d2m.app.ui.components.AstrologyChartView
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MSkeleton
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.DataRow
import com.d2m.app.ui.components.LocalPhotoImageLoader
import com.d2m.app.ui.components.MetaText
import com.d2m.app.ui.components.PersonName
import com.d2m.app.ui.components.SectionHeading
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.strings.ProfileLinkStrings
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/*
 * The page a share link's recipient actually opens -- public, unauthenticated,
 * outside this app's normal authenticated nav chrome, same posture as
 * d2m_web's ProfileLinkScreen.jsx (the screen this mirrors). Reached via a
 * deep link (Routes.PROFILE_LINK, see D2MNavGraph.kt) -- the actual URL
 * pasted into WhatsApp/etc is https://d2m.app/{code} (bare root-level code,
 * see app/routers/links.py's share_preview docstring).
 *
 * Gated: an anonymous visitor gets a 409 "identify_required" from
 * GET /links/{code} and has to submit a name + phone number (LeadCaptureGate
 * below) before anything about the profile renders -- not even the photo.
 * A visitor with a real, resolvable D2M identity (only ever a logged-in
 * CHILD-role identity in practice -- a parent's session has no Primary.id
 * of its own to send) skips this entirely.
 *
 * Photos go through a DEDICATED image loader (PublicLinkViewerGate /
 * createPublicLinkPhotoImageLoader), not the app-wide authenticated one
 * App.kt provides by default -- the gate headers here are per-visit state
 * (an anonymous lead_key or this specific viewer's own id), not derived
 * from IdentityStore the way the default loader's header is.
 */
@Composable
fun ProfileLinkScreen(code: String, onReachOut: () -> Unit) {
    val identityStore: IdentityStore = koinInject()
    val publicLinksRepo: PublicLinksRepository = koinInject()
    val leadKeyCache: LeadKeyCache = koinInject()
    val apiClient: ApiClient = koinInject()
    val identity by identityStore.identity.collectAsState()
    val strings = LocalStrings.current.profileLink

    // Only a child-role identity resolves to a real Primary.id -- see this
    // file's own doc comment above.
    val viewerPrimaryId = if (identity.role == D2MRole.CHILD) identity.primaryId else null
    var leadKey by remember(code) { mutableStateOf(leadKeyCache.load(code)) }

    var profile by remember { mutableStateOf<PublicProfileLinkOut?>(null) }
    var status by remember { mutableStateOf("loading") } // loading | ready | identify_required | not_found | gone | error

    LaunchedEffect(code, viewerPrimaryId, leadKey) {
        PublicLinkViewerGate.viewerPrimaryId = viewerPrimaryId
        PublicLinkViewerGate.leadKey = leadKey
        status = "loading"
        try {
            profile = publicLinksRepo.getProfile(code, viewerPrimaryId, leadKey)
            status = "ready"
        } catch (e: ApiError) {
            status = when (e.status) {
                409 -> "identify_required"
                410 -> "gone"
                404 -> "not_found"
                else -> "error"
            }
        } catch (e: Exception) {
            status = "error"
        }
    }

    val publicLinkImageLoader = remember { createPublicLinkPhotoImageLoader() }

    D2MTheme(flow = D2MFlow.GUEST_SYSTEM) {
        CompositionLocalProvider(LocalPhotoImageLoader provides publicLinkImageLoader) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                when (status) {
                    "loading" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        D2MSkeleton(height = 220.dp)
                        D2MSkeleton(width = 180.dp, height = 20.dp)
                        D2MSkeleton(width = 120.dp, height = 14.dp)
                    }

                    "identify_required" -> LeadCaptureGate(
                        code = code,
                        strings = strings,
                        onIdentified = { identifiedProfile, mintedLeadKey ->
                            if (mintedLeadKey != null) {
                                leadKeyCache.save(code, mintedLeadKey)
                                leadKey = mintedLeadKey
                            }
                            profile = identifiedProfile
                            status = "ready"
                        },
                    )

                    "not_found" -> D2MEmptyState(strings.notFoundTitle, strings.notFoundBody)
                    "gone" -> D2MEmptyState(strings.goneTitle, strings.goneBody)
                    "error" -> D2MErrorBanner(strings.genericErrorBody)

                    "ready" -> profile?.let { p ->
                        ProfileLinkContent(
                            code = code,
                            profile = p,
                            viewerPrimaryId = viewerPrimaryId,
                            leadKey = leadKey,
                            apiClient = apiClient,
                            strings = strings,
                            onReachOut = onReachOut,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeadCaptureGate(
    code: String,
    strings: ProfileLinkStrings,
    onIdentified: (PublicProfileLinkOut, String?) -> Unit,
) {
    val publicLinksRepo: PublicLinksRepository = koinInject()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    D2MCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeading(strings.leadTitle)
            MetaText(strings.leadSubtitle, Modifier.padding(top = 6.dp, bottom = 16.dp))
            D2MTextField(label = strings.leadNameLabel, value = name, onValueChange = { name = it })
            D2MTextField(
                label = strings.leadPhoneLabel, value = phone, onValueChange = { phone = it },
                modifier = Modifier.padding(top = 10.dp),
            )
            error?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }
            D2MButton(
                text = if (busy) strings.leadUnlocking else strings.leadViewProfile,
                enabled = !busy && name.isNotBlank() && phone.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            val (identifiedProfile, mintedLeadKey) = publicLinksRepo.identify(code, name.trim(), phone.trim())
                            onIdentified(identifiedProfile, mintedLeadKey)
                        } catch (e: Exception) {
                            error = friendlyError(e, strings.leadError)
                        } finally {
                            busy = false
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ProfileLinkContent(
    code: String,
    profile: PublicProfileLinkOut,
    viewerPrimaryId: String?,
    leadKey: String?,
    apiClient: ApiClient,
    strings: ProfileLinkStrings,
    onReachOut: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        val heroPhoto = profile.photoUrls.firstOrNull()?.let(apiClient::resolveMediaUrl)
        com.d2m.app.ui.components.ProfilePhoto(
            photoUrl = heroPhoto,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            ratio = 1f,
        )

        Column {
            PersonName(profile.name + (profile.age?.let { ", $it" } ?: ""))
            profile.city?.let { MetaText(it, Modifier.padding(top = 4.dp)) }
        }

        D2MCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeading(strings.reachOutTitle)
                MetaText(strings.reachOutBody, Modifier.padding(top = 6.dp, bottom = 14.dp))
                D2MButton(strings.reachOutCta, onClick = onReachOut, modifier = Modifier.fillMaxWidth())
            }
        }

        BasicDetailsCard(profile, strings)
        FamilyCard(profile, strings)
        HoroscopeCard(code, viewerPrimaryId, leadKey, strings)
        VouchesCard(code, strings)

        MetaText(strings.footerNote, Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp))
    }
}

@Composable
private fun BasicDetailsCard(profile: PublicProfileLinkOut, strings: ProfileLinkStrings) {
    val rows = listOfNotNull(
        strings.fieldEducation to profile.highestEducation,
        strings.fieldInstitution to profile.institution,
        strings.fieldOccupation to profile.occupationTitle,
        strings.fieldEmployer to profile.employer,
        strings.fieldEmploymentSector to profile.employmentSector,
        profile.heightCm?.let { strings.fieldHeight to "$it cm" },
        strings.fieldBodyType to profile.bodyType,
        strings.fieldComplexion to profile.complexion,
        strings.fieldMotherTongue to profile.motherTongue,
        profile.otherLanguages?.takeIf { it.isNotEmpty() }?.let { strings.fieldOtherLanguages to it.joinToString(", ") },
        strings.fieldGothram to profile.gothram,
        strings.fieldReligion to profile.religion,
        strings.fieldCommunity to profile.casteCommunity,
        strings.fieldSect to profile.sect,
        strings.fieldCitizenship to profile.citizenshipStatus,
        strings.fieldFinancialStatus to profile.financialStatus,
    ).filter { !it.second.isNullOrBlank() }
    if (rows.isEmpty()) return

    D2MCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeading(strings.basicDetailsTitle)
            Column(modifier = Modifier.padding(top = 10.dp)) {
                rows.forEach { (label, value) -> DataRow(label, value) }
            }
        }
    }
}

private fun siblingsSummary(profile: PublicProfileLinkOut, strings: ProfileLinkStrings): String? {
    val parts = listOfNotNull(
        profile.elderBrothersCount?.takeIf { it > 0 }?.let { strings.siblingElderBrother(it) },
        profile.youngerBrothersCount?.takeIf { it > 0 }?.let { strings.siblingYoungerBrother(it) },
        profile.elderSistersCount?.takeIf { it > 0 }?.let { strings.siblingElderSister(it) },
        profile.youngerSistersCount?.takeIf { it > 0 }?.let { strings.siblingYoungerSister(it) },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
}

@Composable
private fun FamilyCard(profile: PublicProfileLinkOut, strings: ProfileLinkStrings) {
    val rows = listOfNotNull(
        siblingsSummary(profile, strings)?.let { strings.fieldSiblings to it },
        profile.nativity?.let { strings.fieldNativePlace to it },
        profile.familyType?.let { strings.fieldFamilyType to it },
        profile.familyValues?.let { strings.fieldFamilyValues to it },
    )
    if (rows.isEmpty()) return

    D2MCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeading(strings.familyTitle)
            Column(modifier = Modifier.padding(top = 10.dp)) {
                rows.forEach { (label, value) -> DataRow(label, value) }
            }
        }
    }
}

// Self-fetching (GET /links/{code}/chart), not read off the already-loaded
// profile -- Phase 5 of the backlog this session is working split the
// horoscope chart into its own call precisely so the base profile doesn't
// wait on it; this card shows its own skeleton rather than gating the
// whole page. Mirrors d2m_web's ProfileLinkScreen.jsx HoroscopeSection.
@Composable
private fun HoroscopeCard(code: String, viewerPrimaryId: String?, leadKey: String?, strings: ProfileLinkStrings) {
    val publicLinksRepo: PublicLinksRepository = koinInject()
    var chart by remember { mutableStateOf<PublicProfileChartOut?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(code, viewerPrimaryId, leadKey) {
        try {
            chart = publicLinksRepo.getChart(code, viewerPrimaryId, leadKey)
        } catch (e: Exception) {
            failed = true
        }
    }

    if (failed) return
    if (chart == null) {
        D2MCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeading(strings.horoscopeTitle)
                D2MSkeleton(modifier = Modifier.padding(top = 10.dp), height = 200.dp)
            }
        }
        return
    }
    val c = chart!!
    if (c.d1 == null) return

    D2MCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeading(strings.horoscopeTitle)
            Column(modifier = Modifier.padding(top = 10.dp, bottom = 14.dp)) {
                c.moonNakshatra?.let {
                    DataRow(strings.fieldStarRaasi, it + (c.moonPada?.let { p -> " · $p" } ?: ""))
                }
            }
            // Stacked (not side-by-side) -- same narrow-viewport fallback
            // ProfileTabsPanel.kt's own ChartTab already uses unconditionally
            // on this platform; see that composable's doc comment.
            AstrologyChartView(
                d1 = c.d1,
                d9 = c.d9,
                modifier = Modifier.fillMaxWidth(),
                stacked = true,
            )
        }
    }
}

@Composable
private fun VouchesCard(code: String, strings: ProfileLinkStrings) {
    val publicLinksRepo: PublicLinksRepository = koinInject()
    var vouches by remember { mutableStateOf<List<PublicVouchOut>?>(null) }

    LaunchedEffect(code) {
        vouches = try {
            publicLinksRepo.listVouches(code)
        } catch (e: Exception) {
            emptyList()
        }
    }

    D2MCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeading(strings.vouchesTitle)
            Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    vouches == null -> D2MSkeleton(height = 40.dp)
                    vouches!!.isEmpty() -> MetaText(strings.vouchesEmpty)
                    else -> vouches!!.forEach { v ->
                        Column {
                            val typeLabel = when (v.voucherType) {
                                "parent" -> strings.voucherParent
                                "child" -> strings.voucherChild
                                else -> strings.voucherGuest
                            }
                            DataRow(typeLabel, v.voucherName)
                            v.note?.takeIf { it.isNotBlank() }?.let {
                                MetaText("\"$it\"", Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
            GuestVouchForm(code, strings, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

// "I can also vouch as a guest by sharing my name, phone number, etc.,"
// reported directly -- an anonymous link visitor with no D2M account at
// all. Two steps, same two-gate design as the backend's own guest-vouch
// flow: prove ownership of the email via OTP, then the subject's own
// approval queue.
@Composable
private fun GuestVouchForm(code: String, strings: ProfileLinkStrings, modifier: Modifier = Modifier) {
    val publicLinksRepo: PublicLinksRepository = koinInject()
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf("email") } // email | code | done
    var email by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var guestName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier) {
        when (step) {
            "done" -> D2MBadge(strings.guestVouchDone, D2MBadgeTone.SUCCESS)

            "code" -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetaText(strings.guestVouchSentTo(email))
                D2MTextField(label = strings.guestVouchCodeLabel, value = otpCode, onValueChange = { otpCode = it })
                D2MTextField(label = strings.guestVouchNameLabel, value = guestName, onValueChange = { guestName = it })
                D2MTextField(label = strings.guestVouchPhoneLabel, value = phone, onValueChange = { phone = it })
                D2MTextField(label = strings.guestVouchNoteLabel, value = note, onValueChange = { note = it }, placeholder = strings.guestVouchNotePlaceholder)
                error?.let { D2MErrorBanner(it) }
                D2MButton(
                    text = if (busy) strings.guestVouchSubmitting else strings.guestVouchCta,
                    enabled = !busy && otpCode.isNotBlank() && guestName.isNotBlank() && phone.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            busy = true
                            error = null
                            try {
                                publicLinksRepo.createGuestVouch(code, email.trim(), otpCode.trim(), guestName.trim(), phone.trim(), note.trim().ifBlank { null })
                                step = "done"
                            } catch (e: Exception) {
                                error = friendlyError(e, strings.guestVouchCreateError)
                            } finally {
                                busy = false
                            }
                        }
                    },
                )
            }

            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetaText(strings.guestVouchIntro)
                D2MTextField(label = strings.guestVouchEmailLabel, value = email, onValueChange = { email = it })
                error?.let { D2MErrorBanner(it) }
                D2MButton(
                    text = if (busy) strings.guestVouchSending else strings.guestVouchSendCode,
                    variant = D2MButtonVariant.OUTLINE,
                    enabled = !busy && email.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            busy = true
                            error = null
                            try {
                                publicLinksRepo.requestGuestVouchOtp(code, email.trim())
                                step = "code"
                            } catch (e: Exception) {
                                error = friendlyError(e, strings.guestVouchOtpError)
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
