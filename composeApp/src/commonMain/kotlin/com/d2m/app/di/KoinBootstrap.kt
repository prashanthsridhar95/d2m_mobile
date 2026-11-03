package com.d2m.app.di

import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

/**
 * Single Koin start point, shared by every process-entry path in this app --
 * previously this logic lived private inside MainViewController.kt (iOS)
 * and only ran the first time SwiftUI actually built the Compose view
 * controller. That's fine for a normal foreground launch, but wrong for
 * every OS-triggered background wake this app now has: a PushKit VoIP push
 * can launch the app process (AppDelegate.didFinishLaunchingWithOptions)
 * well before any UI is ever shown, and a Notification Service Extension is
 * an entirely separate OS process that never touches MainViewController at
 * all. Both need Koin's singletons (MessagingRepository, CallManager, etc.
 * -- see IosPushBridge.kt's callers) available before they can do anything,
 * exactly the same guarantee Android gets for free from
 * Application.onCreate() always running before any woken component
 * (BroadcastReceiver, FirebaseMessagingService) in that process.
 *
 * Guarded so calling this more than once in the same process (e.g.
 * AppDelegate calls it at launch, then MainViewController() calls it again
 * once the user actually opens the UI) is always safe -- Koin's own
 * startKoin{} throws "already started" on a second real call, which this
 * avoids by checking KoinPlatform.getKoinOrNull() directly rather than
 * keeping a separate local flag (a local flag wouldn't help across the
 * different call sites needing this in the first place). Uses
 * org.koin.mp.KoinPlatform, NOT org.koin.core.context.GlobalContext --
 * confirmed directly via `klib dump-metadata` against koin-core's real
 * Kotlin/Native klib that GlobalContext is only a JVM/Android-artifact
 * extra (koin-core-android exposes it; koin-core-iossimulatorarm64 does
 * not, only an internal MutableGlobalContext), which is why this compiled
 * fine when copied from the Android push receivers but failed outright the
 * first time this project ever actually compiled for a native iOS target.
 */
object KoinBootstrap {
    fun ensureStarted() {
        if (KoinPlatform.getKoinOrNull() != null) return
        startKoin {
            modules(appModule)
        }
    }
}
