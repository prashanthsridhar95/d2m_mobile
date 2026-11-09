package com.d2m.app.domain.repository

import com.d2m.app.data.model.*
import com.d2m.app.data.network.WeddingApi

/** Uncached on purpose: both families edit the same plan, so every open should see fresh state. */
class WeddingRepository(private val api: WeddingApi) {
    suspend fun get(primaryId: String) = api.get(primaryId)
    suspend fun setDates(primaryId: String, dates: WeddingDatesIn) = api.setDates(primaryId, dates)
    suspend fun createTask(primaryId: String, body: WeddingTaskCreateIn) = api.createTask(primaryId, body)
    suspend fun setConfirmed(primaryId: String, taskId: String, confirmed: Boolean) =
        if (confirmed) api.confirm(primaryId, taskId) else api.unconfirm(primaryId, taskId)
    suspend fun deleteTask(primaryId: String, taskId: String) = api.deleteTask(primaryId, taskId)
}
