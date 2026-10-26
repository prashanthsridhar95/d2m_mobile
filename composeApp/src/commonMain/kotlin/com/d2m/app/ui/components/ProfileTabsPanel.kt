package com.d2m.app.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.data.model.AboutMeDataOut
import com.d2m.app.data.model.ChartOut
import com.d2m.app.data.model.ExtendedBioDataOut
import com.d2m.app.data.network.ApiError
import com.d2m.app.domain.repository.DashboardRepository
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.ui.strings.LocalAppLocale
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.strings.SharedComponentsStrings
import com.d2m.app.ui.theme.d2mNoteStyle
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
fun D2MProfileTabsPanel(candidateId: String, modifier: Modifier = Modifier, showAboutMe: Boolean = false, viewerPrimaryId: String? = null, hideAboutPrompts: Boolean = false) {
    val strings = LocalStrings.current.sharedComponents
    val identityRepo: IdentityRepository = koinInject()
    val dashboardRepo: DashboardRepository = koinInject()

    var tab by remember(candidateId, showAboutMe) { mutableStateOf(0) }
    // Seeded from the cache so a prefetched profile paints on the first
    // frame instead of flashing "Loading"; the effects below still
    // revalidate through the same cache.
    val cachedBio = remember(candidateId) { identityRepo.peekExtendedBio(candidateId) }
    val cachedAboutMe = remember(candidateId, showAboutMe) { if (showAboutMe) identityRepo.peekAboutMe(candidateId) else null }
    val cachedChart = remember(candidateId) { dashboardRepo.peekChart(candidateId) }
    var bio by remember(candidateId) { mutableStateOf(cachedBio) }
    var bioLoaded by remember(candidateId) { mutableStateOf(cachedBio != null) }
    var aboutMe by remember(candidateId) { mutableStateOf(cachedAboutMe) }
    var aboutMeLoaded by remember(candidateId) { mutableStateOf(cachedAboutMe != null) }
    var chart by remember(candidateId) { mutableStateOf(cachedChart) }
    var chartNotComputed by remember(candidateId) { mutableStateOf(false) }
    var chartLoaded by remember(candidateId) { mutableStateOf(cachedChart != null) }

    // The viewer's own About Me/Bio data -- About Me's Lifestyle/Life &
    // future plans/Quick facts bubbles have no saved preference to check
    // against (no one states a "preferred sleep schedule" for a partner),
    // so the only comparison that exists is the viewer's own value for
    // that same field against the candidate's -- see traitMatch below.
    // Only ever read when showAboutMe (the About Me tab exists), but
    // fetched the same unconditional way as candidateId's own bio/aboutMe
    // above when a viewerPrimaryId is actually known.
    var ownBio by remember(viewerPrimaryId) { mutableStateOf<ExtendedBioDataOut?>(null) }
    var ownAboutMe by remember(viewerPrimaryId) { mutableStateOf<AboutMeDataOut?>(null) }

    LaunchedEffect(candidateId) {
        bio = runCatching { identityRepo.getExtendedBio(candidateId) }.getOrNull()
        bioLoaded = true
    }

    LaunchedEffect(candidateId, showAboutMe) {
        if (!showAboutMe) return@LaunchedEffect
        aboutMe = runCatching { identityRepo.getAboutMe(candidateId) }.getOrNull()
        aboutMeLoaded = true
    }

    LaunchedEffect(viewerPrimaryId, showAboutMe) {
        if (!showAboutMe || viewerPrimaryId == null) return@LaunchedEffect
        ownBio = runCatching { identityRepo.getExtendedBio(viewerPrimaryId) }.getOrNull()
        ownAboutMe = runCatching { identityRepo.getAboutMe(viewerPrimaryId) }.getOrNull()
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

    // NOTE: an earlier pass merged About Me + Bio data into one "About"
    // tab here -- reverted ("the earlier flow of having biodata, about
    // me, chart was just fine. The change was asked of only in the
    // settings", reported directly). This panel (Discover/
    // ProfileDetailScreen/PreviewProfileScreen -- viewing a candidate,
    // including yourself in preview) keeps the original 3 tabs; the
    // Settings self-editing dialog is the only place that tab layout
    // actually changed -- see ChildProfileDialogScreen.kt.
    val tabLabels = if (showAboutMe) listOf(strings.tabAboutMe, strings.tabBioData, strings.tabChart) else listOf(strings.tabBioData, strings.tabChart)

    D2MCard(modifier = modifier) {
        Column {
            D2MTabs(tabLabels, tab, { tab = it })
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                if (showAboutMe) {
                    when (tab) {
                        0 -> AboutMeTab(aboutMe, aboutMeLoaded, bio, ownAboutMe, ownBio?.childrenPreference, hideAboutPrompts)
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
    val strings = LocalStrings.current.sharedComponents
    val locale = LocalAppLocale.current
    when {
        !loaded -> Text(strings.loading, color = mutedText(0.45f))
        bio == null -> Text(strings.noBioDataYet, color = mutedText(0.45f))
        else -> Column {
            BioSection(
                strings.sectionBasicPersonal,
                listOf(bio.heightCm, bio.complexion, bio.motherTongue, bio.otherLanguages, bio.bodyType),
            ) {
                BioRow(strings.rowHeight, bio.heightCm?.let(::cmToFeetInches))
                BioRow(strings.rowBodyType, bio.bodyType?.let { Taxonomy.toLabel(it, locale) })
                BioRow(strings.rowComplexion, bio.complexion)
                BioRow(strings.rowMotherTongue, bio.motherTongue)
                BioRow(strings.rowOtherLanguages, bio.otherLanguages?.takeIf { it.isNotEmpty() }?.joinToString(", "))
            }
            BioSection(
                strings.sectionReligiousAstrological,
                listOf(bio.religion, bio.casteCommunity, bio.sect, bio.gothram, bio.horoscopeMatchPreference),
            ) {
                BioRow(strings.rowReligion, bio.religion)
                BioRow(strings.rowCasteCommunity, bio.casteCommunity?.let { Taxonomy.toLabel(it, locale) })
                BioRow(strings.rowSect, bio.sect)
                BioRow(strings.rowGothram, bio.gothram)
                BioRow(strings.rowHoroscopeMatchPreference, bio.horoscopeMatchPreference?.let { Taxonomy.toLabel(it, locale) })
            }
            BioSection(
                strings.sectionEducationCareer,
                listOf(bio.highestEducation, bio.institution, bio.occupationTitle, bio.employer, bio.monthlyIncomeAmount, bio.incomeBand),
            ) {
                BioRow(strings.rowHighestEducation, bio.highestEducation?.let { Taxonomy.toLabel(it, locale) })
                BioRow(strings.rowInstitution, bio.institution)
                BioRow(strings.rowOccupation, bio.occupationTitle)
                BioRow(strings.rowEmployer, bio.employer)
                // "What is 'Employed in'? remove that" (reported directly)
                // -- employmentSector dropped from this display; still an
                // editable field in the Bio data form (and still stored on
                // the backend), just no longer shown here.
                BioRow(strings.rowMonthlyIncome, bio.monthlyIncomeAmount?.let { "$it ${bio.monthlyIncomeCurrency.orEmpty()}".trim() })
                // "Instead of salary field - provide salary range"
                // (reported directly) -- income_band is the replacement
                // display field; monthlyIncomeAmount above stays (no
                // annual_income row ever existed here to remove).
                BioRow(strings.rowIncomeBand, bio.incomeBand?.let { Taxonomy.toLabel(it, locale) })
            }
            BioSection(
                strings.sectionFamilyBackground,
                listOf(bio.fatherName, bio.motherName, bio.nativity, bio.familyType, bio.familyValues, bio.financialStatus),
            ) {
                BioRow(strings.rowFather, listOfNotNull(bio.fatherName, bio.fatherOccupation).joinToString(" -- ").ifBlank { null })
                BioRow(strings.rowMother, listOfNotNull(bio.motherName, bio.motherOccupation).joinToString(" -- ").ifBlank { null })
                BioRow(strings.rowSiblings, siblingsSummary(bio, strings))
                BioRow(strings.rowNativePlace, bio.nativity)
                BioRow(strings.rowFamilyType, bio.familyType?.let { Taxonomy.toLabel(it, locale) })
                BioRow(strings.rowFamilyValues, bio.familyValues?.let { Taxonomy.toLabel(it, locale) })
                BioRow(strings.rowFinancialStatus, bio.financialStatus)
            }
            BioSection(strings.sectionLocationContact, listOf(bio.citizenshipStatus)) {
                BioRow(strings.rowCitizenshipResiding, bio.citizenshipStatus?.let { Taxonomy.toLabel(it, locale) })
            }
        }
    }
}

// Own value vs candidate value -- see D2MProfileTabsPanel's ownBio/
// ownAboutMe doc comment for why this is the only comparison these
// fields support (no saved preference exists for any of them). `null`
// (own value unset, candidate value unset, or either input missing)
// means "nothing to compare," same undefined-means-no-indicator contract
// MatchIndicator already relies on below.
private fun traitMatch(ownValue: String?, candidateValue: String?): Boolean? {
    if (ownValue.isNullOrEmpty() || candidateValue.isNullOrEmpty()) return null
    return ownValue == candidateValue
}

@Composable
private fun AboutMeTab(
    aboutMe: AboutMeDataOut?,
    loaded: Boolean,
    bio: ExtendedBioDataOut? = null,
    ownAboutMe: AboutMeDataOut? = null,
    ownChildrenPreference: String? = null,
    hidePrompts: Boolean = false,
) {
    val strings = LocalStrings.current.sharedComponents
    val cpd = LocalStrings.current.childProfileDialog
    val locale = LocalAppLocale.current
    when {
        !loaded -> Text(strings.loading, color = mutedText(0.45f))
        aboutMe == null -> Text(strings.noAboutMeYet, color = mutedText(0.45f))
        else -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // The Discover feed already shows (and lets you like) each prompt, so it asks for them to be left out here.
            if (!hidePrompts) aboutMe.aboutPrompts.forEach { p -> PromptCard(p.prompt, p.answer) }

            if (aboutMe.fitnessRoutine != null || aboutMe.sleepSchedule != null || aboutMe.pets != null || aboutMe.socialEnergy != null) {
                CasualCard {
                    Text(strings.lifestyleTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = mutedText(0.55f))
                    FlowWrap(modifier = Modifier.padding(top = 12.dp)) {
                        IconBubble("🏃", cpd.fitnessRoutine, aboutMe.fitnessRoutine, traitMatch(ownAboutMe?.fitnessRoutine, aboutMe.fitnessRoutine))
                        IconBubble("🌙", cpd.sleepSchedule, aboutMe.sleepSchedule, traitMatch(ownAboutMe?.sleepSchedule, aboutMe.sleepSchedule))
                        IconBubble("🐾", cpd.pets, aboutMe.pets, traitMatch(ownAboutMe?.pets, aboutMe.pets))
                        IconBubble("🔋", cpd.socialEnergy, aboutMe.socialEnergy, traitMatch(ownAboutMe?.socialEnergy, aboutMe.socialEnergy))
                    }
                }
            }

            if (aboutMe.partnerQualities.isNotEmpty()) {
                CasualCard {
                    Text(strings.whatIValueInPartner, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = mutedText(0.55f))
                    FlowWrap(modifier = Modifier.padding(top = 12.dp)) {
                        aboutMe.partnerQualities.forEach { PlainBubble(it) }
                    }
                }
            }

            if (!aboutMe.whatMattersMost.isNullOrBlank()) {
                PromptCard(strings.whatMattersMostToMe, aboutMe.whatMattersMost)
            }

            // "In future plans, children related question is not seen"
            // (reported directly) -- childrenPreference lives on
            // ExtendedBioData (it's the actual edited/scored field, see
            // the Bio data tab), but reads thematically as a life/future-
            // plans fact, so it's surfaced here too from the same bio
            // param this tab now takes, alongside career/living/relocation.
            if (aboutMe.careerAfterMarriage != null || aboutMe.livingArrangement != null || aboutMe.openToRelocation != null || bio?.childrenPreference != null) {
                CasualCard {
                    Text(strings.lifeAndFuturePlansTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = mutedText(0.55f))
                    FlowWrap(modifier = Modifier.padding(top = 12.dp)) {
                        IconBubble("💼", cpd.careerAfterMarriage, aboutMe.careerAfterMarriage, traitMatch(ownAboutMe?.careerAfterMarriage, aboutMe.careerAfterMarriage))
                        IconBubble("🏠", cpd.livingArrangement, aboutMe.livingArrangement, traitMatch(ownAboutMe?.livingArrangement, aboutMe.livingArrangement))
                        IconBubble("🧳", cpd.openToRelocation, aboutMe.openToRelocation, traitMatch(ownAboutMe?.openToRelocation, aboutMe.openToRelocation))
                        IconBubble("👶", cpd.childrenPreference, bio?.childrenPreference?.let { Taxonomy.toLabel(it, locale) }, traitMatch(ownChildrenPreference, bio?.childrenPreference))
                    }
                }
            }

            if (aboutMe.favoriteCuisine != null || aboutMe.dreamDestination != null || aboutMe.loveLanguage != null) {
                CasualCard {
                    Text(strings.quickFactsTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = mutedText(0.55f))
                    FlowWrap(modifier = Modifier.padding(top = 12.dp)) {
                        IconBubble("🍽️", cpd.favoriteCuisine, aboutMe.favoriteCuisine, traitMatch(ownAboutMe?.favoriteCuisine, aboutMe.favoriteCuisine))
                        IconBubble("✈️", cpd.dreamDestination, aboutMe.dreamDestination, traitMatch(ownAboutMe?.dreamDestination, aboutMe.dreamDestination))
                        IconBubble("💌", cpd.loveLanguage, aboutMe.loveLanguage, traitMatch(ownAboutMe?.loveLanguage, aboutMe.loveLanguage))
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
private fun IconBubble(icon: String, label: String, value: String?, match: Boolean? = null) {
    if (value == null) return
    val palette = LocalD2MStatusPalette.current
    val strings = LocalStrings.current.sharedComponents
    // Tapping the pill explains it, like the web's hover bubble: the field's name, and -- when it was compared with the
    // viewer's own answer -- the verdict, in the verdict's own green/red ("Sleep schedule: you both picked the same answer").
    val tip = when (match) {
        true -> strings.matchTooltipYesShared(label)
        false -> strings.matchTooltipNoShared(label)
        null -> label
    }
    TapTip(
        text = tip,
        container = when (match) { true -> palette.success.fg; false -> palette.danger.fg; null -> null },
        content = if (match != null) MaterialTheme.colorScheme.background else null,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier
                .background(d2m.accentSoft, RoundedCornerShape(999.dp))
                .padding(start = 11.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        ) {
            Text(icon, fontSize = 13.sp)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = d2m.accentStrong)
            MatchMark(match)
        }
    }
}

// Small green-check/red-X circle inside a pill -- a quiet mark only (soft tinted bg + coloured icon, as on web); the pill
// it sits in is what you tap, so the mark itself never has to be a tiny target or make the pill bigger.
@Composable
private fun MatchMark(match: Boolean?, size: Dp = 13.dp) {
    if (match == null) return
    val palette = LocalD2MStatusPalette.current
    val circleColor = if (match) palette.success.bg else palette.danger.bg
    val strokeColor = if (match) palette.success.fg else palette.danger.fg
    Canvas(modifier = Modifier.size(size)) {
        drawCircle(color = circleColor, radius = this.size.minDimension / 2f, center = center)
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = this.size.minDimension * 0.11f
        if (match) {
            val path = Path().apply {
                moveTo(w * 0.292f, h * 0.5125f)
                lineTo(w * 0.425f, h * 0.6458f)
                lineTo(w * 0.708f, h * 0.3583f)
            }
            drawPath(path, color = strokeColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            drawLine(strokeColor, Offset(w * 0.3f, h * 0.3f), Offset(w * 0.7f, h * 0.7f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            drawLine(strokeColor, Offset(w * 0.7f, h * 0.3f), Offset(w * 0.3f, h * 0.7f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }
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
        // Same style as the Discover feed's prompt answers, so every answer in the app reads in one voice.
        Text(answer, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 6.dp))
    }
}

private fun siblingsSummary(bio: ExtendedBioDataOut, strings: SharedComponentsStrings): String? = listOfNotNull(
    bio.elderBrothersCount?.takeIf { it > 0 }?.let(strings::elderBrothersCount),
    bio.youngerBrothersCount?.takeIf { it > 0 }?.let(strings::youngerBrothersCount),
    bio.elderSistersCount?.takeIf { it > 0 }?.let(strings::elderSistersCount),
    bio.youngerSistersCount?.takeIf { it > 0 }?.let(strings::youngerSistersCount),
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

/**
 * label:value pair with a bottom divider -- mirrors BioRow.jsx's own `borderBottom` on every row, not just between
 * sections. Short values sit beside the label (label gets at most ~40% of the width and always a 16dp gutter, so a
 * long label can never run into its value); long or list-like values drop below the label instead of squeezing into
 * a narrow right-aligned column that wraps one word per line.
 */
@Composable
private fun BioRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    val stacked = value.length > 26 || label.length + value.length > 40
    Column {
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = mutedText(0.5f))
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = mutedText(0.5f), modifier = Modifier.weight(0.4f))
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.weight(0.6f),
                )
            }
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
    val strings = LocalStrings.current.sharedComponents
    when {
        !loaded -> Text(strings.loading, color = mutedText(0.45f))
        chartNotComputed || chart == null -> Text(strings.chartNotComputedYet, color = mutedText(0.45f))
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
