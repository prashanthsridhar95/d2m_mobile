package com.d2m.app.data.network

import com.d2m.app.data.model.*

/** Mirrors app/routers/identity.py -- onboarding, account, photos, extended bio. */
class IdentityApi(private val api: ApiClient) {

    suspend fun getMe(): MeOut =
        api.get("/me")

    suspend fun createSponsor(body: SponsorCreateRequest): SponsorCreateResponse =
        api.post("/sponsors", body)

    suspend fun getSponsorProfile(sponsorId: String): SponsorProfileOut =
        api.get("/sponsors/$sponsorId")

    suspend fun redeemInvite(body: InviteRedeemRequest): InviteRedeemResponse =
        api.post("/invites/redeem", body)

    suspend fun setChildPreferences(primaryId: String, body: ChildPreferencesRequest): ChildPreferencesResponse =
        api.post("/primaries/$primaryId/preferences", body)

    suspend fun getPrimaryProfile(primaryId: String): PrimaryProfileOut =
        api.get("/primaries/$primaryId/profile")

    suspend fun updatePrimaryBasicData(primaryId: String, body: PrimaryBasicDataIn): PrimaryBasicDataOut =
        api.put("/primaries/$primaryId/basic-data", body)

    suspend fun setHideNameOverride(primaryId: String, body: HideNameIn): HideNameOut =
        api.put("/primaries/$primaryId/hide-name", body)

    suspend fun getPrimarySponsor(primaryId: String): PrimarySponsorOut =
        api.get("/primaries/$primaryId/sponsor")

    suspend fun searchByShortId(code: String): ShortIdSearchResultOut =
        api.get("/primaries/search", mapOf("code" to code))

    suspend fun updateLocationPreference(sponsorId: String, body: LocationPreferenceIn): LocationPreferenceOut =
        api.put("/sponsors/$sponsorId/location-preference", body)

    suspend fun getPhotos(primaryId: String): List<PhotoOut> =
        api.get("/primaries/$primaryId/photos")

    suspend fun uploadPhoto(primaryId: String, fileName: String, contentType: String, bytes: ByteArray): PhotoOut =
        api.postForm("/primaries/$primaryId/photos", fileName, contentType, bytes)

    suspend fun deletePhoto(primaryId: String, photoId: String) {
        api.delete<Map<String, Boolean>>("/primaries/$primaryId/photos/$photoId")
    }

    suspend fun getExtendedBio(primaryId: String): ExtendedBioDataOut =
        api.get("/primaries/$primaryId/extended-bio")

    suspend fun updateExtendedBio(primaryId: String, body: ExtendedBioDataIn): ExtendedBioDataOut =
        api.put("/primaries/$primaryId/extended-bio", body)

    suspend fun getAboutMe(primaryId: String): AboutMeDataOut =
        api.get("/primaries/$primaryId/about-me")

    suspend fun updateAboutMe(primaryId: String, body: AboutMeDataIn): AboutMeDataOut =
        api.put("/primaries/$primaryId/about-me", body)
}
