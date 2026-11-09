package com.d2m.app.ui.screens.shared.wedding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
            TaskTags(task)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                OwnerPills(task)
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
    val s = LocalWedding.current.s
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
                    g.phase?.let { LinkText(s.addInGroup, onClick = { onAddInGroup(it) }) }
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
    onOpen: (String) -> Unit, onToggle: (WeddingTaskOut) -> Unit, onAddInLane: (TaskGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalWedding.current.s
    LazyRow(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        items(lanes, key = { it.key }) { lane ->
            D2MCard(Modifier.width(340.dp).fillMaxHeight()) {
                Row(
                    Modifier.fillMaxWidth().background(d2m.surfaceSunken).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(lane.label, style = MaterialTheme.typography.titleSmall, color = if (lane.key == "overdue") LocalD2MStatusPalette.current.danger.fg else d2m.ink, modifier = Modifier.weight(1f))
                    Text("${lane.rows.count { it.status == "done" }}/${lane.rows.size}", style = MaterialTheme.typography.labelMedium, color = d2m.meta)
                    if (lane.key != "done") {
                        Box(
                            Modifier.padding(start = 10.dp).size(28.dp).clip(CircleShape).border(BorderStroke(1.dp, d2m.borderStrong), CircleShape)
                                .clickable { onAddInLane(lane) }.semantics { contentDescription = s.laneAdd(lane.label) },
                            contentAlignment = Alignment.Center,
                        ) { Text("+", style = MaterialTheme.typography.titleMedium, color = d2m.ink) }
                    }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(lane.rows.take(80), key = { it.id }) { t ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).border(BorderStroke(1.dp, d2m.border), RoundedCornerShape(6.dp))
                                .clickable { onOpen(t.id) }.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                                CheckCircle(t, busy, { onToggle(t) })
                                Text(
                                    t.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                                    color = if (t.status == "done") d2m.faint else d2m.textPrimary,
                                    textDecoration = if (t.status == "done") TextDecoration.LineThrough else null,
                                )
                            }
                            TaskTags(t)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                                OwnerPills(t); DueChip(t)
                            }
                        }
                    }
                    if (lane.rows.size > 80) item { Text(s.moreNarrow(lane.rows.size - 80), style = MaterialTheme.typography.labelMedium, color = d2m.meta) }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ calendar */

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
    val s = env.s
    // A Brahmin wedding runs about two and a half days: the day before and after the muhurtham day are the Marriage too.
    val marriageBlock = plan.marriageDate?.toDate()?.let { m -> listOf(m.plus(DatePeriod(days = -1)).toString() to s.weddingShort, m.plus(DatePeriod(days = 1)).toString() to s.weddingShort) }.orEmpty().toMap()
    val milestones: Map<String?, String> = marriageBlock + mapOf(plan.engagementDate to s.engagementShort, plan.marriageDate to s.weddingShort, plan.honeymoonDate to s.honeymoonShort)
    val inMonth = tasks.count { it.dueDate?.startsWith("${year}-${month.toString().padStart(2, '0')}") == true }
    val undated = tasks.count { it.dueDate == null }

    LaunchedEffect(year, month) {
        env.pm.ensure(listOf(gridStart.toString(), first.toString(), gridStart.plus(DatePeriod(days = weeks * 7 - 1)).toString()))
    }
    fun go(n: Int) { val d = first.plus(DatePeriod(months = n)); year = d.year; month = d.month.ordinal + 1 }

    Column(modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            D2MButton("‹", { go(-1) }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM)
            Text(monthTitle(year, month, s), style = MaterialTheme.typography.titleMedium, color = d2m.ink, modifier = Modifier.weight(1f), maxLines = 1)
            D2MButton("›", { go(1) }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM)
            D2MButton(s.calToday, { year = today.year; month = today.month.ordinal + 1 }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM)
            plan.marriageDate?.let { m -> D2MButton(s.calWedding, { val d = m.toDate(); year = d.year; month = d.month.ordinal + 1 }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM) }
        }
        Text(
            s.dueThisMonth(inMonth) + if (undated > 0) " · " + s.withoutDate(undated) else "",
            style = MaterialTheme.typography.labelMedium, color = d2m.meta,
        )
        if (env.pm.available) CalendarLegend()
        Row(Modifier.fillMaxWidth()) {
            s.weekdaysShort.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = d2m.label, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(bottom = 8.dp)) {
          val chipMode = maxWidth / 7 >= 88.dp
          Column(Modifier.fillMaxSize()) {
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
                        val isToday = date == today
                        val selectedDay = key == openDay
                        val tile = RoundedCornerShape(8.dp)
                        // Separated rounded tiles like the panchangam calendar. Today = accent outline + filled number; the open day = soft accent wash.
                        val bg = when {
                            selectedDay -> d2m.accentSoft
                            outside -> androidx.compose.ui.graphics.Color.Transparent
                            ms != null -> d2m.goldSoft
                            ch -> p.danger.bg
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val outline = when {
                            isToday -> BorderStroke(2.dp, d2m.accent)
                            outside -> BorderStroke(0.dp, androidx.compose.ui.graphics.Color.Transparent)
                            selectedDay -> BorderStroke(1.dp, d2m.accentBorder)
                            else -> BorderStroke(1.dp, d2m.borderSoft)
                        }
                        Column(
                            Modifier.weight(1f).fillMaxHeight().padding(2.dp).clip(tile).background(bg).border(outline, tile)
                                .clickable { onOpenDay(key) }.padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    date.day.toString(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                    color = when { isToday -> d2m.accentOn; ch -> p.danger.fg; outside -> d2m.faint; else -> d2m.textPrimary },
                                    modifier = if (isToday) Modifier.clip(CircleShape).background(d2m.accent).padding(horizontal = 5.dp) else Modifier,
                                )
                                PmBadges(key)
                            }
                            if (ms != null) Text(ms, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = d2m.goldStrong)
                            if (list.isNotEmpty()) {
                                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                                    if (chipMode) {
                                        // Roomy cell (tablet / landscape): task titles as chips, as many as fit, then "+N".
                                        val capacity = (maxHeight / 17.dp).toInt().coerceAtLeast(1)
                                        val fit = list.size <= capacity
                                        val shown = if (fit) list else list.take((capacity - 1).coerceAtLeast(0))
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            shown.forEach { t ->
                                                Text(
                                                    t.title, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                    color = when { t.status == "done" -> p.success.fg; t.overdue -> p.danger.fg; else -> d2m.textPrimary },
                                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                                                        .background(when { t.status == "done" -> p.success.bg; t.overdue -> p.danger.bg; else -> d2m.accentSoft })
                                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                                )
                                            }
                                            if (!fit) Text("+${list.size - shown.size}", fontSize = 9.sp, color = d2m.meta)
                                        }
                                    } else {
                                        Column {
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
          }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarLegend() {
    val p = LocalD2MStatusPalette.current
    val s = LocalWedding.current.s
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        @Composable fun item(content: @Composable () -> Unit, label: String) =
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { content(); Text(label, style = MaterialTheme.typography.labelSmall, color = d2m.meta) }
        item({ Text("★", color = p.success.fg, fontSize = 11.sp) }, s.pmMuhoortham)
        item({ Box(Modifier.size(9.dp).clip(CircleShape).background(p.warning.fg)) }, s.pmPurnima)
        item({ Box(Modifier.size(10.dp).clip(CircleShape).background(d2m.ink).border(1.5.dp, d2m.meta, CircleShape)) }, s.pmAmavasya)
        item({ Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(p.danger.bg).border(1.dp, p.danger.fg, RoundedCornerShape(3.dp))) }, s.pmChandrashtamam)
    }
}
