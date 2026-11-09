package com.d2m.app.domain.repository

import com.d2m.app.data.model.*
import com.d2m.app.data.network.WeddingApi

/** Uncached on purpose: both families edit the same plan, so every open should see fresh state. */
class WeddingRepository(private val api: WeddingApi) {
    suspend fun get(primaryId: String) = api.get(primaryId)
    suspend fun setDates(primaryId: String, dates: WeddingDatesIn) = api.setDates(primaryId, dates)
    suspend fun setTradition(primaryId: String, tradition: String?) = api.setTradition(primaryId, tradition)
    suspend fun createTask(primaryId: String, body: WeddingTaskCreateIn) = api.createTask(primaryId, body)
    suspend fun setConfirmed(primaryId: String, taskId: String, confirmed: Boolean) =
        if (confirmed) api.confirm(primaryId, taskId) else api.unconfirm(primaryId, taskId)
    suspend fun setDue(primaryId: String, taskId: String, due: String?) = api.patchDue(primaryId, taskId, due)
    suspend fun setTitle(primaryId: String, taskId: String, title: String) = api.patchTitle(primaryId, taskId, title)
    suspend fun setVisibility(primaryId: String, taskId: String, visibility: String) = api.patchVisibility(primaryId, taskId, visibility)
    suspend fun deleteTask(primaryId: String, taskId: String) = api.deleteTask(primaryId, taskId)
    suspend fun activity(primaryId: String, taskId: String) = api.activity(primaryId, taskId)
    suspend fun addComment(primaryId: String, taskId: String, body: String) = api.addComment(primaryId, taskId, body)
    suspend fun deleteComment(primaryId: String, taskId: String, commentId: String) = api.deleteComment(primaryId, taskId, commentId)
    suspend fun upload(primaryId: String, taskId: String, fileName: String, mime: String, bytes: ByteArray) =
        api.uploadAttachment(primaryId, taskId, fileName, mime, bytes)
    suspend fun deleteAttachment(primaryId: String, taskId: String, attachmentId: String) = api.deleteAttachment(primaryId, taskId, attachmentId)
    fun attachmentUrl(primaryId: String, taskId: String, attachmentId: String) = api.attachmentUrl(primaryId, taskId, attachmentId)
    suspend fun download(primaryId: String, taskId: String, attachmentId: String) = api.downloadAttachment(primaryId, taskId, attachmentId)
    suspend fun panchangam(primaryId: String, start: String, end: String) = api.panchangam(primaryId, start, end)
}
