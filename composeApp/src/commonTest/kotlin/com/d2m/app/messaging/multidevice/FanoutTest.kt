package com.d2m.app.messaging.multidevice

import com.d2m.app.messaging.DEFAULT_DEVICE
import com.d2m.app.messaging.DeviceCache
import com.d2m.app.messaging.FanoutTarget
import com.d2m.app.messaging.addPeerToPayload
import com.d2m.app.messaging.addrKey
import com.d2m.app.messaging.archiveTarget
import com.d2m.app.messaging.fanoutTargets
import com.d2m.app.messaging.peerFromPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Port of d2m_web/src/lib/messaging/fanout.test.mjs, plus the sync-copy payload helpers. */
class FanoutTest {
    private val me = "alice"
    private val peer = "bob"

    @Test
    fun addressesAreUserDotDeviceAndDeviceOneIsTheLegacyDefault() {
        assertEquals("bob.3", addrKey("bob", 3))
        assertEquals(1, DEFAULT_DEVICE)
    }

    @Test
    fun aMessageGoesToEveryPeerDeviceThenToOurOtherDevicesAsSyncCopies() {
        val t = fanoutTargets(me, 2, peer, peerDevices = listOf(3, 1), selfDevices = listOf(2, 1, 5))
        assertEquals(
            listOf(
                FanoutTarget("bob", 1, false),
                FanoutTarget("bob", 3, false),
                FanoutTarget("alice", 1, true),
                FanoutTarget("alice", 5, true),
            ),
            t,
        )
    }

    @Test
    fun theSendingDeviceNeverTargetsItself() {
        val t = fanoutTargets(me, 2, peer, peerDevices = listOf(1), selfDevices = listOf(2))
        assertEquals(listOf(FanoutTarget("bob", 1, false)), t)
    }

    @Test
    fun includeSelfFalseDropsSyncCopiesAndMessagingYourselfYieldsNoDuplicateTargets() {
        val noSync = fanoutTargets(me, 2, peer, listOf(1), listOf(1, 2, 3), includeSelf = false)
        assertEquals(listOf(false), noSync.map { it.self })
        val toSelf = fanoutTargets("alice", 1, "alice", listOf(1, 2), listOf(1, 2))
        assertEquals(listOf("alice.1", "alice.2"), toSelf.map { addrKey(it.user, it.device) })
    }

    @Test
    fun duplicateDeviceIdsCollapseAndAPeerWithNoDevicesYieldsNoPeerTargets() {
        assertEquals(1, fanoutTargets(me, 2, peer, listOf(4, 4, 4), emptyList()).size)
        assertEquals(0, fanoutTargets(me, 2, peer, emptyList(), listOf(1)).count { !it.self })
    }

    @Test
    fun theArchiveCopyRidesTheFirstPeerCopyNeverASyncCopy() {
        val targets = fanoutTargets(me, 2, peer, listOf(7, 4), listOf(1, 2))
        assertEquals(FanoutTarget("bob", 4, false), archiveTarget(targets))
        assertNull(archiveTarget(listOf(FanoutTarget("alice", 1, true))))
    }

    @Test
    fun deviceCacheExpiresInvalidatesAndFlagsAMessageFromAnUnlistedDevice() {
        var t = 1000L
        val c = DeviceCache(30_000) { t }
        assertNull(c.get("bob"))
        c.set("bob", listOf(3, 1))
        assertEquals(listOf(1, 3), c.get("bob"))
        assertEquals(false, c.noteSeen("bob", 1), "a known device does not invalidate")
        assertEquals(listOf(1, 3), c.get("bob"))
        assertEquals(true, c.noteSeen("bob", 9), "an unknown device means the list is stale")
        assertNull(c.get("bob"), "and the cached list is dropped so the next send refetches")
        c.set("bob", listOf(1))
        t += 30_001
        assertNull(c.get("bob"), "TTL")
        c.set("a", listOf(1)); c.set("b", listOf(1))
        c.invalidate("a")
        assertNull(c.get("a"))
        assertEquals(listOf(1), c.get("b"))
        c.invalidate()
        assertNull(c.get("b"))
        assertEquals(false, c.noteSeen("never-cached", 2), "nothing cached, nothing to invalidate")
    }

    @Test
    fun syncCopyPayloadCarriesThePeerAndStaysAValidChatPayload() {
        val original = """{"type":"text","text":"hi \"there\"","replyTo":{"id":"m1","from":"bob","preview":"yo"}}"""
        val withPeer = addPeerToPayload(original, "bob")
        assertEquals("bob", peerFromPayload(withPeer))
        assertNull(peerFromPayload(original), "a normal copy names no peer")
        assertNull(peerFromPayload("not json"))
        val decoded = com.d2m.app.messaging.protocol.messagingProtocolJson.decodeFromString(com.d2m.app.messaging.protocol.ChatPayload.serializer(), withPeer)
        assertEquals(com.d2m.app.messaging.protocol.ChatPayload.Text("hi \"there\"", com.d2m.app.messaging.protocol.ReplyContext("m1", "bob", "yo")), decoded)
    }
}
