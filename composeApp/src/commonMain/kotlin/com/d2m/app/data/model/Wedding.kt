package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/** Mirrors app/routers/wedding.py. Field names map via ApiClient's snake_case strategy. */
@Serializable
data class WeddingPlanOut(
    val id: String,
    val unionId: String,
    val myRole: String,
    val tradition: String? = null,
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
    val dueDate: String? = null,
    val status: String,
    val confirmedBy: List<String> = emptyList(),
    val waitingOn: List<String> = emptyList(),
    val iConfirmed: Boolean = false,
    val iAmParticipant: Boolean = false,
    val overdue: Boolean = false,
    val isCustom: Boolean = false,
    val visibility: String = "all",
    val canDelete: Boolean = false,
    val canChangeVisibility: Boolean = false,
)

@Serializable
data class WeddingOut(
    val plan: WeddingPlanOut,
    val phases: List<WeddingPhaseOut> = emptyList(),
    val progress: WeddingProgressOut = WeddingProgressOut(),
    val tasks: List<WeddingTaskOut> = emptyList(),
)

/** Only the fields present are sent as changes (explicitNulls off is not needed: unset fields are omitted by the caller via separate bodies). */
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

@Serializable
data class WeddingDeleteOut(val deleted: Boolean = true)
