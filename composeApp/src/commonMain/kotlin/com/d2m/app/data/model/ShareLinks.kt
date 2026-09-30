package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/** Mirrors app/services/links_service.py's TOGGLEABLE_FIELDS + the
 *  "horoscope" (bundled on/off toggle, not a per-field checkbox) and
 *  "photo_count" (how many photos, not a boolean) keys -- see that
 *  module for the exact shape a "custom" link's custom_fields dict takes.
 *  Every default is false/1, matching the backend's own
 *  _normalize_custom_fields ("opt IN each field"), so a caller only needs
 *  to set what it wants turned on. namingStrategy = SnakeCase
 *  (ApiClient.kt) converts every field below to its snake_case wire name
 *  automatically -- no per-field @SerialName needed. */
@Serializable
data class CustomFieldSelection(
    val occupationTitle: Boolean = false,
    val heightCm: Boolean = false,
    val complexion: Boolean = false,
    val highestEducation: Boolean = false,
    val motherTongue: Boolean = false,
    val otherLanguages: Boolean = false,
    val bodyType: Boolean = false,
    val religion: Boolean = false,
    val casteCommunity: Boolean = false,
    val sect: Boolean = false,
    val gothram: Boolean = false,
    val institution: Boolean = false,
    val employer: Boolean = false,
    val employmentSector: Boolean = false,
    val financialStatus: Boolean = false,
    val elderBrothersCount: Boolean = false,
    val youngerBrothersCount: Boolean = false,
    val elderSistersCount: Boolean = false,
    val youngerSistersCount: Boolean = false,
    val nativity: Boolean = false,
    val familyType: Boolean = false,
    val familyValues: Boolean = false,
    val citizenshipStatus: Boolean = false,
    val horoscope: Boolean = false,
    val photoCount: Int = 1,
) {
    /** "Custom (N fields)" for a link-list summary label -- counts only
     *  the boolean toggles that are on (horoscope included, photoCount
     *  excluded since it's a count, not a field). */
    fun trueFieldCount(): Int = listOf(
        occupationTitle, heightCm, complexion, highestEducation, motherTongue, otherLanguages,
        bodyType, religion, casteCommunity, sect, gothram, institution, employer, employmentSector,
        financialStatus, elderBrothersCount, youngerBrothersCount, elderSistersCount,
        youngerSistersCount, nativity, familyType, familyValues, citizenshipStatus, horoscope,
    ).count { it }
}

/** Mirrors app/schemas.py's ShareLinkCreateIn. */
@Serializable
data class ShareLinkCreateIn(
    val detailLevel: String, // "full" | "minimal" | "custom"
    val ttlHours: Int? = null,
    val customFields: CustomFieldSelection? = null, // required (and only valid) when detailLevel == "custom"
)

/** Mirrors app/schemas.py's ShareLinkOut -- `code` is the short (10-char)
 *  public identifier; the caller builds the full share URL as
 *  `${apiClient.baseUrl}/${code}` (app/routers/links.py's root-level
 *  preview/unfurl page). */
@Serializable
data class ShareLinkOut(
    val id: String,
    val code: String,
    val detailLevel: String,
    val isActive: Boolean,
    val expiresAt: String,
    val createdAt: String,
    val viewCount: Int,
    val customFields: CustomFieldSelection? = null,
    val lastViewedAt: String? = null,
)

/** Mirrors app/schemas.py's ShareLinkUpdateIn -- optional-field partial update. */
@Serializable
data class ShareLinkUpdateIn(
    val isActive: Boolean? = null,
    val extendHours: Int? = null,
)
