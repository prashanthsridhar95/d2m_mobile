# Messaging + Calling -- status

## Text messaging: full feature parity with d2m_web's useMessaging.js

- `Identity.kt` -- username derivation (hyphen-stripped D2M id).
- `protocol/Types.kt` -- full wire protocol, ported 1:1 from `messaging-framework/packages/protocol/src/index.ts`: message envelope, WS events, `ChatPayload` (text/media/edit/delete/reaction/call), `CallSignal` and all its nested types.
- `transport/MessagingWsClient.kt` -- Ktor WebSocket client, same reconnect/heartbeat behavior as `wsClient.ts`.
- `MessagingRepository.kt` -- send/receive text + media, edit, delete-for-everyone, delete-for-me, reactions, typing (with auto-clear timeout), presence subscribe, delivered/read receipts, unread-by-peer counters, active-peer tracking, best-effort resend on `session.reset`.
- `ui/ChatPane.kt` -- full chat UI: reply, edit, delete, reactions (long-press), received media rendering, typing indicator, per-message status ticks, audio/video call buttons.

Deliberately not ported: the Archive Keypair cross-device history-recovery system (only makes sense on top of a real Signal Protocol identity, not this build's stub crypto), and a real Double Ratchet re-handshake on `session.reset` (nothing to actually resync yet -- see below).

**No attach-media UI wired on either platform.** `MessagingRepository.sendMedia()` and the full `ChatPayload.Media`/`MediaMeta` wire format work end-to-end (received media renders fine in `ChatPane`), but there's no "pick a photo" button yet -- adding one needs a small platform image-picker seam (`ActivityResultContracts.PickVisualMedia` on Android, `PHPickerViewController` on iOS) that wasn't built in this pass to keep scope on what was actually asked for (text + calls). Straightforward follow-up.

## Calling: real, working audio/video calls on Android; not yet on iOS

- `call/CallManager.kt` -- direct port of `d2m_web/src/lib/messaging/callManager.ts`'s state machine: invite/ringing/accept/decline, perfect-negotiation renegotiation (mic/camera toggle, upgrade audio->video, front/back camera flip), 30s ring timeout, ICE restart on failure, call-log messages on teardown ("Missed call", "Video call · 3:12", ...). Not ported: screen sharing (no mobile equivalent of `getDisplayMedia` without a whole separate feature per platform) and device-picker menus (mobile's camera flip covers the mobile-relevant case).
- `call/WebRtcEngine.kt` -- the native-WebRTC-object-model interface CallManager drives instead of the browser DOM API the original was written against.
- `call/WebRtcEngine.android.kt` -- **real implementation** using `io.getstream:stream-webrtc-android` (Maven Central republish of Google's official prebuilt WebRTC AAR, same `org.webrtc.*` API). PeerConnection, camera capture, audio, ICE, SDP negotiation all wired for real.
- `call/WebRtcEngine.ios.kt` -- **not implemented.** Every method throws a clear error. Needs a real Xcode project (`iosApp/` doesn't have one yet -- see `iosApp/README.md`) with CocoaPods integrated, then `pod("GoogleWebRTC")` + porting the Android file's method bodies to the equivalent `RTCXxx` Kotlin bindings Kotlin/Native's CocoaPods plugin auto-generates. `CallManager.kt` and the rest of `messaging/` need zero changes when that happens.
- `call/ui/` -- `CallLayer.kt` (global dispatcher, mounted once in `App.kt` so a call survives navigation), `CallOverlay.kt` (in-call screen: mute/camera/flip/hangup, local PIP + remote video), incoming-call full-screen toast, minimized call bar, runtime mic/camera permission request.
- TURN/STUN credentials fetched from the messaging server's `/turn-credentials` endpoint (`MessagingRepository.fetchIceServers()`), with a public-STUN fallback on failure -- same behavior as `callManager.ts`.

**Known limitations, all deliberate scope calls, not oversights:**
- Calling only works while the app is foregrounded or backgrounded-but-still-process-alive (the WS connection has to be open) -- there's no CallKit/PushKit VoIP-push wake-the-app-from-killed integration. That's a legitimately separate, large iOS-specific project (and the backend doesn't send any real push yet regardless -- see the root README).
- No screen sharing.
- The `PeerConnectionObserver` threading contract (native SDK callbacks must be hopped onto `CallManager`'s own dispatcher) is documented and followed by the Android actual, but `CallManager` itself has no internal locking -- acceptable for this pass, a real concurrency audit is a reasonable follow-up before this carries production call volume.

## Encryption: still the explicitly non-production stub

`crypto/CryptoProvider.kt`'s default binding is `StubUnencryptedCryptoProvider` -- base64, not Signal Protocol. Unchanged by this pass; see that file's doc comment for why and what the real implementation paths are. `ChatPane` surfaces this to the user as a visible dev-build warning. Call *signaling* (SDP/ICE) rides through this same non-production channel; call *media* itself (the actual audio/video RTP streams) is still real DTLS-SRTP end-to-end encrypted P2P by WebRTC itself, independent of the Signal layer -- that part was never affected by the crypto stub.

Before this carries real user conversations or calls: implement a real `CryptoProvider`, and get a second set of eyes on it specifically.
