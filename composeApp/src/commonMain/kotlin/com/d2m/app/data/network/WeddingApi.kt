package com.d2m.app.data.network

import com.d2m.app.data.model.*

/** Mirrors app/routers/wedding.py -- the post-union wedding checklist. */
class WeddingApi(private val api: ApiClient) {
    private fun base(primaryId: String) = "/primaries/$primaryId/wedding"

    suspend fun get(primaryId: String): WeddingOut = api.get(base(primaryId))

    suspend fun setDates(primaryId: String, dates: WeddingDatesIn): WeddingOut =
        api.put("${base(primaryId)}/dates", dates)

    suspend fun setTradition(primaryId: String, tradition: String?): WeddingOut =
        api.put("${base(primaryId)}/dates", WeddingTraditionIn(tradition))

    suspend fun createTask(primaryId: String, body: WeddingTaskCreateIn): WeddingTaskOut =
        api.post("${base(primaryId)}/tasks", body)

    suspend fun confirm(primaryId: String, taskId: String): WeddingTaskOut =
        api.post("${base(primaryId)}/tasks/$taskId/confirm")

    suspend fun unconfirm(primaryId: String, taskId: String): WeddingTaskOut =
        api.post("${base(primaryId)}/tasks/$taskId/unconfirm")

    suspend fun patchDue(primaryId: String, taskId: String, dueDate: String?): WeddingTaskOut =
        api.patch("${base(primaryId)}/tasks/$taskId", WeddingDuePatchIn(dueDate))

    suspend fun patchTitle(primaryId: String, taskId: String, title: String): WeddingTaskOut =
        api.patch("${base(primaryId)}/tasks/$taskId", WeddingTitlePatchIn(title))

    suspend fun patchVisibility(primaryId: String, taskId: String, visibility: String): WeddingTaskOut =
        api.patch("${base(primaryId)}/tasks/$taskId", WeddingVisibilityPatchIn(visibility))

    suspend fun deleteTask(primaryId: String, taskId: String): WeddingDeleteOut =
        api.delete("${base(primaryId)}/tasks/$taskId")

    suspend fun activity(primaryId: String, taskId: String): WeddingActivityOut =
        api.get("${base(primaryId)}/tasks/$taskId/activity")

    suspend fun addComment(primaryId: String, taskId: String, body: String): WeddingCommentOut =
        api.post("${base(primaryId)}/tasks/$taskId/comments", WeddingCommentIn(body))

    suspend fun deleteComment(primaryId: String, taskId: String, commentId: String): WeddingDeleteOut =
        api.delete("${base(primaryId)}/tasks/$taskId/comments/$commentId")

    suspend fun uploadAttachment(primaryId: String, taskId: String, fileName: String, mime: String, bytes: ByteArray): WeddingAttachmentOut =
        api.postForm("${base(primaryId)}/tasks/$taskId/attachments", fileName, mime, bytes)

    suspend fun deleteAttachment(primaryId: String, taskId: String, attachmentId: String): WeddingDeleteOut =
        api.delete("${base(primaryId)}/tasks/$taskId/attachments/$attachmentId")

    /** Absolute URL of an attachment's bytes -- loaded with the authenticated photo ImageLoader (images only). */
    fun attachmentUrl(primaryId: String, taskId: String, attachmentId: String): String =
        api.resolveMediaUrl("${base(primaryId)}/tasks/$taskId/attachments/$attachmentId")

    suspend fun downloadAttachment(primaryId: String, taskId: String, attachmentId: String): ByteArray =
        api.getBytes("${base(primaryId)}/tasks/$taskId/attachments/$attachmentId")

    suspend fun panchangam(primaryId: String, start: String, end: String): WeddingPanchangamOut =
        api.get("${base(primaryId)}/panchangam", mapOf("start" to start, "end" to end))
}
