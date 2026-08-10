package com.d2m.app.domain.repository

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.model.*
import com.d2m.app.data.network.DashboardApi
import com.d2m.app.data.network.PanchangamApi

class DashboardRepository(
    private val api: DashboardApi,
    private val cache: ApiCache,
) {
    suspend fun getChart(primaryId: String): ChartOut =
        cache.get("chart:$primaryId", ttlMillis = 10 * 60_000) { api.getChart(primaryId) }.value

    suspend fun getSponsorChild(sponsorId: String, forceRefresh: Boolean = false): SponsorChildOut {
        val key = "sponsor-child:$sponsorId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 60_000) { api.getSponsorChild(sponsorId) }.value
    }

    suspend fun getPrimaryDashboard(primaryId: String, forceRefresh: Boolean = false): PrimaryDashboardOut {
        val key = "dashboard:primary:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 30_000) { api.getPrimaryDashboard(primaryId) }.value
    }

    suspend fun getSponsorDashboard(sponsorId: String, forceRefresh: Boolean = false): SponsorDashboardOut {
        val key = "dashboard:sponsor:$sponsorId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 30_000) { api.getSponsorDashboard(sponsorId) }.value
    }
}

class PanchangamRepository(
    private val api: PanchangamApi,
    private val cache: ApiCache,
) {
    suspend fun getDay(primaryId: String, on: String? = null): PanchangamDayOut =
        cache.get("panchangam-day:$primaryId:${on ?: "today"}", ttlMillis = 60 * 60_000) { api.getDay(primaryId, on) }.value

    suspend fun getMonth(primaryId: String, year: Int? = null, month: Int? = null): PanchangamMonthOut =
        cache.get("panchangam-month:$primaryId:$year:$month", ttlMillis = 60 * 60_000) { api.getMonth(primaryId, year, month) }.value
}
