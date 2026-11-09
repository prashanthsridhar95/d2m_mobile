package com.d2m.app.ui.screens.shared.wedding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.d2m.app.data.model.WeddingPanchangamDay
import com.d2m.app.data.model.WeddingPanchangamOut
import com.d2m.app.data.model.WeddingPhaseOut
import com.d2m.app.data.model.WeddingPlanOut
import com.d2m.app.data.model.WeddingTaskOut
import com.d2m.app.ui.strings.WeddingStrings
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
val BUCKET_KEYS = listOf("overdue", "week", "month", "later", "nodate", "done")
val VISIBILITY_KEYS = listOf("all", "side", "private")
val TRADITION_KEYS = listOf("", "tamil", "telugu", "kannada", "malayali", "north", "bengali", "marathi", "gujarati", "odia")
val GROUP_KEYS = listOf("phase", "due", "owner", "party")
/** What a new to-do starts with (from a list group's + , a calendar day, or a board lane). */
data class NewPreset(val phase: String = "planning", val due: String? = null, val participants: List<String>? = null, val kind: String = "task")

/** A to-do added from a board lane should land in that lane. */
fun lanePreset(lane: TaskGroup, groupBy: String, today: LocalDate): NewPreset = when (groupBy) {
    "phase" -> NewPreset(phase = lane.key)
    "owner" -> NewPreset(participants = if (lane.key == "shared") listOf("bride", "groom") else listOf(lane.key))
    else -> NewPreset(due = when (lane.key) {
        "week" -> today.plus(DatePeriod(days = 3)).toString()
        "month" -> today.plus(DatePeriod(days = 14)).toString()
        "later" -> today.plus(DatePeriod(days = 60)).toString()
        else -> null
    })
}

val QUICK_KEYS = listOf("mine", "overdue", "week", "joint")
val SCOPE_KEYS = listOf("individual", "couple", "parents", "family", "group")

fun WeddingStrings.scope(k: String) = when (k) {
    "individual" -> scopeIndividual; "couple" -> scopeCouple; "parents" -> scopeParents
    "family" -> scopeFamily; "group" -> scopeGroup; else -> scopeShared
}
fun WeddingStrings.bucket(k: String) = when (k) {
    "overdue" -> bucketOverdue; "week" -> bucketNext7; "month" -> bucketNext30
    "later" -> bucketLater; "nodate" -> bucketNoDate; else -> bucketDone
}
fun WeddingStrings.visibility(k: String) = when (k) { "all" -> visEveryone; "side" -> visMyFamily; else -> visOnlyMe }
fun WeddingStrings.group(k: String) = when (k) { "phase" -> groupPhase; "due" -> groupDue; "party" -> groupParty; else -> groupOwner }
fun WeddingStrings.quick(k: String) = when (k) { "mine" -> quickMine; "overdue" -> quickOverdue; "week" -> quickWeek; else -> quickJoint }
fun WeddingStrings.anchorName(a: String?) = when (a) { "engagement" -> anchorEngagement; "honeymoon" -> anchorHoneymoon; else -> anchorMarriage }

/** "Me" / the person's first name / "<name>'s parents" -- falls back to Bride/Groom when no name is available. */
fun labeler(plan: WeddingPlanOut, s: WeddingStrings): (String) -> String = { role ->
    val bride = plan.names.bride
    val groom = plan.names.groom
    when {
        role == plan.myRole -> s.me
        role == "bride" -> bride ?: s.bride
        role == "groom" -> groom ?: s.groom
        role == "bride_parents" -> if (bride != null) s.parentsOf(bride) else s.brideParents
        role == "groom_parents" -> if (groom != null) s.parentsOf(groom) else s.groomParents
        else -> role
    }
}

/** Everything an avatar pill needs for a role: the viewer's wording ("Me"), the real name (for the initial), the photo. */
data class Who(val label: String, val name: String, val photoUrl: String?, val parents: Boolean)

fun whoFor(plan: WeddingPlanOut, s: WeddingStrings, mediaUrl: (String) -> String): (String) -> Who {
    val label = labeler(plan, s)
    return { role ->
        val bride = role.startsWith("bride")
        val base = (if (bride) plan.names.bride else plan.names.groom) ?: (if (bride) s.bride else s.groom)
        val parents = role.endsWith("_parents")
        val photo = if (parents) null else (if (bride) plan.photos.bride else plan.photos.groom)
        Who(label(role), if (parents) s.parentsOf(base) else base, photo?.let(mediaUrl), parents)
    }
}

/** How a task's owners read in one line: named pills for one or two short names, otherwise a summary phrase. */
fun ownersSummary(participants: List<String>, who: (String) -> Who, s: WeddingStrings): String? {
    val labels = participants.map { who(it).label }
    if (participants.size == 1 || (participants.size == 2 && labels.joinToString("").length <= 16)) return null
    return when {
        participants.size == 4 -> s.everyone
        participants.size == 2 && participants.all { it.endsWith("_parents") } -> s.bothParents
        participants.size == 2 && participants[0].removeSuffix("_parents") == participants[1].removeSuffix("_parents") ->
            s.nameAndParents(labels[participants.indexOfFirst { !it.endsWith("_parents") }])
        else -> s.nPeople(participants.size)
    }
}

/* ------------------------------------------------------------------ dates */

fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
fun String.toDate(): LocalDate = LocalDate.parse(this)
fun daysBetween(a: LocalDate, b: LocalDate): Int = a.daysUntil(b)

fun LocalDate.short(s: WeddingStrings): String = "$day ${s.monthsShort[month.ordinal]}"
fun LocalDate.medium(s: WeddingStrings): String = "$day ${s.monthsShort[month.ordinal]} $year"
fun LocalDate.full(s: WeddingStrings): String = "${s.weekdaysLong[dayOfWeek.ordinal]}, $day ${s.monthsLong[month.ordinal]} $year"
fun monthTitle(year: Int, month: Int, s: WeddingStrings): String = "${s.monthsLong[month - 1]} $year"

fun relDue(t: WeddingTaskOut, today: LocalDate, s: WeddingStrings): String {
    val due = t.dueDate?.toDate() ?: return ""
    if (t.status == "done") return due.short(s)
    val n = daysBetween(today, due)
    return when {
        n == 0 -> s.relToday
        n == 1 -> s.relTomorrow
        n == -1 -> s.relYesterday
        n < 0 -> s.relDaysOverdue(-n)
        n <= 14 -> s.relInDays(n)
        else -> due.short(s)
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

fun buildGroups(tasks: List<WeddingTaskOut>, phases: List<WeddingPhaseOut>, groupBy: String, label: (String) -> String, today: LocalDate, s: WeddingStrings): List<TaskGroup> {
    val defs: List<TaskGroup> = when (groupBy) {
        "due" -> BUCKET_KEYS.map { k -> TaskGroup(k, s.bucket(k), null, tasks.filter { bucketOf(it, today) == k }) }
        "owner" -> ROLES.map { r -> TaskGroup(r, label(r), null, tasks.filter { it.participants.size == 1 && it.participants[0] == r }) } +
            TaskGroup("shared", s.sharedDecisions, null, tasks.filter { it.participants.size > 1 })
        "party" -> tasks.mapNotNull { it.externalParty }.distinct().sorted().map { p ->
            TaskGroup(p, p.replaceFirstChar { c -> c.uppercase() }, null, tasks.filter { it.externalParty == p })
        } + TaskGroup("none", s.noParty, null, tasks.filter { it.externalParty == null })
        else -> phases.map { p -> TaskGroup(p.key, p.label, p.key, tasks.filter { it.phase == p.key }) }
    }
    return defs.filter { it.rows.isNotEmpty() }.map { it.copy(rows = it.rows.sortedWith(byDue)) }
}

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
class PanchangamStore(
    private val fetch: suspend (start: String, end: String) -> WeddingPanchangamOut,
    private val scope: CoroutineScope,
) {
    private val days = HashMap<String, WeddingPanchangamDay>()
    private val asked = HashSet<String>()
    private var version by mutableIntStateOf(0)
    var available by mutableStateOf(true)
        private set

    /** The wedding itself (muhurtham day and the day either side) is a given: no muhoortham / Amavasya / Purnima / chandrashtamam markers there. */
    var quiet by mutableStateOf<Set<String>>(emptySet())

    fun isQuiet(date: String?): Boolean = date != null && date in quiet

    fun get(date: String?): WeddingPanchangamDay? {
        @Suppress("UNUSED_VARIABLE") val subscribe = version
        val d = if (date == null) null else days[date]
        return if (d != null && date in quiet) d.copy(isAmavasya = false, isPurnima = false, isMuhoortham = false, chandrashtamam = emptyList()) else d
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
                runCatching { fetch("${run.first()}-01", lastDay(run.last())) }
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

/* ------------------------------------------------------------------ @mentions */

/** A comment stores @{role} tokens; every client words them for the viewer ("@Me", "@Priya", "@Priya's parents"). */
val MENTION_TOKEN = Regex("""@\{(bride|groom|bride_parents|groom_parents)\}""")
private val MENTION_QUERY = Regex("""(^|\s)@([^\s@]*)$""")

/** The text typed after a trailing "@" (empty right after the @), or null when the draft isn't ending in a mention. */
fun mentionQueryOf(text: String): String? = MENTION_QUERY.find(text)?.groupValues?.get(2)

/** Who can be suggested for [query]: everyone on the plan except me, matched loosely against their display name. */
fun mentionOptions(query: String?, roles: List<String>, label: (String) -> String, me: String): List<String> {
    if (query == null) return emptyList()
    val q = query.replace(" ", "")
    return roles.filter { r -> label(r) != me && (q.isEmpty() || label(r).replace(" ", "").contains(q, ignoreCase = true)) }
}

/** Draft text -> stored text: "@Priya's parents" becomes "@{bride_parents}" (longest names first so "@Priya" can't eat part of it). */
fun toStoredMentions(text: String, picks: List<Pair<String, String>>): String =
    picks.sortedByDescending { it.second.length }.fold(text) { acc, (role, label) -> acc.replace("@$label", "@{$role}") }

/** Stored text -> display text for the viewer, with the character ranges of each mention (for highlighting). */
fun displayMentions(body: String, label: (String) -> String): Pair<String, List<IntRange>> {
    val sb = StringBuilder()
    val spans = mutableListOf<IntRange>()
    var last = 0
    for (m in MENTION_TOKEN.findAll(body)) {
        sb.append(body, last, m.range.first)
        val shown = "@" + label(m.groupValues[1])
        spans += sb.length until sb.length + shown.length
        sb.append(shown)
        last = m.range.last + 1
    }
    sb.append(body, last, body.length)
    return sb.toString() to spans
}
