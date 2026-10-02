package com.d2m.app.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun httpEngine(): HttpClient = HttpClient(Darwin)

// Points at the real d2m_core_engine deployment behind a Cloudflare Tunnel,
// same pattern as the web app's app.prashanthsridhar.com frontend tunnel --
// see HttpEngine.android.kt's doc comment. Works unchanged on the simulator
// or a physical iPhone on any network.
//
// For local-only dev against a laptop-hosted backend instead, swap this for
// "http://127.0.0.1:8000" (the iOS simulator shares the host's loopback
// interface directly, unlike the Android emulator) or the host's real LAN
// IP for a physical device.
actual fun resolveDefaultBaseUrl(): String = "http://127.0.0.1:8000"

// WedLock IAM has no production tunnel yet -- see WedLockApi.kt's doc
// comment on resolveWedlockBaseUrl(). The iOS simulator shares the host's
// loopback interface directly, so 127.0.0.1 reaches the locally-running
// wedlock_iam service exactly like it reaches d2m_core_engine above.
actual fun resolveWedlockBaseUrl(): String = "http://127.0.0.1:8010/api/v1"
