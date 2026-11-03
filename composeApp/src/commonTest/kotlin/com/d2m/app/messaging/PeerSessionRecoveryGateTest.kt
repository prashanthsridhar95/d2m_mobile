package com.d2m.app.messaging

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeerSessionRecoveryGateTest {
    @Test
    fun coalescesRecoveryBurstsPerPeer() = runTest {
        var now = 1_000L
        val gate = PeerSessionRecoveryGate(cooldownMs = 10_000L, nowMs = { now })

        assertTrue(gate.tryStart("alice"))
        assertFalse(gate.tryStart("alice"))
        assertTrue(gate.tryStart("bob"))

        now += 10_000L
        assertTrue(gate.tryStart("alice"))
    }

    @Test
    fun successfulDecryptEndsCooldown() = runTest {
        val gate = PeerSessionRecoveryGate(cooldownMs = 10_000L, nowMs = { 1_000L })

        assertTrue(gate.tryStart("alice"))
        gate.markHealthy("alice")
        assertTrue(gate.tryStart("alice"))
    }
}