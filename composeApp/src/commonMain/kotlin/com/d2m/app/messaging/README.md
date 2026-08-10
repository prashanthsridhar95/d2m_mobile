# Messaging (Phase 6) -- status

What's real in this build:
- `Identity.kt` -- username derivation (hyphen-stripped D2M id), matching D2M_Messaging_Integration_Plan.md §3 exactly.
- `protocol/Types.kt` -- wire envelope + WS event types, ported 1:1 from `messaging-framework/packages/protocol/src/index.ts`.
- `transport/MessagingWsClient.kt` -- Ktor WebSocket client with the same reconnect/heartbeat behavior as `wsClient.ts` (15s ping, 1500ms reconnect delay, no-stacked-sockets guard).
- `MessagingRepository.kt` -- per-peer message/typing/presence state as Flows, wired to a real WS connection against a real messaging-framework server.
- `ui/ChatPane.kt` -- a real chat UI, wired into MatchesScreen.

What's NOT real yet, on purpose:
- **Encryption.** `crypto/CryptoProvider.kt`'s default binding is `StubUnencryptedCryptoProvider` -- base64, not Signal Protocol. See that file's doc comment for why this wasn't hand-rolled under time pressure, and the three real implementation paths considered. `ChatPane` surfaces this to the user as a visible dev-build warning rather than hiding it.
- **Media & voice notes** (integration plan's Phase 2) -- `ChatPayload` only has text/edit/delete/reaction variants; media isn't wired.
- **Calls** (integration plan's Phase 3) -- no `CallManager` port, no WebRTC. `CallSignal`'s wire shape isn't even ported yet, since nothing consumes it.
- **App-lifecycle-aware reconnect** -- the web client's `document.visibilitychange`/`online`/`focus` "wake up and reconnect instantly" listeners have no port here; the plain retry loop still recovers within ~1.5s on its own, but a proper Android/iOS lifecycle hook would be faster and is a natural platform-actual follow-up.

Before this carries real user conversations: implement a real `CryptoProvider`, connect this build to a real `messaging-framework` server instance and confirm the wire protocol round-trips against the actual TypeScript server (not just against itself), and get a second set of eyes on the crypto specifically.
