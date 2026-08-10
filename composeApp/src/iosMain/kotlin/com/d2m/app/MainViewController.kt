package com.d2m.app

import androidx.compose.ui.window.ComposeUIViewController
import com.d2m.app.di.appModule
import org.koin.core.context.startKoin

/**
 * iOS entry point -- called from iosApp/iosApp/iOSApp.swift's
 * `ComposeView()` wrapper (see that file's doc comment). Koin has no
 * separate "Application.onCreate()" equivalent on iOS, so it's started here,
 * guarded so a hot-reload/multiple-call scenario during development doesn't
 * crash on Koin's "already started" check.
 */
fun MainViewController() = ComposeUIViewController {
    ensureKoinStarted()
    App()
}

private var koinStarted = false

private fun ensureKoinStarted() {
    if (koinStarted) return
    koinStarted = true
    startKoin {
        modules(appModule)
    }
}
