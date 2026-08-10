package com.d2m.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SponsorChildOut(val primaryId: String)

@Serializable
data class PrimaryDashboardOut(
    val profileCompleted: Boolean,
    val pendingSuggestionCount: Int,
    val topSuggestions: List<SuggestionOut> = emptyList(),
    val threadStatus: String? = null,
    val unreadNotificationCount: Int,
)

@Serializable
data class SponsorDashboardOut(
    val childProfileCompleted: Boolean,
    val pendingSuggestionCount: Int,
    val topSuggestions: List<SuggestionOut> = emptyList(),
    // Sponsor-safe: never exposes thread participants, message content, or scores --
    // mirrors dashboard.py's own "sponsor-safe" comment.
    val childStatusLabel: String? = null,
    val unreadNotificationCount: Int,
)
