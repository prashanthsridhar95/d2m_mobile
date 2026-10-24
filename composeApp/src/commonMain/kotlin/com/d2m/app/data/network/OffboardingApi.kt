package com.d2m.app.data.network

import com.d2m.app.data.model.*

/** Mirrors app/routers/offboarding.py -- union confirmation / Success Gallery. */
class OffboardingApi(private val api: ApiClient) {

    suspend fun getUnionStatus(primaryId: String, otherPrimaryId: String): UnionStatusOut =
        api.get("/primaries/$primaryId/unions/$otherPrimaryId")

    suspend fun confirmUnion(primaryId: String, otherPrimaryId: String): UnionConfirmOut =
        api.post("/primaries/$primaryId/unions/$otherPrimaryId/confirm")

    suspend fun galleryOptIn(primaryId: String, otherPrimaryId: String, consent: Boolean): GalleryOptInOut =
        api.post("/primaries/$primaryId/unions/$otherPrimaryId/gallery-opt-in", GalleryOptInIn(consent))

    suspend fun getSuccessGallery(): List<SuccessGalleryEntryOut> =
        api.get("/success-gallery")
}

/** Mirrors app/routers/admin.py -- dev-tooling only, not registered when D2M_ENVIRONMENT=production. */
class AdminApi(private val api: ApiClient) {

    suspend fun getAccounts(): AccountsListOut =
        api.get("/admin/accounts")

    suspend fun getInviteLink(sponsorId: String): InviteLinkOut =
        api.post("/admin/sponsors/$sponsorId/invite-link")
}

/**
 * Mirrors app/routers/staff_ops.py gallery moderation (REL-14). Authenticated
 * by the normal Bearer session token that ApiClient attaches; the server's
 * require_staff dependency enforces access (403 for non-staff). No internal
 * secret is involved anywhere in the client.
 */
class StaffGalleryApi(private val api: ApiClient) {
    suspend fun queue(status: String = "pending", limit: Int = 50, offset: Int = 0): List<GalleryQueueItemOut> =
        api.get("/staff/gallery/queue", mapOf("status" to status, "limit" to limit.toString(), "offset" to offset.toString()))

    suspend fun publish(entryId: String, acknowledgeRisk: Boolean, note: String?): GalleryQueueItemOut =
        api.post("/staff/gallery/$entryId/publish", GalleryPublishIn(acknowledgeRisk, note))

    suspend fun reject(entryId: String, note: String): GalleryQueueItemOut =
        api.post("/staff/gallery/$entryId/reject", GalleryDecisionIn(note))

    suspend fun unpublish(entryId: String, note: String): GalleryQueueItemOut =
        api.post("/staff/gallery/$entryId/unpublish", GalleryDecisionIn(note))
}
