# iosApp

The iOS app. Unlike when this folder was first written, it now contains a
real, generated `iosApp.xcodeproj` and a resolved `iosApp.xcworkspace` -- the
app builds, installs and runs on a simulator, and also builds for a physical
device.

**`iosApp.xcodeproj` is GENERATED. Do not hand-edit it, and do not hand-edit
`iosApp/Info.plist` or either `.entitlements` file either** -- all three are
regenerated from `project.yml` by XcodeGen, which overwrites whatever is on
disk every time it runs. Put changes in `project.yml`.

That is not a stylistic preference. Editing `Info.plist` directly appears to
work until the next `xcodegen generate` silently reverts it, and that is
exactly how this project previously lost its `UILaunchScreen` key along with
every camera/mic/photo usage string, the `d2m://` URL scheme, and both
entitlements. An app with no `UILaunchScreen` is run by iOS in legacy
compatibility mode -- a scaled-down, letterboxed window rather than the real
screen bounds -- which showed up as "Compose only fills ~60% of the screen"
and was misdiagnosed for a while as a Compose safe-area bug (see
`ContentView.swift`'s comment about that dead end). The missing microphone
usage string was the more dangerous half: iOS terminates the process outright
the first time it opens the mic without one, so every call would have
hard-crashed.

## Regenerating the project

From this directory, after any change to `project.yml`:

```
xcodegen generate && pod install
```

Then open `iosApp.xcworkspace` (never `iosApp.xcodeproj` -- CocoaPods).

## Feature parity with Android

As of this pass, iOS has real (not stubbed) implementations of everything
Android has: text messaging, rich message notifications (real sender name +
preview + Reply + Mark as read), real audio/video calling (WebRTC), and a
real ringing incoming-call experience with Answer/Decline -- via PushKit +
CallKit, which is Apple's mandatory mechanism for this, not a copy of
Android's NotificationCompat.CallStyle approach. See each file's own doc
comment for the design; the short version:

| Android piece | iOS equivalent |
|---|---|
| FCM push (`D2MFirebaseMessagingService.kt`) | APNs alert push (`push_service.py`'s `send_apns_alert`) + `NotificationServiceExtension/NotificationService.swift` |
| FCM push for calls | APNs VoIP push (`send_apns_voip`) delivered via `PKPushRegistry`, handled in `IosCallKitBridge.kt` |
| `NotificationCompat.CallStyle` ringing UI | `CXProvider` (CallKit) -- the OS renders the actual ringing screen, not app code |
| `CallActionReceiver`/`MessageReplyReceiver`/`MarkReadReceiver.kt` | `IosCallKitBridge.kt`'s `CXProviderDelegate` (Answer/Decline) + `iOSApp.swift`'s `UNUserNotificationCenterDelegate` (Reply/Mark as read) |
| `stream-webrtc-android` (`WebRtcEngine.android.kt`) | `GoogleWebRTC` CocoaPod (`WebRtcEngine.ios.kt`) |
| Shared decrypt/accept/reply logic | `IosPushBridge.kt` (commonMain) -- one place both the main app and the extension call into |

## Building and running

The project and workspace are committed, so there is no manual Xcode setup
to do any more. From this directory:

```
pod install
```

then open `iosApp.xcworkspace` and run, or from the command line:

```
xcodebuild -workspace iosApp.xcworkspace -scheme iosApp -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
```

`pod install` pulls in both `ComposeApp` (this repo's own KMP module,
auto-podspec'd via `composeApp/build.gradle.kts`'s `cocoapods {}` block) and
`WebRTC-lib` (declared as `ComposeApp`'s own pod dependency).

Everything that used to require manual Signing & Capabilities clicking --
App Groups (`group.com.d2m.app`, which must match `D2M_APP_GROUP_ID` in
`SqlDriverFactory.ios.kt`/`Settings.ios.kt`), Push Notifications
(`aps-environment`), Background Modes (voip/audio/remote-notification), and
the Notification Service Extension target -- is declared in `project.yml`
and generated from it. You still need to select a development Team in Xcode
for a device build.

**Simulator vs device:** PushKit VoIP and CallKit do not work in the iOS
Simulator at all, so calling and the ringing-notification flow can only be
exercised on a real device (iOS 15+) -- same as this project's Android
calling work has always required a real phone. Everything else, including
messaging and its encryption, runs fine in the Simulator.

## Push notifications -- backend setup

Real APNs sends need an Apple Developer Program membership (paid) and a
Push Notifications auth key:

1. Apple Developer portal > Certificates, IDs & Profiles > Keys > `+` >
   check "Apple Push Notifications service (APNs)" > Continue > download
   the `.p8` **once** (Apple never lets you re-download it -- store it
   somewhere safe immediately).
2. Set `D2M_APNS_AUTH_KEY_PATH`, `D2M_APNS_KEY_ID`, `D2M_APNS_TEAM_ID`,
   `D2M_APNS_BUNDLE_ID`, `D2M_APNS_ENVIRONMENT` in `d2m_core_engine`'s `.env`
   -- see that repo's `.env.example` for exactly what each value is and
   where it comes from. `D2M_APNS_ENVIRONMENT=development` matches a
   Debug/Xcode-installed build (what step 7 above produces); switch both
   this and the app's own `aps-environment` entitlement to `production`
   together for a TestFlight/App Store build -- a mismatch between the two
   is exactly what APNs' `BadDeviceToken` error means.
3. Nothing else is iOS-specific on the backend -- `push_service.py`'s
   `send_apns_alert`/`send_apns_voip` and `notification_service.py`'s
   per-platform routing are already real, not stubs (see those files'
   own doc comments), the same way FCM already was for Android.

Without step 1/2 configured, `apns_configured()` stays false and every push
attempt to an iOS device records `"stubbed"` -- registering a token and
opening the app still works, but nothing actually arrives on the device.

## Audio/video calling

Real (`WebRtcEngine.ios.kt`, via the `GoogleWebRTC` pod) once the CocoaPods
step above is done. `WebRtcEngine.ios.kt`'s own doc comment flags the two
spots most likely to need a small fix in Xcode (bitrate parameter API shape,
camera capture start signature) -- this is a close method-for-method port of
`WebRtcEngine.android.kt`, not a redesign, so a mismatch here should be a
quick fix, not a design problem.

CallKit owns the call's audio session once a call is answered/started (see
`WebRtcAudioSessionBridge.kt`) -- this is required, not optional, for
CallKit + WebRTC to coexist without fighting over the microphone/speaker.

## What's verified, and what isn't

**Verified:** the whole module compiles for both `iosSimulatorArm64` and
`iosArm64`; the app builds, installs, launches and renders correctly on an
iPhone simulator; the app also builds for a generic iOS device; and the
shared test suite passes on `iosSimulatorArm64Test`, which covers the real
Signal Protocol crypto, the Archive Keypair crypto, and the CommonCrypto
bindings behind both (RFC 7748, FIPS 180-4, NIST GCM and PBKDF2 vectors --
see the root README's "Security" section).

**Not verified, and needs a real device:** `IosCallKitBridge.kt` remains the
riskiest file here for the reason its own doc comment gives (PushKit/CallKit
Objective-C delegate-to-Kotlin/Native signature mapping), with
`WebRtcEngine.ios.kt` second. Both now compile and link against the real
SDKs, which rules out a whole class of problem, but neither has been run
against live signalling. Real APNs delivery likewise needs the backend
credentials below.
