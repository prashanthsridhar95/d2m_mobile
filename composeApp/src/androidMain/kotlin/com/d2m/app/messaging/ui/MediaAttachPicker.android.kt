package com.d2m.app.messaging.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberMediaAttachLauncher(onPicked: (PickedMedia) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"

        // OpenableColumns.DISPLAY_NAME is the standard way to recover a
        // human-readable filename from a content:// Uri (which itself is
        // usually an opaque id, not a path) -- same approach every Android
        // file-attach flow uses. Falls back to a generic name rather than
        // failing the whole attach if a provider doesn't support the query.
        var fileName = "attachment"
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)?.let { fileName = it }
                }
            }
        }

        val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        if (bytes == null) return@rememberLauncherForActivityResult

        val kind = when {
            mime.startsWith("image/") -> "image"
            mime.startsWith("video/") -> "video"
            mime.startsWith("audio/") -> "audio"
            else -> "file"
        }
        onPicked(PickedMedia(bytes, fileName, mime, kind))
    }
    // "*/*" -- any file type, matches web's unrestricted <input type="file">
    // (no `accept` attribute set in MessageComposer.jsx's handleAttachClick).
    return { launcher.launch("*/*") }
}
