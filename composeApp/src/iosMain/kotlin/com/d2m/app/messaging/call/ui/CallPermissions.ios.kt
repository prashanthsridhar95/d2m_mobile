package com.d2m.app.messaging.call.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberCallPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit = { onResult(true) }
