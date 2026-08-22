package com.d2m.app.push.ui

import androidx.compose.runtime.Composable

// No-op -- see BatteryOptimizationCompat.kt's doc comment. iOS has no
// per-app battery-optimization allowlist concept for this to request.
@Composable
actual fun rememberBatteryOptimizationRequester(): () -> Unit = {}
