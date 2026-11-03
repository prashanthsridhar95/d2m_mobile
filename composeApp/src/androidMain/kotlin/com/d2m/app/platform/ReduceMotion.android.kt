package com.d2m.app.platform

import android.provider.Settings
import com.d2m.app.data.session.AndroidSettingsContextHolder

actual fun systemReduceMotion(): Boolean = try {
    Settings.Global.getFloat(AndroidSettingsContextHolder.appContext.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
} catch (_: Throwable) {
    false
}
