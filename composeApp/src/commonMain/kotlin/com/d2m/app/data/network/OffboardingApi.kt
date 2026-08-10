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

    suspend fun publishGalleryEntry(entryId: String): SuccessGalleryEntryOut =
        api.post("/admin/success-gallery/$entryId/publish")

    suspend fun getAccounts(): AccountsListOut =
        api.get("/admin/accounts")

    suspend fun getInviteLink(sponsorId: String): InviteLinkOut =
        api.post("/admin/sponsors/$sponsorId/invite-link")
}
