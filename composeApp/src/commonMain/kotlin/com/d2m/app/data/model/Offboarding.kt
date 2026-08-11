package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/**
 * Mirrors app/schemas.py's UnionStatusOut exactly -- `unionId` was
 * non-nullable here, but the backend's own docstring says it's explicitly
 * `None` "when neither side has confirmed yet -- no row created", which is
 * the normal pre-confirmation state, not an edge case. Decoding that state
 * would have crashed (null into non-nullable String) every time this screen
 * loaded before either side confirmed. Also added the required
 * primaryAId/primaryBId/purged/galleryStatus fields and optional entryId,
 * none of which existed here before.
 */
@Serializable
data class UnionStatusOut(
    val unionId: String? = null,
    val primaryAId: String,
    val primaryBId: String,
    val status: String, // pending | confirmed
    val confirmedA: Boolean,
    val confirmedB: Boolean,
    val purged: Boolean,
    val galleryConsentA: Boolean = false,
    val galleryConsentB: Boolean = false,
    val galleryStatus: String, // not_opted_in | partial | opted_in
    val entryId: String? = null,
)

/** Mirrors app/schemas.py's UnionConfirmOut exactly -- was missing four required fields (primaryAId, primaryBId, confirmedA, confirmedB, purged). */
@Serializable
data class UnionConfirmOut(
    val unionId: String,
    val primaryAId: String,
    val primaryBId: String,
    val status: String,
    val confirmedA: Boolean,
    val confirmedB: Boolean,
    val purged: Boolean,
)

@Serializable
data class GalleryOptInIn(val consent: Boolean)

/**
 * Mirrors app/schemas.py's GalleryOptInOut exactly -- previously had a
 * single required `galleryConsent: Boolean` field that doesn't exist on the
 * backend at all (it tracks each side's consent separately, galleryConsentA/
 * galleryConsentB, plus a derived galleryStatus label), which would have
 * crashed decode on every gallery opt-in submission.
 */
@Serializable
data class GalleryOptInOut(
    val unionId: String,
    val galleryConsentA: Boolean,
    val galleryConsentB: Boolean,
    val galleryStatus: String, // not_opted_in | partial | opted_in
    val entryId: String? = null,
)

/**
 * Mirrors app/schemas.py's SuccessGalleryEntryOut exactly -- previously
 * named `anonymizedStory`/`mediaRefs`/`publishedAt`, none of which match the
 * backend's real (much smaller) `story`/`published` shape -- `anonymizedStory`
 * being required and never matching the real `story` key would have crashed
 * decode on every load of the Success Gallery screen.
 */
@Serializable
data class SuccessGalleryEntryOut(
    val entryId: String,
    val story: String,
    val published: Boolean,
)

/** Mirrors app/schemas.py's SponsorSummaryOut exactly (admin-only, see AdminApi.kt's doc comment). */
@Serializable
data class SponsorSummaryOut(val sponsorId: String, val name: String, val hasChild: Boolean)

/** Mirrors app/schemas.py's PrimarySummaryOut exactly (admin-only). */
@Serializable
data class PrimarySummaryOut(
    val primaryId: String,
    val name: String,
    val sponsorId: String,
    val sponsorName: String,
    val profileCompleted: Boolean,
)

/**
 * Mirrors app/schemas.py's AccountsListOut exactly -- previously used one
 * shared `AccountListEntry{id, name}` shape for both sponsors and primaries,
 * but the backend returns two different, richer, differently-keyed types
 * (SponsorSummaryOut/PrimarySummaryOut, neither of which has a generic "id"
 * field), so both arrays would have crashed decode immediately.
 */
@Serializable
data class AccountsListOut(
    val sponsors: List<SponsorSummaryOut> = emptyList(),
    val primaries: List<PrimarySummaryOut> = emptyList(),
)

/** Mirrors app/schemas.py's InviteLinkOut exactly -- `expiresAt` never matched the real `invite_expires_at` key (required, so this would have crashed rather than just silently decoding null), and `reused` was missing entirely. */
@Serializable
data class InviteLinkOut(
    val sponsorId: String,
    val inviteToken: String,
    val inviteExpiresAt: String,
    val reused: Boolean,
)
