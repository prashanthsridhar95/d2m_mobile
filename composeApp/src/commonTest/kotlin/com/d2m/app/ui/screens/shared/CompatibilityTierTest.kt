package com.d2m.app.ui.screens.shared

import com.d2m.app.ui.components.compatibilityTier
import com.d2m.app.ui.components.overallRating

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompatibilityTierTest {
    @Test
    fun tiersFollowThePercentageCutoffs() {
        assertEquals("excellent", compatibilityTier(100.0).key)
        assertEquals("excellent", compatibilityTier(90.0).key)
        assertEquals("very_good", compatibilityTier(89.9).key)
        assertEquals("very_good", compatibilityTier(75.0).key)
        assertEquals("good", compatibilityTier(74.9).key)
        assertEquals("good", compatibilityTier(60.0).key)
        assertEquals("moderate", compatibilityTier(59.9).key)
        assertEquals("moderate", compatibilityTier(40.0).key)
        assertEquals("unexpected", compatibilityTier(39.9).key)
        assertEquals("unexpected", compatibilityTier(0.0).key)
        assertEquals("unexpected", compatibilityTier(-5.0).key)
    }

    @Test
    fun eachTierMapsOntoAnExistingLevelColour() {
        assertEquals("Exceptional", compatibilityTier(95.0).level)
        assertEquals("High", compatibilityTier(80.0).level)
        assertEquals("High", compatibilityTier(65.0).level)
        assertEquals("Medium", compatibilityTier(50.0).level)
        assertEquals("Low", compatibilityTier(10.0).level)
    }

    @Test
    fun tiersAreAtLeastAsCoarseAsTheOverallRatingColours() {
        // The tier colour must never contradict the level pill used elsewhere for the same score.
        for (score in listOf(0.0, 39.9, 40.0, 59.9, 60.0, 79.9, 80.0, 89.9, 90.0, 100.0)) {
            val tier = compatibilityTier(score).level
            val pill = overallRating(score)
            val order = listOf("Low", "Medium", "High", "Exceptional")
            // High/Exceptional may differ by at most one step (tiers split 60-89 into good/very good).
            assertTrue(kotlin.math.abs(order.indexOf(tier) - order.indexOf(pill)) <= 1, "score $score: tier $tier vs pill $pill")
        }
    }
}
