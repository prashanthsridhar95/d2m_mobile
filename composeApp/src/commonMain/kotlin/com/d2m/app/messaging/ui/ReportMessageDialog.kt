package com.d2m.app.messaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.d2m.app.data.network.BlockingApi
import com.d2m.app.data.network.friendlyError
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MTextArea
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val REASON_MAX = 64
private const val DETAILS_MAX = 1000
private const val SNIPPET_PREVIEW_MAX = 140

/**
 * Report a specific chat message or media item -- "Profanity filter - may
 * be provide report or flag for media in chat" (reported directly). Direct
 * port of d2m_web's ReportMessageModal.jsx: same reason (required, min 3
 * chars, 64-char cap with a counter) + details (optional, 1000-char cap)
 * shape, same quoted-preview-then-success-state flow. Posts straight to
 * d2m_core_engine's message-reports route (BlockingApi.kt) -- D2M-native,
 * not WedLock's trust subsystem -- since the server never sees chat
 * content itself (Signal-protocol E2E text + per-attachment AES-GCM
 * media); `contentSnippet` is whatever this client already had decrypted
 * on screen.
 *
 * `primaryId` is the REPORTER's own D2M primary id (the require_owns_primary
 * path param -- see app/auth.py), NOT the messaging-layer username d2m_web's
 * own ReportMessageModal caller happens to thread through as that prop
 * (MatchesScreen.jsx/ParentMessagesScreen.jsx both pass `messaging.myUsername`,
 * a hyphen-stripped reformatting of the real id -- see
 * lib/messaging/identity.js's messagingUsername()). That reformatted string
 * never matches the hyphenated id actually stored in the primaries table, so
 * passing it straight through here would 404 "primary not found" against the
 * real backend. ChatPane.kt resolves the real one instead
 * (identity.primaryId ?: identity.childPrimaryId, the same pattern every
 * other /primaries/{id}/... caller in this app already uses for "my own
 * profile id regardless of child/parent role").
 */
@Composable
fun ReportMessageDialog(
    primaryId: String,
    reportedPrimaryId: String,
    reportedName: String,
    contentType: String,
    contentSnippet: String?,
    onDismiss: () -> Unit,
) {
    val blockingApi: BlockingApi = koinInject()
    val strings = LocalStrings.current.messaging
    val commonStrings = LocalStrings.current.common
    var reason by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        val trimmedReason = reason.trim()
        if (trimmedReason.length < 3) {
            error = strings.reportReasonTooShort
            return
        }
        busy = true
        error = null
        scope.launch {
            runCatching {
                blockingApi.reportMessage(
                    primaryId = primaryId,
                    reportedPrimaryId = reportedPrimaryId,
                    contentType = contentType,
                    contentSnippet = contentSnippet,
                    reason = trimmedReason,
                    details = details.trim().ifBlank { null },
                )
            }.onSuccess {
                done = true
                busy = false
            }.onFailure { e ->
                error = friendlyError(e, strings.reportError)
                busy = false
            }
        }
    }

    val preview = contentSnippet?.takeIf { it.isNotBlank() }?.let {
        if (it.length > SNIPPET_PREVIEW_MAX) "${it.take(SNIPPET_PREVIEW_MAX)}…" else it
    }

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
    ) {
        D2MCard(modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(strings.reportDialogTitle, style = MaterialTheme.typography.titleMedium)
                if (done) {
                    Text(
                        strings.reportSubmitted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    D2MButton(
                        text = commonStrings.done,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        onClick = onDismiss,
                    )
                } else {
                    Text(
                        if (contentType == "media") strings.reportMediaIntro(reportedName) else strings.reportTextIntro(reportedName),
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedText(0.55f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    )
                    preview?.let {
                        Text(
                            "“$it”",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = mutedText(0.55f),
                            modifier = Modifier.fillMaxWidth()
                                .background(mutedText(0.06f), RoundedCornerShape(D2MRadius.sm))
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                        )
                    }
                    D2MTextField(
                        label = strings.reportReasonLabel,
                        value = reason,
                        onValueChange = { reason = it.take(REASON_MAX); error = null },
                        placeholder = strings.reportReasonPlaceholder,
                        hint = "${reason.length}/$REASON_MAX",
                        enabled = !busy,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    D2MTextArea(
                        label = strings.reportDetailsLabel,
                        value = details,
                        onValueChange = { details = it.take(DETAILS_MAX) },
                        optional = true,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    error?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        D2MButton(
                            text = commonStrings.cancel,
                            variant = D2MButtonVariant.OUTLINE,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss,
                        )
                        D2MButton(
                            text = if (busy) strings.reportSubmitting else strings.reportSubmit,
                            enabled = !busy && reason.trim().length >= 3,
                            modifier = Modifier.weight(1f),
                            onClick = { submit() },
                        )
                    }
                }
            }
        }
    }
}
