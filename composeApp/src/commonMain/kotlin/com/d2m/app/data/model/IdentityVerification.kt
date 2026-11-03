package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/** Mirrors app/schemas.py's IdentityVerificationOut -- a Primary's own latest KYC submission. */
@Serializable
data class IdentityVerificationOut(
    val id: String,
    val documentType: String,
    val status: String, // "pending" | "approved" | "rejected"
    val rejectionReason: String? = null,
    val submittedAt: String,
    val reviewedAt: String? = null,
)

/** Self-reported label only, never validated/enforced server-side beyond the fixed set -- see identity_verification_service.VALID_DOCUMENT_TYPES. */
object IdentityDocumentTypes {
    const val PASSPORT = "passport"
    const val DRIVERS_LICENSE = "drivers_license"
    const val NATIONAL_ID = "national_id"
    const val OTHER = "other"
    val ALL = listOf(PASSPORT, DRIVERS_LICENSE, NATIONAL_ID, OTHER)
}
