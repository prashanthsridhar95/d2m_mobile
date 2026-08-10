package com.d2m.app.data.session

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Set by D2MApplication.onCreate() before any screen reads IdentityStore --
 * see androidMain/.../D2MApplication.kt. Kept as a nullable holder rather
 * than threading Context through the whole commonMain dependency graph
 * (commonMain must stay platform-agnostic; this is the one Android-only seam).
 */
object AndroidSettingsContextHolder {
    lateinit var appContext: Context
}

actual fun createSettings(): Settings {
    val prefs = AndroidSettingsContextHolder.appContext
        .getSharedPreferences("d2m_settings", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(prefs)
}
