package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/** Mirrors app/routers/wedding.py. Field names map via ApiClient's snake_case strategy. */
@Serializable
data class WeddingNamesOut(val bride: String? = null, val groom: String? = null)

@Serializable
data class WeddingPlanOut(
    val id: String,
    val unionId: String,
    val myRole: String,
    val tradition: String? = null,
    val names: WeddingNamesOut = WeddingNamesOut(),
    val engagementDate: String? = null,
    val marriageDate: String? = null,
    val honeymoonDate: String? = null,
)

@Serializable
data class WeddingPhaseOut(val key: String, val label: String)

@Serializable
data class WeddingProgressOut(
    val total: Int = 0,
    val done: Int = 0,
    val myOpen: Int = 0,
    val overdue: Int = 0,
    val waitingOnOthers: Int = 0,
)

@Serializable
data class WeddingTaskOut(
    val id: String,
    val title: String,
    val notes: String? = null,
    val phase: String,
    val scope: String,
    val participants: List<String> = emptyList(),
    val externalParty: String? = null,
    val traditions: List<String> = emptyList(),
    val anchor: String? = null,
    val offsetDays: Int = 0,
    val dueDate: String? = null,
    val dueDateIsCustom: Boolean = false,
    val status: String,
    val confirmedBy: List<String> = emptyList(),
    val waitingOn: List<String> = emptyList(),
    val iConfirmed: Boolean = false,
    val iAmParticipant: Boolean = false,
    val overdue: Boolean = false,
    val isCustom: Boolean = false,
    val visibility: String = "all",
    val createdByRole: String? = null,
    val canDelete: Boolean = false,
    val canChangeVisibility: Boolean = false,
    val commentCount: Int = 0,
    val attachmentCount: Int = 0,
)

@Serializable
data class WeddingOut(
    val plan: WeddingPlanOut,
    val phases: List<WeddingPhaseOut> = emptyList(),
    val progress: WeddingProgressOut = WeddingProgressOut(),
    val tasks: List<WeddingTaskOut> = emptyList(),
)

/** PUT /wedding/dates: every field is sent (null clears), matching how the dialog always submits all three. */
@Serializable
data class WeddingDatesIn(
    val engagementDate: String? = null,
    val marriageDate: String? = null,
    val honeymoonDate: String? = null,
)

@Serializable
data class WeddingTaskCreateIn(
    val title: String,
    val phase: String = "planning",
    val visibility: String = "all",
    val dueDate: String? = null,
    val participants: List<String>? = null,
)

/** PUT /wedding/dates body that changes only the tradition (null = all Brahmin traditions). */
@Serializable
data class WeddingTraditionIn(val tradition: String? = null)

/** PATCH body that touches only the due date (null = back to the calculated date). */
@Serializable
data class WeddingDuePatchIn(val dueDate: String? = null)

@Serializable
data class WeddingCommentIn(val body: String)

@Serializable
data class WeddingCommentOut(
    val id: String,
    val authorRole: String,
    val body: String,
    val createdAt: String,
    val mine: Boolean = false,
)

@Serializable
data class WeddingAttachmentOut(
    val id: String,
    val filename: String,
    val contentType: String,
    val sizeBytes: Int,
    val uploadedByRole: String,
    val createdAt: String,
    val isImage: Boolean = false,
    val mine: Boolean = false,
)

@Serializable
data class WeddingActivityOut(
    val legacyNote: String? = null,
    val comments: List<WeddingCommentOut> = emptyList(),
    val attachments: List<WeddingAttachmentOut> = emptyList(),
)

/** One day's panchangam flags for the couple (GET /wedding/panchangam). */
@Serializable
data class WeddingPanchangamDay(
    val date: String,
    val tithi: String = "",
    val nakshatra: String = "",
    val isAmavasya: Boolean = false,
    val isPurnima: Boolean = false,
    val isMuhoortham: Boolean = false,
    val chandrashtamam: List<String> = emptyList(),
)

@Serializable
data class WeddingPanchangamOut(val available: Boolean = false, val days: List<WeddingPanchangamDay> = emptyList())

@Serializable
data class WeddingDeleteOut(val deleted: Boolean = true)
