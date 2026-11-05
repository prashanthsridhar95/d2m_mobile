package com.d2m.app.messaging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

/**
 * Multi-device addressing and fan-out rules, kept free of crypto/network dependencies so they can be unit-tested.
 * Direct port of d2m_web/src/lib/messaging/fanout.ts (see messaging-framework/docs/MULTI_DEVICE.md).
 *
 * An account (username) has several devices, each with its own Signal identity, addressed "<username>.<deviceId>".
 * A message to a peer is encrypted once PER TARGET DEVICE: every device of the peer, plus every OTHER device of our own
 * account (a "sync" copy, so a conversation looks the same on all of our devices).
 */
const val DEFAULT_DEVICE = 1

/** libsignal-style address string for one device of an account. */
fun addrKey(user: String, device: Int): String = "$user.$device"

data class FanoutTarget(
    val user: String,
    val device: Int,
    /** A copy for one of OUR OWN other devices (sync), not for the peer. */
    val self: Boolean,
)

/** The devices a message must be encrypted for: peer devices first (ascending), then our own other devices (ascending). */
fun fanoutTargets(
    me: String,
    myDevice: Int,
    peer: String,
    peerDevices: Collection<Int>,
    selfDevices: Collection<Int>,
    includeSelf: Boolean = true,
): List<FanoutTarget> {
    val peers = peerDevices.toSet().sorted().map { FanoutTarget(peer, it, self = false) }
    if (!includeSelf || peer == me) return peers
    val own = selfDevices.toSet().sorted().filter { it != myDevice }.map { FanoutTarget(me, it, self = true) }
    return peers + own
}

/**
 * The single copy that carries the message's archive: the first (lowest-numbered) PEER device. History reads dedupe
 * fanned-out copies by id and prefer the archive-bearing one, so it must be a copy filed under the peer's conversation,
 * never a sync copy.
 */
fun archiveTarget(targets: List<FanoutTarget>): FanoutTarget? = targets.firstOrNull { !it.self }

/** Per-user device lists with a TTL; a message from an unlisted device means the list is stale. Safe to use from several coroutines. */
class DeviceCache(private val ttlMs: Long, private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }) {
    private class Entry(val at: Long, val devices: List<Int>)

    private val entries = MutableStateFlow<Map<String, Entry>>(emptyMap())

    fun get(user: String): List<Int>? {
        val e = entries.value[user] ?: return null
        return if (now() - e.at < ttlMs) e.devices else null
    }

    fun set(user: String, devices: Collection<Int>) {
        val entry = Entry(now(), devices.toSet().sorted())
        entries.update { it + (user to entry) }
    }

    /** Drops one user's cached list, or every list when [user] is null. */
    fun invalidate(user: String? = null) {
        if (user == null) entries.value = emptyMap() else entries.update { it - user }
    }

    /** Records that [device] was seen for [user]. True (and the cached list dropped) if it wasn't in the cached list. */
    fun noteSeen(user: String, device: Int): Boolean {
        val e = entries.value[user] ?: return false
        if (device in e.devices) return false
        entries.update { it - user }
        return true
    }
}

/**
 * A sync copy (a message we sent from another of our devices) is addressed to our own account, so the receiver cannot tell
 * which conversation it belongs to from the envelope. The sender therefore adds the conversation's `peer` to the encrypted
 * payload (the ChatPayload decoder ignores unknown keys, so older clients just see the payload).
 */
fun addPeerToPayload(payloadJson: String, peer: String): String {
    val obj = Json.parseToJsonElement(payloadJson).jsonObject
    return JsonObject(obj + ("peer" to JsonPrimitive(peer))).toString()
}

/** The conversation a decrypted sync copy belongs to (null if the payload doesn't say). */
fun peerFromPayload(plaintext: String): String? =
    runCatching { Json.parseToJsonElement(plaintext).jsonObject["peer"]?.jsonPrimitive?.contentOrNull }.getOrNull()?.takeIf { it.isNotEmpty() }
