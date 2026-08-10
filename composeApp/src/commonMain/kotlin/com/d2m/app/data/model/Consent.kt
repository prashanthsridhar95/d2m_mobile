package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/** Mirrors app/models/consent.py -- the Consent Gateway (parent access-to-prospect-details flow). */
@Serializable
data class ConsentRequestOut(
    val requestId: String,
    val sponsorId: String,
    val primaryId: String,
    val prospectId: String,
    val status: String, // pending | granted | denied
    val requestedAt: String,
    val decidedAt: String? = null,
)

@Serializable
data class ConsentDecisionIn(val decision: String) // grant | deny

/** Masked-by-default prospect card, unlocked field by field once access is granted. */
@Serializable
data class ProspectCardOut(
    val prospectId: String,
    val name: String,
    val age: Int? = null,
    val city: String? = null,
    val compositeScore: Double? = null,
    val unlocked: Boolean = false,
    val occupationTitle: String? = null,
    val contactInfo: String? = null,
    val gothram: String? = null,
    val photoUrl: String? = null,
)
