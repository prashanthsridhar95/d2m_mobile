package com.d2m.app.ui.screens.shared.wedding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.d2m.app.data.model.WeddingActivityOut
import com.d2m.app.data.model.WeddingPhaseOut
import com.d2m.app.data.model.WeddingTaskCreateIn
import com.d2m.app.data.model.WeddingTaskOut
import com.d2m.app.data.network.friendlyError
import com.d2m.app.messaging.ui.rememberMediaAttachLauncher
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonSize
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCheckboxGroup
import com.d2m.app.ui.components.D2MCheckboxRow
import com.d2m.app.ui.components.D2MSegmented
import com.d2m.app.ui.components.D2MSelectField
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.FacetMode
import com.d2m.app.ui.components.FacetState
import com.d2m.app.ui.components.LinkText
import com.d2m.app.ui.components.MetaText
import com.d2m.app.ui.components.SectionHeading
import com.d2m.app.ui.components.rememberPhotoImageLoader
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val MAX_ATTACHMENT_BYTES = 8 * 1024 * 1024

/* ------------------------------------------------------------------ date picking */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickDialog(initial: String?, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.toDate()?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString()) }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state) }
}

/** "Tap to set" date button + the day's panchangam note underneath. */
@Composable
private fun DateField(label: String, value: String?, onChange: (String?) -> Unit, clearable: Boolean = false) {
    var picking by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = d2m.meta)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            D2MButton(value?.toDate()?.medium() ?: "Set date", { picking = true }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM)
            if (clearable && value != null) LinkText("Clear", onClick = { onChange(null) })
        }
        PmNote(value)
    }
    if (picking) DatePickDialog(value, { picking = false }) { picking = false; onChange(it) }
}

/* ------------------------------------------------------------------ task detail */

private fun agoText(iso: String): String {
    val then = runCatching { Instant.parse(iso).toEpochMilliseconds() }.getOrNull() ?: return ""
    val s = (Clock.System.now().toEpochMilliseconds() - then) / 1000
    return when {
        s < 60 -> "just now"
        s < 3600 -> "${s / 60}m ago"
        s < 86400 -> "${s / 3600}h ago"
        s < 604800 -> "${s / 86400}d ago"
        else -> Instant.fromEpochMilliseconds(then).toLocalDateTime(TimeZone.currentSystemDefault()).date.medium()
    }
}

private fun sizeText(n: Int) = if (n < 1024) "$n B" else if (n < 1048576) "${n / 1024} KB" else "${(n * 10 / 1048576) / 10.0} MB"

private fun anchorText(t: WeddingTaskOut): String {
    if (t.dueDateIsCustom) return "Custom date"
    val a = t.anchor ?: "marriage"
    val n = t.offsetDays
    return if (n == 0) "On the $a date" else "${if (n < 0) -n else n} days ${if (n < 0) "before" else "after"} the $a date"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskSheet(c: WeddingController, task: WeddingTaskOut, phases: List<WeddingPhaseOut>, onDismiss: () -> Unit) {
    val env = LocalWedding.current
    val scope = rememberCoroutineScope()
    var activity by remember(task.id) { mutableStateOf<WeddingActivityOut?>(null) }
    var err by remember(task.id) { mutableStateOf<String?>(null) }
    var comment by remember(task.id) { mutableStateOf("") }
    var posting by remember(task.id) { mutableStateOf(false) }
    var uploading by remember(task.id) { mutableStateOf(false) }
    var expandedImage by remember(task.id) { mutableStateOf<String?>(null) }
    var confirmRemove by remember(task.id) { mutableStateOf(false) }
    val loader = rememberPhotoImageLoader()
    val done = task.status == "done"

    LaunchedEffect(task.id) {
        runCatching { c.repo.activity(c.primaryId, task.id) }
            .onSuccess { activity = it; c.updateCounts(task.id, it.comments.size, it.attachments.size) }
            .onFailure { err = friendlyError(it, "Couldn't load comments.") }
    }
    fun sync(a: WeddingActivityOut) { activity = a; c.updateCounts(task.id, a.comments.size, a.attachments.size) }

    val pick = rememberMediaAttachLauncher { m ->
        if (m.bytes.size > MAX_ATTACHMENT_BYTES) { err = "${m.fileName} is over 8 MB."; return@rememberMediaAttachLauncher }
        scope.launch {
            uploading = true; err = null
            runCatching { c.repo.upload(c.primaryId, task.id, m.fileName, m.mime, m.bytes) }
                .onSuccess { f -> activity?.let { sync(it.copy(attachments = it.attachments + f)) } }
                .onFailure { err = friendlyError(it, "Couldn't upload ${m.fileName}.") }
            uploading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, contentColor = d2m.textPrimary,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MetaText(phases.firstOrNull { it.key == task.phase }?.label ?: "Task")
                SectionHeading(task.title)
            }

            // Status: only the assignees can change it (and only the signer can reopen).
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                D2MButton(
                    text = if (done) "✓ Done — reopen" else if (task.participants.size > 1) "Mark as agreed / done" else "Mark as done",
                    onClick = { c.toggle(task) },
                    variant = if (done) D2MButtonVariant.OUTLINE else D2MButtonVariant.SOLID,
                    enabled = !c.busy && task.iAmParticipant && !(done && !task.iConfirmed),
                    modifier = Modifier.fillMaxWidth(),
                )
                when {
                    !task.iAmParticipant -> MetaText("Assigned to ${task.participants.joinToString { env.label(it) }} — only they can mark it done")
                    done && !task.iConfirmed -> MetaText("Signed off by ${task.confirmedBy.joinToString { env.label(it) }}")
                }
            }

            HorizontalDivider(color = d2m.borderSoft)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TypeTag(task)
                task.externalParty?.let { D2MBadge("with $it", D2MBadgeTone.INFO) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Assigned", style = MaterialTheme.typography.labelMedium, color = d2m.meta)
                task.participants.forEach { r ->
                    val signed = r in task.confirmedBy
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(env.label(r) + if (r == task.createdByRole) " · creator" else "", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(if (signed) "signed off" else "pending", style = MaterialTheme.typography.labelMedium, color = if (signed) LocalD2MStatusPalette.current.success.fg else d2m.faint)
                    }
                }
            }
            // Anyone who can see the task may reschedule it.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DateField("Due", task.dueDate, onChange = { c.setDue(task, it) })
                Text(
                    (if (task.dueDate != null) anchorText(task) else "Set the key dates to schedule this"),
                    style = MaterialTheme.typography.labelSmall, color = d2m.faint,
                )
                if (task.dueDateIsCustom && !task.isCustom) LinkText("Reset to calculated date", onClick = { c.setDue(task, null) })
            }

            HorizontalDivider(color = d2m.borderSoft)
            err?.let { Text(it, color = LocalD2MStatusPalette.current.danger.fg, style = MaterialTheme.typography.bodyMedium) }

            // ---- attachments ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Attachments", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    LinkText(if (uploading) "Uploading…" else "+ Add file", onClick = { pick() }, enabled = !uploading)
                }
                val files = activity?.attachments.orEmpty()
                if (files.isEmpty() && !uploading) MetaText("Quotes, photos, PDFs — both families can see them.")
                files.forEach { a ->
                    Column(Modifier.border(BorderStroke(1.dp, d2m.border), RoundedCornerShape(6.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth().clickable(enabled = a.isImage) { expandedImage = if (expandedImage == a.id) null else a.id }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (a.isImage) {
                                AsyncImage(
                                    model = c.repo.attachmentUrl(c.primaryId, task.id, a.id), imageLoader = loader, contentDescription = a.filename,
                                    contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                                )
                            } else {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, d2m.border, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                    Text(a.filename.substringAfterLast('.', "FILE").uppercase().take(4), style = MaterialTheme.typography.labelSmall, color = d2m.meta)
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(a.filename, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text("${sizeText(a.sizeBytes)} · ${env.label(a.uploadedByRole)} · ${agoText(a.createdAt)}", style = MaterialTheme.typography.labelSmall, color = d2m.meta)
                            }
                            if (a.mine) LinkText("Remove", onClick = {
                                scope.launch {
                                    runCatching { c.repo.deleteAttachment(c.primaryId, task.id, a.id) }
                                        .onSuccess { activity?.let { cur -> sync(cur.copy(attachments = cur.attachments.filter { it.id != a.id })) } }
                                        .onFailure { err = friendlyError(it, "Couldn't remove the file.") }
                                }
                            })
                        }
                        if (a.isImage && expandedImage == a.id) {
                            AsyncImage(
                                model = c.repo.attachmentUrl(c.primaryId, task.id, a.id), imageLoader = loader, contentDescription = a.filename,
                                contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)),
                            )
                        }
                    }
                }
                if (files.any { !it.isImage }) MetaText("Open PDFs and documents from the web app.")
            }

            // ---- comments ----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Comments", style = MaterialTheme.typography.titleSmall)
                activity?.legacyNote?.let { note ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).border(1.dp, d2m.borderSoft, RoundedCornerShape(6.dp)).padding(10.dp)) {
                        Text("Earlier note", style = MaterialTheme.typography.labelMedium, color = d2m.meta)
                        Text(note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                val comments = activity?.comments.orEmpty()
                if (activity != null && comments.isEmpty() && activity?.legacyNote == null) MetaText("No comments yet. Start the conversation — both families can see it.")
                comments.forEach { cm ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(env.label(cm.authorRole), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Text(agoText(cm.createdAt), style = MaterialTheme.typography.labelSmall, color = d2m.meta, modifier = Modifier.weight(1f))
                            if (cm.mine) LinkText("Delete", onClick = {
                                scope.launch {
                                    runCatching { c.repo.deleteComment(c.primaryId, task.id, cm.id) }
                                        .onSuccess { activity?.let { cur -> sync(cur.copy(comments = cur.comments.filter { it.id != cm.id })) } }
                                        .onFailure { err = friendlyError(it, "Couldn't delete the comment.") }
                                }
                            })
                        }
                        Text(cm.body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                D2MTextField(
                    label = "", value = comment, onValueChange = { if (it.length <= 2000) comment = it },
                    placeholder = "Write a comment…", singleLine = false, minLines = 2,
                )
                D2MButton(
                    text = if (posting) "Posting…" else "Comment",
                    onClick = {
                        val body = comment.trim()
                        if (body.isEmpty()) return@D2MButton
                        scope.launch {
                            posting = true; err = null
                            runCatching { c.repo.addComment(c.primaryId, task.id, body) }
                                .onSuccess { cm -> activity?.let { sync(it.copy(comments = it.comments + cm)) }; comment = "" }
                                .onFailure { err = friendlyError(it, "Couldn't post the comment.") }
                            posting = false
                        }
                    },
                    enabled = comment.isNotBlank() && !posting, size = D2MButtonSize.SM,
                )
            }

            if (task.canDelete) {
                HorizontalDivider(color = d2m.borderSoft)
                LinkText("Remove task", onClick = { confirmRemove = true })
            }
        }
    }
    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove this task?") }, text = { Text("It disappears for everyone.") },
            confirmButton = { TextButton(onClick = { confirmRemove = false; c.delete(task) { onDismiss() } }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } },
        )
    }
}

/* ------------------------------------------------------------------ day sheet */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySheet(
    date: String, tasks: List<WeddingTaskOut>, milestone: String?, busy: Boolean,
    onOpen: (String) -> Unit, onToggle: (WeddingTaskOut) -> Unit, onAdd: (String) -> Unit, onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface, contentColor = d2m.textPrimary,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                milestone?.let { D2MBadge(it, D2MBadgeTone.GOLD_OUTLINE) }
                SectionHeading(date.toDate().full())
                PmNote(date)
                MetaText(if (tasks.isEmpty()) "Nothing is due this day" else "${tasks.size} task${if (tasks.size == 1) "" else "s"} · ${tasks.count { it.status == "done" }} done")
            }
            Spacer(Modifier.height(8.dp))
            tasks.forEach { t ->
                HorizontalDivider(color = d2m.borderSoft)
                TaskRow(t, busy, onOpen = { onOpen(t.id) }, onToggle = { onToggle(t) })
            }
            Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                D2MButton("+ Add a task on this day", { onAdd(date) }, variant = D2MButtonVariant.OUTLINE, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/* ------------------------------------------------------------------ new task */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskSheet(
    phases: List<WeddingPhaseOut>, myRole: String, presetPhase: String, presetDue: String?, busy: Boolean,
    onCreate: (WeddingTaskCreateIn) -> Unit, onDismiss: () -> Unit,
) {
    val env = LocalWedding.current
    var title by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf(presetPhase) }
    var participants by remember { mutableStateOf(setOf(myRole)) }
    var visibility by remember { mutableStateOf("all") }
    var due by remember { mutableStateOf(presetDue) }
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, contentColor = d2m.textPrimary,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeading("New task")
            D2MTextField("What needs to be done?", title, { if (it.length <= 200) title = it })
            D2MCheckboxGroup(label = "Assign to", first = true) {
                ROLES.forEach { r ->
                    D2MCheckboxRow(env.label(r), r in participants, { on -> participants = if (on) participants + r else participants - r })
                }
                MetaText("Anyone you pick can mark it done.")
            }
            D2MSelectField("Phase", phase, phases.map { it.key }, { if (it.isNotEmpty()) phase = it }, optionLabel = { k -> phases.firstOrNull { it.key == k }?.label ?: k })
            DateField("Due date", due, onChange = { due = it }, clearable = true)
            D2MSelectField("Visible to", visibility, VISIBILITY.map { it.first }, { if (it.isNotEmpty()) visibility = it }, optionLabel = { k -> VISIBILITY.first { it.first == k }.second })
            D2MButton(
                "Create task", { onCreate(WeddingTaskCreateIn(title.trim(), phase, visibility, due, ROLES.filter { it in participants })) },
                enabled = title.isNotBlank() && participants.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/* ------------------------------------------------------------------ key dates */

@Composable
fun KeyDatesDialog(
    engagement: String?, marriage: String?, honeymoon: String?, tradition: String?, busy: Boolean,
    onSave: (String?, String?, String?, String?) -> Unit, onDismiss: () -> Unit,
) {
    val env = LocalWedding.current
    var e by remember { mutableStateOf(engagement) }
    var m by remember { mutableStateOf(marriage) }
    var h by remember { mutableStateOf(honeymoon) }
    var t by remember { mutableStateOf(tradition ?: "") }
    LaunchedEffect(e, m, h) { env.pm.ensure(listOf(e, m, h)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Key dates") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetaText("Every task's due date is worked out from these. Change one and the whole plan reschedules.")
                DateField("Engagement", e, { e = it }, clearable = true)
                DateField("Marriage", m, { m = it }, clearable = true)
                DateField("Honeymoon", h, { h = it }, clearable = true)
                D2MSelectField("Tradition", t, TRADITIONS.map { it.first }.filter { it.isNotEmpty() }, { t = it }, optionLabel = { k -> TRADITIONS.first { it.first == k }.second }, placeholderWhenEmpty = TRADITIONS[0].second)
                MetaText("Engagement must be on or before the marriage, and the honeymoon on or after it.")
            }
        },
        confirmButton = { TextButton(onClick = { onSave(e, m, h, t) }, enabled = !busy) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/* ------------------------------------------------------------------ filters */

@Composable
private fun Facet(title: String, options: List<Pair<String, String>>, counts: Map<String, Int>, state: FacetState, first: Boolean = false, onChange: (FacetState) -> Unit) {
    if (options.isEmpty()) return
    D2MCheckboxGroup(
        label = title, first = first,
        trailing = { if (state.isActive) LinkText("Clear", onClick = { onChange(state.copy(values = emptySet())) }) },
    ) {
        if (state.isActive) {
            D2MSegmented(
                options = listOf("Include", "Exclude"), selectedIndex = if (state.mode == FacetMode.EXCLUDE) 1 else 0,
                onSelect = { onChange(state.copy(mode = if (it == 1) FacetMode.EXCLUDE else FacetMode.INCLUDE)) },
                accentActive = true, small = true, modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        options.forEach { (key, label) ->
            val n = counts[key] ?: 0
            D2MCheckboxRow(label, key in state.values, { onChange(state.toggled(key)) }, count = n, dimmed = n == 0 && key !in state.values)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingFilterSheet(
    filters: Filters, onChange: (Filters) -> Unit, phases: List<WeddingPhaseOut>,
    tasks: List<WeddingTaskOut>, resultCount: Int, onDismiss: () -> Unit,
) {
    val env = LocalWedding.current
    val today = env.today
    // Faceted counts: each option is counted over the tasks that pass every OTHER filter.
    fun count(skip: String, values: (WeddingTaskOut) -> List<String>): Map<String, Int> {
        val out = HashMap<String, Int>()
        tasks.forEach { t -> if (passes(t, filters, today, skip)) values(t).forEach { out[it] = (out[it] ?: 0) + 1 } }
        return out
    }
    val owners = count("owners") { it.participants }
    val scope = count("scope") { listOf(it.scope) }
    val phase = count("phase") { listOf(it.phase) }
    val status = count("status") { listOf(it.status) }
    val quick = HashMap<String, Int>().also { q -> tasks.forEach { t -> if (passes(t, filters, today, "quick")) QUICK.forEach { (k, _) -> if (quickMatches(k, t, today)) q[k] = (q[k] ?: 0) + 1 } } }

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, contentColor = d2m.textPrimary,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeading("Filters", Modifier.weight(1f))
                LinkText("Clear all", onClick = { onChange(Filters(q = filters.q)) }, enabled = filters.quick.isNotEmpty() || listOf(filters.owners, filters.scope, filters.phase, filters.status).any { it.isActive })
            }
            MetaText("$resultCount of ${tasks.size} tasks match", Modifier.padding(bottom = 6.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                D2MCheckboxGroup(label = "Quick views", first = true, trailing = { if (filters.quick.isNotEmpty()) LinkText("Clear", onClick = { onChange(filters.copy(quick = emptySet())) }) }) {
                    QUICK.forEach { (k, label) ->
                        val n = quick[k] ?: 0
                        D2MCheckboxRow(label, k in filters.quick, { on -> onChange(filters.copy(quick = if (on) filters.quick + k else filters.quick - k)) }, count = n, dimmed = n == 0 && k !in filters.quick)
                    }
                }
                Facet("Assigned to", ROLES.map { it to env.label(it) }, owners, filters.owners) { onChange(filters.copy(owners = it)) }
                Facet("Type", SCOPE_LABEL.filterKeys { it != "mixed" }.toList(), scope, filters.scope) { onChange(filters.copy(scope = it)) }
                Facet("Phase", phases.map { it.key to it.label }, phase, filters.phase) { onChange(filters.copy(phase = it)) }
                Facet("Status", listOf("todo" to "To do", "done" to "Done"), status, filters.status) { onChange(filters.copy(status = it)) }
            }
            Spacer(Modifier.height(12.dp))
            D2MButton("Show $resultCount tasks", onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
