package com.d2m.app.push.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberBatteryOptimizationRequester(): () -> Unit {
    val context = LocalContext.current
    // Result ignored -- this is a courtesy prompt the person can decline
    // (same as any other OS permission-style dialog); nothing in this app
    // gates functionality on the answer, it only improves killed-app
    // delivery reliability when granted.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    return {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val alreadyExempt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        if (!alreadyExempt) {
            // Requires the REQUEST_IGNORE_BATTERY_OPTIMIZATIONS manifest
            // permission (declared in AndroidManifest.xml) -- without it this
            // intent throws a SecurityException. runCatching defensively:
            // some OEM builds ship without this settings screen at all.
            runCatching {
                launcher.launch(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
            }
        }
    }
}
