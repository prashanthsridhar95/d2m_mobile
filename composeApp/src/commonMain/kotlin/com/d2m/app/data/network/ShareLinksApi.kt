package com.d2m.app.data.network

import com.d2m.app.data.model.*

/** Mirrors the /primaries/{id}/share-links routes in app/routers/identity.py --
 *  create/manage side only. The public landing page a link's recipient
 *  actually opens is a web-only route (app/routers/links.py's GET
 *  /links/{token}); this app never renders that page itself, only creates
 *  and manages the links from the parent/child side. */
class ShareLinksApi(private val api: ApiClient) {

    suspend fun listShareLinks(primaryId: String): List<ShareLinkOut> =
        api.get("/primaries/$primaryId/share-links")

    suspend fun createShareLink(
        primaryId: String,
        detailLevel: String,
        ttlHours: Int? = null,
        customFields: CustomFieldSelection? = null,
    ): ShareLinkOut =
        api.post("/primaries/$primaryId/share-links", ShareLinkCreateIn(detailLevel, ttlHours, customFields))

    suspend fun updateShareLink(primaryId: String, linkId: String, isActive: Boolean? = null, extendHours: Int? = null): ShareLinkOut =
        api.put("/primaries/$primaryId/share-links/$linkId", ShareLinkUpdateIn(isActive, extendHours))
}
