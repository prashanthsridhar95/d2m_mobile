package com.d2m.app.domain.repository

import com.d2m.app.data.model.PublicProfileChartOut
import com.d2m.app.data.model.PublicProfileLinkOut
import com.d2m.app.data.model.PublicVouchOut
import com.d2m.app.data.network.PublicLinksApi

/**
 * Thin wrapper over PublicLinksApi -- no ApiCache here, unlike most other
 * repositories in this app. Every call here is a one-shot anonymous visit
 * to a page this app never otherwise shows (a share link isn't revisited
 * the way Discover/Browse/Matches are), so there's nothing worth caching
 * across requests the way a 2-minute suggestion-feed TTL would be.
 */
class PublicLinksRepository(private val api: PublicLinksApi) {
    suspend fun getProfile(code: String, viewerPrimaryId: String?, leadKey: String?): PublicProfileLinkOut =
        api.getProfile(code, viewerPrimaryId, leadKey)

    suspend fun getChart(code: String, viewerPrimaryId: String?, leadKey: String?): PublicProfileChartOut =
        api.getChart(code, viewerPrimaryId, leadKey)

    suspend fun identify(code: String, name: String, phone: String): Pair<PublicProfileLinkOut, String?> =
        api.identify(code, name, phone)

    suspend fun listVouches(code: String): List<PublicVouchOut> =
        api.listVouches(code)

    suspend fun requestGuestVouchOtp(code: String, email: String) {
        api.requestGuestVouchOtp(code, email)
    }

    suspend fun createGuestVouch(code: String, email: String, otpCode: String, guestName: String, phone: String, note: String?): PublicVouchOut =
        api.createGuestVouch(code, email, otpCode, guestName, phone, note)
}
