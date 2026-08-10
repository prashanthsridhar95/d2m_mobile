package com.d2m.app.data.network

import com.d2m.app.data.model.*

/** Mirrors app/routers/notifications.py -- in-app log + push registration (see plan §4/§7: push send is a backend no-op today). */
class NotificationsApi(private val api: ApiClient) {

    suspend fun getNotifications(accountId: String): List<NotificationOut> =
        api.get("/accounts/$accountId/notifications")

    suspend fun markRead(accountId: String, notificationId: String): NotificationOut =
        api.post("/accounts/$accountId/notifications/$notificationId/read")

    suspend fun getPreferences(accountId: String): NotificationPreferenceOut =
        api.get("/accounts/$accountId/notification-preferences")

    suspend fun setPreferences(accountId: String, body: NotificationPreferenceIn): NotificationPreferenceOut =
        api.put("/accounts/$accountId/notification-preferences", body)

    /** Registers a push token. Delivery itself is a no-op server-side until the backend's
     *  notification_service SWAP POINT is implemented -- registering early costs nothing. */
    suspend fun registerDeviceToken(accountId: String, platform: String, token: String): DeviceTokenOut =
        api.post("/accounts/$accountId/device-tokens", DeviceTokenIn(platform, token))
}
