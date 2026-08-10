package com.d2m.app.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

actual fun httpEngine(): HttpClient = HttpClient(Android)

// A physical/emulated Android device can't resolve `127.0.0.1` as "the host
// machine running the dev backend" the way a desktop browser can -- 10.0.2.2
// is the Android emulator's documented alias for the host loopback interface.
// A real device on the same LAN needs the host's actual local IP instead
// (set via a build config field once you know it); this default only covers
// the emulator case, matching d2m_web's `VITE_API_BASE_URL` dev default in
// spirit, not by value.
actual fun resolveDefaultBaseUrl(): String = "http://10.0.2.2:8000"
