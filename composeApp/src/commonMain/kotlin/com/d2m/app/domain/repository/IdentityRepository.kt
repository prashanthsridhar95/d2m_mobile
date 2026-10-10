package com.d2m.app.domain.repository

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.model.*
import com.d2m.app.data.network.IdentityApi

/**
 * Wraps IdentityApi with ApiCache the same way d2m_web/src/lib/api/identity.js's
 * functions get wrapped by useApiQuery at the call site -- reads are cached
 * (2 min default TTL), writes invalidate the relevant prefix so the next
 * read is fresh rather than serving a stale cached value back immediately
 * after a mutation.
 */
class IdentityRepository(
    private val api: IdentityApi,
    private val cache: ApiCache,
) {
    // Not cached -- only ever called once, right after a fresh WedLock
    // login (see LoginScreen.kt), never re-read by another screen.
    suspend fun getMe(): MeOut = api.getMe()

    suspend fun createSponsor(body: SponsorCreateRequest): SponsorCreateResponse =
        api.createSponsor(body)

    suspend fun getSponsorProfile(sponsorId: String, forceRefresh: Boolean = false): SponsorProfileOut {
        val key = "sponsor-profile:$sponsorId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key) { api.getSponsorProfile(sponsorId) }.value
    }

    suspend fun redeemInvite(body: InviteRedeemRequest): InviteRedeemResponse =
        api.redeemInvite(body)

    suspend fun setChildPreferences(primaryId: String, body: ChildPreferencesRequest): ChildPreferencesResponse {
        val result = api.setChildPreferences(primaryId, body)
        cache.invalidate("primary-profile:$primaryId")
        cache.invalidate("dashboard:")
        return result
    }

    suspend fun getPrimaryProfile(primaryId: String, forceRefresh: Boolean = false): PrimaryProfileOut {
        val key = "primary-profile:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key) { api.getPrimaryProfile(primaryId) }.value
    }

    suspend fun updatePrimaryBasicData(primaryId: String, body: PrimaryBasicDataIn): PrimaryBasicDataOut {
        val result = api.updatePrimaryBasicData(primaryId, body)
        cache.invalidateKey("primary-profile:$primaryId")
        return result
    }

    suspend fun setHideNameOverride(primaryId: String, hidden: Boolean): HideNameOut {
        val result = api.setHideNameOverride(primaryId, HideNameIn(hidden))
        cache.invalidateKey("primary-profile:$primaryId")
        return result
    }

    // Not cached -- a status that can change server-side (admin review)
    // without any action on this device, so a stale cached "pending"
    // would silently hide an approval/rejection.
    suspend fun getIdentityVerification(primaryId: String): IdentityVerificationOut? =
        api.getIdentityVerification(primaryId)

    suspend fun submitIdentityVerification(
        primaryId: String, documentType: String,
        documentFileName: String, documentContentType: String, documentBytes: ByteArray,
        selfieFileName: String, selfieContentType: String, selfieBytes: ByteArray,
    ): IdentityVerificationOut =
        api.submitIdentityVerification(
            primaryId, documentType,
            documentFileName, documentContentType, documentBytes,
            selfieFileName, selfieContentType, selfieBytes,
        )

    suspend fun getPrimarySponsor(primaryId: String): PrimarySponsorOut =
        cache.get("primary-sponsor:$primaryId") { api.getPrimarySponsor(primaryId) }.value

    // Not cached -- a one-off lookup triggered by an explicit search
    // action, not a value any other screen would ever re-read.
    suspend fun searchByShortId(code: String): ShortIdSearchResultOut =
        api.searchByShortId(code)

    suspend fun updateLocationPreference(sponsorId: String, body: LocationPreferenceIn): LocationPreferenceOut {
        val result = api.updateLocationPreference(sponsorId, body)
        cache.invalidate("dashboard:")
        cache.invalidate("suggestions:")
        return result
    }

    suspend fun getPhotos(primaryId: String, forceRefresh: Boolean = false): List<PhotoOut> {
        val key = "photos:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key) { api.getPhotos(primaryId) }.value
    }

    suspend fun uploadPhoto(primaryId: String, fileName: String, contentType: String, bytes: ByteArray): PhotoOut {
        val result = api.uploadPhoto(primaryId, fileName, contentType, bytes)
        cache.invalidateKey("photos:$primaryId")
        return result
    }

    suspend fun deletePhoto(primaryId: String, photoId: String) {
        api.deletePhoto(primaryId, photoId)
        cache.invalidateKey("photos:$primaryId")
    }

    suspend fun getExtendedBio(primaryId: String, forceRefresh: Boolean = false): ExtendedBioDataOut {
        val key = "extended-bio:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key) { api.getExtendedBio(primaryId) }.value
    }

    suspend fun updateExtendedBio(primaryId: String, body: ExtendedBioDataIn): ExtendedBioDataOut {
        val result = api.updateExtendedBio(primaryId, body)
        cache.setCached("extended-bio:$primaryId", result)
        return result
    }

    suspend fun getAboutMe(primaryId: String, forceRefresh: Boolean = false): AboutMeDataOut {
        val key = "about-me:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key) { api.getAboutMe(primaryId) }.value
    }

    suspend fun updateAboutMe(primaryId: String, body: AboutMeDataIn): AboutMeDataOut {
        val result = api.updateAboutMe(primaryId, body)
        cache.setCached("about-me:$primaryId", result)
        return result
    }
}
