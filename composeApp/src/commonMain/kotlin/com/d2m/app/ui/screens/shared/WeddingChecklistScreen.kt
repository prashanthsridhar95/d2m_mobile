package com.d2m.app.ui.screens.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.WeddingDatesIn
import com.d2m.app.data.model.WeddingOut
import com.d2m.app.data.model.WeddingTaskCreateIn
import com.d2m.app.data.model.WeddingTaskOut
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.DashboardRepository
import com.d2m.app.domain.repository.WeddingRepository
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonSize
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MCheckboxRow
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MSelectField
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val ROLE_LABEL = mapOf(
    "bride" to "Bride", "groom" to "Groom",
    "bride_parents" to "Bride's parents", "groom_parents" to "Groom's parents",
)
private val SCOPE_LABEL = mapOf(
    "individual" to "Individual", "couple" to "Bride & groom", "parents" to "Both parents",
    "family" to "Within family", "group" to "All four", "mixed" to "Shared",
)
private val VISIBILITY_LABEL = mapOf("all" to "Everyone", "side" to "My side only", "private" to "Only me")
private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")

/** Mirrors screens/shared/WeddingChecklistScreen.jsx -- post-union checklist that replaces Discover/Browse. */
@Composable
fun WeddingChecklistScreen() {
    val identityStore: IdentityStore = koinInject()
    val repo: WeddingRepository = koinInject()
    val dashboardRepo: DashboardRepository = koinInject()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    var data by remember { mutableStateOf<WeddingOut?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryTick by remember { mutableStateOf(0) }
    var busyTaskId by remember { mutableStateOf<String?>(null) }
    var mineOnly by remember { mutableStateOf(false) }
    var openOnly by remember { mutableStateOf(false) }
    val collapsed = remember { mutableStateOf(setOf<String>()) }

    var engagement by remember { mutableStateOf("") }
    var marriage by remember { mutableStateOf("") }
    var honeymoon by remember { mutableStateOf("") }
    var savingDates by remember { mutableStateOf(false) }

    var newTitle by remember { mutableStateOf("") }
    var newVisibility by remember { mutableStateOf("all") }
    var adding by remember { mutableStateOf(false) }

    val primaryId = if (identity.role == D2MRole.CHILD) identity.primaryId else identity.childPrimaryId

    fun apply(out: WeddingOut) {
        data = out
        engagement = out.plan.engagementDate.orEmpty()
        marriage = out.plan.marriageDate.orEmpty()
        honeymoon = out.plan.honeymoonDate.orEmpty()
    }

    LaunchedEffect(identity.role, identity.sponsorId, primaryId, retryTick) {
        var pid = primaryId
        if (pid == null && identity.role == D2MRole.PARENT) {
            val sponsorId = identity.sponsorId ?: return@LaunchedEffect
            runCatching { dashboardRepo.getSponsorChild(sponsorId) }
                .onSuccess { identityStore.updateChildPrimaryId(it.primaryId); pid = it.primaryId }
        }
        if (pid == null) { loading = false; return@LaunchedEffect }
        loading = true
        error = null
        try {
            apply(repo.get(pid!!))
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load the wedding checklist.")
        } finally {
            loading = false
        }
    }

    fun mutate(taskId: String? = null, block: suspend (String) -> Unit) {
        val pid = primaryId ?: return
        scope.launch {
            busyTaskId = taskId
            try {
                block(pid)
                apply(repo.get(pid))
                error = null
            } catch (e: Exception) {
                error = friendlyError(e, "That didn't work. Please try again.")
            } finally {
                busyTaskId = null
            }
        }
    }

    val flow = if (identity.role == D2MRole.CHILD) D2MFlow.CHILD else D2MFlow.PARENT
    D2MTheme(flow = flow) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PageTitle("Wedding checklist")
                    Text(
                        "Everything to do, decide and confirm together for the big day.",
                        style = MaterialTheme.typography.bodyMedium, color = mutedText(),
                    )
                }
            }

            val d = data
            if (error != null) item(key = "error") {
                D2MErrorBanner(message = error!!, onRetry = { retryTick++ }, inline = d != null)
            }
            if (loading && d == null) item(key = "loading") { Text("Loading…", color = mutedText()) }
            if (d == null) return@LazyColumn

            item(key = "dates") {
                D2MCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Key dates", style = MaterialTheme.typography.titleMedium, color = d2m.ink)
                        Text(
                            "Due dates for every task follow these. Use YYYY-MM-DD.",
                            style = MaterialTheme.typography.bodySmall, color = mutedText(),
                        )
                        D2MTextField("Engagement", engagement, { engagement = it }, placeholder = "2027-03-01")
                        D2MTextField("Marriage", marriage, { marriage = it }, placeholder = "2027-09-12")
                        D2MTextField("Honeymoon", honeymoon, { honeymoon = it }, placeholder = "2027-09-20")
                        val valid = listOf(engagement, marriage, honeymoon).all { it.isBlank() || ISO_DATE.matches(it) }
                        D2MButton(
                            text = if (savingDates) "Saving…" else "Save dates",
                            enabled = valid && !savingDates,
                            onClick = {
                                savingDates = true
                                mutate {
                                    repo.setDates(it, WeddingDatesIn(
                                        engagementDate = engagement.ifBlank { null },
                                        marriageDate = marriage.ifBlank { null },
                                        honeymoonDate = honeymoon.ifBlank { null },
                                    ))
                                }
                                savingDates = false
                            },
                        )
                    }
                }
            }

            item(key = "progress") {
                val p = d.progress
                D2MCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${p.done} of ${p.total} done", style = MaterialTheme.typography.titleMedium, color = d2m.ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            D2MBadge("${p.myOpen} for you", D2MBadgeTone.INFO)
                            if (p.overdue > 0) D2MBadge("${p.overdue} overdue", D2MBadgeTone.DANGER)
                            if (p.waitingOnOthers > 0) D2MBadge("${p.waitingOnOthers} waiting on others", D2MBadgeTone.WARNING)
                        }
                        D2MCheckboxRow("Only tasks I'm part of", mineOnly, { mineOnly = it })
                        D2MCheckboxRow("Hide completed", openOnly, { openOnly = it })
                    }
                }
            }

            item(key = "add") {
                D2MCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Add your own task", style = MaterialTheme.typography.titleMedium, color = d2m.ink)
                        D2MTextField("Task", newTitle, { newTitle = it })
                        D2MSelectField(
                            label = "Visible to", value = newVisibility,
                            options = VISIBILITY_LABEL.keys.toList(), onValueChange = { newVisibility = it },
                            optionLabel = { VISIBILITY_LABEL[it] ?: it },
                        )
                        D2MButton(
                            text = if (adding) "Adding…" else "Add task",
                            enabled = newTitle.isNotBlank() && !adding,
                            onClick = {
                                adding = true
                                val title = newTitle.trim()
                                mutate { repo.createTask(it, WeddingTaskCreateIn(title = title, visibility = newVisibility)) }
                                newTitle = ""
                                adding = false
                            },
                        )
                    }
                }
            }

            val visible = d.tasks.filter { (!mineOnly || it.iAmParticipant) && (!openOnly || it.status != "done") }
            if (visible.isEmpty()) item(key = "empty") {
                D2MEmptyState(title = "Nothing to show", subtitle = "Try clearing the filters.")
            }
            d.phases.forEach { phase ->
                val rows = visible.filter { it.phase == phase.key }
                if (rows.isEmpty()) return@forEach
                val isCollapsed = phase.key in collapsed.value
                item(key = "phase-${phase.key}") {
                    D2MButton(
                        text = "${phase.label} (${rows.count { it.status == "done" }}/${rows.size}) ${if (isCollapsed) "▸" else "▾"}",
                        variant = D2MButtonVariant.GHOST, size = D2MButtonSize.SM,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            collapsed.value = if (isCollapsed) collapsed.value - phase.key else collapsed.value + phase.key
                        },
                    )
                }
                if (!isCollapsed) rows.forEach { task ->
                    item(key = "task-${task.id}") {
                        TaskCard(
                            task = task,
                            busy = busyTaskId == task.id,
                            onToggle = { mutate(task.id) { pid -> repo.setConfirmed(pid, task.id, !task.iConfirmed) } },
                            onDelete = { mutate(task.id) { pid -> repo.deleteTask(pid, task.id) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: WeddingTaskOut, busy: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    D2MCard {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (task.iAmParticipant) {
                D2MCheckboxRow(label = task.title, checked = task.iConfirmed, onCheckedChange = { onToggle() }, enabled = !busy)
            } else {
                Text(task.title, style = MaterialTheme.typography.bodyMedium, color = d2m.textPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                D2MBadge(SCOPE_LABEL[task.scope] ?: task.scope)
                if (task.status == "done") D2MBadge("Done", D2MBadgeTone.SUCCESS)
                else if (task.overdue) D2MBadge("Overdue", D2MBadgeTone.DANGER)
                task.dueDate?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = mutedText()) }
            }
            if (task.participants.size > 1 || !task.iAmParticipant) {
                Text(
                    "With: " + task.participants.joinToString { ROLE_LABEL[it] ?: it } +
                        if (task.status != "done" && task.waitingOn.isNotEmpty() && task.confirmedBy.isNotEmpty())
                            " · waiting on " + task.waitingOn.joinToString { ROLE_LABEL[it] ?: it } else "",
                    style = MaterialTheme.typography.bodySmall, color = mutedText(),
                )
            }
            if (!task.notes.isNullOrBlank()) {
                Text(task.notes, style = MaterialTheme.typography.bodySmall, color = mutedText())
            }
            if (task.canDelete) {
                D2MButton("Remove", onClick = onDelete, variant = D2MButtonVariant.GHOST, size = D2MButtonSize.SM, enabled = !busy)
            }
        }
    }
}
