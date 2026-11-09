package com.d2m.app.ui.screens.shared.wedding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.d2m.app.data.model.WeddingPanchangamDay
import com.d2m.app.data.model.WeddingPhaseOut
import com.d2m.app.data.model.WeddingPlanOut
import com.d2m.app.data.model.WeddingTaskOut
import com.d2m.app.domain.repository.WeddingRepository
import com.d2m.app.ui.components.FacetMode
import com.d2m.app.ui.components.FacetState
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/* ------------------------------------------------------------------ vocabulary */

val ROLES = listOf("bride", "groom", "bride_parents", "groom_parents")
val ROLE_SHORT = mapOf("bride" to "B", "groom" to "G", "bride_parents" to "BP", "groom_parents" to "GP")
val SCOPE_LABEL = mapOf(
    "individual" to "Individual", "couple" to "Bride & groom", "parents" to "Both parents",
    "family" to "Within family", "group" to "All four", "mixed" to "Shared",
)
val VISIBILITY = listOf("all" to "Everyone", "side" to "My family only", "private" to "Only me")
val TRADITIONS = listOf(
    "" to "All Brahmin traditions", "tamil" to "Tamil (Iyer / Iyengar)", "telugu" to "Telugu",
    "kannada" to "Kannada (Smarta / Madhwa)", "malayali" to "Malayali (Nambudiri / Iyer)",
    "north" to "North Indian", "bengali" to "Bengali", "marathi" to "Marathi", "gujarati" to "Gujarati", "odia" to "Odia",
)
val BUCKETS = listOf(
    "overdue" to "Overdue", "week" to "Next 7 days", "month" to "Next 30 days", "later" to "Later", "nodate" to "No date", "done" to "Done",
)
val GROUP_OPTIONS = listOf("phase" to "Phase", "due" to "Due date", "owner" to "Owner")

/** "Me" / the person's first name / "<name>'s parents" -- falls back to Bride/Groom when no name is available. */
fun labeler(plan: WeddingPlanOut): (String) -> String = { role ->
    val bride = plan.names.bride
    val groom = plan.names.groom
    when {
        role == plan.myRole -> "Me"
        role == "bride" -> bride ?: "Bride"
        role == "groom" -> groom ?: "Groom"
        role == "bride_parents" -> if (bride != null) "$bride's parents" else "Bride's parents"
        role == "groom_parents" -> if (groom != null) "$groom's parents" else "Groom's parents"
        else -> role
    }
}

/* ------------------------------------------------------------------ dates */

fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
fun String.toDate(): LocalDate = LocalDate.parse(this)
fun daysBetween(a: LocalDate, b: LocalDate): Int = a.daysUntil(b)

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val MONTHS_LONG = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
private val WEEKDAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
fun LocalDate.short(): String = "$day ${MONTHS[month.ordinal]}"
fun LocalDate.medium(): String = "$day ${MONTHS[month.ordinal]} $year"
fun LocalDate.full(): String = "${WEEKDAYS[dayOfWeek.ordinal]}, $day ${MONTHS_LONG[month.ordinal]} $year"
fun monthTitle(year: Int, month: Int): String = "${MONTHS_LONG[month - 1]} $year"

fun relDue(t: WeddingTaskOut, today: LocalDate): String {
    val due = t.dueDate?.toDate() ?: return ""
    if (t.status == "done") return due.short()
    val n = daysBetween(today, due)
    return when {
        n == 0 -> "Today"
        n == 1 -> "Tomorrow"
        n == -1 -> "Yesterday"
        n < 0 -> "${-n}d overdue"
        n <= 14 -> "In $n days"
        else -> due.short()
    }
}

fun bucketOf(t: WeddingTaskOut, today: LocalDate): String {
    if (t.status == "done") return "done"
    val due = t.dueDate ?: return "nodate"
    val n = daysBetween(today, due.toDate())
    return when {
        n < 0 -> "overdue"
        n <= 7 -> "week"
        n <= 30 -> "month"
        else -> "later"
    }
}

/* ------------------------------------------------------------------ grouping & filtering */

data class TaskGroup(val key: String, val label: String, val phase: String?, val rows: List<WeddingTaskOut>)

private val byDue = compareBy<WeddingTaskOut>({ it.dueDate ?: "9999" }, { it.title })

fun buildGroups(tasks: List<WeddingTaskOut>, phases: List<WeddingPhaseOut>, groupBy: String, label: (String) -> String, today: LocalDate): List<TaskGroup> {
    val defs: List<TaskGroup> = when (groupBy) {
        "due" -> BUCKETS.map { (k, l) -> TaskGroup(k, l, null, tasks.filter { bucketOf(it, today) == k }) }
        "owner" -> ROLES.map { r -> TaskGroup(r, label(r), null, tasks.filter { it.participants.size == 1 && it.participants[0] == r }) } +
            TaskGroup("shared", "Shared decisions", null, tasks.filter { it.participants.size > 1 })
        else -> phases.map { p -> TaskGroup(p.key, p.label, p.key, tasks.filter { it.phase == p.key }) }
    }
    return defs.filter { it.rows.isNotEmpty() }.map { it.copy(rows = it.rows.sortedWith(byDue)) }
}

val QUICK = listOf("mine" to "My open tasks", "overdue" to "Overdue", "week" to "Due in the next 7 days", "joint" to "Joint decisions")

data class Filters(
    val q: String = "",
    val quick: Set<String> = emptySet(),
    val owners: FacetState = FacetState(),
    val scope: FacetState = FacetState(),
    val phase: FacetState = FacetState(),
    val status: FacetState = FacetState(),
) {
    val activeCount: Int get() = quick.size + listOf(owners, scope, phase, status).count { it.isActive } + (if (q.isNotBlank()) 1 else 0)
}

fun quickMatches(key: String, t: WeddingTaskOut, today: LocalDate): Boolean = when (key) {
    "mine" -> t.iAmParticipant && t.status != "done"
    "overdue" -> t.overdue && t.status != "done"
    "week" -> bucketOf(t, today) == "week"
    "joint" -> t.participants.size > 1
    else -> true
}

/** skip: leave one facet out (used to compute faceted counts: each option is counted over tasks passing every OTHER filter). */
fun passes(t: WeddingTaskOut, f: Filters, today: LocalDate, skip: String? = null): Boolean {
    if (skip != "q" && f.q.isNotBlank()) {
        val x = f.q.trim().lowercase()
        if (!t.title.lowercase().contains(x)) return false
    }
    if (skip != "quick" && !f.quick.all { quickMatches(it, t, today) }) return false
    if (skip != "owners" && !f.owners.allowsAny(t.participants)) return false
    if (skip != "scope" && !f.scope.allows(t.scope)) return false
    if (skip != "phase" && !f.phase.allows(t.phase)) return false
    if (skip != "status" && !f.status.allows(t.status)) return false
    return true
}

fun FacetState.withMode(mode: FacetMode) = copy(mode = mode)

/* ------------------------------------------------------------------ panchangam cache */

/** Lazy, per-month cache of the couple's panchangam flags; reading [get] subscribes the caller to updates. */
class PanchangamStore(private val repo: WeddingRepository, private val primaryId: String, private val scope: CoroutineScope) {
    private val days = HashMap<String, WeddingPanchangamDay>()
    private val asked = HashSet<String>()
    private var version by mutableIntStateOf(0)
    var available by mutableStateOf(true)
        private set

    fun get(date: String?): WeddingPanchangamDay? {
        @Suppress("UNUSED_VARIABLE") val subscribe = version
        return if (date == null) null else days[date]
    }

    fun ensure(dates: Collection<String?>) {
        val months = dates.filterNotNull().map { it.take(7) }.toSet().filter { it !in asked }.sorted()
        if (months.isEmpty()) return
        months.forEach { asked.add(it) }
        // contiguous months go out as one request (the endpoint allows up to 400 days)
        val runs = mutableListOf<MutableList<String>>()
        for (m in months) {
            val run = runs.lastOrNull()
            if (run != null && nextMonth(run.last()) == m && run.size < 12) run.add(m) else runs.add(mutableListOf(m))
        }
        for (run in runs) {
            scope.launch {
                runCatching { repo.panchangam(primaryId, "${run.first()}-01", lastDay(run.last())) }
                    .onSuccess { r ->
                        if (!r.available) available = false else { r.days.forEach { days[it.date] = it }; version++ }
                    }
                    .onFailure { run.forEach { asked.remove(it) } }
            }
        }
    }

    private fun nextMonth(ym: String): String {
        val d = LocalDate(ym.take(4).toInt(), ym.takeLast(2).toInt(), 1).plus(DatePeriod(months = 1))
        return "${d.year}-${(d.month.ordinal + 1).toString().padStart(2, '0')}"
    }

    private fun lastDay(ym: String): String =
        LocalDate(ym.take(4).toInt(), ym.takeLast(2).toInt(), 1).plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).toString()
}
