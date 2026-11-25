package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/**
 * Mirrors app/routers/lagna.py + app/schemas.py (LagnaAnswers, LagnaDeduceRequest/Out): the Nashta Jataka lagna
 * deduction used when the time of birth is unknown. Answers are never stored server-side -- only the engine's
 * summary (tob_source / tob_confidence) -- and are kept out of the wizard's saved draft on this side too.
 */
@Serializable
data class LagnaLifeEvent(val eventType: String, val year: Int, val month: Int? = null)

@Serializable
data class LagnaAnswers(
    val tobWindow: String = "UNKNOWN",
    val traits: List<String>? = null,
    val parentalEnvironment: String? = null,
    val educationDomain: String? = null,
    val studiedAbroad: Boolean? = null,
    val youngerSibling: String? = null,
    val siblingBondClose: Boolean? = null,
    val familyAffluent: Boolean? = null,
    val conventionalEarlyMarriage: Boolean? = null,
    val settledAbroadAfterMarriage: Boolean? = null,
    val lifeEvents: List<LagnaLifeEvent> = emptyList(),
)

@Serializable
data class LagnaOption(val value: String, val label: String)

/** One questionnaire entry. kind = single_choice | multi_choice | yes_no | events. */
@Serializable
data class LagnaQuestion(
    val id: String,
    val kind: String = "single_choice",
    val weight: Double = 0.0,
    val text: String = "",
    val options: List<LagnaOption> = emptyList(),
    /** Applies only when every listed field has one of the listed values, e.g. {"younger_sibling": ["BROTHER"]}. */
    val onlyIf: Map<String, List<String>>? = null,
)

@Serializable
data class LagnaQuestionsOut(val engineVersion: String = "", val questions: List<LagnaQuestion> = emptyList())

@Serializable
data class LagnaDeduceRequest(
    val dob: String,
    val birthLat: Double,
    val birthLon: Double,
    val birthTzOffsetHours: Double,
    val maritalStatus: String? = null,
    val answers: LagnaAnswers,
)

@Serializable
data class LagnaCandidate(val lagna: String = "", val confidence: Double = 0.0)

@Serializable
data class LagnaDeduceOut(
    /** LOCKED | NEEDS_CLARIFICATION */
    val status: String,
    val lagna: String? = null,
    val confidence: Double = 0.0,
    /** "HH:MM" the server would store if the user stops here. */
    val provisionalTob: String? = null,
    /** DEDUCED_LOCKED | DEDUCED_PROVISIONAL | ESTIMATED_NO_DATA */
    val tobSource: String = "ESTIMATED_NO_DATA",
    val informative: Boolean = false,
    val candidates: List<LagnaCandidate> = emptyList(),
    val nextQuestions: List<LagnaQuestion> = emptyList(),
)
