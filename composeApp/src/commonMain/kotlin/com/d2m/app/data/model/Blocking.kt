package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/**
 * Mirrors app/routers/blocking.py -- specifically the chat message/media
 * report route (app/models/message_report.py), the "report or flag" half
 * of "Profanity filter - may be provide report or flag for media in chat"
 * (reported directly). D2M-native, NOT WedLock-proxied like TrustApi's
 * createReport-equivalent profile report -- a chat message report doesn't
 * fit WedLock's trust-subsystem schema (see that model's own docstring),
 * and the server can't see chat content at all (Signal-protocol E2E text
 * + per-attachment AES-GCM media), so `contentSnippet` is whatever this
 * client already had decrypted on screen -- the only way a report carries
 * any content for admin review.
 *
 * Block/unblock (the other half of app/routers/blocking.py) has no mobile
 * surface yet -- out of scope for this pass, see that router's own two
 * other routes if that's ever ported too.
 */
@Serializable
data class MessageReportIn(
    val reportedPrimaryId: String,
    val contentType: String, // "text" | "media"
    val contentSnippet: String? = null,
    val reason: String,
    val details: String? = null,
)

@Serializable
data class MessageReportOut(
    val id: String,
    val reporterPrimaryId: String,
    val reportedPrimaryId: String,
    val contentType: String,
    val contentSnippet: String? = null,
    val reason: String,
    val details: String? = null,
    val status: String,
    val resolutionNote: String? = null,
    val createdAt: String,
    val resolvedAt: String? = null,
)
