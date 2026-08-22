package com.d2m.app.messaging.ui

import androidx.compose.runtime.Composable

// No real iOS target for this build yet (same precedent as CryptoProvider.ios.kt/
// WebRtcEngine.ios.kt) -- a no-op launcher rather than NotImplementedError,
// since this one is wired directly to a visible button tap: crashing on tap
// would be a worse experience than the attach button simply doing nothing
// until a real iOS photo/file picker (PHPickerViewController/
// UIDocumentPickerViewController) is implemented here.
@Composable
actual fun rememberMediaAttachLauncher(onPicked: (PickedMedia) -> Unit): () -> Unit = {}
