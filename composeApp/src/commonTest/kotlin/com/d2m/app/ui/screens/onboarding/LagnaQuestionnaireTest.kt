package com.d2m.app.ui.screens.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.d2m.app.data.model.LagnaAnswers
import com.d2m.app.data.model.LagnaDeduceOut
import com.d2m.app.data.model.LagnaDeduceRequest
import com.d2m.app.data.model.LagnaOption
import com.d2m.app.data.model.LagnaQuestion
import com.d2m.app.data.model.LagnaQuestionsOut
import com.d2m.app.data.network.LagnaService
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeLagna(var out: LagnaDeduceOut, val fail: Boolean = false) : LagnaService {
    var deduceCalls = 0
    var lastRequest: LagnaDeduceRequest? = null
    override suspend fun questions() = LagnaQuestionsOut(
        "test",
        listOf(
            LagnaQuestion("studied_abroad", "yes_no", 0.2, "Did you study abroad?"),
            LagnaQuestion("younger_sibling", "single_choice", 0.1, "Do you have younger siblings?", listOf(LagnaOption("NONE", "None"))),
            LagnaQuestion("sibling_bond_close", "yes_no", 0.1, "Close with your sibling?", onlyIf = mapOf("younger_sibling" to listOf("BROTHER"))),
            LagnaQuestion("conventional_early_marriage", "yes_no", 0.1, "Was your earlier marriage conventional?", onlyIf = mapOf("marital_status" to listOf("divorced"))),
        ),
    )

    override suspend fun deduce(body: LagnaDeduceRequest): LagnaDeduceOut {
        deduceCalls++; lastRequest = body
        if (fail) error("boom")
        return out
    }
}

@OptIn(ExperimentalTestApi::class)
class LagnaQuestionnaireTest {
    private fun locked() = LagnaDeduceOut(status = "LOCKED", lagna = "Leo", confidence = 0.82, provisionalTob = "07:42", tobSource = "DEDUCED_LOCKED", informative = true)

    @Test
    fun rendersServerQuestionsAndHidesOnesThatDoNotApply() = runComposeUiTest {
        val fake = FakeLagna(locked())
        setContent {
            MaterialTheme { LagnaQuestionnaire("1996-03-12", 13.08, 80.27, 5.5, LagnaAnswers(), {}, null, {}, api = fake) }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTextCount("Did you study abroad?") == 1 }
        assertEquals(1, onAllNodesWithTextCount("Do you have younger siblings?"))
        // follow-up needs a brother; the marital-status one needs the child's status, which the wizard doesn't collect
        assertEquals(0, onAllNodesWithTextCount("Close with your sibling?"))
        assertEquals(0, onAllNodesWithTextCount("Was your earlier marriage conventional?"))
    }

    @Test
    fun checkSendsBirthInputsAndShowsLockedResult() = runComposeUiTest {
        val fake = FakeLagna(locked())
        var result by mutableStateOf<LagnaDeduceOut?>(null)
        setContent {
            MaterialTheme {
                LagnaQuestionnaire("1996-03-12", 13.08, 80.27, 5.5, LagnaAnswers(tobWindow = "MORNING"), {}, result, { result = it }, api = fake)
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTextCount("See what this points to") == 1 }
        onNodeWithText("See what this points to").performClick()
        waitUntil(timeoutMillis = 5_000) { result != null }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTextCount("Likely rising sign: Leo (82% match), around 07:42.") == 1 }
        assertEquals(1, fake.deduceCalls)
        assertEquals("1996-03-12", fake.lastRequest?.dob)
        assertEquals("MORNING", fake.lastRequest?.answers?.tobWindow)
    }

    @Test
    fun provisionalResultListsTheMostHelpfulQuestions() = runComposeUiTest {
        val fake = FakeLagna(LagnaDeduceOut(
            status = "NEEDS_CLARIFICATION", lagna = "Virgo", confidence = 0.55, provisionalTob = "11:05",
            tobSource = "DEDUCED_PROVISIONAL", informative = true,
            nextQuestions = listOf(LagnaQuestion("studied_abroad", "yes_no", text = "Did you study abroad?")),
        ))
        var result by mutableStateOf<LagnaDeduceOut?>(null)
        setContent {
            MaterialTheme { LagnaQuestionnaire("1996-03-12", 13.08, 80.27, 5.5, LagnaAnswers(), {}, result, { result = it }, api = fake) }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTextCount("See what this points to") == 1 }
        onNodeWithText("See what this points to").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTextCount("Answering these would help most:") == 1 }
        onNodeWithText("• Did you study abroad?").assertExists()
    }

    @Test
    fun deduceFailureDoesNotCrashAndKeepsTheFormUsable() = runComposeUiTest {
        val fake = FakeLagna(locked(), fail = true)
        setContent {
            MaterialTheme { LagnaQuestionnaire("1996-03-12", 13.08, 80.27, 5.5, LagnaAnswers(), {}, null, {}, api = fake) }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTextCount("See what this points to") == 1 }
        onNodeWithText("See what this points to").performClick()
        waitUntil(timeoutMillis = 5_000) { fake.deduceCalls == 1 }
        onNodeWithText("See what this points to").assertExists() // button is back, not stuck on "Checking…"
    }

    private fun androidx.compose.ui.test.ComposeUiTest.onAllNodesWithTextCount(text: String) =
        onAllNodesWithText(text, substring = true).fetchSemanticsNodes().size
}
