package com.d2m.app.messaging.call.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
actual fun rememberCallPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        onResult(results[Manifest.permission.RECORD_AUDIO] == true)
    }
    return { launcher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)) }
}
