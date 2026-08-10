package com.d2m.app.data.network

import com.d2m.app.data.model.*

/** Mirrors app/routers/dashboard.py -- chart read + composed dashboards + sponsor-push. */
class DashboardApi(private val api: ApiClient) {

    suspend fun getChart(primaryId: String): ChartOut =
        api.get("/primaries/$primaryId/chart")

    suspend fun getSponsorChild(sponsorId: String): SponsorChildOut =
        api.get("/sponsors/$sponsorId/child")

    suspend fun getPrimaryDashboard(primaryId: String): PrimaryDashboardOut =
        api.get("/primaries/$primaryId/dashboard")

    suspend fun getSponsorDashboard(sponsorId: String): SponsorDashboardOut =
        api.get("/sponsors/$sponsorId/dashboard")
}

/** Mirrors app/routers/panchangam.py. */
class PanchangamApi(private val api: ApiClient) {

    suspend fun getDay(primaryId: String, on: String? = null): PanchangamDayOut =
        api.get("/primaries/$primaryId/panchangam/day", mapOf("on" to on))

    suspend fun getMonth(primaryId: String, year: Int? = null, month: Int? = null): PanchangamMonthOut =
        api.get(
            "/primaries/$primaryId/panchangam/month",
            mapOf("year" to year?.toString(), "month" to month?.toString()),
        )
}
