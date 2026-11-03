package com.d2m.app.ui.screens.parity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.GalleryQueueItemOut
import com.d2m.app.data.network.ApiError
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.data.session.isStaff
import com.d2m.app.domain.repository.StaffGalleryRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Staff gallery moderation queue (REL-14) against /staff/gallery/... using the
 * normal Bearer session token. The 'staff' role check is UI gating only; the
 * server enforces (403 -> "staff access required" state). Publishing an entry
 * the server flags as a re-identification risk returns 409
 * "reidentification_risk: ..." and requires an explicit acknowledgement.
 */
@Composable
fun AdminGalleryModerationScreen() {
    val repo: StaffGalleryRepository = koinInject()
    val identityStore: IdentityStore = koinInject()
    val identity by identityStore.identity.collectAsState()
    val strings = LocalStrings.current.parity
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<GalleryQueueItemOut>>(emptyList()) }
    var forbidden by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var riskPending by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        try {
            items = repo.queue("pending")
            forbidden = false
        } catch (e: ApiError) {
            if (e.status == 403) forbidden = true else error = friendlyError(e, strings.errPublish)
        } catch (e: Exception) {
            error = friendlyError(e, strings.errPublish)
        }
    }

    suspend fun act(block: suspend () -> Unit, okMsg: String) {
        error = null
        status = null
        try {
            block()
            status = okMsg
            riskPending = null
            note = ""
            reload()
        } catch (e: ApiError) {
            when {
                e.status == 403 -> forbidden = true
                e.status == 409 && e.detail?.startsWith("reidentification_risk") == true -> error = e.detail
                else -> error = friendlyError(e, strings.errPublish)
            }
        } catch (e: Exception) {
            error = friendlyError(e, strings.errPublish)
        }
    }

    val isStaff = identity.isStaff()
    LaunchedEffect(isStaff) { if (isStaff) reload() }

    D2MTheme(flow = D2MFlow.GUEST_SYSTEM) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageTitle(strings.moderationTitle)
            if (!isStaff || forbidden) {
                Text(strings.staffAccessRequired, color = mutedText(0.55f))
                return@Column
            }
            D2MTextField(strings.noteLabel, note, { note = it })
            status?.let { Text(it) }
            error?.let { D2MErrorBanner(it) }
            if (items.isEmpty()) Text(strings.queueEmpty, color = mutedText(0.55f))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.entryId }) { e ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(e.story)
                        if (e.riskLevel != "low" && e.riskReasons.isNotEmpty()) {
                            Text("${e.riskLevel} (cohort ${e.riskCohortSize}): ${e.riskReasons.joinToString("; ")}", color = mutedText(0.55f))
                        }
                        if (riskPending == e.entryId) {
                            Text(strings.acknowledgeRiskTitle)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                D2MButton(strings.acknowledgeRiskCta, onClick = {
                                    scope.launch { act({ repo.publish(e.entryId, true, note.ifBlank { null }) }, strings.published) }
                                })
                                D2MButton(strings.cancel, onClick = { riskPending = null })
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                D2MButton(strings.publishCta, onClick = {
                                    scope.launch {
                                        act({
                                            try {
                                                repo.publish(e.entryId, false, note.ifBlank { null })
                                            } catch (x: ApiError) {
                                                if (x.status == 409 && x.detail?.startsWith("reidentification_risk") == true) {
                                                    riskPending = e.entryId
                                                }
                                                throw x
                                            }
                                        }, strings.published)
                                    }
                                })
                                D2MButton(strings.rejectCta, enabled = note.trim().length >= 3, onClick = {
                                    scope.launch { act({ repo.reject(e.entryId, note.trim()) }, strings.rejected) }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
