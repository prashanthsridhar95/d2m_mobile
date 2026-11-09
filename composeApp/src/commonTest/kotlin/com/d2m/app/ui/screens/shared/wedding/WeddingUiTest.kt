package com.d2m.app.ui.screens.shared.wedding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.d2m.app.data.model.WeddingNamesOut
import com.d2m.app.data.model.WeddingPanchangamDay
import com.d2m.app.data.model.WeddingPanchangamOut
import com.d2m.app.data.model.WeddingPhaseOut
import com.d2m.app.data.model.WeddingPlanOut
import com.d2m.app.data.model.WeddingTaskOut
import com.d2m.app.ui.strings.WeddingStrings
import com.d2m.app.ui.strings.WeddingStringsEn
import com.d2m.app.ui.strings.WeddingStringsHi
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Renders the real checklist composables with fake data -- catches layout crashes and wiring mistakes without a device. */
@OptIn(ExperimentalTestApi::class)
class WeddingUiTest {
    private val today = LocalDate(2026, 11, 9)
    private val plan = WeddingPlanOut(
        id = "p", unionId = "u", myRole = "groom", names = WeddingNamesOut("Priya", "Prakash"),
        engagementDate = "2026-11-03", marriageDate = "2026-11-20", honeymoonDate = "2026-11-28",
    )
    private val phases = listOf(WeddingPhaseOut("planning", "Planning"), WeddingPhaseOut("foundations", "Foundations"))

    private fun task(id: String, title: String, due: String?, participants: List<String> = listOf("groom"), status: String = "todo", overdue: Boolean = false, phase: String = "planning", scope: String = "individual") =
        WeddingTaskOut(id = id, title = title, phase = phase, scope = scope, participants = participants, dueDate = due, status = status,
            overdue = overdue, iAmParticipant = "groom" in participants, confirmedBy = if (status == "done") listOf("groom") else emptyList(), iConfirmed = status == "done", commentCount = if (id == "1") 2 else 0)

    private val tasks = listOf(
        task("1", "Book the venue", "2026-11-12"),
        task("2", "Choose attire together", "2026-11-12", listOf("bride", "groom"), scope = "couple"),
        task("3", "Fix the muhurtham", "2026-11-05", overdue = true, phase = "foundations"),
        task("4", "Order invitations", null),
        task("5", "Finalise caterer", "2026-11-20", status = "done"),
    )

    private fun env(s: WeddingStrings = WeddingStringsEn): WeddingEnv {
        val pm = PanchangamStore(
            fetch = { start, _ ->
                WeddingPanchangamOut(true, listOf(
                    WeddingPanchangamDay("2026-11-12", "Dwitiya", "Anuradha", isMuhoortham = true),
                    WeddingPanchangamDay("2026-11-15", "Amavasya", "Mula", isAmavasya = true, chandrashtamam = listOf("bride")),
                    WeddingPanchangamDay("2026-11-24", "Purnima", "Krittika", isPurnima = true),
                ))
            },
            scope = CoroutineScope(Job() + Dispatchers.Unconfined),
        )
        pm.ensure(listOf("2026-11-12"))
        return WeddingEnv(labeler(plan, s), pm, today, s)
    }

    private fun androidx.compose.ui.test.ComposeUiTest.render(e: WeddingEnv, content: @androidx.compose.runtime.Composable () -> Unit) = setContent {
        MaterialTheme { D2MTheme(flow = D2MFlow.CHILD) { CompositionLocalProvider(LocalWedding provides e) { Box(Modifier.fillMaxSize()) { content() } } } }
    }

    @Test fun listShowsGroupsRowsNamesAndMe() = runComposeUiTest {
        val e = env()
        var toggled: String? = null
        var opened: String? = null
        render(e) {
            WeddingListView(buildGroups(tasks, phases, "phase", e.label, today, e.s), busy = false,
                onOpen = { opened = it }, onToggle = { toggled = it.id }, onAddInGroup = {})
        }
        onNodeWithText("Planning").assertIsDisplayed()
        onNodeWithText("Book the venue").assertIsDisplayed()
        assertTrue(onAllNodesWithText("Me").fetchSemanticsNodes().isNotEmpty(), "my own tasks should be labelled Me")
        onNodeWithText("Book the venue").performClick()
        assertEquals("1", opened)
        onNodeWithText("Foundations").performClick() // collapses its group
    }

    @Test fun boardShowsOneLanePerBucket() = runComposeUiTest {
        val e = env()
        render(e) { WeddingBoardView(buildGroups(tasks, phases, "due", e.label, today, e.s), busy = false, onOpen = {}, onToggle = {}) }
        onNodeWithText("Overdue").assertIsDisplayed()
        onNodeWithText("Next 7 days").assertIsDisplayed()
        onNodeWithText("Fix the muhurtham").assertIsDisplayed()
    }

    @Test fun calendarShowsMonthMarkersAndOpensADay() = runComposeUiTest {
        val e = env()
        var day: String? = null
        render(e) { WeddingCalendarView(tasks, plan, openDay = null, onOpenDay = { day = it }) }
        onNodeWithText("November 2026").assertIsDisplayed()
        onNodeWithText("Chandrashtamam").assertIsDisplayed() // legend
        assertTrue(onAllNodesWithText("Ch").fetchSemanticsNodes().isNotEmpty(), "chandrashtamam day should carry the Ch marker")
        onNodeWithText("12").performClick()
        assertEquals("2026-11-12", day)
    }

    @Test fun filterContentListsNamesWithCounts() = runComposeUiTest {
        val e = env()
        render(e) { WeddingFilterContent(Filters(), {}, phases, tasks, tasks.size) }
        onNodeWithText("Priya").assertIsDisplayed()
        onNodeWithText("Priya's parents").assertIsDisplayed()
        assertTrue(onAllNodesWithText("Me").fetchSemanticsNodes().isNotEmpty())
        onNodeWithText("5 of 5 tasks match").assertIsDisplayed()
    }

    @Test fun hindiRendersLocalizedLabels() = runComposeUiTest {
        val e = env(WeddingStringsHi)
        render(e) { WeddingBoardView(buildGroups(tasks, phases, "due", e.label, today, e.s), busy = false, onOpen = {}, onToggle = {}) }
        onNodeWithText("विलंबित").assertIsDisplayed()
        onNodeWithText("अगले 7 दिन").assertIsDisplayed()
    }
}
