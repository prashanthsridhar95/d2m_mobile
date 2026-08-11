package com.d2m.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.ChartOut
import com.d2m.app.data.model.ExtendedBioDataOut
import com.d2m.app.data.network.ApiError
import com.d2m.app.domain.repository.DashboardRepository
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.mutedText
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
 * Both fetches now run eagerly on mount (not chart-on-tab-open as before)
 * -- matching web's useCandidateProfile hook, which fetches chart/photos/
 * extendedBio unconditionally the moment a candidateId is known, not
 * lazily per tab. That's also load-bearing for the fixed-height behavior
 * below: pinning the panel's height to the Chart tab's size only works if
 * the chart is already known regardless of which tab happens to be open.
 *
 * Fixed body height, baseline = Chart tab (reported directly: "in web,
 * right pane height is fixed & content change based on tab selection...
 * why nothing is followed as per web?") -- ProfileTabsPanel.jsx's own
 * docstring: "switching tabs changes height. I want the height of the
 * chart tab to be baseline for both tabs." This used to just let each
 * tab's Column size itself naturally, so the whole card visibly grew or
 * shrank on every tab switch. Compose has no ResizeObserver equivalent, so
 * this does the same thing manually: an invisible copy of the Chart tab's
 * content is always laid out (Modifier.alpha(0f) -- still measured, just
 * not painted) so its natural height is captured via onSizeChanged the
 * moment chart data resolves, then that height is applied to whichever
 * tab is actually visible. Bio data is wrapped in its own vertical scroll
 * so it can't overflow that fixed height once pinned -- same as
 * ProfileTabsPanel.jsx's own `overflowY: "auto"` on the tab body.
 */
@Composable
fun D2MProfileTabsPanel(candidateId: String, modifier: Modifier = Modifier) {
    val identityRepo: IdentityRepository = koinInject()
    val dashboardRepo: DashboardRepository = koinInject()
    val density = LocalDensity.current

    var tab by remember(candidateId) { mutableStateOf(0) }
    var bio by remember(candidateId) { mutableStateOf<ExtendedBioDataOut?>(null) }
    var bioLoaded by remember(candidateId) { mutableStateOf(false) }
    var chart by remember(candidateId) { mutableStateOf<ChartOut?>(null) }
    var chartNotComputed by remember(candidateId) { mutableStateOf(false) }
    var chartLoaded by remember(candidateId) { mutableStateOf(false) }
    var bodyHeightPx by remember(candidateId) { mutableStateOf(0) }

    LaunchedEffect(candidateId) {
        bio = runCatching { identityRepo.getExtendedBio(candidateId) }.getOrNull()
        bioLoaded = true
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

    Card(modifier = modifier, shape = RoundedCornerShape(D2MRadius.lg)) {
        Column {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Bio data") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Chart") })
            }

            Box {
                if (chartLoaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .alpha(0f)
                            .onSizeChanged { bodyHeightPx = it.height },
                    ) {
                        ChartTab(chart, chartNotComputed, chartLoaded)
                    }
                }

                val bodyModifier = Modifier
                    .fillMaxWidth()
                    .then(if (bodyHeightPx > 0) Modifier.height(with(density) { bodyHeightPx.toDp() }) else Modifier)
                    .padding(16.dp)

                when (tab) {
                    0 -> Column(modifier = bodyModifier.verticalScroll(rememberScrollState())) {
                        BioDataTab(bio, bioLoaded)
                    }
                    else -> Box(modifier = bodyModifier) {
                        ChartTab(chart, chartNotComputed, chartLoaded)
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
