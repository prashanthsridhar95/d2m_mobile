package com.d2m.app.ui.screens.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.DashboardRepository
import com.d2m.app.domain.repository.WeddingRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MButtonSize
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MSegmented
import com.d2m.app.ui.components.MetaText
import com.d2m.app.ui.screens.shared.wedding.*
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m
import org.koin.compose.koinInject

/**
 * Post-union wedding checklist (replaces Discover/Browse once wedding_mode is on) -- mirrors d2m_web's
 * WeddingChecklistScreen: List / Board / Calendar, filters in a bottom sheet, a task sheet with comments and attachments,
 * names and "Me" instead of role labels, and panchangam markers (muhoortham, Purnima, Amavasya, chandrashtamam) on dates.
 */
@Composable
fun WeddingChecklistScreen() {
    val identityStore: IdentityStore = koinInject()
    val dashboardRepo: DashboardRepository = koinInject()
    val identity by identityStore.identity.collectAsState()
    val s = LocalStrings.current.wedding
    val primaryId = if (identity.role == D2MRole.CHILD) identity.primaryId else identity.childPrimaryId
    var unresolved by remember { mutableStateOf(false) }

    // A parent's linked child id may not be cached yet -- resolve it the same way the other shared screens do.
    LaunchedEffect(identity.role, identity.sponsorId, primaryId) {
        if (identity.role == D2MRole.PARENT && primaryId == null) {
            val sponsorId = identity.sponsorId ?: return@LaunchedEffect
            runCatching { dashboardRepo.getSponsorChild(sponsorId) }
                .onSuccess { identityStore.updateChildPrimaryId(it.primaryId) }
                .onFailure { unresolved = true }
        }
    }

    val flow = if (identity.role == D2MRole.CHILD) D2MFlow.CHILD else D2MFlow.PARENT
    D2MTheme(flow = flow) {
        when {
            primaryId != null -> WeddingWorkspace(primaryId)
            unresolved -> Box(Modifier.fillMaxSize().padding(24.dp)) { D2MEmptyState(s.notAvailableTitle, s.notAvailableBody) }
            else -> WeddingSkeleton()
        }
    }
}

@Composable
private fun WeddingWorkspace(primaryId: String) {
    val repo: WeddingRepository = koinInject()
    val scope = rememberCoroutineScope()
    val c = remember(primaryId) { WeddingController(repo, primaryId, scope) }
    val s = LocalStrings.current.wedding
    c.s = s
    LaunchedEffect(c) { c.load() }

    val prefs: WeddingPrefs = koinInject()
    var view by remember { mutableStateOf(prefs.view()) }
    var listGroup by remember { mutableStateOf(prefs.listGroup()) }
    var boardGroup by remember { mutableStateOf(prefs.boardGroup()) }
    LaunchedEffect(view, listGroup, boardGroup) { prefs.save(view, listGroup, boardGroup) }
    var filters by remember { mutableStateOf(Filters()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf<NewPreset?>(null) }
    var dayOpen by remember { mutableStateOf<String?>(null) }
    var filtersOpen by remember { mutableStateOf(false) }
    var datesOpen by remember { mutableStateOf(false) }

    val d = c.data
    if (d == null) {
        val err = c.error
        if (err != null) Column(Modifier.fillMaxSize().padding(16.dp)) { D2MErrorBanner(message = err, onRetry = { c.load() }) } else WeddingSkeleton()
        return
    }

    LaunchedEffect(d.plan.marriageDate) {
        c.pm.quiet = d.plan.marriageDate?.toDate()?.let { m -> setOf(m.plus(DatePeriod(days = -1)).toString(), m.toString(), m.plus(DatePeriod(days = 1)).toString()) }.orEmpty()
    }
    val env = remember(d.plan.myRole, d.plan.names, d.plan.photos, c.pm, s) { WeddingEnv(labeler(d.plan, s), c.pm, today(), s, whoFor(d.plan, s) { c.repo.mediaUrl(it) }) }
    CompositionLocalProvider(LocalWedding provides env) {
      BoxWithConstraints(Modifier.fillMaxSize()) {
        // Tablets and landscape get the web layout: filters in a persistent rail beside the tasks.
        val wide = maxWidth >= 840.dp
        // To-dos and events arrive in one list; events (ceremonies, gatherings) have no status and never count as work.
        val todos = d.tasks.filter { it.kind != "event" }
        val events = d.tasks.filter { it.kind == "event" }
        val filtered = todos.filter { passes(it, filters, env.today) }
        val groupBy = if (view == "board") boardGroup else listGroup
        val pct = if (d.progress.total == 0) 0 else d.progress.done * 100 / d.progress.total
        val toWedding = d.plan.marriageDate?.let { daysBetween(env.today, it.toDate()) }
        val selected = selectedId?.let { id -> d.tasks.firstOrNull { it.id == id } }

        Row(Modifier.fillMaxSize()) {
        if (wide) D2MCard(Modifier.width(300.dp).fillMaxHeight().padding(start = 16.dp, top = 8.dp, bottom = 12.dp)) {
            WeddingFilterContent(filters, { filters = it }, d.phases, todos, filtered.size, Modifier.padding(16.dp))
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            // ---- controls ----
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    D2MSegmented(
                        listOf(s.layoutList, s.layoutBoard, s.layoutCalendar), listOf("list", "board", "calendar").indexOf(view),
                        { view = listOf("list", "board", "calendar")[it] }, Modifier.weight(1f),
                    )
                    if (!wide) D2MButton(
                        if (filters.activeCount > 0) s.filtersButtonCount(filters.activeCount) else s.filtersButton, { filtersOpen = true },
                        variant = if (filters.activeCount > 0) D2MButtonVariant.ACCENT_OUTLINE else D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchBox(filters.q, s.searchPlaceholder, { filters = filters.copy(q = it) }, Modifier.weight(1f))
                    if (view != "calendar") GroupMenu(groupBy, s) { if (view == "board") boardGroup = it else listGroup = it }
                    D2MButton(s.newTaskButton, { selectedId = null; creating = NewPreset() }, size = D2MButtonSize.SM)
                }
                // Summary: progress, countdown, and tappable quick filters.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(s.percentDone(pct), style = MaterialTheme.typography.titleSmall, color = d2m.ink)
                        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(99.dp)).background(d2m.surfaceSunken)) {
                            Box(Modifier.fillMaxWidth(pct / 100f).height(6.dp).background(d2m.accent))
                        }
                        Text(
                            when { toWedding == null -> s.setKeyDatesHint; toWedding >= 0 -> s.daysToWedding(toWedding); else -> s.daysSinceWedding(-toWedding) },
                            style = MaterialTheme.typography.labelMedium, color = d2m.meta,
                            modifier = Modifier.clickable { datesOpen = true },
                        )
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickChip(s.chipOverdue(d.progress.overdue), "overdue" in filters.quick, danger = d.progress.overdue > 0) { filters = filters.toggleQuick("overdue") }
                        QuickChip(s.chipThisWeek(todos.count { bucketOf(it, env.today) == "week" }), "week" in filters.quick) { filters = filters.toggleQuick("week") }
                        QuickChip(s.chipMine(d.progress.myOpen), "mine" in filters.quick) { filters = filters.toggleQuick("mine") }
                        QuickChip(s.keyDates, false) { datesOpen = true }
                    }
                    Text(s.tasksOfTotal(filtered.size, todos.size), style = MaterialTheme.typography.labelSmall, color = d2m.faint)
                }
                c.error?.let { D2MErrorBanner(message = it, onRetry = { c.load() }, inline = true) }
            }

            // ---- content ----
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp)) {
                when {
                    filtered.isEmpty() && view != "calendar" ->
                        D2MEmptyState(s.noTasksTitle, s.noTasksBody, action = { D2MButton(s.clearFilters, { filters = Filters() }, variant = D2MButtonVariant.OUTLINE) })
                    view == "list" -> WeddingListView(
                        buildGroups(filtered, d.phases, groupBy, env.label, env.today, s), c.busy,
                        onOpen = { creating = null; selectedId = it }, onToggle = c::toggle,
                        onAddInGroup = { selectedId = null; creating = NewPreset(phase = it) },
                    )
                    view == "board" -> WeddingBoardView(
                        buildGroups(filtered, d.phases, groupBy, env.label, env.today, s), c.busy,
                        onOpen = { creating = null; selectedId = it }, onToggle = c::toggle,
                        onAddInLane = { lane -> selectedId = null; creating = lanePreset(lane, groupBy, env.today) },
                    )
                    else -> WeddingCalendarView(filtered, events, d.plan, dayOpen, onOpenDay = { selectedId = null; creating = null; dayOpen = it })
                }
            }
        }
        }

        // ---- sheets (one at a time; closing a task returns to its day) ----
        val dayTasks = dayOpen?.let { day -> filtered.filter { it.dueDate == day } }.orEmpty()
        val dayEvents = dayOpen?.let { day -> events.filter { it.dueDate == day } }.orEmpty()
        when {
            creating != null -> NewTaskSheet(
                d.phases, d.plan.myRole, creating!!, c.busy,
                onCreate = { body -> c.create(body) { id -> creating = null; selectedId = id } },
                onDismiss = { creating = null },
            )
            selected != null -> TaskSheet(c, selected, d.phases, onDismiss = { selectedId = null })
            dayOpen != null -> DaySheet(
                dayOpen!!, dayTasks, dayEvents, todos,
                listOf(d.plan.engagementDate to s.engagement, d.plan.marriageDate to s.marriage, d.plan.honeymoonDate to s.honeymoon).firstOrNull { it.first == dayOpen }?.second,
                c.busy, onOpen = { selectedId = it }, onToggle = c::toggle,
                onAdd = { creating = NewPreset(due = it) }, onAddEvent = { creating = NewPreset(due = it, kind = "event") }, onSchedule = { t -> c.setDue(t, dayOpen) }, onDismiss = { dayOpen = null },
            )
        }
        if (filtersOpen && !wide) WeddingFilterSheet(filters, { filters = it }, d.phases, todos, filtered.size, onDismiss = { filtersOpen = false })
        if (datesOpen) KeyDatesDialog(
            d.plan.engagementDate, d.plan.marriageDate, d.plan.honeymoonDate, d.plan.tradition, c.busy,
            onSave = { e, m, h, t -> c.saveDates(e, m, h, t) { datesOpen = false } }, onDismiss = { datesOpen = false },
        )
      }
    }
}

private fun Filters.toggleQuick(k: String) = copy(quick = if (k in quick) quick - k else quick + k)

@Composable
private fun SearchBox(value: String, placeholder: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.border(BorderStroke(1.dp, d2m.borderStrong), RoundedCornerShape(4.dp)).background(d2m.field, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("⌕", color = d2m.faint)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = d2m.faint)
            BasicTextField(value, onChange, singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = d2m.textPrimary), cursorBrush = SolidColor(d2m.accent), modifier = Modifier.fillMaxWidth())
        }
        if (value.isNotEmpty()) Text("✕", color = d2m.meta, modifier = Modifier.clickable { onChange("") })
    }
}

@Composable
private fun GroupMenu(current: String, s: com.d2m.app.ui.strings.WeddingStrings, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        D2MButton(s.groupBy(s.group(current)), { open = true }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM)
        DropdownMenu(open, { open = false }) {
            GROUP_KEYS.forEach { k -> DropdownMenuItem(text = { Text(s.group(k)) }, onClick = { open = false; onPick(k) }) }
        }
    }
}

@Composable
private fun QuickChip(text: String, selected: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    val pal = LocalD2MStatusPalette.current
    val fg = when { selected -> d2m.accent; danger -> pal.danger.fg; else -> d2m.textPrimary }
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = fg,
        modifier = Modifier.clip(RoundedCornerShape(99.dp))
            .background(if (selected) d2m.accentSoft else MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, if (selected) d2m.accent else d2m.border), RoundedCornerShape(99.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
