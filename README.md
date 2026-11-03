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
  behavior, working chat UI. Encryption is real Signal Protocol on both
  platforms now -- see "Security" below.

## Build status

Both apps build, and the shared + platform test suites pass, on a machine
with Xcode 26 and JDK 21:

```
./gradlew :composeApp:testDebugUnitTest        # Android unit tests
./gradlew :composeApp:iosSimulatorArm64Test    # the same commonTest suite, on iOS
```

For the iOS app itself, `iosApp/` now contains a real generated Xcode
project and a resolved CocoaPods workspace -- see `iosApp/README.md`. It has
been built and launched on an iPhone simulator, and also builds for a real
device (`-destination 'generic/platform=iOS'`).

Things worth knowing before relying on this:

1. **Real-device-only features are still unexercised.** PushKit VoIP,
   CallKit's ringing UI and real APNs delivery cannot run in the Simulator at
   all, and WebRTC audio/video needs real hardware to be meaningfully tested.
   `IosCallKitBridge.kt` and `WebRtcEngine.ios.kt` compile and link, but
   "compiles" is a much weaker claim than "works" for those two specifically.
2. **Push needs backend credentials on both sides.** See the push sections
   below and in `iosApp/README.md`; without them token registration still
   succeeds and nothing ever arrives.
3. By default the app points at the real `d2m_core_engine`/`messaging-framework`
   deployments behind Cloudflare Tunnels (`api.prashanthsridhar.com`,
   `chat.prashanthsridhar.com`), same pattern as the web app's
   `app.prashanthsridhar.com`. To point at a backend running on your own
   laptop instead, edit `HttpEngine.android.kt`/`HttpEngine.ios.kt`'s
   `resolveDefaultBaseUrl()` and `MessagingRepository.kt`'s
   `MessagingConfig` -- each has a doc comment with the local-dev values
   (`10.0.2.2:8000` for the Android emulator, `127.0.0.1:8000` for the iOS
   simulator, `:4000` for messaging-framework).
4. **Android<->iOS messaging has not been run end to end against a live
   backend.** The two crypto stacks are pinned to each other by tests (see
   "Security" below), which is a strong claim about the hard part, but it is
   not the same as two real devices exchanging a real message.

## Security: messaging encryption

Both platforms now run the real, from-spec Signal Protocol implementation in
`messaging/crypto/signal/` (X3DH + Double Ratchet + protobuf framing, all
pure Kotlin in commonMain). `StubUnencryptedCryptoProvider` -- base64, **not
encryption** -- is no longer bound on either platform.

iOS was the last holdout and was the more serious half of that gap: it was
not merely "iOS is less secure than Android", it was **Android and iOS could
not exchange messages at all**, because each side handed the other bytes it
had no way to interpret. Closing it needed a `CryptoPrimitives` actual for
iOS, and Apple ships no Curve25519 API that Kotlin/Native can bind to
(CryptoKit is Swift-only; CommonCrypto and Security cover hashes, HMAC, AES
and the NIST curves, but no Montgomery/Edwards curve). So:

- **`messaging/crypto/signal/Curve25519.kt`** (commonMain) -- X25519 ECDH and
  XEdDSA sign/verify in pure Kotlin.
- **`messaging/crypto/signal/CryptoPrimitives.ios.kt`** -- Apple's own
  CommonCrypto/Security for RNG, HMAC-SHA256 and AES-256-CBC, over that
  shared curve code.
- Android is **unchanged** and still uses BouncyCastle + `Ed25519Math.kt`.

The same applies to the Archive Keypair backup system: `archive/P256.kt` and
`archive/AesGcm.kt` (both commonMain) back `ArchivePrimitives.ios.kt`, whose
eight methods previously all threw `NotImplementedError`. P-256 had to be
implemented rather than delegated to Security framework because
`SecKeyCreateWithData` cannot build an EC private key from a bare scalar, and
a bare scalar is all Android's 67-byte PKCS#8 encoding carries -- see
`P256.kt`'s doc comment.

### How this is verified

Hand-written curve code is exactly the kind of thing that should not be
trusted on the strength of having been written carefully, so it is pinned
from two directions:

- `commonTest/.../Curve25519Test.kt` and
  `commonTest/.../ArchivePrimitivesTest.kt` check the published RFC 7748,
  FIPS 180-4, NIST SP 800-38D (GCM) and PBKDF2 vectors. These run on **every**
  target, so a green `iosSimulatorArm64Test` is direct evidence the iOS build
  -- including its CommonCrypto bindings -- computes the specified answers.
- `androidUnitTest/.../CurveInteropAndroidTest.kt` and
  `androidUnitTest/.../ArchiveInteropAndroidTest.kt` diff the shared code
  against the implementations Android already ships -- BouncyCastle,
  `Ed25519Math`, `java.security` P-256, `javax.crypto` AES-GCM -- over
  randomised inputs, including asserting XEdDSA signatures are BYTE-IDENTICAL
  and that an archive wrapped by Android unwraps with the shared code alone.

That second half is the load-bearing one. Two implementations can each be
perfectly self-consistent and still be unable to talk to each other -- which
is precisely the state iOS was in before, since base64 round-trips fine
against itself. During development these tests caught exactly that failure
mode: a transcribed `BB` where RFC 7748's ladder specifies `AA`, which
produced a working, self-consistent Diffie-Hellman that agreed with nobody.

**Still worth a second set of eyes before shipping to real users carrying
real conversations.** The tests above make interop and spec-conformance
tested properties, but they are not a substitute for review of the crypto by
someone who does this professionally, and none of this code is constant-time
(a deliberate trade, documented in each file: these run a handful of times
per session, locally, never per message).

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
push *send* is also wired server-side now (`d2m_core_engine/app/services/push_service.py`,
`firebase_admin`) for `fcm` tokens -- confirmed real, not a stub -- but BOTH
halves below need to actually be set up before push notifications work end to
end (received while the app is backgrounded or fully killed, per a direct
report: "even when i'm not on the app, i should be receiving notifications
regarding calls & messages"). Neither is optional; missing either one means
zero push notifications, silently:

1. **Client (this repo, Android):** create a Firebase project, register an
   Android app under this project's `applicationId` (`com.d2m.app`, see
   `composeApp/build.gradle.kts`), download that app's `google-services.json`,
   and drop it into `composeApp/` (gitignored -- every developer/deploy needs
   their own copy, or all point at the same Firebase project's file). The
   `com.google.gms.google-services` Gradle plugin is already wired
   (`composeApp/build.gradle.kts`, applied conditionally so a checkout
   without the file still builds) -- it just needs that file present.
   Without it, `FirebaseMessaging` never mints a real token, so
   `PlatformPushInitializer` silently degrades to a no-op (never crashes)
   and no `DeviceToken` row is ever registered server-side.
2. **Server (`d2m_core_engine`, not this repo):** set `D2M_FCM_SERVICE_ACCOUNT_JSON`
   on the deployed backend to a Firebase service account key (same Firebase
   project as step 1 -- Firebase console → Project settings → Service
   accounts → Generate new private key). Without it, `push_service.configured()`
   is false and every push attempt records `push_result = "stubbed"` even
   with a real device token registered.

Messages are sent data-only (no `notification=` block) at Android high
priority specifically so `D2MFirebaseMessagingService.onMessageReceived`
always runs -- including while the app is backgrounded or killed -- rather
than the OS auto-displaying a generic tray notification and skipping the
app's own call/message channel + ringtone + action logic for exactly that
case. See that file's doc comment for the full payload contract and its one
real, load-bearing limitation: the messaging relay server is E2E and never
decrypts anything, so a push-triggered call notification knows a sender's
raw username but not their display name or the call's id, so it can invite
the user to open the app but can't offer real Answer/Decline actions the way
the live in-app notification (open socket, real `CallSignal`) can.
iOS-side (`apns` tokens) still registers cleanly but has no real send
implemented -- no APNs SDK integration exists yet.

## Crash & error reporting

Firebase Crashlytics rides the same `google-services.json` push already
needs (see above) -- nothing extra to configure once that file is in place.
The `com.google.firebase.crashlytics` Gradle plugin is applied conditionally
alongside `com.google.gms.google-services`, same reasoning: a checkout with
no Firebase project still builds cleanly, it just doesn't report anywhere.
Every coroutine's last-line-of-defense `CoroutineExceptionHandler`
(`MessagingRepository.kt`, `CallManager.kt`) forwards to
`reportNonFatal()` (`messaging/CrashReporter.kt`) in addition to its
existing `println`, so a recovered-from bug is still visible in the
Crashlytics dashboard after the fact, not just in a live logcat session.

## Release signing

`./gradlew :composeApp:assembleRelease` works out of the box with no setup
at all -- `composeApp/build.gradle.kts`'s `release` build type falls back to
debug signing (logging a Gradle warning when it does) if no real keystore is
configured. That's fine for sanity-checking a release build locally, but it
is **not** a build Play Store (or any real user) should ever receive: debug
signing uses a fixed, publicly-known password/alias that every AGP install
ships, so it proves nothing about who built the APK.

To sign with a real key:

1. Generate a keystore once (`keytool` ships with the JDK):
   ```
   keytool -genkeypair -v -keystore release.keystore.jks \
     -alias d2m-release -keyalg RSA -keysize 2048 -validity 10000
   ```
   Keep the resulting `.jks` file and both passwords it prompts for
   somewhere safe outside this repo -- `.gitignore` already excludes
   `*.jks`/`*.keystore`, but a keystore that's lost can't be regenerated
   with the same signature, and Play Store ties app updates to that
   signature permanently.
2. Locally: add these four keys to `local.properties` (gitignored, same
   file `GIPHY_API_KEY` already lives in -- see "GIF picker" below):
   ```
   RELEASE_STORE_FILE=/absolute/or/project-relative/path/to/release.keystore.jks
   RELEASE_STORE_PASSWORD=<your store password>
   RELEASE_KEY_ALIAS=d2m-release
   RELEASE_KEY_PASSWORD=<your key password>
   ```
3. In CI: the same four keys, but as GitHub Actions repository secrets
   (Settings -> Secrets and variables -> Actions) rather than a committed
   file, injected as environment variables of the same names just before
   a signed build/task runs -- `composeApp/build.gradle.kts` reads
   `local.properties` first and falls back to `System.getenv(...)`, so no
   workflow change is needed beyond setting the job's `env:`:
   ```yaml
   - name: Build signed release
     run: ./gradlew :composeApp:assembleRelease
     env:
       RELEASE_STORE_FILE: ${{ secrets.RELEASE_STORE_FILE }}   # path to a keystore written out by an earlier step
       RELEASE_STORE_PASSWORD: ${{ secrets.RELEASE_STORE_PASSWORD }}
       RELEASE_KEY_ALIAS: ${{ secrets.RELEASE_KEY_ALIAS }}
       RELEASE_KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }}
   ```
   (`RELEASE_STORE_FILE` can't be a secret's raw bytes -- a typical setup
   base64-encodes the keystore into its own secret and adds a step that
   decodes it to a file path before the build, then points
   `RELEASE_STORE_FILE` at that path.)

   `.github/workflows/ci.yml` does **not** do any of this yet -- there is
   no real release keystore for this project yet to put behind a secret,
   so wiring up actual CI-side signing is left for whoever adds one, per
   the three steps above.

## GIF picker

The composer's GIF tab (`messaging/ui/GifPicker.kt`) calls GIPHY directly.
Falls back to GIPHY's own public "beta" key (rate-limited, shared across
every app that hasn't set its own) if none is configured. To use a real
key: add `GIPHY_API_KEY=<your key>` to `local.properties` (gitignored --
get a free key at https://developers.giphy.com/dashboard); read into
`BuildConfig.GIPHY_API_KEY` at build time, see `composeApp/build.gradle.kts`'s
`giphyApiKey` val.

A sent/received GIF autoplays for 3 loops in its bubble, then freezes on
the last frame; tapping it replays 3 more loops (`messaging/ui/GifBubbleImage`
in `ChatPane.kt`, backed by `messaging/ui/GifImageLoader.kt`'s Coil
ImageLoader -- real animated-GIF decoding on Android via `coil-gif`, static
first-frame only on iOS since that artifact publishes no iOS variant).

## Known remaining iOS/Android differences

Everything else in this README applies to both platforms. These are the
places they still genuinely differ:

- **Animated GIFs render as a static first frame on iOS.** `coil-gif`
  publishes no iOS artifact at the pinned Coil 3.0.0 (confirmed: the
  `coil-gif-iossimulatorarm64` coordinate 404s), and Coil's own Skia-based
  animated decoder for non-Android targets landed after that. Bumping Coil
  is not free here -- `coil-network-ktor3` pins an exact Ktor version and
  this catalog is deliberately aligned to it (see `libs.versions.toml`).
- **Crash reporting is Android-only.** Firebase Crashlytics rides
  `google-services.json`; `CrashReporter.ios.kt` prints instead of
  reporting.
- **`BackHandlerCompat` is a no-op on iOS.** It exists to intercept
  Android's system back button inside a single nav route. iOS has no system
  back button, and this Compose target does not own UIKit's edge-swipe
  gesture, so there is nothing to intercept -- in-app back affordances are
  the iOS path.
- **Compose UI tests do not run on Android's JVM unit-test task.**
  `runComposeUiTest` needs a real Android environment there (it is an
  instrumented test by nature) and is excluded in `build.gradle.kts`; the
  same tests do run on `iosSimulatorArm64Test`.

## Local chat cache

Chat history (including call-log bubbles, which are local-only and never
touch the server -- see below) is cached on-device via SQLDelight
(`data/local/ChatDatabase.sq` -- the `D2MDatabase` SQLDelight was already a
declared dependency for but not yet wired to anything). `MessagingRepository.start()`
hydrates instantly from this cache before the WebSocket even connects, and
every message mutation persists back to it, so history survives the process
being killed instead of living purely in an in-memory `StateFlow`. The
Archive Keypair cross-device restore (`messaging/crypto/archive/ArchiveManager.kt`)
now also passes the cache's own latest timestamp as an optional `since` param
to `GET /messages/history` (supported server-side in `messaging-framework`
as of the same change), so an already-set-up device's normal reconnect asks
for only what's new instead of re-fetching and re-decrypting this account's
entire message history every single time.

Call-log bubbles are architecturally local-only, matching d2m_web exactly:
call signaling rides the messaging channel as `ephemeral: true` and never
reaches the relay server's durable message log at all, so there's no
server-side copy for a cross-device restore to pull them from -- only this
device's own local cache remembers its own view of past calls.

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
