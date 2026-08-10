package com.d2m.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class NotificationOut(
    val notificationId: String,
    val accountId: String,
    val type: String, // new_suggestion | mutual_match | serious_mode_request | serious_mode_accepted |
                       // serious_mode_revoked | consent_request | consent_granted | request_received
    val payload: JsonObject? = null,
    val createdAt: String,
    val readAt: String? = null,
)

@Serializable
data class NotificationPreferenceIn(
    val muted: Boolean? = null,
    val frequency: String? = null, // immediate | daily_digest
)

@Serializable
data class NotificationPreferenceOut(
    val accountId: String,
    val muted: Boolean,
    val frequency: String,
)

@Serializable
data class DeviceTokenIn(
    val platform: String, // fcm | apns
    val token: String,
)

@Serializable
data class DeviceTokenOut(
    val deviceTokenId: String,
    val accountId: String,
    val platform: String,
)
