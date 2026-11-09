package com.d2m.app.data

import com.d2m.app.data.model.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Same Json settings as ApiClient, fed payloads shaped exactly like app/routers/wedding.py returns. */
@OptIn(ExperimentalSerializationApi::class)
class WeddingModelsTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; namingStrategy = JsonNamingStrategy.SnakeCase }

    @Test fun decodesPlanWithNamesAndTasksWithCounts() {
        val out = json.decodeFromString<WeddingOut>(
            """{"plan":{"id":"p","union_id":"u","my_role":"groom","tradition":null,"names":{"bride":"Priya","groom":"Prakash"},
            "engagement_date":"2027-01-01","marriage_date":null,"honeymoon_date":null},
            "phases":[{"key":"planning","label":"Planning"}],
            "progress":{"total":1,"done":0,"my_open":1,"overdue":0,"waiting_on_others":0},
            "tasks":[{"id":"t","template_key":"planning-001","title":"Book venue","notes":null,"phase":"planning","scope":"couple",
            "participants":["bride","groom"],"external_party":null,"traditions":[],"anchor":"marriage","offset_days":-180,"due_date":null,
            "due_date_is_custom":false,"status":"todo","confirmed_by":[],"waiting_on":["bride","groom"],"i_confirmed":false,
            "i_am_participant":true,"overdue":false,"is_custom":false,"visibility":"all","created_by_role":null,"can_change_visibility":false,
            "can_delete":true,"completed_at":null,"comment_count":2,"attachment_count":1}]}""",
        )
        assertEquals("Priya", out.plan.names.bride)
        assertEquals(2, out.tasks[0].commentCount)
        assertEquals(-180, out.tasks[0].offsetDays)
        assertTrue(out.tasks[0].iAmParticipant)
    }

    @Test fun decodesActivityAndPanchangam() {
        val a = json.decodeFromString<WeddingActivityOut>(
            """{"legacy_note":"old","comments":[{"id":"c","author_role":"bride","body":"hi","created_at":"2026-11-09T10:00:00Z","mine":false}],
            "attachments":[{"id":"a","filename":"q.pdf","content_type":"application/pdf","size_bytes":12,"uploaded_by_role":"groom",
            "created_at":"2026-11-09T10:00:00Z","is_image":false,"mine":true}]}""",
        )
        assertEquals("old", a.legacyNote)
        assertEquals("bride", a.comments[0].authorRole)
        assertEquals(12, a.attachments[0].sizeBytes)
        val p = json.decodeFromString<WeddingPanchangamOut>(
            """{"available":true,"days":[{"date":"2027-01-01","tithi":"Amavasya","nakshatra":"Mula","is_amavasya":true,"is_purnima":false,
            "is_muhoortham":false,"chandrashtamam":["bride"]}]}""",
        )
        assertTrue(p.available && p.days[0].isAmavasya)
        assertEquals(listOf("bride"), p.days[0].chandrashtamam)
    }

    @Test fun requestBodiesUseSnakeCaseAndTouchOnlyWhatTheyShould() {
        assertEquals("""{"due_date":null}""", json.encodeToString(WeddingDuePatchIn(null)))
        assertEquals("""{"due_date":"2027-02-02"}""", json.encodeToString(WeddingDuePatchIn("2027-02-02")))
        assertEquals("""{"tradition":"tamil"}""", json.encodeToString(WeddingTraditionIn("tamil")))
        assertEquals("""{"body":"hello"}""", json.encodeToString(WeddingCommentIn("hello")))
        assertEquals(
            """{"engagement_date":"2027-01-01","marriage_date":null,"honeymoon_date":null}""",
            json.encodeToString(WeddingDatesIn("2027-01-01", null, null)),
        )
    }
}
