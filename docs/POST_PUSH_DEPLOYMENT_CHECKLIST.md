# Post-push mobile checklist

Use this after the current iOS/mobile changes are reviewed and pushed to
`d2m_mobile`'s `master` branch.

## Changes included

- Production iOS API URL points to `https://api.prashanthsridhar.com`.
- Audit requests use the stored WedLock bearer token.
- Messaging supports `thread.resync`.
- Thread responses include participant photos and media-permission state.
- Chat attachments enable media permission when the current participant is
  allowed to do so.
- Attachment failures display useful server errors.
- Login copy uses the Soul'd brand.
- CocoaPods/Xcode 27 compatibility updates are included.

## Update the development Mac

```bash
git checkout master
git pull --ff-only origin master

cd iosApp
pod install --no-repo-update
open iosApp.xcworkspace
```

Never open `iosApp.xcodeproj`; use `iosApp.xcworkspace`.

## Build and install on an iPhone

1. Connect and unlock the iPhone.
2. Select the Personal Team or paid Apple Developer Team in Xcode.
3. Select the connected iPhone as the run destination.
4. Build and run the `iosApp` scheme.

Command-line verification:

```bash
xcodebuild \
  -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -configuration Debug \
  -destination 'id=<IPHONE_UDID>' \
  -allowProvisioningUpdates \
  build
```

## Smoke test

- [ ] Login subtitle says `Log in with your Soul'd account.`
- [ ] Login and account resolution succeed.
- [ ] Dashboard and profile data load.
- [ ] Chat WebSocket receives `AuthOk`, `ThreadResync`, and `Pong`.
- [ ] Text messages send and receive.
- [ ] Attachment control explains when the bride must enable media.
- [ ] Authorized participant can enable media and select a photo.
- [ ] A photo under 30 MB uploads after the messaging server repair.
- [ ] Failed uploads show a specific reason instead of only `Attachment failed`.

## Known signing limitation

A free Personal Team cannot provide the APNs entitlement. Push notifications,
PushKit, and production incoming-call testing require a paid Apple Developer
Team and matching backend APNs credentials.
