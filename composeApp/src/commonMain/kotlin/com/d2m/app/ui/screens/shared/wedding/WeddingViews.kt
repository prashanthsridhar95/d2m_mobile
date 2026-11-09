package com.d2m.app.ui.screens.shared.wedding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d2m.app.data.model.WeddingPlanOut
import com.d2m.app.data.model.WeddingTaskOut
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonSize
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.LinkText
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/* ------------------------------------------------------------------ rows */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskRow(task: WeddingTaskOut, busy: Boolean, onOpen: () -> Unit, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CheckCircle(task, busy, onToggle, Modifier.padding(top = 1.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                task.title, style = MaterialTheme.typography.bodyLarge,
                color = if (task.status == "done") d2m.faint else d2m.textPrimary,
                textDecoration = if (task.status == "done") TextDecoration.LineThrough else null,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                TypeTag(task)
                Avatars(task)
                Counts(task)
                DueChip(task)
            }
        }
    }
}

/* ------------------------------------------------------------------ list */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeddingListView(
    groups: List<TaskGroup>, busy: Boolean,
    onOpen: (String) -> Unit, onToggle: (WeddingTaskOut) -> Unit, onAddInGroup: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var collapsed by remember { mutableStateOf(setOf<String>()) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        groups.forEach { g ->
            val isCollapsed = g.key in collapsed
            stickyHeader(key = "h:${g.key}") {
                Row(
                    Modifier.fillMaxWidth().background(d2m.surfaceSunken)
                        .clickable { collapsed = if (isCollapsed) collapsed - g.key else collapsed + g.key }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(if (isCollapsed) "▸" else "▾", color = d2m.meta)
                    Text(g.label, style = MaterialTheme.typography.titleSmall, color = d2m.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val done = g.rows.count { it.status == "done" }
                    Text("$done/${g.rows.size}", style = MaterialTheme.typography.labelMedium, color = d2m.meta)
                    g.phase?.let { LinkText("+ Add", onClick = { onAddInGroup(it) }) }
                }
            }
            if (!isCollapsed) items(g.rows, key = { it.id }) { t ->
                TaskRow(t, busy, onOpen = { onOpen(t.id) }, onToggle = { onToggle(t) })
                androidx.compose.material3.HorizontalDivider(color = d2m.borderSoft)
            }
        }
    }
}

/* ------------------------------------------------------------------ board */

/** Swimlanes side by side: the board scrolls sideways, each lane keeps its header and scrolls only up and down. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeddingBoardView(
    lanes: List<TaskGroup>, busy: Boolean,
    onOpen: (String) -> Unit, onToggle: (WeddingTaskOut) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(lanes, key = { it.key }) { lane ->
            D2MCard(Modifier.width(284.dp).fillMaxHeight()) {
                Row(
                    Modifier.fillMaxWidth().background(d2m.surfaceSunken).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(lane.label, style = MaterialTheme.typography.titleSmall, color = if (lane.key == "overdue") LocalD2MStatusPalette.current.danger.fg else d2m.ink, modifier = Modifier.weight(1f))
                    Text("${lane.rows.count { it.status == "done" }}/${lane.rows.size}", style = MaterialTheme.typography.labelMedium, color = d2m.meta)
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lane.rows.take(80), key = { it.id }) { t ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).border(BorderStroke(1.dp, d2m.border), RoundedCornerShape(6.dp))
                                .clickable { onOpen(t.id) }.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                CheckCircle(t, busy, { onToggle(t) })
                                Text(
                                    t.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                                    color = if (t.status == "done") d2m.faint else d2m.textPrimary,
                                    textDecoration = if (t.status == "done") TextDecoration.LineThrough else null,
                                )
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                                TypeTag(t); Avatars(t, named = false); Counts(t); DueChip(t)
                            }
                        }
                    }
                    if (lane.rows.size > 80) item { Text("+${lane.rows.size - 80} more — narrow it with filters", style = MaterialTheme.typography.labelMedium, color = d2m.meta) }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ calendar */

private val DOW = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** One month that fits the screen: rows share the height, each day shows its panchangam markers and a dot per task. Tap a day for its list. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeddingCalendarView(
    tasks: List<WeddingTaskOut>, plan: WeddingPlanOut, openDay: String?,
    onOpenDay: (String) -> Unit, modifier: Modifier = Modifier,
) {
    val env = LocalWedding.current
    val p = LocalD2MStatusPalette.current
    val today = env.today
    val seed = (plan.marriageDate ?: plan.engagementDate)?.toDate() ?: today
    var year by remember { mutableStateOf(seed.year) }
    var month by remember { mutableStateOf(seed.month.ordinal + 1) }
    val first = LocalDate(year, month, 1)
    val startOffset = first.dayOfWeek.ordinal // Monday = 0
    val daysInMonth = first.plus(DatePeriod(months = 1)).let { daysBetween(first, it) }
    val weeks = (startOffset + daysInMonth + 6) / 7
    val gridStart = first.plus(DatePeriod(days = -startOffset))
    val byDay = remember(tasks) { tasks.filter { it.dueDate != null }.groupBy { it.dueDate!! } }
    val milestones = mapOf(plan.engagementDate to "Eng", plan.marriageDate to "Wed", plan.honeymoonDate to "Hon")
    val inMonth = tasks.count { it.dueDate?.startsWith("${year}-${month.toString().padStart(2, '0')}") == true }
    val undated = tasks.count { it.dueDate == null }

    LaunchedEffect(year, month) {
        env.pm.ensure(listOf(gridStart.toString(), first.toString(), gridStart.plus(DatePeriod(days = weeks * 7 - 1)).toString()))
    }
    fun go(n: Int) { val d = first.plus(DatePeriod(months = n)); year = d.year; month = d.month.ordinal + 1 }

    Column(modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            D2MButton("‹", { go(-1) }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM)
            Text(monthTitle(year, month), style = MaterialTheme.typography.titleMedium, color = d2m.ink, modifier = Modifier.weight(1f), maxLines = 1)
            D2MButton("›", { go(1) }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM)
            D2MButton("Today", { year = today.year; month = today.month.ordinal + 1 }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM)
            plan.marriageDate?.let { m -> D2MButton("Wedding", { val d = m.toDate(); year = d.year; month = d.month.ordinal + 1 }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM) }
        }
        Text(
            "$inMonth task${if (inMonth == 1) "" else "s"} due this month" + if (undated > 0) " · $undated without a date" else "",
            style = MaterialTheme.typography.labelMedium, color = d2m.meta,
        )
        if (env.pm.available) CalendarLegend()
        Row(Modifier.fillMaxWidth()) {
            DOW.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = d2m.label, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(8.dp)).border(BorderStroke(1.dp, d2m.border), RoundedCornerShape(8.dp))) {
            repeat(weeks) { w ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    repeat(7) { dIdx ->
                        val date = gridStart.plus(DatePeriod(days = w * 7 + dIdx))
                        val key = date.toString()
                        val list = byDay[key].orEmpty()
                        val pmd = env.pm.get(key)
                        val ch = pmd != null && pmd.chandrashtamam.isNotEmpty()
                        val outside = date.month.ordinal + 1 != month
                        val ms = milestones[key]
                        val bg = when {
                            ms != null -> d2m.goldSoft
                            ch -> p.danger.bg
                            outside -> d2m.surfaceSunken
                            else -> MaterialTheme.colorScheme.surface
                        }
                        Column(
                            Modifier.weight(1f).fillMaxHeight().background(bg)
                                .border(BorderStroke(0.5.dp, if (key == openDay) d2m.accent else d2m.borderSoft))
                                .clickable { onOpenDay(key) }.padding(3.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                val isToday = date == today
                                Text(
                                    date.day.toString(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                    color = when { isToday -> d2m.accentOn; ch -> p.danger.fg; outside -> d2m.faint; else -> d2m.textPrimary },
                                    modifier = if (isToday) Modifier.clip(CircleShape).background(d2m.accent).padding(horizontal = 5.dp) else Modifier,
                                )
                                PmBadges(key)
                            }
                            if (ms != null) Text(ms, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = d2m.goldStrong)
                            if (list.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp), maxLines = 2) {
                                    list.take(6).forEach { t ->
                                        Box(Modifier.size(7.dp).clip(CircleShape).background(when { t.status == "done" -> p.success.fg; t.overdue -> p.danger.fg; else -> d2m.accent }))
                                    }
                                }
                                if (list.size > 6) Text("+${list.size - 6}", fontSize = 8.sp, color = d2m.meta)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarLegend() {
    val p = LocalD2MStatusPalette.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        @Composable fun item(content: @Composable () -> Unit, label: String) =
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { content(); Text(label, style = MaterialTheme.typography.labelSmall, color = d2m.meta) }
        item({ Text("★", color = p.success.fg, fontSize = 11.sp) }, "Muhoortham")
        item({ Box(Modifier.size(9.dp).clip(CircleShape).background(p.warning.fg)) }, "Purnima")
        item({ Box(Modifier.size(9.dp).clip(CircleShape).background(d2m.meta)) }, "Amavasya")
        item({ Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(p.danger.bg).border(1.dp, p.danger.fg, RoundedCornerShape(3.dp))) }, "Chandrashtamam")
    }
}
