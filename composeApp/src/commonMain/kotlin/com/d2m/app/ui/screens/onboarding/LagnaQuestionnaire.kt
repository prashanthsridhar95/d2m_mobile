package com.d2m.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.LagnaAnswers
import com.d2m.app.data.model.LagnaDeduceOut
import com.d2m.app.data.model.LagnaDeduceRequest
import com.d2m.app.data.model.LagnaLifeEvent
import com.d2m.app.data.model.LagnaQuestion
import com.d2m.app.data.network.LagnaApi
import com.d2m.app.data.network.LagnaService
import com.d2m.app.data.network.friendlyError
import com.d2m.app.ui.components.AstroNames
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonSize
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MChipGroup
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MSelectField
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.strings.LocalAppLocale
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.d2m
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val SIGN_ORDER = listOf(
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces",
)

// ---- answers <-> question ids (ids are the server's LagnaAnswers field names in snake_case) --------------------

private fun LagnaAnswers.single(id: String): String? = when (id) {
    "tob_window" -> tobWindow
    "parental_environment" -> parentalEnvironment
    "education_domain" -> educationDomain
    "younger_sibling" -> youngerSibling
    else -> null
}

private fun LagnaAnswers.withSingle(id: String, v: String?): LagnaAnswers = when (id) {
    "tob_window" -> copy(tobWindow = v ?: "UNKNOWN")
    "parental_environment" -> copy(parentalEnvironment = v)
    "education_domain" -> copy(educationDomain = v)
    // changing the sibling answer invalidates the follow-up about them
    "younger_sibling" -> copy(youngerSibling = v, siblingBondClose = if (v == null || v == "NONE") null else siblingBondClose)
    else -> this
}

private fun LagnaAnswers.bool(id: String): Boolean? = when (id) {
    "studied_abroad" -> studiedAbroad
    "sibling_bond_close" -> siblingBondClose
    "family_affluent" -> familyAffluent
    "conventional_early_marriage" -> conventionalEarlyMarriage
    "settled_abroad_after_marriage" -> settledAbroadAfterMarriage
    else -> null
}

private fun LagnaAnswers.withBool(id: String, v: Boolean?): LagnaAnswers = when (id) {
    "studied_abroad" -> copy(studiedAbroad = v)
    "sibling_bond_close" -> copy(siblingBondClose = v)
    "family_affluent" -> copy(familyAffluent = v)
    "conventional_early_marriage" -> copy(conventionalEarlyMarriage = v)
    "settled_abroad_after_marriage" -> copy(settledAbroadAfterMarriage = v)
    else -> this
}

/** `only_if` gate. marital_status is the child's own status, which this wizard does not collect, so those stay hidden. */
private fun applies(q: LagnaQuestion, a: LagnaAnswers): Boolean = q.onlyIf.orEmpty().all { (field, allowed) ->
    field != "marital_status" && a.single(field) in allowed
}

/**
 * Nashta Jataka questionnaire for an unknown time of birth. Renders the server's question catalogue (served in
 * English; the surrounding chrome is localized), previews the deduction on demand, and reports both the answers
 * and the latest result to the caller. The wizard submits the answers with createSponsor(); the server re-runs the
 * engine and stores only the deduced time with its provenance.
 */
@Composable
internal fun LagnaQuestionnaire(
    dob: String,
    lat: Double,
    lon: Double,
    tzOffsetHours: Double,
    answers: LagnaAnswers,
    onAnswers: (LagnaAnswers) -> Unit,
    result: LagnaDeduceOut?,
    onResult: (LagnaDeduceOut?) -> Unit,
    modifier: Modifier = Modifier,
    api: LagnaService = koinInject<LagnaApi>(),
) {
    val strings = LocalStrings.current.onboarding
    val locale = LocalAppLocale.current
    val scope = rememberCoroutineScope()

    var questions by remember { mutableStateOf<List<LagnaQuestion>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            questions = api.questions().questions.sortedByDescending { it.weight }.sortedBy { if (it.id == "tob_window") 0 else 1 }
        } catch (e: Exception) {
            loadFailed = true
        }
    }

    fun answersChanged(next: LagnaAnswers) {
        onAnswers(next)
        onResult(null) // an old preview no longer reflects what was answered
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(strings.lagnaIntro, style = MaterialTheme.typography.bodyMedium, color = d2m.meta)

        val qs = questions
        when {
            loadFailed -> D2MErrorBanner(strings.lagnaQuestionsFailed)
            qs == null -> Text(strings.lagnaLoadingQuestions, style = MaterialTheme.typography.bodySmall, color = d2m.meta)
            else -> qs.filter { applies(it, answers) }.forEach { q ->
                when (q.kind) {
                    "single_choice" -> D2MSelectField(
                        label = q.text,
                        value = answers.single(q.id).orEmpty(),
                        options = q.options.map { it.value },
                        onValueChange = { answersChanged(answers.withSingle(q.id, it)) },
                        optionLabel = { v -> q.options.firstOrNull { it.value == v }?.label ?: v },
                        optional = true,
                    )
                    "multi_choice" -> D2MChipGroup(
                        label = q.text,
                        options = q.options.map { it.value },
                        value = answers.traits.orEmpty(),
                        onChange = { answersChanged(answers.copy(traits = it.ifEmpty { null })) },
                        optionLabel = { v -> q.options.firstOrNull { it.value == v }?.label ?: v },
                    )
                    "yes_no" -> D2MSelectField(
                        label = q.text,
                        value = when (answers.bool(q.id)) { true -> "yes"; false -> "no"; null -> "" },
                        options = listOf("yes", "no"),
                        onValueChange = { answersChanged(answers.withBool(q.id, if (it == "yes") true else if (it == "no") false else null)) },
                        optionLabel = { if (it == "yes") strings.lagnaYes else strings.lagnaNo },
                        optional = true,
                    )
                    "events" -> LifeEvents(q, answers.lifeEvents) { answersChanged(answers.copy(lifeEvents = it)) }
                }
            }
        }

        if (qs != null) {
            D2MButton(
                text = if (checking) strings.lagnaChecking else strings.lagnaCheck,
                enabled = !checking,
                variant = D2MButtonVariant.OUTLINE,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        checking = true
                        error = null
                        try {
                            onResult(api.deduce(LagnaDeduceRequest(dob, lat, lon, tzOffsetHours, null, answers)))
                        } catch (e: Exception) {
                            error = friendlyError(e, strings.lagnaCheckFailed)
                        } finally {
                            checking = false
                        }
                    }
                },
            )
        }
        error?.let { D2MErrorBanner(it) }

        result?.let { r ->
            val lagnaName = r.lagna?.let { l -> SIGN_ORDER.indexOf(l).takeIf { it >= 0 }?.let { AstroNames.sign(it, l, locale) } ?: l }
            D2MCard(modifier = Modifier.fillMaxWidth(), accented = r.status == "LOCKED") {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val pct = (r.confidence * 100).toInt()
                    val headline = when {
                        r.status == "LOCKED" && lagnaName != null -> strings.lagnaLocked(lagnaName, pct, r.provisionalTob.orEmpty())
                        r.informative && lagnaName != null -> strings.lagnaProvisional(lagnaName, pct, r.provisionalTob.orEmpty())
                        else -> strings.lagnaNoSignal
                    }
                    Text(headline, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    if (r.status != "LOCKED" && r.nextQuestions.isNotEmpty()) {
                        Text(strings.lagnaHelpfulQuestions, style = MaterialTheme.typography.labelMedium, color = d2m.meta)
                        r.nextQuestions.take(3).forEach { Text("• ${it.text}", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LifeEvents(q: LagnaQuestion, events: List<LagnaLifeEvent>, onChange: (List<LagnaLifeEvent>) -> Unit) {
    val strings = LocalStrings.current.onboarding
    var type by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(q.text, style = MaterialTheme.typography.labelLarge)
        events.forEachIndexed { i, e ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val label = q.options.firstOrNull { it.value == e.eventType }?.label ?: e.eventType
                Text("$label · ${e.month?.let { "$it/" } ?: ""}${e.year}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                D2MButton(strings.lagnaRemove, onClick = { onChange(events.filterIndexed { j, _ -> j != i }) }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM)
            }
        }
        if (events.size < 10) {
            D2MSelectField(
                label = strings.lagnaEventType, value = type, options = q.options.map { it.value }, onValueChange = { type = it },
                optionLabel = { v -> q.options.firstOrNull { it.value == v }?.label ?: v }, optional = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                D2MTextField(strings.lagnaEventYear, year, { year = it.filter(Char::isDigit).take(4) }, modifier = Modifier.weight(1f))
                D2MTextField(strings.lagnaEventMonth, month, { month = it.filter(Char::isDigit).take(2) }, modifier = Modifier.weight(1f), optional = true)
            }
            val y = year.toIntOrNull()
            val m = month.toIntOrNull()
            D2MButton(
                text = strings.lagnaAddEvent,
                variant = D2MButtonVariant.OUTLINE,
                size = D2MButtonSize.SM,
                enabled = type.isNotEmpty() && y != null && y in 1900..2100 && (month.isEmpty() || m in 1..12),
                onClick = {
                    onChange(events + LagnaLifeEvent(type, y!!, m))
                    type = ""; year = ""; month = ""
                },
            )
        }
    }
}
