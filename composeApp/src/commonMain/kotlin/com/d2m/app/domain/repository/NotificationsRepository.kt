package com.d2m.app.domain.repository

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.model.*
import com.d2m.app.data.network.NotificationsApi

class NotificationsRepository(
    private val api: NotificationsApi,
    private val cache: ApiCache,
) {
    suspend fun getNotifications(accountId: String, forceRefresh: Boolean = false): List<NotificationOut> {
        val key = "notifications:$accountId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 20_000) { api.getNotifications(accountId) }.value
    }

    suspend fun markRead(accountId: String, notificationId: String): NotificationOut {
        val r = api.markRead(accountId, notificationId)
        cache.invalidateKey("notifications:$accountId")
        return r
    }

    suspend fun getPreferences(accountId: String): NotificationPreferenceOut =
        api.getPreferences(accountId)

    suspend fun setPreferences(accountId: String, body: NotificationPreferenceIn): NotificationPreferenceOut =
        api.setPreferences(accountId, body)

    suspend fun registerDeviceToken(accountId: String, platform: String, token: String): DeviceTokenOut =
        api.registerDeviceToken(accountId, platform, token)
}
