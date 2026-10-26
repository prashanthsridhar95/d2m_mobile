package com.d2m.app.ui.screens.shared

import com.d2m.app.data.model.NotificationOut
import com.d2m.app.data.session.D2MRole
import com.d2m.app.ui.navigation.Routes
import com.d2m.app.ui.strings.NotificationsStringsEn
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationsLogicTest {
    private val s = NotificationsStringsEn
    private fun n(type: String, vararg kv: Pair<String, String>) = NotificationOut(
        notificationId = "1", type = type, createdAt = "2026-01-01T00:00:00Z",
        payload = buildJsonObject { kv.forEach { (k, v) -> put(k, v) } },
    )

    @Test fun titlesMatchWeb() {
        assertEquals("Serious Mode ended", notificationTitle("serious_mode_revoked", s))
        assertEquals("Shortlist update", notificationTitle("shortlisted_profile_updated", s))
        // unknown types read as plain text, never the raw key or payload
        assertEquals("Calls unlocked", notificationTitle("calls_unlocked", s))
        assertEquals("Ashwin", notificationBody(n("calls_unlocked", "counterpart_name" to "Ashwin", "thread_id" to "t"), s))
        assertEquals("", notificationBody(n("calls_unlocked", "thread_id" to "t"), s))
    }

    @Test fun bodiesTruncateIdsLikeWeb() {
        assertEquals("Candidate 12345678…", notificationBody(n("mutual_match", "candidate_id" to "1234567890"), s))
        assertEquals("From abcdefgh… -- see Requests", notificationBody(n("request_received", "sender_id" to "abcdefghij"), s))
        assertEquals("Ravi opened one of your profile share links.", notificationBody(n("share_link_viewed", "viewer_name" to "Ravi"), s))
        assertEquals(s.previewViewerAnonymous, notificationBody(n("share_link_viewed"), s))
        assertEquals("", notificationBody(n("consent_request"), s))
    }

    @Test fun ctaRoleGating() {
        assertEquals(Routes.MATCHES, notificationCta(n("serious_mode_request", "thread_id" to "t"), D2MRole.CHILD)?.route)
        assertNull(notificationCta(n("serious_mode_request"), D2MRole.PARENT))
        assertEquals(Routes.PARENT_HOME, notificationCta(n("consent_granted"), D2MRole.PARENT)?.route)
        assertEquals("v1", notificationCta(n("share_link_viewed", "viewer_primary_id" to "v1"), D2MRole.CHILD)?.profileId)
        assertEquals(Routes.SHARE_LINKS, notificationCta(n("share_link_viewed", "viewer_primary_id" to "v1"), D2MRole.PARENT)?.route)
        assertEquals("c1", notificationCta(n("shortlisted_profile_updated", "candidate_id" to "c1"), D2MRole.CHILD)?.profileId)
        assertNull(notificationCta(n("mutual_sponsor_interest"), D2MRole.PARENT))
    }

    @Test fun relativeTimes() {
        assertEquals("Just now", relativeTime(10_000, "9:00 AM", s))
        assertEquals("5m ago", relativeTime(5 * 60_000L, "9:00 AM", s))
        assertEquals("3h ago", relativeTime(3 * 3_600_000L, "9:00 AM", s))
        assertEquals("9:00 AM", relativeTime(30 * 3_600_000L, "9:00 AM", s))
    }
}
