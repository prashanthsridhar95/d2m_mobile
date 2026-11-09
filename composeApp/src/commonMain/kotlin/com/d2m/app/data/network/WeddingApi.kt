package com.d2m.app.data.network

import com.d2m.app.data.model.*

/** Mirrors app/routers/wedding.py -- the post-union wedding checklist. */
class WeddingApi(private val api: ApiClient) {

    suspend fun get(primaryId: String): WeddingOut = api.get("/primaries/$primaryId/wedding")

    suspend fun setDates(primaryId: String, dates: WeddingDatesIn): WeddingOut =
        api.put("/primaries/$primaryId/wedding/dates", dates)

    suspend fun createTask(primaryId: String, body: WeddingTaskCreateIn): WeddingTaskOut =
        api.post("/primaries/$primaryId/wedding/tasks", body)

    suspend fun confirm(primaryId: String, taskId: String): WeddingTaskOut =
        api.post("/primaries/$primaryId/wedding/tasks/$taskId/confirm")

    suspend fun unconfirm(primaryId: String, taskId: String): WeddingTaskOut =
        api.post("/primaries/$primaryId/wedding/tasks/$taskId/unconfirm")

    suspend fun deleteTask(primaryId: String, taskId: String): WeddingDeleteOut =
        api.delete("/primaries/$primaryId/wedding/tasks/$taskId")
}
