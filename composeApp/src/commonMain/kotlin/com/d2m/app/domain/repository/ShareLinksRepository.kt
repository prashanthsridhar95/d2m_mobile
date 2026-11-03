package com.d2m.app.domain.repository

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.model.CustomFieldSelection
import com.d2m.app.data.model.ShareLinkOut
import com.d2m.app.data.model.ShareLinkViewerOut
import com.d2m.app.data.network.ShareLinksApi

class ShareLinksRepository(
    private val api: ShareLinksApi,
    private val cache: ApiCache,
) {
    suspend fun listShareLinks(primaryId: String, forceRefresh: Boolean = false): List<ShareLinkOut> {
        val key = "share-links:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 15_000) { api.listShareLinks(primaryId) }.value
    }

    suspend fun createShareLink(
        primaryId: String,
        detailLevel: String,
        ttlHours: Int? = null,
        customFields: CustomFieldSelection? = null,
    ): ShareLinkOut {
        val r = api.createShareLink(primaryId, detailLevel, ttlHours, customFields)
        cache.invalidateKey("share-links:$primaryId")
        return r
    }

    suspend fun updateShareLink(primaryId: String, linkId: String, isActive: Boolean? = null, extendHours: Int? = null): ShareLinkOut {
        val r = api.updateShareLink(primaryId, linkId, isActive, extendHours)
        cache.invalidateKey("share-links:$primaryId")
        return r
    }

    /** Not cached: who has looked is exactly the thing that changes between two opens of the sheet. */
    suspend fun listViewers(primaryId: String, linkId: String): List<ShareLinkViewerOut> = api.listViewers(primaryId, linkId)
}
