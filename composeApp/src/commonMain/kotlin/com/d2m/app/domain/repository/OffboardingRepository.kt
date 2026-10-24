package com.d2m.app.domain.repository

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.model.*
import com.d2m.app.data.network.AdminApi
import com.d2m.app.data.network.OffboardingApi
import com.d2m.app.data.network.StaffGalleryApi

class OffboardingRepository(
    private val api: OffboardingApi,
    private val cache: ApiCache,
) {
    suspend fun getUnionStatus(primaryId: String, otherPrimaryId: String): UnionStatusOut =
        api.getUnionStatus(primaryId, otherPrimaryId)

    suspend fun confirmUnion(primaryId: String, otherPrimaryId: String): UnionConfirmOut =
        api.confirmUnion(primaryId, otherPrimaryId)

    suspend fun galleryOptIn(primaryId: String, otherPrimaryId: String, consent: Boolean): GalleryOptInOut =
        api.galleryOptIn(primaryId, otherPrimaryId, consent)

    suspend fun getSuccessGallery(forceRefresh: Boolean = false): List<SuccessGalleryEntryOut> {
        val key = "success-gallery"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 5 * 60_000) { api.getSuccessGallery() }.value
    }
}

/** Internal-only, mirrors GalleryModerationScreen.jsx -- see plan §4 (Phase 4 parity closeout). */
class AdminRepository(private val api: AdminApi) {
    suspend fun getAccounts(): AccountsListOut = api.getAccounts()
    suspend fun getInviteLink(sponsorId: String): InviteLinkOut = api.getInviteLink(sponsorId)
}

/** Staff-token gallery moderation (/staff/gallery/...). */
class StaffGalleryRepository(private val api: StaffGalleryApi) {
    suspend fun queue(status: String = "pending") = api.queue(status)
    suspend fun publish(entryId: String, acknowledgeRisk: Boolean, note: String? = null) = api.publish(entryId, acknowledgeRisk, note)
    suspend fun reject(entryId: String, note: String) = api.reject(entryId, note)
    suspend fun unpublish(entryId: String, note: String) = api.unpublish(entryId, note)
}
