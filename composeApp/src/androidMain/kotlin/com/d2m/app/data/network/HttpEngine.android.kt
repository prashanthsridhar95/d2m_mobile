package com.d2m.app.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

actual fun httpEngine(): HttpClient = HttpClient(Android)

// Points at the real d2m_core_engine deployment behind a Cloudflare Tunnel,
// same pattern as the web app's app.prashanthsridhar.com frontend tunnel.
// Works unchanged on the emulator, a physical device on the same LAN, or a
// device on cellular -- unlike a bare IP, this doesn't depend on the
// emulator's 10.0.2.2 loopback alias or the host machine's LAN address.
//
// For local-only dev against a laptop-hosted backend instead, swap this for
// "http://10.0.2.2:8000" (Android emulator's alias for the host's
// 127.0.0.1:8000) or your host's real LAN IP for a physical device.
actual fun resolveDefaultBaseUrl(): String = "https://api.prashanthsridhar.com"
