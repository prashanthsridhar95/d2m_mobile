package com.d2m.app.push.ui

import androidx.compose.runtime.Composable

/**
 * Requests exemption from Android's battery-optimization/Doze restrictions
 * -- one of the most common real-world causes of "notifications don't
 * arrive when the app is killed" on Android, especially on aggressive OEM
 * skins (Xiaomi/MIUI, Samsung, OnePlus/OxygenOS, etc.) that kill background
 * processes and throttle FCM delivery well beyond what stock Android/Doze
 * does, regardless of the push being sent data-only at high priority (see
 * D2MFirebaseMessagingService.kt's doc comment on that half of the fix).
 * Reported directly, after confirming both the client (google-services.json)
 * and server (D2M_FCM_SERVICE_ACCOUNT_JSON) push config were already
 * correctly set up on the same machine: "runs in the same machine - but
 * notifications not received when app is not live."
 *
 * No-op on iOS (no equivalent OS concept -- APNs delivery isn't subject to
 * a per-app battery-optimization allowlist the way Android's Doze/App
 * Standby is).
 */
@Composable
expect fun rememberBatteryOptimizationRequester(): () -> Unit
