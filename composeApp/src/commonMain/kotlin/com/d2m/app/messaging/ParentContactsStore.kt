package com.d2m.app.messaging

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// See ApiCache.kt's import comment -- deprecated kotlinx.datetime.Clock
// typealias, actually resolves to kotlin.time.Clock in the 0.7.1 that this
// project really compiles against.
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Locally-persisted "who has this Sponsor talked to, and what's their
 * name/kind" registry -- direct port of d2m_web's lib/messaging/contacts.js
 * (see that file's own extensive doc comment for the full history, including
 * the v1->v2 key bump after a real "wrong name shown" bug).
 *
 * Needed because, unlike the child flow (whose match list comes from an
 * authoritative backend `GET /primaries/{id}/threads`), there is no
 * Sponsor-to-Sponsor or Sponsor-to-Child "threads" concept anywhere in
 * d2m_core_engine -- confirmed directly against the backend, see
 * ParentMessagesScreen.kt's own doc comment. A parent's contact list only
 * ever comes from THIS APP explicitly resolving "who is this person" at the
 * moment a conversation is started (ProfileDetailScreen.kt's "Message their
 * parent" button), so it has to be remembered locally or a cold start would
 * forget every parent contact's display name and kind.
 *
 * `kind` is "parent" | "child" -- which relationship this contact is (see
 * contactQualifier below for how it drives the header/thread-list label).
 * Only "parent" is actually produced by this app today (Parent-to-Child
 * messaging needs the consent-unlock gate, which isn't built on mobile yet
 * -- see ParentMessagesScreen.kt) but the shape stays generic so wiring that
 * up later doesn't need a data-model change.
 *
 * `name` is ALWAYS this contact's own real name (the Sponsor's own name --
 * GET /sponsors/{id} -- never their child's), matching web's own fix for
 * "message notification is received in the name of the child instead of the
 * parent's name" (reported directly against the web app; ported here from
 * the start rather than repeating that bug).
 *
 * Key namespaced by (this account's own) username, same reasoning as
 * ArchiveKeyStore.kt -- a device can hold contacts for whichever Sponsor
 * account is currently signed in, without one account's contacts leaking
 * into another's list after a re-login.
 */
@Serializable
data class ParentContact(
    val d2mId: String,
    val name: String,
    val kind: String, // "parent" | "child"
    val childId: String? = null,
    val childName: String? = null,
    /** Set at registerContact time -- a coarse recency signal for sorting the thread list (see ParentMessagesScreen.kt) alongside MessagingRepository.lastMessageAt, since there's no backend thread list to sort by last-activity here. */
    val updatedAt: Long = 0L,
)

class ParentContactsStore(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true }
    private fun key(username: String) = "d2m_parent_contacts_v1:$username"

    private var attachedUsername: String? = null
    private val _contacts = MutableStateFlow<Map<String, ParentContact>>(emptyMap())
    /** peerUsername -> ParentContact. */
    val contacts: StateFlow<Map<String, ParentContact>> = _contacts.asStateFlow()

    /** Loads this account's persisted contacts into [contacts]. Idempotent -- safe to call on every screen mount. */
    fun attach(username: String) {
        if (attachedUsername == username) return
        attachedUsername = username
        val raw = settings.getStringOrNull(key(username))
        _contacts.value = raw
            ?.let { runCatching { json.decodeFromString<Map<String, ParentContact>>(it) }.getOrNull() }
            ?: emptyMap()
    }

    /** Called from ProfileDetailScreen.kt's "Message their parent" handler once the Sponsor's id/name are resolved. */
    fun registerContact(
        myUsername: String,
        peerUsername: String,
        peerD2mId: String,
        name: String,
        kind: String,
        childId: String? = null,
        childName: String? = null,
    ) {
        attach(myUsername)
        val updated = _contacts.value + (peerUsername to ParentContact(peerD2mId, name, kind, childId, childName, Clock.System.now().toEpochMilliseconds()))
        _contacts.value = updated
        settings.putString(key(myUsername), json.encodeToString(updated))
    }
}

/**
 * Direct port of contacts.js's contactQualifier -- the qualifying half only
 * ("Deepak's parent", or bare "parent" before childName has resolved), no
 * surrounding punctuation, so callers can render it as its own line (see
 * ParentMessagesScreen.kt) rather than fusing it into the name string.
 * Returns null for kind "child" (their own name already says everything) or
 * an unrecognized kind.
 */
fun contactQualifier(kind: String?, childName: String?): String? {
    if (kind != "parent") return null
    return if (!childName.isNullOrBlank()) "$childName's parent" else "parent"
}
