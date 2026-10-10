package com.d2m.app.ui.screens.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.IdentityDocumentTypes
import com.d2m.app.data.model.IdentityVerificationOut
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.messaging.ui.PickedMedia
import com.d2m.app.messaging.ui.rememberMediaAttachLauncher
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MSelectField
import com.d2m.app.ui.components.FieldSkeleton
import com.d2m.app.ui.components.LinkText
import com.d2m.app.ui.components.MetaText
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * KYC "Verified" badge submission (Phase 3 of the backlog this session is
 * working, confirmed scope: one govt photo ID + one selfie, admin-
 * reviewed only through the control panel -- there is no approve/reject
 * UI here, just submit + read-your-own-status). Mirrors d2m_web's
 * TrustScreen.jsx Verification tab.
 *
 * Document/selfie pick reuses the messaging composer's existing gallery/
 * file-picker launcher (rememberMediaAttachLauncher) rather than a live
 * camera capture -- this app has no camera-capture expect/actual on
 * either platform today (confirmed before building this), and adding one
 * is a materially larger, separate task from the rest of this screen.
 */
@Composable
fun IdentityVerificationScreen(onBack: () -> Unit) {
    val identityStore: IdentityStore = koinInject()
    val identityRepo: IdentityRepository = koinInject()
    val identity by identityStore.identity.collectAsState()
    val strings = LocalStrings.current.identityVerification
    val scope = rememberCoroutineScope()

    val primaryId = if (identity.role == D2MRole.CHILD) identity.primaryId else identity.childPrimaryId

    var status by remember { mutableStateOf<IdentityVerificationOut?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var documentType by remember { mutableStateOf(IdentityDocumentTypes.PASSPORT) }
    var documentPick by remember { mutableStateOf<PickedMedia?>(null) }
    var selfiePick by remember { mutableStateOf<PickedMedia?>(null) }
    var busy by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(primaryId) {
        if (primaryId == null) return@LaunchedEffect
        loading = true
        loadError = null
        try {
            status = identityRepo.getIdentityVerification(primaryId)
        } catch (e: Exception) {
            loadError = friendlyError(e, strings.loadError)
        } finally {
            loading = false
        }
    }

    val documentTypeLabel: (String) -> String = {
        when (it) {
            IdentityDocumentTypes.PASSPORT -> strings.documentTypePassport
            IdentityDocumentTypes.DRIVERS_LICENSE -> strings.documentTypeDriversLicense
            IdentityDocumentTypes.NATIONAL_ID -> strings.documentTypeNationalId
            else -> strings.documentTypeOther
        }
    }

    val pickDocument = rememberMediaAttachLauncher { picked -> documentPick = picked }
    val pickSelfie = rememberMediaAttachLauncher { picked -> selfiePick = picked }

    val canSubmit = status == null || status?.status == "rejected"

    fun submit() {
        val pid = primaryId ?: return
        val doc = documentPick ?: return
        val selfie = selfiePick ?: return
        scope.launch {
            busy = true
            submitError = null
            try {
                status = identityRepo.submitIdentityVerification(
                    pid, documentType,
                    doc.fileName, doc.mime, doc.bytes,
                    selfie.fileName, selfie.mime, selfie.bytes,
                )
                documentPick = null
                selfiePick = null
            } catch (e: Exception) {
                submitError = friendlyError(e, strings.submitError)
            } finally {
                busy = false
            }
        }
    }

    D2MTheme(flow = D2MFlow.PARENT) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            LinkText(strings.back, onClick = onBack, modifier = Modifier.padding(bottom = 4.dp))
            PageTitle(strings.pageTitle)

            when {
                loading -> FieldSkeleton(modifier = Modifier.padding(top = 16.dp))
                loadError != null -> D2MErrorBanner(loadError!!, modifier = Modifier.padding(top = 16.dp))
                else -> D2MCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val current = status
                        if (current?.status == "pending") {
                            D2MBadge(strings.statusPending, D2MBadgeTone.WARNING)
                            MetaText(strings.pendingNote, Modifier.padding(top = 10.dp, bottom = 16.dp))
                        }
                        if (current?.status == "approved") {
                            D2MBadge(strings.statusApproved, D2MBadgeTone.ACCENT)
                            MetaText(strings.approvedNote, Modifier.padding(top = 10.dp, bottom = 16.dp))
                        }
                        if (current?.status == "rejected") {
                            D2MErrorBanner(
                                current.rejectionReason?.let { strings.rejectedWithReason(it) } ?: strings.rejectedNoReason,
                                modifier = Modifier.padding(bottom = 16.dp),
                            )
                        }

                        if (canSubmit) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                D2MSelectField(
                                    label = strings.documentTypeLabel,
                                    value = documentType,
                                    options = IdentityDocumentTypes.ALL,
                                    onValueChange = { if (it.isNotEmpty()) documentType = it },
                                    optionLabel = documentTypeLabel,
                                )

                                Column {
                                    MetaText(strings.documentFileLabel)
                                    D2MButton(
                                        text = if (documentPick != null) strings.changePhoto else strings.choosePhoto,
                                        variant = D2MButtonVariant.OUTLINE,
                                        modifier = Modifier.padding(top = 6.dp),
                                        onClick = pickDocument,
                                    )
                                }

                                Column {
                                    MetaText(strings.selfieFileLabel)
                                    D2MButton(
                                        text = if (selfiePick != null) strings.changePhoto else strings.choosePhoto,
                                        variant = D2MButtonVariant.OUTLINE,
                                        modifier = Modifier.padding(top = 6.dp),
                                        onClick = pickSelfie,
                                    )
                                }

                                submitError?.let { D2MErrorBanner(it) }

                                D2MButton(
                                    text = if (busy) strings.submitting else strings.submit,
                                    enabled = !busy && documentPick != null && selfiePick != null,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    onClick = { submit() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
