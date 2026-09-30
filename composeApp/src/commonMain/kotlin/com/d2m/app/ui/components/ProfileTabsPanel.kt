package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d2m.app.data.model.AboutMeDataOut
import com.d2m.app.data.model.ChartOut
import com.d2m.app.data.model.ExtendedBioDataOut
import com.d2m.app.data.network.ApiError
import com.d2m.app.domain.repository.DashboardRepository
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.ui.theme.D2MNoteStyle
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText
import com.d2m.app.ui.components.D2MTabs
import com.d2m.app.ui.components.D2MCard
import kotlinx.serialization.json.JsonObject
import org.koin.compose.koinInject

/**
 * Right-hand/below detail panel of the profile view (ProfileDetailScreen +
 * DiscoveryScreen) -- Bio data / Chart tabs, mirrors ProfileTabsPanel.jsx.
 * Data-fetching lives IN this component (unlike web's useCandidateProfile
 * hook feeding props down from a parent) since every call site just wants
 * "the detail panel for this candidateId" with nothing else to coordinate.
 *
 * getExtendedBio and getChart hit the exact same endpoints regardless of
 * whose id is passed (/primaries/{id}/extended-bio, /primaries/{id}/chart
 * -- see app/routers/identity.py and astrology.py, neither has a separate
 * "candidate" variant), so this reuses IdentityRepository/DashboardRepository
 * exactly as-is with candidateId in place of the viewer's own primaryId,
 * same as web's getExtendedBio(candidateId)/getChart(candidateId). A 404 on
 * either just means "nothing on file for this profile yet" (most profiles
 * don't have an imported extended bio, and not every chart is computed) --
 * shown as a quiet empty state, not an error banner, same convention as web.
 *
 * Both fetches run eagerly on mount (not chart-on-tab-open) -- matching
 * web's useCandidateProfile hook, which fetches chart/photos/extendedBio
 * unconditionally the moment a candidateId is known, not lazily per tab.
 *
 * Fixed body height was tried and reverted (crashed on device): web's
 * ProfileTabsPanel.jsx pins its height to the Chart tab's natural size and
 * scrolls Bio data internally within that fixed box ("switching tabs
 * changes height. I want the height of the chart tab to be baseline for
 * both tabs"). The direct Compose port of that -- measure the Chart tab's
 * height, apply it via Modifier.height(...), give the Bio data Column its
 * own Modifier.verticalScroll() to fit inside it -- crashed with
 * "Vertically scrollable component was measured with an infinity maximum
 * height constraints" the moment this panel opened. Root cause: both call
 * sites (DiscoveryScreen, ProfileDetailScreen) are themselves one
 * full-page Modifier.verticalScroll() Column already -- web's page isn't
 * structured that way (the panel sits in a non-scrolling flex layout, with
 * a *different* div owning page-level overflow), so nesting a second
 * vertically-scrollable region inside this panel is safe there but not
 * here. Compose explicitly disallows a scrollable measured with an
 * unbounded max-height constraint, which is exactly what an outer
 * full-page verticalScroll Column hands to the very first frame's worth of
 * children before this panel's own height has been measured. Rather than
 * fight that with a custom two-pass layout, each tab here just sizes to
 * its own natural content height (like before) -- the page underneath is
 * already one continuous scroll region, so a resizing panel reads as
 * normal content reflow rather than a jarring layout shift the way it
 * would inside web's fixed-viewport page.
 */
// `showAboutMe` -- About Me is parent-invisible ("About me shouldn't be
// visible for the parents - anywhere", reported directly) and, when it IS
// shown (viewer is the child role), leads the tab bar and is the default
// tab. Callers resolve this from their own `identity.role` (see
// ProfileDetailScreen.kt/DiscoveryScreen.kt) rather than this panel
// reading IdentityStore itself -- DiscoveryScreen is a child-only route
// with no ambiguity, while ProfileDetailScreen serves both roles and
// needs the real check.
@Composable
fun D2MProfileTabsPanel(candidateId: String, modifier: Modifier = Modifier, showAboutMe: Boolean = false) {
    val identityRepo: IdentityRepository = koinInject()
    val dashboardRepo: DashboardRepository = koinInject()

    var tab by remember(candidateId, showAboutMe) { mutableStateOf(0) }
    var bio by remember(candidateId) { mutableStateOf<ExtendedBioDataOut?>(null) }
    var bioLoaded by remember(candidateId) { mutableStateOf(false) }
    var aboutMe by remember(candidateId) { mutableStateOf<AboutMeDataOut?>(null) }
    var aboutMeLoaded by remember(candidateId) { mutableStateOf(false) }
    var chart by remember(candidateId) { mutableStateOf<ChartOut?>(null) }
    var chartNotComputed by remember(candidateId) { mutableStateOf(false) }
    var chartLoaded by remember(candidateId) { mutableStateOf(false) }

    LaunchedEffect(candidateId) {
        bio = runCatching { identityRepo.getExtendedBio(candidateId) }.getOrNull()
        bioLoaded = true
    }

    LaunchedEffect(candidateId, showAboutMe) {
        if (!showAboutMe) return@LaunchedEffect
        aboutMe = runCatching { identityRepo.getAboutMe(candidateId) }.getOrNull()
        aboutMeLoaded = true
    }

    LaunchedEffect(candidateId) {
        try {
            chart = dashboardRepo.getChart(candidateId)
        } catch (e: ApiError) {
            if (e.status == 404) chartNotComputed = true
        } catch (_: Exception) {
            // Leave both null/false -- an unexpected error here just
            // falls back to the same "not computed yet" copy rather
            // than a dedicated error state for a secondary detail tab.
        }
        chartLoaded = true
    }

    val tabLabels = if (showAboutMe) listOf("About Me", "Bio data", "Chart") else listOf("Bio data", "Chart")

    D2MCard(modifier = modifier) {
        Column {
            D2MTabs(tabLabels, tab, { tab = it })
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                if (showAboutMe) {
                    when (tab) {
                        0 -> AboutMeTab(aboutMe, aboutMeLoaded)
                        1 -> BioDataTab(bio, bioLoaded)
                        else -> ChartTab(chart, chartNotComputed, chartLoaded)
                    }
                } else {
                    when (tab) {
                        0 -> BioDataTab(bio, bioLoaded)
                        else -> ChartTab(chart, chartNotComputed, chartLoaded)
                    }
                }
            }
        }
    }
}

@Composable
private fun BioDataTab(bio: ExtendedBioDataOut?, loaded: Boolean) {
    when {
        !loaded -> Text("Loading…", color = mutedText(0.45f))
        bio == null -> Text("No bio data on file for this profile yet.", color = mutedText(0.45f))
        else -> Column {
            BioSection(
                "Basic & personal",
                listOf(bio.heightCm, bio.complexion, bio.motherTongue, bio.otherLanguages, bio.bodyType),
            ) {
                BioRow("Height", bio.heightCm?.let(::cmToFeetInches))
                BioRow("Body type", bio.bodyType?.let(Taxonomy::toLabel))
                BioRow("Complexion", bio.complexion)
                BioRow("Mother tongue", bio.motherTongue)
                BioRow("Other languages", bio.otherLanguages?.takeIf { it.isNotEmpty() }?.joinToString(", "))
            }
            BioSection(
                "Religious & astrological",
                listOf(bio.religion, bio.casteCommunity, bio.sect, bio.gothram, bio.horoscopeMatchPreference),
            ) {
                BioRow("Religion", bio.religion)
                BioRow("Caste / community", bio.casteCommunity?.let(Taxonomy::toLabel))
                BioRow("Sect", bio.sect)
                BioRow("Gothram", bio.gothram)
                BioRow("Horoscope match preference", bio.horoscopeMatchPreference?.let(Taxonomy::toLabel))
            }
            BioSection(
                "Education & career",
                listOf(bio.highestEducation, bio.institution, bio.occupationTitle, bio.employer, bio.employmentSector, bio.monthlyIncomeAmount),
            ) {
                BioRow("Highest education", bio.highestEducation?.let(Taxonomy::toLabel))
                BioRow("Institution", bio.institution)
                BioRow("Occupation", bio.occupationTitle)
                BioRow("Employer", bio.employer)
                BioRow("Employed in", bio.employmentSector?.let(Taxonomy::toLabel))
                BioRow("Monthly income", bio.monthlyIncomeAmount?.let { "$it ${bio.monthlyIncomeCurrency.orEmpty()}".trim() })
            }
            BioSection(
                "Family background",
                listOf(bio.fatherName, bio.motherName, bio.nativity, bio.familyType, bio.familyValues, bio.financialStatus),
            ) {
                BioRow("Father", listOfNotNull(bio.fatherName, bio.fatherOccupation).joinToString(" -- ").ifBlank { null })
                BioRow("Mother", listOfNotNull(bio.motherName, bio.motherOccupation).joinToString(" -- ").ifBlank { null })
                BioRow("Siblings", siblingsSummary(bio))
                BioRow("Native place", bio.nativity)
                BioRow("Family type", bio.familyType?.let(Taxonomy::toLabel))
                BioRow("Family values", bio.familyValues?.let(Taxonomy::toLabel))
                BioRow("Financial status", bio.financialStatus)
            }
            BioSection("Location & contact", listOf(bio.citizenshipStatus)) {
                BioRow("Citizenship / residing status", bio.citizenshipStatus?.let(Taxonomy::toLabel))
            }
        }
    }
}

@Composable
private fun AboutMeTab(aboutMe: AboutMeDataOut?, loaded: Boolean) {
    when {
        !loaded -> Text("Loading…", color = mutedText(0.45f))
        aboutMe == null -> Text("This profile hasn't filled in About Me yet.", color = mutedText(0.45f))
        else -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            aboutMe.aboutPrompts.forEach { p -> PromptCard(p.prompt, p.answer) }

            if (aboutMe.fitnessRoutine != null || aboutMe.sleepSchedule != null || aboutMe.pets != null || aboutMe.socialEnergy != null) {
                CasualCard {
                    Text("Lifestyle", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = mutedText(0.55f))
                    FlowWrap(modifier = Modifier.padding(top = 12.dp)) {
                        IconBubble("🏃", aboutMe.fitnessRoutine)
                        IconBubble("🌙", aboutMe.sleepSchedule)
                        IconBubble("🐾", aboutMe.pets)
                        IconBubble("🔋", aboutMe.socialEnergy)
                    }
                }
            }

            if (aboutMe.partnerQualities.isNotEmpty()) {
                CasualCard {
                    Text("What I value in a partner", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = mutedText(0.55f))
                    FlowWrap(modifier = Modifier.padding(top = 12.dp)) {
                        aboutMe.partnerQualities.forEach { PlainBubble(it) }
                    }
                }
            }

            if (!aboutMe.whatMattersMost.isNullOrBlank()) {
                PromptCard("What matters most to me", aboutMe.whatMattersMost)
            }

            if (aboutMe.careerAfterMarriage != null || aboutMe.livingArrangement != null || aboutMe.openToRelocation != null) {
                CasualCard {
                    Text("Life & future plans", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = mutedText(0.55f))
                    FlowWrap(modifier = Modifier.padding(top = 12.dp)) {
                        IconBubble("💼", aboutMe.careerAfterMarriage)
                        IconBubble("🏠", aboutMe.livingArrangement)
                        IconBubble("🧳", aboutMe.openToRelocation)
                    }
                }
            }

            if (aboutMe.favoriteCuisine != null || aboutMe.dreamDestination != null || aboutMe.loveLanguage != null) {
                CasualCard {
                    Text("Quick facts", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = mutedText(0.55f))
                    FlowWrap(modifier = Modifier.padding(top = 12.dp)) {
                        IconBubble("🍽️", aboutMe.favoriteCuisine)
                        IconBubble("✈️", aboutMe.dreamDestination)
                        IconBubble("💌", aboutMe.loveLanguage)
                    }
                }
            }
        }
    }
}

// Third cut of this tab: stacking a question-pill directly above an
// answer-pill made both read as the same kind of thing -- "I don't even
// understand what they are" (reported directly), the opposite of the
// distinction that pairing was meant to create. Two pills of the same
// shape don't read as "label, then value" no matter how they're
// arranged. So closed-set/short-fact fields (fitness, sleep schedule,
// cuisine, ...) now drop the separate question pill entirely -- a small
// icon prefixed inside the ONE answer bubble carries the category instead
// ("🏃 Regularly"), the same compact "icon + fact" chip Bumble itself
// uses. Free-text answers (prompts, "what matters most") never had this
// problem the same way -- a sentence doesn't fit in a pill -- but their
// question label was ALSO wrapped in a pill last pass for visual
// consistency; dropped back to plain text here ("for custom answers, I
// don't want bubbles for questions", reported directly), leaving the
// praised-as-nice italic answer (D2MNoteStyle, the comps' own
// "attribution" convention) as the only pill-free voice on this tab.
//
// CasualCard is the soft, borderless, larger-radius container everything
// sits in -- a deliberate, local departure from the rest of the app's
// hairline-border formality, confined to this one section. Mirrors
// d2m_web's ProfileTabsPanel.jsx CasualCard/IconBubble/PlainBubble.
@Composable
private fun CasualCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(18.dp))
            .padding(16.dp),
        content = content,
    )
}

// icon + value in one pill -- no separate label. The icon alone carries
// "what category is this" (a small, muted-adjacent glyph reads as a
// category marker, not as the "childish" full-size emoji sticker the
// very first cut of this tab used).
@Composable
private fun IconBubble(icon: String, value: String?) {
    if (value == null) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .background(d2m.accentSoft, RoundedCornerShape(999.dp))
            .padding(start = 11.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
    ) {
        Text(icon, fontSize = 13.sp)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = d2m.accentStrong)
    }
}

// Plain bubble, no icon -- for values with no natural single glyph
// (partner qualities are abstract adjectives, not things).
@Composable
private fun PlainBubble(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = d2m.accentStrong,
        modifier = Modifier
            .background(d2m.accentSoft, RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowWrap(modifier: Modifier = Modifier, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun PromptCard(prompt: String, answer: String) {
    CasualCard {
        Text(prompt, style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
        Text(answer, style = D2MNoteStyle.copy(fontSize = 17.sp, lineHeight = 24.sp), modifier = Modifier.padding(top = 6.dp))
    }
}

private fun siblingsSummary(bio: ExtendedBioDataOut): String? = listOfNotNull(
    bio.elderBrothersCount?.takeIf { it > 0 }?.let { "$it elder brother${if (it > 1) "s" else ""}" },
    bio.youngerBrothersCount?.takeIf { it > 0 }?.let { "$it younger brother${if (it > 1) "s" else ""}" },
    bio.elderSistersCount?.takeIf { it > 0 }?.let { "$it elder sister${if (it > 1) "s" else ""}" },
    bio.youngerSistersCount?.takeIf { it > 0 }?.let { "$it younger sister${if (it > 1) "s" else ""}" },
).joinToString(", ").ifBlank { null }

/** label:value pairs, skipping anything not set -- an empty section would just be visual noise. */
@Composable
private fun BioSection(title: String, anyValues: List<Any?>, content: @Composable () -> Unit) {
    val hasAny = anyValues.any { v -> v != null && v != "" && (v as? Collection<*>)?.isEmpty() != true }
    if (!hasAny) return
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = mutedText(0.55f))
        Column(modifier = Modifier.padding(top = 6.dp)) { content() }
    }
}

/** label:value pair with a bottom divider -- mirrors BioRow.jsx's own `borderBottom` on every row, not just between sections. */
@Composable
private fun BioRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = mutedText(0.45f))
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = mutedText(0.1f))
    }
}

/**
 * D1/D9 stacked one above the other, not side by side -- AstrologyChartView
 * defaults to `stacked = false` (side by side), which is what web does too
 * ON A WIDE VIEWPORT, but AstrologyChart.jsx only goes side by side once it
 * measures real room for two full-size grids plus a gap (`wrapWidth >=
 * size*2 + GRID_GAP`) and stacks below that threshold -- exactly the
 * "not enough room" case this panel is always in on a phone. Passing
 * `stacked = true` explicitly here reproduces that narrow-viewport
 * fallback unconditionally, since this platform has no bigger breakpoint
 * where side-by-side would ever actually fit.
 */
@Composable
private fun ChartTab(chart: ChartOut?, chartNotComputed: Boolean, loaded: Boolean) {
    when {
        !loaded -> Text("Loading…", color = mutedText(0.45f))
        chartNotComputed || chart == null -> Text("This profile's chart hasn't been computed yet.", color = mutedText(0.45f))
        else -> AstrologyChartView(
            d1 = chart.chartJson["d1"] as? JsonObject,
            d9 = chart.chartJson["d9"] as? JsonObject,
            modifier = Modifier.fillMaxWidth(),
            stacked = true,
        )
    }
}

private fun cmToFeetInches(cm: Int): String {
    val totalInches = kotlin.math.round(cm / 2.54).toInt()
    val feet = totalInches / 12
    val inches = totalInches % 12
    return "$feet'$inches\" ($cm cm)"
}
