package com.d2m.app.ui.screens.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.ParentMatchCheckIn
import com.d2m.app.data.model.ParentMatchCheckOut
import com.d2m.app.data.network.ApiError
import com.d2m.app.data.network.ErrorCopy
import com.d2m.app.data.network.GeocodingApi
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.DashboardRepository
import com.d2m.app.ui.components.CityAutocomplete
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MChipGroup
import com.d2m.app.ui.components.D2MCheckboxRow
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MOptionGroup
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.components.SectionHeading
import com.d2m.app.ui.components.Taxonomy
import com.d2m.app.ui.strings.LocalAppLocale
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/**
 * Parent Home -> "Check a match". Mirrors d2m_web's MatchCheckScreen.jsx: the parent enters someone's
 * birth details and sees how they fit with the linked child (astrology engine + the parent's saved
 * preferences). Nothing is stored. Copy is English-only for now (hi/ta strings are a follow-up).
 */
private data class MatchForm(
    val name: String = "", val dob: String = "", val tob: String = "", val tobApprox: Boolean = false,
    val place: String = "", val lat: Double? = null, val lon: Double? = null, val tz: String? = null,
    val marital: String = "never_married", val religion: String = "", val community: String = "",
    val education: String = "", val income: String = "", val profession: String = "",
)

private val VERDICTS = mapOf(
    "strong_fit" to "Strong fit", "worth_exploring" to "Worth exploring",
    "needs_discussion" to "Needs discussion", "not_recommended" to "Not recommended",
)
private val TIERS = mapOf(
    "TIER_1_PRIME_ALIGN" to "Prime alignment", "TIER_2_STRONG_POTENTIAL" to "Strong potential",
    "TIER_3_TIMING_OR_RECIPROCITY_GATED" to "Promising, timing-dependent",
    "TIER_4_LOW_ALIGNMENT" to "Low alignment", "DORMANT_BLOCKED" to "Blocked",
)
private val PARTS = listOf("kp_core" to "KP core", "structural" to "Structural", "synastry" to "Synastry", "timing" to "Timing")

/** UTC offset (hours) the zone had at that local birth moment, or null while the inputs are incomplete/invalid. */
private fun offsetHoursAt(tz: String?, dob: String, tob: String): Double? = runCatching {
    val zone = TimeZone.of(tz ?: return null)
    val instant: Instant = LocalDateTime.parse("${dob}T$tob").toInstant(zone)
    zone.offsetAt(instant).totalSeconds / 3600.0
}.getOrNull()

private fun ageOk(dob: String) = Regex("""\d{4}-\d{2}-\d{2}""").matches(dob)
private fun timeOk(tob: String) = Regex("""\d{2}:\d{2}""").matches(tob)

@Composable
fun MatchCheckScreen() {
    val identityStore: IdentityStore = koinInject()
    val repo: DashboardRepository = koinInject()
    val geocodingApi: GeocodingApi = koinInject()
    val identity by identityStore.identity.collectAsState()
    val locale = LocalAppLocale.current
    val scope = rememberCoroutineScope()

    var form by remember { mutableStateOf(MatchForm()) }
    var result by remember { mutableStateOf<ParentMatchCheckOut?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val offset = if (ageOk(form.dob) && timeOk(form.tob)) offsetHoursAt(form.tz, form.dob, form.tob) else null
    val ready = offset != null && form.lat != null && form.lon != null && form.place.isNotBlank()

    fun submit() {
        val sponsorId = identity.sponsorId ?: return
        busy = true; error = null
        scope.launch {
            try {
                fun opt(s: String) = s.trim().ifEmpty { null }
                result = repo.checkMatch(sponsorId, ParentMatchCheckIn(
                    name = opt(form.name), dob = form.dob, tob = form.tob, birthPlace = form.place,
                    birthLat = form.lat!!, birthLon = form.lon!!, birthTzOffsetHours = offset!!,
                    maritalStatus = form.marital, religion = opt(form.religion), casteCommunity = opt(form.community),
                    highestEducation = opt(form.education), incomeBand = opt(form.income),
                    professionTier = opt(form.profession), tobIsExact = !form.tobApprox,
                ))
            } catch (e: ApiError) {
                error = if (e.status == 400) (e.detail ?: ErrorCopy.somethingWentWrong) else ErrorCopy.somethingWentWrong
            } catch (e: Exception) {
                error = ErrorCopy.somethingWentWrong
            } finally {
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageTitle("Check a match")
        error?.let { D2MErrorBanner(it, inline = true) }
        val r = result
        if (r != null) {
            MatchResult(r, onEdit = { result = null }, onAgain = { result = null; form = MatchForm() })
        } else {
            Text(
                "Have someone's birth details? See how they fit with your child, using the same astrology and your saved preferences. We don't save these details.",
                style = MaterialTheme.typography.bodyMedium, color = mutedText(0.55f),
            )
            D2MTextField("Name", form.name, { form = form.copy(name = it) }, optional = true)
            D2MTextField("Date of birth (YYYY-MM-DD)", form.dob, { form = form.copy(dob = it) })
            D2MTextField("Time of birth (HH:MM, 24h)", form.tob, { form = form.copy(tob = it) })
            D2MCheckboxRow("I'm not sure of the exact time", form.tobApprox, { form = form.copy(tobApprox = it) })
            CityAutocomplete(
                label = "Place of birth", value = form.place, geocodingApi = geocodingApi,
                onSelect = { c -> form = form.copy(place = c.label, lat = c.lat, lon = c.lon, tz = c.timezone) },
                onRawTextChange = { t -> form = form.copy(place = t, lat = null, lon = null, tz = null) },
            )
            if (form.place.isNotBlank() && form.tz == null) {
                Text("Pick a suggestion so coordinates and timezone fill in.", style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
            }
            D2MOptionGroup("Marital status", Taxonomy.MARITAL_STATUSES, form.marital, { form = form.copy(marital = it) },
                optionLabel = { Taxonomy.toLabel(it, locale) })
            OptionalPick("Religion", Taxonomy.RELIGIONS, form.religion, { form = form.copy(religion = it) })
            OptionalPick("Community", Taxonomy.COMMUNITIES, form.community, { form = form.copy(community = it) })
            OptionalPick("Education", Taxonomy.EDUCATION_LEVELS, form.education, { form = form.copy(education = it) })
            OptionalPick("Income band", Taxonomy.INCOME_BANDS, form.income, { form = form.copy(income = it) })
            OptionalPick("Profession", Taxonomy.PROFESSION_TIERS, form.profession, { form = form.copy(profession = it) })
            Text("Anything you skip is treated as neutral, not as a mismatch.", style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
            D2MButton(if (busy) "Checking…" else "Check compatibility", ::submit, Modifier.fillMaxWidth(), enabled = ready && !busy)
        }
    }
}

/** Single-select chip row that can be cleared by tapping the selected chip again. */
@Composable
private fun OptionalPick(label: String, options: List<String>, value: String, onChange: (String) -> Unit) {
    val locale = LocalAppLocale.current
    D2MChipGroup(label, options, if (value.isEmpty()) emptyList() else listOf(value),
        { picked -> onChange(picked.firstOrNull { it != value } ?: "") },
        optionLabel = { Taxonomy.toLabel(it, locale) })
}

@Composable
private fun ScoreBar(label: String, value: Double, max: Double = 100.0) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("${(value / max * 100).coerceIn(0.0, 100.0).roundToInt()}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MatchResult(r: ParentMatchCheckOut, onEdit: () -> Unit, onAgain: () -> Unit) {
    val a = r.astrology
    val p = r.preferences
    D2MCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${r.candidateName ?: "Prospective match"}, ${r.candidateAge}", style = MaterialTheme.typography.bodyLarge)
            SectionHeading(VERDICTS[r.verdict] ?: r.verdict)
            Text("${r.compositeScore.roundToInt()} / 100", style = MaterialTheme.typography.titleMedium)
        }
    }
    D2MCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Your preferences", style = MaterialTheme.typography.titleMedium)
            if (!p.hardRequirementsMet) {
                Text("This profile doesn't meet ${p.hardChecks.count { !it.ok }} of your requirements.", color = MaterialTheme.colorScheme.error)
            }
            p.hardChecks.forEach { Text("${if (it.ok) "✓" else "✗"}  ${it.label}", style = MaterialTheme.typography.bodyMedium) }
            ScoreBar("Community", p.scores.community)
            ScoreBar("Child's preferences", p.scores.childPreferences)
            ScoreBar("Your soft preferences", p.scores.parentPreferences)
            if (p.notProvided.isNotEmpty()) {
                Text("Add religion / community to sharpen this result.", style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
            }
        }
    }
    D2MCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Astrology", style = MaterialTheme.typography.titleMedium)
            Text("${TIERS[a.tier] ?: a.tier} · ${a.score.roundToInt()} / 100", style = MaterialTheme.typography.bodyLarge)
            a.parts?.let { parts -> PARTS.forEach { (k, label) -> parts[k]?.let { ScoreBar(label, it, 1.0) } } }
            if (a.dealbreakers.isNotEmpty()) {
                Text("Dealbreakers: " + a.dealbreakers.joinToString(", ") { it.replace('_', ' ') }, color = MaterialTheme.colorScheme.error)
            }
            if (a.doshas.isNotEmpty()) {
                Text(a.doshas.joinToString(" · ") { it.name.replace('_', ' ') + if (it.cancelled) " (cancelled)" else "" }, style = MaterialTheme.typography.bodyMedium)
            }
            if (a.confidence == "verify_birth_time") {
                Text("Confirm the exact birth time for a firmer result.", style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
            }
        }
    }
    Text("Not saved. These details are discarded when you leave this page.", style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
    D2MButton("Edit details", onEdit, Modifier.fillMaxWidth(), variant = D2MButtonVariant.OUTLINE)
    D2MButton("Check another", onAgain, Modifier.fillMaxWidth())
}
