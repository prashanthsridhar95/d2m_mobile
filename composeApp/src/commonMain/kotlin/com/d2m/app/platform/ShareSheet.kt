package com.d2m.app.platform

/**
 * Opens the operating system's share sheet with [text] (WhatsApp, Messages, e-mail, copy...). Used by the parent
 * hand-off screen to send the child's claim link -- the button used to have an empty click handler.
 */
expect fun shareText(text: String)

/** The link a child opens to claim the profile their parent set up (matches the manifest's d2m.app/claim deep link). */
fun claimLink(token: String): String = "https://d2m.app/claim/$token"
