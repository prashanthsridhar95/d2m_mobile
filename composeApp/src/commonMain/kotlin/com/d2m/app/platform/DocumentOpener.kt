package com.d2m.app.platform

/**
 * Hands a downloaded document to the operating system's viewer (Android: a chooser over the installed apps via a
 * FileProvider; iOS: Quick Look). Returns false when nothing could show it, so the caller can say so.
 */
expect fun openDocument(fileName: String, mime: String, bytes: ByteArray): Boolean
