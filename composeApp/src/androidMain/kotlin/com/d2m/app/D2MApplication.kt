package com.d2m.app

import android.app.Application
import com.d2m.app.data.session.AndroidSettingsContextHolder
import com.d2m.app.di.appModule
import com.d2m.app.messaging.call.AndroidWebRtcContextHolder
import com.d2m.app.push.installLocalNotificationBridge
import org.koin.core.context.startKoin

/**
 * Application entry point -- must be referenced via android:name in
 * AndroidManifest.xml's <application> tag (wired there in this same pass).
 * Two jobs, both must happen before any screen resolves a Koin dependency:
 *  1. Set AndroidSettingsContextHolder.appContext so createSettings() (see
 *     data/session/Settings.android.kt) can open SharedPreferences.
 *  2. startKoin { modules(appModule) } so every screen's koinInject() call
 *     resolves.
 *  3. installLocalNotificationBridge(this) (push/LocalNotificationBridge.kt)
 *     -- must run after startKoin since it resolves MessagingRepository/
 *     CallManager from the Koin container to listen for messages/calls to
 *     notify about while the app is backgrounded.
 *
 * Deliberately not using koin-android's androidContext()/androidLogger() --
 * the project only depends on koin-core + koin-compose (see
 * composeApp/build.gradle.kts and PlatformPushInitializer.android.kt's own
 * GlobalContext.get() usage), and nothing here needs Android Context
 * injected *through* Koin itself (AndroidSettingsContextHolder already
 * covers the one place that needs it). Adding koin-android is a one-line
 * follow-up if a future dependency wants an injected Context.
 */
class D2MApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidSettingsContextHolder.appContext = applicationContext
        AndroidWebRtcContextHolder.appContext = applicationContext
        startKoin {
            modules(appModule)
        }
        installLocalNotificationBridge(this)
    }
}
