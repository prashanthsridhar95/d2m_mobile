package com.d2m.app.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun httpEngine(): HttpClient = HttpClient(Darwin)

// The iOS simulator (unlike the Android emulator) shares the host's loopback
// interface directly, so `localhost` reaches a dev backend running on the
// same Mac with no NAT alias needed. A physical iPhone still needs the
// host's real LAN IP, same caveat as the Android actual's doc comment.
actual fun resolveDefaultBaseUrl(): String = "http://127.0.0.1:8000"
