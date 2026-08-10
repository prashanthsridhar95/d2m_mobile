# iosApp

This folder has the 3 source files an iOS shell needs -- `iosApp/iOSApp.swift`,
`iosApp/ContentView.swift`, `iosApp/Info.plist` -- but deliberately **not** an
`.xcodeproj`/`.pbxproj`. That file format is auto-generated, extremely easy to
corrupt by hand-editing, and this sandbox has no Xcode to generate or verify
one against (see the root README's "What's been verified" section). Handing
you a hand-written `.pbxproj` would be worse than not having one.

## To get this running in Xcode

1. In Xcode: **File > New > Project > iOS > App**. Product name `iosApp`,
   interface **SwiftUI**, language **Swift**, uncheck tests/Core Data. Save
   it as this `iosApp/` folder (or a sibling folder and move the generated
   `.xcodeproj` in here).
2. Replace the generated `iosApp/iosAppApp.swift`, `ContentView.swift`, and
   `Info.plist` with the 3 files already in this folder.
3. Add a **Framework dependency** on the KMP shared module: Project settings
   > General > Frameworks, Libraries, and Embedded Content > `+` >
   `ComposeApp.framework` (the `iosX64`/`iosArm64`/`iosSimulatorArm64`
   framework declared in `composeApp/build.gradle.kts`). If Xcode can't find
   it directly, add a **Run Script build phase** that invokes
   `embedAndSignAppleFrameworkForXcode` (the standard KMP Gradle task) --
   this is the same wiring the Kotlin Multiplatform Xcode wizard generates
   automatically when you start a project via the wizard instead, which is
   the faster path if you have the KMP Xcode plugin installed.
4. Set the bundle identifier to `com.d2m.app` (matches `Info.plist` and the
   Android `applicationId` in `composeApp/build.gradle.kts`).
5. Build target: physical device or simulator running iOS 15+.

## Push notifications

`AppDelegate` in `iOSApp.swift` captures the APNs device token but only
prints it -- wiring it into `PushTokenRegistrar.registerCurrentToken("apns",
tokenHex)` needs a small `@objc` Kotlin bridge function exposed from
`MainViewController.kt`'s file (Koin's `GlobalContext.get()` can be called
from Swift once such a bridge exists, same pattern as
`PlatformPushInitializer.android.kt` uses internally). Left as the concrete
next step here since real push *send* is explicitly deferred backend-side
(see root README) -- there's nothing to receive yet either way.

## What's not verified

Nothing in this folder has been opened in Xcode or compiled -- this sandbox
is Linux-only with no Apple toolchain. Source is structured to match the
standard KMP+SwiftUI wiring pattern but review it in Xcode before relying on
it.
