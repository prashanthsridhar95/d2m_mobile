package com.d2m.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Mirrors app/models/consent.py -- the Consent Gateway (parent access-to-
 * prospect-details flow). Previously had `requestedAt: String` as a required
 * field, but the backend's own docstring says it's only ever populated by
 * the GET list endpoint -- the request/decide endpoints return it as null,
 * which would have crashed decode (null into a non-nullable String) the
 * first time either of those was actually called. Also had a `decidedAt`
 * field that doesn't exist on the backend at all.
 */
@Serializable
data class ConsentRequestOut(
    val requestId: String,
    val sponsorId: String,
    val primaryId: String,
    val prospectId: String,
    val status: String, // pending | granted | denied
    val prospectName: String? = null,
    val requestedAt: String? = null,
)

@Serializable
data class ConsentDecisionIn(val decision: String) // grant | deny

/**
 * Mirrors app/schemas.py's ProspectCardOut exactly -- the masked-by-default
 * prospect card. The previous version of this type bore almost no
 * resemblance to the real shape (required non-nullable `name`, a `city`/
 * `compositeScore`/`occupationTitle`/`contactInfo`/`gothram` that don't
 * exist on this endpoint, and an `unlocked: Boolean` where the backend
 * actually sends a `consent_status` string) -- decoding a genuinely masked
 * card (the common case: `name` is null until consent is granted, that's
 * the entire point of this schema) would have thrown MissingFieldException
 * immediately.
 */
@Serializable
data class ProspectCardOut(
    val prospectId: String,
    val consentStatus: String, // none | pending | granted
    // Masked fields -- present while consentStatus != "granted"
    val nameMasked: String? = null,
    val ageBucket: String? = null,
    val photoBlurred: Boolean? = null,
    // Unlocked fields -- present only once consentStatus == "granted"
    val name: String? = null,
    val age: Int? = null,
    val religion: String? = null,
    val community: String? = null,
    val hobbies: List<String>? = null,
    val lifestyleTags: List<String>? = null,
    val astrologySummary: JsonObject? = null,
    val photoUrl: String? = null,
)
