package com.d2m.app.platform

import android.content.Intent
import com.d2m.app.data.session.AndroidSettingsContextHolder

actual fun shareText(text: String) {
    val context = AndroidSettingsContextHolder.appContext
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    // Launched from the application context (no Activity handle in commonMain), hence NEW_TASK.
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
