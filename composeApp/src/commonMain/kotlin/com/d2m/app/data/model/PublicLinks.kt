package com.d2m.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Mirrors app/schemas.py's PublicProfileLinkOut -- what an anonymous share-
 * link visitor sees. The horoscope chart is a SEPARATE call (see
 * PublicProfileChartOut below, GET /links/{code}/chart) -- split out on the
 * backend to keep the base profile from being gated behind the heaviest
 * part of the old combined payload (d2m_core_engine's Phase 5).
 */
@Serializable
data class PublicProfileLinkOut(
    val detailLevel: String,
    val name: String,
    val age: Int? = null,
    val city: String? = null,
    val photoUrls: List<String> = emptyList(),
    val occupationTitle: String? = null,
    val heightCm: Int? = null,
    val complexion: String? = null,
    val highestEducation: String? = null,
    val motherTongue: String? = null,
    val otherLanguages: List<String>? = null,
    val bodyType: String? = null,
    val religion: String? = null,
    val casteCommunity: String? = null,
    val sect: String? = null,
    val gothram: String? = null,
    val institution: String? = null,
    val employer: String? = null,
    val employmentSector: String? = null,
    val financialStatus: String? = null,
    val elderBrothersCount: Int? = null,
    val youngerBrothersCount: Int? = null,
    val elderSistersCount: Int? = null,
    val youngerSistersCount: Int? = null,
    val nativity: String? = null,
    val familyType: String? = null,
    val familyValues: String? = null,
    val citizenshipStatus: String? = null,
)

/** Mirrors app/schemas.py's PublicProfileChartOut -- GET /links/{code}/chart. */
@Serializable
data class PublicProfileChartOut(
    val moonNakshatra: String? = null,
    val moonPada: Int? = null,
    val d1: JsonObject? = null,
    val d9: JsonObject? = null,
    val dashaAtBirth: JsonObject? = null,
)

/** Body for POST /links/{code}/identify -- see that schema's own docstring. */
@Serializable
data class IdentifyIn(val name: String, val phone: String)

/**
 * Mirrors app/schemas.py's VouchOut exactly for the public GET /links/{code}
 * /vouches read path -- deliberately its own type rather than reusing
 * data/model/Trust.kt's VouchOut, which mirrors a different shape (the
 * WedLock-account-linked trust subsystem's own vouch concept, not
 * vouch_service's d2m-native one this endpoint actually returns).
 */
@Serializable
data class PublicVouchOut(
    val id: String,
    val subjectPrimaryId: String,
    val subjectName: String,
    val voucherType: String, // "parent" | "child" | "guest"
    val voucherName: String,
    val voucherSponsorId: String? = null,
    val voucherPrimaryId: String? = null,
    val voucherProfileId: String? = null,
    val guestContact: String? = null,
    val guestPhone: String? = null,
    val note: String? = null,
    val status: String, // "approved" | "pending" | "declined"
)

@Serializable
data class VouchGuestOtpRequestIn(val email: String)

@Serializable
data class GuestVouchOtpSentOut(val sent: Boolean = true)

@Serializable
data class VouchGuestCreateIn(
    val email: String,
    val code: String,
    val guestName: String,
    val phone: String,
    val note: String? = null,
)
