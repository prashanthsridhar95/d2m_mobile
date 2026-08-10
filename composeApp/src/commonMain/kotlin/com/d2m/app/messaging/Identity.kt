package com.d2m.app.messaging

/**
 * Direct port of D2M_Messaging_Integration_Plan.md §3: a Primary's (or
 * Sponsor's) messaging username is their existing D2M id with hyphens
 * stripped -- no password, no separate login step, no vault. A bare UUID is
 * exactly 36 characters with 4 hyphens, so stripping them gives exactly 32
 * hex characters, matching messaging-framework's username validator
 * (`^[a-z0-9_]{2,32}$`) exactly. No collision risk (it's still the same
 * UUID, just reformatted), computable identically on every client.
 *
 * Deliberate trade-off carried over unchanged from the plan: each device is
 * its own Signal identity keyed by this derived username -- opening the app
 * on a second device generates a fresh identity under the same username,
 * and old-device-only chat history doesn't carry over (no multi-device
 * vault here, same as the plan's §3 "traded off, explicitly" section).
 */
fun d2mIdToMessagingUsername(id: String): String = id.replace("-", "").lowercase()
