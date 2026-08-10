package com.d2m.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UnionStatusOut(
    val unionId: String,
    val status: String, // pending | confirmed
    val confirmedA: Boolean,
    val confirmedB: Boolean,
    val galleryConsentA: Boolean = false,
    val galleryConsentB: Boolean = false,
)

@Serializable
data class UnionConfirmOut(
    val unionId: String,
    val status: String,
)

@Serializable
data class GalleryOptInIn(val consent: Boolean)

@Serializable
data class GalleryOptInOut(val unionId: String, val galleryConsent: Boolean)

@Serializable
data class SuccessGalleryEntryOut(
    val entryId: String,
    val anonymizedStory: String,
    val mediaRefs: List<String> = emptyList(),
    val publishedAt: String? = null,
)

/** Admin-only, excluded from production builds server-side -- see AdminApi.kt's doc comment. */
@Serializable
data class AccountsListOut(
    val sponsors: List<AccountListEntry> = emptyList(),
    val primaries: List<AccountListEntry> = emptyList(),
)

@Serializable
data class AccountListEntry(val id: String, val name: String)

@Serializable
data class InviteLinkOut(val sponsorId: String, val inviteToken: String, val expiresAt: String)
