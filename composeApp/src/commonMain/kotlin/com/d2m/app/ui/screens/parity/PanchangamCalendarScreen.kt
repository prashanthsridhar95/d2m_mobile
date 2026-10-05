package com.d2m.app.ui.screens.parity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.PanchangamDayOut
import com.d2m.app.data.model.PanchangamMonthOut
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.PanchangamRepository
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.SubHeading
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import org.koin.compose.koinInject

/**
 * Mirrors PanchangamDialog's full month/day view (Home's PanchangamCard is
 * the compact summary, shipped in Phase 1 -- this is the fuller calendar,
 * a Phase 4 parity item per plan §8). Month grid (tithi/nakshatra/flags per
 * day) plus the selected day's full detail below it.
 */
@Composable
fun PanchangamCalendarScreen() {
    val identityStore: IdentityStore = koinInject()
    val panchangamRepo: PanchangamRepository = koinInject()
    val identity by identityStore.identity.collectAsState()
    val strings = LocalStrings.current.parity

    var month by remember { mutableStateOf<PanchangamMonthOut?>(null) }
    var selectedDay by remember { mutableStateOf<PanchangamDayOut?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val primaryId = identity.primaryId ?: identity.childPrimaryId

    LaunchedEffect(primaryId) {
        if (primaryId == null) return@LaunchedEffect
        try {
            month = panchangamRepo.getMonth(primaryId)
            selectedDay = panchangamRepo.getDay(primaryId)
        } catch (e: Exception) {
            error = friendlyError(e, strings.errLoadPanchangam)
        }
    }

    D2MTheme(flow = D2MFlow.PARENT) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            PageTitle(strings.panchangamTitle)

            error?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }

            selectedDay?.let { day ->
                D2MCard(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SubHeading(day.date)
                        Text("${strings.tithiLabel}: ${day.tithi.name}  ·  ${strings.nakshatraLabel}: ${day.nakshatra.name}", color = mutedText(0.55f))
                        Text("${strings.yogaLabel}: ${day.yoga.name}  ·  ${strings.karanaLabel}: ${day.karana.name}", color = mutedText(0.55f))
                        Text(
                            "${strings.rahuKalamLabel}: ${day.rahuKalam.start} – ${day.rahuKalam.end}",
                            color = mutedText(0.45f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            month?.let { m ->
                LazyVerticalGrid(columns = GridCells.Fixed(7), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(m.days) { d ->
                        D2MCard {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(d.date.takeLast(2), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                if (d.isChandrashtamam) Text("⚠", style = MaterialTheme.typography.labelSmall)
                                if (d.isMuhoortham) Text("✦", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
