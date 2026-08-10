package com.d2m.app.domain.repository

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.model.*
import com.d2m.app.data.network.AdminApi
import com.d2m.app.data.network.OffboardingApi

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
    suspend fun publishGalleryEntry(entryId: String): SuccessGalleryEntryOut = api.publishGalleryEntry(entryId)
    suspend fun getAccounts(): AccountsListOut = api.getAccounts()
    suspend fun getInviteLink(sponsorId: String): InviteLinkOut = api.getInviteLink(sponsorId)
}
