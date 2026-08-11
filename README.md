# D2M Mobile

Native Android + iOS client for D2M, built on Kotlin Multiplatform / Compose
Multiplatform (KMP/CMP) -- one `commonMain` source set drives both platforms,
per the plan's Phase decisions (see `D2M_Mobile_App_Plan.md` in the D2M
folder for the full rationale and scope discussion this build followed).

Package: `com.d2m.app`. Backend: `d2m_core_engine` (FastAPI, dev-mode auth,
same limitation this app inherits -- see "Auth" below). Visual language
adopted from `app-ui.prashanthsridhar.com`, applied only to screens the
current backend API actually supports (see plan §5's explicit scope
boundary -- multi-parent, guest links, password login, i18n, etc. from that
reference are intentionally NOT built here).

## What's implemented (all 7 phases)

- **Phase 0** -- project scaffold, data layer (models/networking/cache/
  session), domain repositories, design system, navigation.
- **Phase 1** -- entry/onboarding/parent screens (Login, Role picker,
  Onboarding wizard, Handoff, Parent home/browse, Profile detail).
- **Phase 2** -- child screens (Claim flow, Child home, Discovery/swipe,
  Matches, Child profile dialog).
- **Phase 3** -- push scaffolding (FCM on Android, APNs request + device
  token capture on iOS; see "Push" below for what's stubbed).
- **Phase 4** -- parity closeout: Success Gallery, admin gallery moderation,
  full Panchangam calendar, Browse table view.
- **Phase 5** -- hardening: 429 retry/backoff in `ApiClient`, a real fix to
  `ApiCache`'s stale-while-revalidate path (it was blocking instead of
  backgrounding -- see that file's git history), `commonTest` unit tests for
  `ApiCache`/`ApiClient`/`SuggestionsRepository`, a Compose UI test.
- **Phase 6** -- messaging: full wire-protocol port from `messaging-framework`,
  a real WebSocket client with the reference client's reconnect/heartbeat
  behavior, working chat UI. Encryption is explicitly NOT real yet -- see
  "Security" below, this is the one deliberate exception to "all phases
  implemented."

## What's NOT verified

**This was built in a sandbox with no Android SDK, no Xcode, and no network
access to Maven Central or the Gradle distribution server.** Every file here
has been written and reviewed by hand for structural/type correctness, but
none of it has been compiled, run, or tested end-to-end. Before relying on
this:

1. **Finish the Gradle wrapper.** `gradle/wrapper/gradle-wrapper.properties`,
   `gradlew`, and `gradlew.bat` are committed (pinned to Gradle 8.7, the
   minimum AGP 8.5.2 requires), but `gradle/wrapper/gradle-wrapper.jar` --
   a small binary bootstrap jar -- could not be added from this sandbox (no
   network access, and it's binary so it can't be hand-authored safely). In
   a terminal with network access, run this once from the repo root:
   ```
   gradle wrapper --gradle-version 8.7
   ```
   (any locally installed Gradle works to run this -- it just regenerates
   the jar to match the properties file already here; `brew install gradle`
   first if you don't have one). If you don't want to install anything,
   Android Studio's own "Sync Project with Gradle Files" will usually offer
   to create the missing wrapper jar itself the first time you open this
   project -- accept that prompt if you see it.
2. **Check the Gradle JDK.** AGP 8.5.2 requires JDK 17+ to *run* Gradle
   itself (separate from the app's own `sourceCompatibility`, which targets
   JVM 11 bytecode). In Android Studio: Settings/Preferences > Build,
   Execution, Deployment > Build Tools > Gradle > "Gradle JDK" -- set it to
   17 or newer. Running AGP 8.5 under an older Gradle JDK is a common cause
   of opaque `Unable to load class ...` sync errors like
   `DefaultArtifactPublicationSet`.
3. Open in Android Studio (or IntelliJ with the KMP plugin), let Gradle sync,
   and fix whatever the compiler actually flags -- treat this as a
   thoroughly-drafted first pass, not a verified build.
4. By default the app points at the real `d2m_core_engine`/`messaging-framework`
   deployments behind Cloudflare Tunnels (`api.prashanthsridhar.com`,
   `chat.prashanthsridhar.com`), same pattern as the web app's
   `app.prashanthsridhar.com`. To point at a backend running on your own
   laptop instead, edit `HttpEngine.android.kt`/`HttpEngine.ios.kt`'s
   `resolveDefaultBaseUrl()` and `MessagingRepository.kt`'s
   `MessagingConfig` -- each has a doc comment with the local-dev values
   (`10.0.2.2:8000` for the Android emulator, `127.0.0.1:8000` for the iOS
   simulator, `:4000` for messaging-framework).
5. Run `./gradlew :composeApp:testDebugUnitTest` (Android target) once Gradle
   can actually resolve dependencies, to exercise the Phase 5 test suite.
6. For iOS, see `iosApp/README.md` -- the `.xcodeproj` itself isn't included
   (see that file for why, and the 3-step process to generate one).

## Security: messaging encryption is a stub

`messaging/crypto/CryptoProvider.kt`'s default binding
(`StubUnencryptedCryptoProvider`) is base64, **not encryption**. This was a
deliberate choice, not an oversight -- hand-rolling Signal Protocol
cryptography under this build's constraints would have been worse than
flagging the gap clearly. See that file's doc comment and
`messaging/README.md` for the three real implementation paths considered
(Signal's own `libsignal` Android JNI bindings + no current iOS KMP
equivalent; a vetted third-party KMP Signal Protocol library; hand-porting
`messaging-framework`'s `crypto.ts` against a KMP crypto primitives library).
**Do not ship this to real users carrying real conversations without
replacing this binding and getting a second set of eyes on the crypto
specifically.**

## Auth

There is no real session/token auth layer here, because there isn't one on
the backend yet either (explicitly deferred, along with real push send and
production hosting, per the scoping conversation this build followed).
`IdentityStore` is a direct port of the web app's `DevIdentity.jsx`: it
remembers a plain `sponsorId`/`primaryId` locally, with no credential check.
`ApiClient.kt`'s doc comment marks the one place a bearer-token interceptor
needs to go when real auth lands.

## Push

Token *capture* is wired (FCM on Android via `PlatformPushInitializer.android.kt`;
APNs permission request + device token capture on iOS via `iOSApp.swift`'s
`AppDelegate`) and registers against `POST /accounts/{id}/device-tokens`. Real
push *send* doesn't exist server-side yet (`notification_service.dispatch()`
only logs a stub result) -- this is expected, not a mobile-side bug. Android
additionally needs your own Firebase project's `google-services.json` dropped
into `composeApp/` (gitignored) before FCM will actually initialize; without
it, `PlatformPushInitializer` degrades to a no-op rather than crashing.

## Project structure

```
composeApp/src/
  commonMain/kotlin/com/d2m/app/
    data/         # models, ApiClient + per-router Api classes, ApiCache, IdentityStore
    domain/       # repositories (cache-aware wrappers around the Api classes)
    di/           # AppModule.kt -- single Koin module wiring everything above
    ui/           # theme, reusable components, navigation graph, all screens
    messaging/    # Phase 6 -- see messaging/README.md
    push/         # PushTokenRegistrar + expect PlatformPushInitializer
    App.kt        # root composable: start-destination resolution, bottom-tab chrome, push init
  commonTest/      # Phase 5 test suite
  androidMain/     # HttpEngine/Settings actuals, D2MApplication, MainActivity, FCM service
  iosMain/         # HttpEngine/Settings actuals, MainViewController
iosApp/            # Swift/Info.plist sources -- see iosApp/README.md
```

## Build instructions (once you have the toolchain)

Android: open the repo root in Android Studio, let Gradle sync, run the
`composeApp` configuration on an emulator or device (min SDK 26).

iOS: follow `iosApp/README.md` to generate the Xcode project shell, then
build/run from Xcode (iOS 15+).
