package com.d2m.app.data.network

import com.d2m.app.data.model.GuestVouchOtpSentOut
import com.d2m.app.data.model.IdentifyIn
import com.d2m.app.data.model.PublicProfileChartOut
import com.d2m.app.data.model.PublicProfileLinkOut
import com.d2m.app.data.model.PublicVouchOut
import com.d2m.app.data.model.VouchGuestCreateIn
import com.d2m.app.data.model.VouchGuestOtpRequestIn
import io.ktor.client.call.body

/**
 * Mirrors app/routers/links.py's PUBLIC (unauthenticated) surface -- the
 * landing page a share link's recipient actually opens. Distinct from
 * ShareLinksApi.kt, which is the link OWNER'S management surface (create/
 * list/update their own links); this is the anonymous-visitor-facing half.
 *
 * Every call here is gated the same way d2m_web's getPublicProfileLink is:
 * an X-D2M-User-Id header (only ever set for a logged-in CHILD-role
 * visitor -- a parent's browser/app has no Primary.id of its own to send)
 * or an X-D2M-Lead-Key header (a returning anonymous visitor's cached key
 * from a prior identify() call). This class doesn't attach either
 * implicitly -- unlike ApiClient's own applyAuthHeader(), this app's
 * WedLock bearer token means nothing to this endpoint -- callers pass
 * whichever of the two they currently have.
 */
class PublicLinksApi(private val api: ApiClient) {

    private fun gateHeaders(viewerPrimaryId: String?, leadKey: String?): Map<String, String> = buildMap {
        if (viewerPrimaryId != null) put("X-D2M-User-Id", viewerPrimaryId)
        if (leadKey != null) put("X-D2M-Lead-Key", leadKey)
    }

    suspend fun getProfile(code: String, viewerPrimaryId: String?, leadKey: String?): PublicProfileLinkOut =
        api.get("/links/$code", extraHeaders = gateHeaders(viewerPrimaryId, leadKey))

    // Split from getProfile above (Phase 5: "split heavy APIs") -- the
    // heaviest part of the old combined payload, fetched independently so
    // the base profile doesn't wait on it. Same gate as getProfile, but
    // deliberately never recorded as a second page view server-side.
    suspend fun getChart(code: String, viewerPrimaryId: String?, leadKey: String?): PublicProfileChartOut =
        api.get("/links/$code/chart", extraHeaders = gateHeaders(viewerPrimaryId, leadKey))

    /**
     * The anonymous-visitor half of the gate -- submits the name+phone a
     * LeadCaptureGate collected and gets back the exact same payload
     * getProfile() would have, in one round trip, PLUS the freshly-minted
     * lead_key (X-D2M-Lead-Key response header) the caller should cache for
     * next time (see app/routers/links.py's identify_share_link docstring).
     */
    suspend fun identify(code: String, name: String, phone: String): Pair<PublicProfileLinkOut, String?> {
        val response = api.postRaw("/links/$code/identify", IdentifyIn(name, phone))
        return response.body<PublicProfileLinkOut>() to response.headers["X-D2M-Lead-Key"]
    }

    /** Approved vouches for the link's own profile -- no gate (this list carries no sensitive detail). */
    suspend fun listVouches(code: String): List<PublicVouchOut> =
        api.get("/links/$code/vouches")

    suspend fun requestGuestVouchOtp(code: String, email: String): GuestVouchOtpSentOut =
        api.post("/links/$code/vouches/guest/otp", VouchGuestOtpRequestIn(email))

    suspend fun createGuestVouch(
        code: String,
        email: String,
        otpCode: String,
        guestName: String,
        phone: String,
        note: String?,
    ): PublicVouchOut =
        api.post("/links/$code/vouches/guest", VouchGuestCreateIn(email, otpCode, guestName, phone, note))
}
