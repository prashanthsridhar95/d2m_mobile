package com.d2m.app.messaging.ui

import androidx.compose.runtime.Composable

/** What comes back from the platform file/photo picker -- enough for MessagingRepository.sendMedia's existing (bytes, fileName, mime, kind, width, height) signature. */
data class PickedMedia(
    val bytes: ByteArray,
    val fileName: String,
    val mime: String,
    val kind: String, // "image" | "video" | "audio" | "file" -- same vocabulary sendMedia already expects
)

/**
 * Platform file/photo picker for the composer's paperclip button (ChatPane.kt).
 * Returns a plain launch callback rather than exposing the platform picker
 * API directly -- ChatPane.kt (commonMain) just calls the returned function
 * on click and gets [onPicked] later, with zero Android/iOS-specific imports
 * needed at the call site. Any file type is allowed (mirrors web's plain
 * `<input type="file">` with no `accept` restriction) -- MessagingRepository.
 * sendMedia already accepts any mime/kind.
 */
@Composable
expect fun rememberMediaAttachLauncher(onPicked: (PickedMedia) -> Unit): () -> Unit
