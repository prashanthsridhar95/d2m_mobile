package com.d2m.app.messaging.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.crypto.archive.ArchivePrompt
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import com.d2m.app.ui.strings.LocalStrings

/**
 * Cross-device history backup -- set up (first device ever) or restore (any
 * later device) the account's Archive Keypair. Direct Compose port of
 * d2m_web's `ArchivePinModal.jsx` -- same copy, same layout intent (fixed
 * centered card over a scrim), same field set. This is the ONLY place a
 * recovery PIN is ever typed; it goes straight into
 * `messagingRepo.submitArchiveSetupPin`/`submitArchiveRestorePin` and is
 * never otherwise persisted or logged.
 *
 * Rendered unconditionally by MatchesScreen (mirrors ArchivePinModal.jsx
 * being unconditionally mounted from MatchesScreen.jsx) -- this composable
 * itself no-ops (renders nothing) whenever `messagingRepo.archivePrompt` is
 * null, so callers don't need their own visibility check.
 */
@Composable
fun ArchivePinDialog(messagingRepo: MessagingRepository) {
    val prompt by messagingRepo.archivePrompt.collectAsState()
    val busy by messagingRepo.archiveBusy.collectAsState()
    val error by messagingRepo.archiveError.collectAsState()
    val current = prompt ?: return
    val isSetup = current is ArchivePrompt.Setup
    val scope = rememberCoroutineScope()

    var pin by remember(current) { mutableStateOf("") }
    var confirmPin by remember(current) { mutableStateOf("") }

    // Setup only -- restore just needs to match whatever PIN was chosen
    // originally, so there's nothing to validate client-side beyond
    // non-empty (a wrong PIN is caught server-round-trip-free by the
    // AES-GCM tag check in ArchiveCrypto.unwrapPrivateKeyWithPin -- see
    // `error` above).
    val setupInvalid = isSetup && (pin.length < 4 || pin != confirmPin)

    fun submit() {
        if (busy) return
        scope.launch {
            if (isSetup) messagingRepo.submitArchiveSetupPin(pin) else messagingRepo.submitArchiveRestorePin(pin)
        }
    }

    Dialog(
        onDismissRequest = { if (!busy) messagingRepo.dismissArchivePrompt() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = false),
    ) {
        Card(
            modifier = Modifier.width(340.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (isSetup) LocalStrings.current.messaging.backUpMessages else LocalStrings.current.messaging.restoreMessages,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (isSetup) {
                        LocalStrings.current.messaging.archiveSetupDesc
                    } else {
                        LocalStrings.current.messaging.archiveRestoreDesc
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedText(0.7f),
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text(if (isSetup) LocalStrings.current.messaging.choosePin else LocalStrings.current.messaging.recoveryPin) },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isSetup) {
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it },
                        label = { Text(LocalStrings.current.messaging.confirmPin) },
                        singleLine = true,
                        enabled = !busy,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (confirmPin.isNotEmpty() && pin != confirmPin) {
                        Text(LocalStrings.current.messaging.pinsDontMatch, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                if (error.isNotEmpty()) {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (!isSetup) {
                    Text(
                        LocalStrings.current.messaging.pinNeverSent,
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedText(0.55f),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    D2MButton(
                        text = if (isSetup) LocalStrings.current.messaging.notNow else LocalStrings.current.messaging.iDontRememberIt,
                        onClick = { messagingRepo.dismissArchivePrompt() },
                        variant = D2MButtonVariant.OUTLINE,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    )
                    D2MButton(
                        text = if (busy) LocalStrings.current.messaging.working else if (isSetup) LocalStrings.current.messaging.setUpBackup else LocalStrings.current.messaging.restore,
                        onClick = ::submit,
                        enabled = !busy && pin.isNotEmpty() && !(isSetup && setupInvalid),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
