package com.d2m.app.ui.strings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The three languages d2m_web already ships (src/i18n/index.js's
 * SUPPORTED_LANGUAGES) -- kept in lockstep with that file rather than
 * independently decided, since this app and the web app serve the same
 * families and should read as the same product in whichever language
 * someone picks. EN is both the default and the fallback: every [AppStrings]
 * implementation is complete (no missing-key fallback mechanism exists here
 * the way i18next's t(key, default) has one), so EN is the one language
 * that can never be incomplete by construction -- HiStrings.kt/TaStrings.kt
 * are checked against EnStrings.kt's field set by the compiler itself
 * (all three implement the same [AppStrings] interface), not by a runtime
 * key-parity script the way the web locale JSON files needed.
 */
enum class AppLocale(val code: String, val nativeName: String) {
    EN("en", "English"),
    HI("hi", "हिन्दी"),
    TA("ta", "தமிழ்");

    companion object {
        fun fromCode(code: String?): AppLocale = entries.firstOrNull { it.code == code } ?: EN
    }
}

private const val KEY_LOCALE = "d2m_locale"

/**
 * Direct Kotlin port of d2m_web/src/i18n/index.js's persistence half
 * (there: localStorage, keyed by STORAGE_KEY = "d2m_language"; here: the
 * same [Settings] IdentityStore.kt already uses). Deliberately its own
 * tiny class rather than a field on IdentityStore -- language is a
 * device/app preference, not part of a signed-in identity (it should
 * survive logout, and has a sensible meaning before any identity exists
 * at all, e.g. on LoginScreen.kt).
 */
class LocaleStore(private val settings: Settings) {
    private val _locale = MutableStateFlow(load())
    val locale: StateFlow<AppLocale> = _locale.asStateFlow()

    private fun load(): AppLocale = AppLocale.fromCode(settings[KEY_LOCALE, ""].takeIf { it.isNotEmpty() })

    fun setLocale(locale: AppLocale) {
        settings.putString(KEY_LOCALE, locale.code)
        _locale.value = locale
    }
}
