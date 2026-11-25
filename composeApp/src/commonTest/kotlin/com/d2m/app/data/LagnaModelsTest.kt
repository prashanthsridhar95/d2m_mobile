package com.d2m.app.data

import com.d2m.app.data.model.LagnaAnswers
import com.d2m.app.data.model.LagnaDeduceOut
import com.d2m.app.data.model.LagnaLifeEvent
import com.d2m.app.data.model.LagnaQuestionsOut
import com.d2m.app.data.model.SponsorCreateRequest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Wire format of the Nashta Jataka lagna deduction (app/routers/lagna.py), same Json config as ApiClient. */
@OptIn(ExperimentalSerializationApi::class)
class LagnaModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    @Test
    fun sponsorRequestWithUnknownTimeSendsNullTobAndSnakeCaseAnswers() {
        val req = SponsorCreateRequest(
            name = "A", contactInfo = "", ownReligion = "Hindu", ownCasteCommunity = "iyer", acceptReligions = listOf("Hindu"),
            minAge = 25, maxAge = 32, childName = "C", childGender = "female", childSeekingGender = "male",
            childDob = "1996-03-12", childTob = null, childBirthPlace = "Chennai", childBirthLat = 13.08, childBirthLon = 80.27,
            childBirthTzOffsetHours = 5.5,
            lagnaAnswers = LagnaAnswers(
                tobWindow = "MORNING", traits = listOf("ASSERTIVE"), studiedAbroad = true,
                lifeEvents = listOf(LagnaLifeEvent("MARRIAGE", 2022, 5)),
            ),
        )
        val obj = json.parseToJsonElement(json.encodeToString(SponsorCreateRequest.serializer(), req)).jsonObject
        assertTrue(obj["child_tob"].toString() == "null")
        val answers = obj["lagna_answers"]!!.jsonObject
        assertEquals("\"MORNING\"", answers["tob_window"].toString())
        assertEquals("true", answers["studied_abroad"].toString())
        assertTrue(answers["life_events"].toString().contains("\"event_type\":\"MARRIAGE\""))
    }

    @Test
    fun knownTimeRequestOmitsNothingTheServerRejects() {
        val req = SponsorCreateRequest(
            name = "A", contactInfo = "", ownReligion = "Hindu", ownCasteCommunity = "iyer", acceptReligions = emptyList(),
            minAge = 25, maxAge = 32, childName = "C", childGender = "female", childSeekingGender = "male",
            childDob = "1996-03-12", childTob = "08:15", childBirthPlace = "Chennai", childBirthLat = 13.08, childBirthLon = 80.27,
            childBirthTzOffsetHours = 5.5,
        )
        val obj = json.parseToJsonElement(json.encodeToString(SponsorCreateRequest.serializer(), req)).jsonObject
        assertEquals("\"08:15\"", obj["child_tob"].toString())
        assertEquals("null", obj["lagna_answers"].toString()) // server: send either child_tob or lagna_answers, not both
    }

    @Test
    fun decodesQuestionCatalogueAndDeduceResult() {
        val q = json.decodeFromString(
            LagnaQuestionsOut.serializer(),
            """{"engine_version":"v","questions":[{"id":"sibling_bond_close","kind":"yes_no","weight":0.1,"text":"Close?",
               "only_if":{"younger_sibling":["BROTHER","BOTH"]}},{"id":"traits","kind":"multi_choice","weight":0.25,"text":"t",
               "options":[{"value":"ASSERTIVE","label":"Blunt"}]}]}""",
        )
        assertEquals(listOf("BROTHER", "BOTH"), q.questions[0].onlyIf?.get("younger_sibling"))
        assertEquals("Blunt", q.questions[1].options[0].label)
        assertNull(q.questions[1].onlyIf)

        val r = json.decodeFromString(
            LagnaDeduceOut.serializer(),
            """{"status":"NEEDS_CLARIFICATION","lagna":"Leo","confidence":0.61,"provisional_tob":"07:42","tob_source":"DEDUCED_PROVISIONAL",
               "informative":true,"candidates":[{"lagna":"Leo","d9_lagna":"Aries","confidence":0.61}],
               "next_questions":[{"id":"studied_abroad","kind":"yes_no","text":"Abroad?"}]}""",
        )
        assertEquals("07:42", r.provisionalTob)
        assertEquals("studied_abroad", r.nextQuestions.single().id)
    }
}
