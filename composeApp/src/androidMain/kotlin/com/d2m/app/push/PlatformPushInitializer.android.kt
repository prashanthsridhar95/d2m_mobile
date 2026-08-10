package com.d2m.app.push

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Requires the google-services Gradle plugin + your own project's
 * google-services.json to actually initialize (see README.md) -- wrapped in
 * runCatching so a build without that config degrades to "no push" rather
 * than crashing on launch.
 */
actual class PlatformPushInitializer {
    actual fun initialize() {
        runCatching {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener
                val token = task.result ?: return@addOnCompleteListener
                CoroutineScope(Dispatchers.Default).launch {
                    org.koin.core.context.GlobalContext.get()
                        .get<PushTokenRegistrar>()
                        .registerCurrentToken(platform = "fcm", token = token)
                }
            }
        }
    }
}
