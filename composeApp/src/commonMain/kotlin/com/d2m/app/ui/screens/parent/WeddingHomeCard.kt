package com.d2m.app.ui.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.WeddingOut
import com.d2m.app.domain.repository.WeddingRepository
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.LinkText
import com.d2m.app.ui.components.MetaText
import com.d2m.app.ui.screens.shared.wedding.daysBetween
import com.d2m.app.ui.screens.shared.wedding.medium
import com.d2m.app.ui.screens.shared.wedding.relDue
import com.d2m.app.ui.screens.shared.wedding.today
import com.d2m.app.ui.screens.shared.wedding.toDate
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.d2m
import org.koin.compose.koinInject

/** Parent home once the union is confirmed: countdown, progress and what is next, in place of the matching cards. */
@Composable
fun WeddingHomeCard(primaryId: String, onOpenChecklist: () -> Unit) {
    val repo: WeddingRepository = koinInject()
    val s = LocalStrings.current.wedding
    var data by remember(primaryId) { mutableStateOf<WeddingOut?>(null) }
    LaunchedEffect(primaryId) { runCatching { repo.get(primaryId) }.onSuccess { data = it } }

    D2MCard(Modifier.fillMaxWidth().clickable { onOpenChecklist() }, accented = true) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(s.homeEyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = d2m.accent)
            LinkText(s.openChecklist + " \u2192", onClick = onOpenChecklist)
        }
        val d = data
        if (d == null) { Box(Modifier.fillMaxWidth().height(64.dp)) ; return@D2MCard }
        val today = today()
        val days = d.plan.marriageDate?.toDate()?.let { daysBetween(today, it) }
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (days == null) Text(s.homeSetDates, style = MaterialTheme.typography.titleMedium, color = d2m.ink)
            else {
                Text(kotlin.math.abs(days).toString(), style = MaterialTheme.typography.displaySmall, color = d2m.ink)
                MetaText((if (days < 0) s.daysSinceWedding(-days) else s.daysToWedding(days)) + " \u00b7 " + d.plan.marriageDate!!.toDate().medium(s))
            }
        }
        val pct = if (d.progress.total == 0) 0f else d.progress.done.toFloat() / d.progress.total
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(d2m.surfaceSunken)) {
                Box(Modifier.fillMaxWidth(pct).fillMaxHeight().clip(RoundedCornerShape(99.dp)).background(d2m.accent))
            }
            MetaText(s.progressOf(d.progress.done, d.progress.total))
        }
        val next = d.tasks.filter { it.status != "done" }.sortedWith(compareBy({ it.dueDate ?: "9999" }, { it.title })).take(3)
        if (next.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = d2m.borderSoft)
            Text(s.nextUp.uppercase(), style = MaterialTheme.typography.labelSmall, color = d2m.meta)
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                next.forEach { t ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(t.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(relDue(t, today, s), style = MaterialTheme.typography.labelMedium, color = d2m.meta)
                    }
                }
            }
        }
    }
}
