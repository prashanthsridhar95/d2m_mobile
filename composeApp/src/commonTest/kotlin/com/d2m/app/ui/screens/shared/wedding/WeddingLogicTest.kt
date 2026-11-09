package com.d2m.app.ui.screens.shared.wedding

import com.d2m.app.data.model.WeddingNamesOut
import com.d2m.app.data.model.WeddingPhaseOut
import com.d2m.app.data.model.WeddingPlanOut
import com.d2m.app.data.model.WeddingTaskOut
import com.d2m.app.ui.components.FacetMode
import com.d2m.app.ui.components.FacetState
import com.d2m.app.ui.strings.WeddingStringsEn
import com.d2m.app.ui.strings.WeddingStringsHi
import com.d2m.app.ui.strings.WeddingStringsTa
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeddingLogicTest {
    private val today = LocalDate(2026, 11, 9)
    private val en = WeddingStringsEn

    private fun plan(myRole: String = "groom", bride: String? = "Priya", groom: String? = "Prakash") = WeddingPlanOut(
        id = "p", unionId = "u", myRole = myRole, names = WeddingNamesOut(bride, groom),
    )

    private fun task(
        id: String, participants: List<String> = listOf("groom"), due: String? = null, status: String = "todo",
        phase: String = "planning", scope: String = "individual", overdue: Boolean = false, mine: Boolean = true,
    ) = WeddingTaskOut(
        id = id, title = "Task $id", phase = phase, scope = scope, participants = participants, dueDate = due,
        status = status, overdue = overdue, iAmParticipant = mine,
    )

    @Test fun labelsUseNamesAndMe() {
        val label = labeler(plan(myRole = "groom"), en)
        assertEquals("Me", label("groom"))
        assertEquals("Priya", label("bride"))
        assertEquals("Priya's parents", label("bride_parents"))
        assertEquals("Prakash's parents", label("groom_parents"))
        // a parent viewing: their own role is "Me", their child is named
        val parent = labeler(plan(myRole = "groom_parents"), en)
        assertEquals("Me", parent("groom_parents"))
        assertEquals("Prakash", parent("groom"))
    }

    @Test fun labelsFallBackWhenNamesAreMissing() {
        val label = labeler(plan(myRole = "groom", bride = null, groom = null), en)
        assertEquals("Bride", label("bride"))
        assertEquals("Bride's parents", label("bride_parents"))
    }

    @Test fun bucketsFollowTheDueDate() {
        assertEquals("overdue", bucketOf(task("a", due = "2026-11-01"), today))
        assertEquals("week", bucketOf(task("b", due = "2026-11-16"), today))
        assertEquals("month", bucketOf(task("c", due = "2026-12-09"), today))
        assertEquals("later", bucketOf(task("d", due = "2027-03-01"), today))
        assertEquals("nodate", bucketOf(task("e"), today))
        assertEquals("done", bucketOf(task("f", due = "2026-11-01", status = "done"), today))
    }

    @Test fun relativeDueText() {
        assertEquals("Today", relDue(task("a", due = "2026-11-09"), today, en))
        assertEquals("Tomorrow", relDue(task("b", due = "2026-11-10"), today, en))
        assertEquals("3d overdue", relDue(task("c", due = "2026-11-06"), today, en))
        assertEquals("In 5 days", relDue(task("d", due = "2026-11-14"), today, en))
        assertEquals("25 Dec", relDue(task("e", due = "2026-12-25"), today, en))
    }

    @Test fun groupingByPhaseOwnerAndDue() {
        val tasks = listOf(
            task("1", phase = "planning", due = "2026-11-20"),
            task("2", phase = "foundations", participants = listOf("bride")),
            task("3", phase = "planning", participants = listOf("bride", "groom"), scope = "couple"),
        )
        val phases = listOf(WeddingPhaseOut("foundations", "Foundations"), WeddingPhaseOut("planning", "Planning"), WeddingPhaseOut("empty", "Empty"))
        val label = labeler(plan(), en)
        assertEquals(listOf("foundations", "planning"), buildGroups(tasks, phases, "phase", label, today, en).map { it.key })
        val owners = buildGroups(tasks, phases, "owner", label, today, en)
        assertEquals(listOf("bride", "groom", "shared"), owners.map { it.key })
        assertEquals("Me", owners.first { it.key == "groom" }.label)
        assertEquals(listOf("month", "nodate"), buildGroups(tasks, phases, "due", label, today, en).map { it.key })
    }

    @Test fun filtersAndFacetedCounts() {
        val tasks = listOf(
            task("1", participants = listOf("groom")),
            task("2", participants = listOf("bride"), mine = false),
            task("3", participants = listOf("bride", "groom"), scope = "couple", status = "done"),
        )
        val include = Filters(owners = FacetState(FacetMode.INCLUDE, setOf("bride")))
        assertEquals(listOf("2", "3"), tasks.filter { passes(it, include, today) }.map { it.id })
        val exclude = Filters(owners = FacetState(FacetMode.EXCLUDE, setOf("bride")))
        assertEquals(listOf("1"), tasks.filter { passes(it, exclude, today) }.map { it.id })
        val mine = Filters(quick = setOf("mine"))
        assertEquals(listOf("1"), tasks.filter { passes(it, mine, today) }.map { it.id })
        // skipping a facet leaves it out of the predicate (what the counts rely on)
        assertEquals(3, tasks.count { passes(it, include, today, skip = "owners") })
        assertTrue(Filters().activeCount == 0 && include.activeCount == 1 && Filters(q = "x").activeCount == 1)
    }

    @Test fun everyLanguageHasEveryCalendarListAndTradition() {
        listOf(WeddingStringsEn, WeddingStringsHi, WeddingStringsTa).forEach { x ->
            assertEquals(12, x.monthsShort.size); assertEquals(12, x.monthsLong.size)
            assertEquals(7, x.weekdaysShort.size); assertEquals(7, x.weekdaysLong.size)
            assertEquals(TRADITION_KEYS.toSet(), x.traditionNames.keys)
            BUCKET_KEYS.forEach { k -> assertTrue(x.bucket(k).isNotBlank()) }
            SCOPE_KEYS.forEach { k -> assertTrue(x.scope(k).isNotBlank()) }
        }
    }

    @Test fun localizedLabelsAndDates() {
        val hi = labeler(plan(myRole = "groom"), WeddingStringsHi)
        assertEquals("मैं", hi("groom"))
        assertEquals("Priya के माता-पिता", hi("bride_parents"))
        assertEquals("9 नव॰ 2026", today.medium(WeddingStringsHi))
        assertEquals("இன்று", relDue(task("x", due = "2026-11-09"), today, WeddingStringsTa))
    }

    @Test fun mentionsRoundTripBetweenDraftStoredAndDisplay() {
        val label = labeler(plan(myRole = "groom"), en)
        // typing: suggestions come from everyone except me, matched loosely
        assertEquals("", mentionQueryOf("hello @"))
        assertEquals("pri", mentionQueryOf("hello @pri"))
        assertEquals(null, mentionQueryOf("mail me at a@b.com now"))
        assertEquals(listOf("bride", "bride_parents", "groom_parents"), mentionOptions("", ROLES, label, "Me"))
        assertEquals(listOf("bride", "bride_parents"), mentionOptions("priya", ROLES, label, "Me"))
        assertEquals(listOf("bride_parents"), mentionOptions("priya's p", ROLES, label, "Me"))
        // sending: longest label first so "@Priya" can't eat "@Priya's parents"
        val stored = toStoredMentions("@Priya's parents please check, cc @Priya", listOf("bride" to "Priya", "bride_parents" to "Priya's parents"))
        assertEquals("@{bride_parents} please check, cc @{bride}", stored)
        // reading: worded for the viewer, with the ranges to highlight
        val (shown, spans) = displayMentions(stored, label)
        assertEquals("@Priya's parents please check, cc @Priya", shown)
        assertEquals(listOf(0..15, 34..39), spans.map { it })
        val (asBride, _) = displayMentions("hi @{groom}", labeler(plan(myRole = "groom"), en))
        assertEquals("hi @Me", asBride)
    }

    @Test fun laneAddsLandInTheirLane() {
        val lane = { k: String -> TaskGroup(k, k, null, emptyList()) }
        assertEquals(NewPreset(phase = "foundations"), lanePreset(lane("foundations"), "phase", today))
        assertEquals(NewPreset(participants = listOf("bride")), lanePreset(lane("bride"), "owner", today))
        assertEquals(NewPreset(participants = listOf("bride", "groom")), lanePreset(lane("shared"), "owner", today))
        assertEquals(NewPreset(due = "2026-11-12"), lanePreset(lane("week"), "due", today))
        assertEquals(NewPreset(due = "2026-11-23"), lanePreset(lane("month"), "due", today))
        assertEquals(NewPreset(due = null), lanePreset(lane("overdue"), "due", today))
    }

    @Test fun wordingIsToDoNotTask() {
        listOf(WeddingStringsEn, WeddingStringsHi, WeddingStringsTa).forEach { x ->
            val all = listOf(x.noTasksTitle, x.searchPlaceholder, x.createTask, x.removeTask, x.addOnThisDay, x.quickMine, x.matchesCount(2, 5), x.showTasks(3), x.dueThisMonth(2))
            all.forEach { assertTrue(!it.contains("task", ignoreCase = true) && !it.contains("कार्य") && !it.contains("பணி"), "still formal: $it") }
        }
        assertEquals("1 to-do · 0 done", WeddingStringsEn.tasksSummary(1, 0))
    }

    @Test fun searchMatchesTitles() {
        val t = task("7")
        assertTrue(passes(t, Filters(q = "task 7"), today))
        assertTrue(!passes(t, Filters(q = "venue"), today))
    }
}
