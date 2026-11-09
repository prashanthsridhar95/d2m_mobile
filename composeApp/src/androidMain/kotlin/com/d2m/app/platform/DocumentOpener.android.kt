package com.d2m.app.platform

import android.content.Intent
import androidx.core.content.FileProvider
import com.d2m.app.data.session.AndroidSettingsContextHolder
import java.io.File

actual fun openDocument(fileName: String, mime: String, bytes: ByteArray): Boolean {
    val context = AndroidSettingsContextHolder.appContext
    return runCatching {
        // One document at a time: clear whatever was opened before so decrypted files don't pile up in the cache.
        val dir = File(context.cacheDir, "opened_documents").apply { deleteRecursively(); mkdirs() }
        val safe = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100).ifBlank { "document" }
        val file = File(dir, safe).apply { writeBytes(bytes) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Launched from the application context (no Activity handle in commonMain), hence NEW_TASK.
        context.startActivity(Intent.createChooser(view, fileName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrElse { false } // incl. ActivityNotFoundException: no installed app can show this type
}
