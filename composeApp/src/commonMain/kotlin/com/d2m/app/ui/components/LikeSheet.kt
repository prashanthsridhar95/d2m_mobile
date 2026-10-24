package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.d2m.app.data.network.ApiClient
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText
import org.koin.compose.koinInject

/** What a like is tied to -- mirrors web's `{ type, id, photoUrl?, prompt?, answer? }` (LikeSheet.jsx). */
data class LikeTarget(
    /** "photo" | "prompt" | "profile" -- sent as liked_item_type. */
    val type: String,
    val id: String? = null,
    val photoUrl: String? = null,
    val prompt: String? = null,
    val answer: String? = null,
)

private const val MAX_COMMENT = 300

/**
 * Bottom sheet shown after tapping a heart (or swiping right): quotes the liked item, takes an optional comment and
 * sends the request tied to that item. Port of web's discover/LikeSheet.jsx.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikeSheet(
    target: LikeTarget,
    name: String,
    busy: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onSend: (comment: String?) -> Unit,
) {
    val strings = LocalStrings.current.discovery
    val apiClient: ApiClient = koinInject()
    var text by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val first = name.substringBefore(" ")

    val title = when (target.type) {
        "photo" -> strings.likePhotoTitle(first)
        "prompt" -> strings.likePromptTitle(first)
        else -> strings.likeProfileTitle(first)
    }

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onCancel() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = d2m.textPrimary,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).imePadding()) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp)
                    .clip(RoundedCornerShape(14.dp)).background(d2m.surfaceSunken).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (target.type == "photo") {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(mutedText(0.12f))) {
                        AsyncImage(
                            model = target.photoUrl?.let(apiClient::resolveMediaUrl),
                            imageLoader = rememberPhotoImageLoader(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp),
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    when (target.type) {
                        "prompt" -> {
                            Text(target.prompt.orEmpty(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = mutedText(0.55f))
                            Text(target.answer.orEmpty(), style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 4.dp))
                        }
                        "photo" -> Text(strings.likePhotoHint, style = MaterialTheme.typography.bodyMedium)
                        else -> Text(strings.likeProfileHint(first), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            D2MTextArea(
                label = "",
                value = text,
                onValueChange = { if (it.length <= MAX_COMMENT) text = it },
                placeholder = strings.commentPlaceholder,
                minLines = 3,
            )

            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                D2MButton(strings.cancel, variant = D2MButtonVariant.OUTLINE, enabled = !busy, onClick = onCancel, modifier = Modifier.weight(1f))
                D2MButton(
                    if (busy) strings.sending else strings.sendLike,
                    enabled = !busy,
                    onClick = { onSend(text.trim().ifEmpty { null }) },
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
    }
}
