package com.d2m.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.d2m.app.data.network.friendlyError
import com.d2m.app.domain.repository.TrustRepository
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

/**
 * Reusable "confirm your password" step-up dialog. Every WedLock trust-
 * subsystem mutation -- create/approve/decline/withdraw a vouch, invite/
 * accept/revoke a trusted connection, create/approve/decline/withdraw an
 * endorsement, and every later round built on the same /trust/... surface
 * -- needs a step_up_token minted within the last 5 minutes via
 * POST /trust/reauthenticate immediately before the actual mutating call
 * (see app/routers/trust.py's reauthenticate(), and TrustApi.kt/
 * TrustRepository.kt's stepUpToken params).
 *
 * Modelled as a suspend function rather than a StateFlow-driven prompt
 * (compare messaging/ui/ArchivePinDialog.kt, which is repo-driven because
 * its setup/restore prompt can be triggered from deep inside
 * MessagingRepository's own connect flow) -- every step-up call site here
 * is a screen/ViewModel doing a single one-shot "get a token, then make
 * the one call that needed it", so a plain suspend-and-resume is the
 * simpler fit and needs no per-flow state machine.
 *
 * One StepUpController is shared app-wide (Koin single, see
 * di/AppModule.kt) and StepUpConfirmDialogHost is mounted once at the
 * App.kt shell level (same pattern as CallLayer/InAppNotificationLayer),
 * so any screen just does:
 *
 *   val stepUp: StepUpController = koinInject()
 *   val token = stepUp.confirmStepUp() ?: return@launch // user cancelled
 *   trustRepo.createVouch(primaryId, scopes, visibility, token)
 *
 * with no per-screen Dialog wiring needed.
 */
class StepUpController(private val trustRepo: TrustRepository) {
    /** Non-null while the dialog should be showing. Compose-observable so
     *  StepUpConfirmDialogHost recomposes the instant a caller awaits
     *  confirmStepUp(); the deferred itself is how that caller's suspend
     *  call resumes once the host resolves it (submit -> token, cancel ->
     *  null). */
    internal var pending by mutableStateOf<CompletableDeferred<String?>?>(null)
        private set

    /** Shows the dialog and suspends until the user submits (returns the
     *  fresh step_up_token) or cancels/dismisses (returns null). Attach a
     *  non-null result as the X-Step-Up-Token header on the mutating call
     *  that needed it -- right away, since it's only valid for 5 minutes. */
    suspend fun confirmStepUp(): String? {
        // Only one prompt at a time: a second call arriving while one's
        // already up (shouldn't normally happen -- callers await this
        // before proceeding) resolves the stale one as cancelled rather
        // than stacking dialogs.
        pending?.complete(null)
        val deferred = CompletableDeferred<String?>()
        pending = deferred
        return deferred.await()
    }

    /** Called by the host on Confirm -- does the actual POST /trust/reauthenticate. */
    internal suspend fun submit(password: String): Result<String> =
        runCatching { trustRepo.reauthenticate(password) }

    /** Called by the host on Confirm-success, Cancel, or dismiss. */
    internal fun resolve(token: String?) {
        pending?.complete(token)
        pending = null
    }
}

/** Mount once, above the nav tree (see App.kt). Renders nothing while
 *  `controller.pending` is null. */
@Composable
fun StepUpConfirmDialogHost(controller: StepUpController) {
    val deferred = controller.pending ?: return
    var password by remember(deferred) { mutableStateOf("") }
    var busy by remember(deferred) { mutableStateOf(false) }
    var error by remember(deferred) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (busy || password.isBlank()) return
        busy = true
        error = null
        scope.launch {
            controller.submit(password)
                .onSuccess { token -> controller.resolve(token) }
                .onFailure { e ->
                    error = friendlyError(e, "Couldn't confirm your password.")
                    busy = false
                }
        }
    }

    Dialog(
        onDismissRequest = { if (!busy) controller.resolve(null) },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = false),
    ) {
        D2MCard(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Confirm your password", style = MaterialTheme.typography.titleMedium)
                Text(
                    "For your security, re-enter your password to continue with this action.",
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedText(0.55f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                D2MTextField(
                    label = "Password",
                    value = password,
                    onValueChange = { password = it; error = null },
                    isPassword = true,
                    enabled = !busy,
                )
                error?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    D2MButton(
                        text = "Cancel",
                        variant = D2MButtonVariant.OUTLINE,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        onClick = { controller.resolve(null) },
                    )
                    D2MButton(
                        text = if (busy) "Confirming…" else "Confirm",
                        enabled = !busy && password.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        onClick = { submit() },
                    )
                }
            }
        }
    }
}
