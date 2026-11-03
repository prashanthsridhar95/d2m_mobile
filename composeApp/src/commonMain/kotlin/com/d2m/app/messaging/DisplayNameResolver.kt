package com.d2m.app.messaging

import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.SeriousModeRepository

/**
 * Resolves a messaging-framework username (raw, hyphen-stripped d2m id --
 * see Identity.kt's d2mIdToMessagingUsername) to a real display name, for
 * contexts that only have that raw string and need a person's actual name.
 * Built for D2MFirebaseMessagingService: an FCM push payload's `sender`
 * field is exactly this raw username (see ws.ts's triggerPush call sites),
 * and previously there was nothing to turn it into anything better than the
 * generic "New message"/"Incoming call" copy baked in server-side -- the
 * relay is E2E and never learns a display name itself (see
 * push_service.py's TITLES/BODIES).
 *
 * No single local, synchronous source covers both roles (see the two
 * branches below), so this is a suspend function even though the Parent
 * branch never actually suspends.
 *
 * Falls back to [fallback] (typically the raw username) if nothing
 * resolves it -- a first message from a brand-new contact, a network
 * failure on the Child branch's API call, or a role this app doesn't have
 * a contact source for yet.
 */
suspend fun resolveDisplayName(
    identityStore: IdentityStore,
    parentContactsStore: ParentContactsStore,
    seriousModeRepository: SeriousModeRepository,
    senderUsername: String,
    fallback: String = senderUsername,
): String {
    val identity = identityStore.identity.value
    val myId = identity.primaryId ?: identity.sponsorId ?: return fallback
    val myUsername = d2mIdToMessagingUsername(myId)

    // Parent flow: contacts are local-only, keyed directly by username --
    // see ParentContactsStore.kt's doc comment on why there's no backend
    // "threads" concept to ask instead.
    parentContactsStore.attach(myUsername)
    parentContactsStore.contacts.value[senderUsername]?.name?.let { return it }

    // Child flow: SeriousModeRepository.getThreads is the authoritative
    // backend match list, keyed by d2m id (not this messaging username) --
    // match by re-deriving each thread's messaging username the same way
    // this app derives its own (d2mIdToMessagingUsername), rather than
    // trying to reverse the hyphen-stripping (lossy in general, safe here
    // only because it's a fixed 8-4-4-4-12 UUID shape -- comparing forward
    // avoids relying on that assumption holding).
    if (identity.role == D2MRole.CHILD) {
        val threads = runCatching { seriousModeRepository.getThreads(myId) }
            .onFailure { e -> println("resolveDisplayName: getThreads failed: ${e.message ?: e::class.simpleName}") }
            .getOrNull()
            .orEmpty()
        threads.firstOrNull { d2mIdToMessagingUsername(it.otherParticipantId) == senderUsername }
            ?.otherParticipantName
            ?.let { return it }
    }

    return fallback
}
