package com.d2m.app.ui.screens.shared.wedding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.d2m.app.data.model.WeddingActivityOut
import com.d2m.app.data.model.WeddingPhaseOut
import com.d2m.app.data.model.WeddingTaskCreateIn
import com.d2m.app.data.model.WeddingTaskOut
import com.d2m.app.data.network.friendlyError
import com.d2m.app.ui.strings.WeddingStrings
import com.d2m.app.messaging.ui.rememberMediaAttachLauncher
import com.d2m.app.platform.openDocument
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
import com.d2m.app.ui.components.LabelText
import com.d2m.app.ui.components.LinkText
import com.d2m.app.ui.components.MetaText
import com.d2m.app.ui.components.SectionHeading
import com.d2m.app.ui.components.rememberPhotoImageLoader
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val MAX_ATTACHMENT_BYTES = 8 * 1024 * 1024

/* ------------------------------------------------------------------ date picking */

@Composable
fun DatePickDialog(initial: String?, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val env = LocalWedding.current
    val s = env.s
    val p = LocalD2MStatusPalette.current
    val today = env.today
    val chosen = initial?.toDate()
    val seed = chosen ?: today
    var year by remember { mutableStateOf(seed.year) }
    var month by remember { mutableStateOf(seed.month.ordinal + 1) }
    val first = LocalDate(year, month, 1)
    val startOffset = first.dayOfWeek.ordinal
    val daysInMonth = first.plus(DatePeriod(months = 1)).let { daysBetween(first, it) }
    val weeks = (startOffset + daysInMonth + 6) / 7
    val gridStart = first.plus(DatePeriod(days = -startOffset))
    LaunchedEffect(year, month) {
        env.pm.ensure(listOf(gridStart.toString(), first.toString(), gridStart.plus(DatePeriod(days = weeks * 7 - 1)).toString()))
    }
    fun go(n: Int) { val d = first.plus(DatePeriod(months = n)); year = d.year; month = d.month.ordinal + 1 }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, d2m.borderSoft, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                D2MButton("‹", { go(-1) }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM)
                Text(monthTitle(year, month, s), style = MaterialTheme.typography.titleMedium, color = d2m.ink,
                    modifier = Modifier.weight(1f), maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                D2MButton("›", { go(1) }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                s.weekdaysShort.forEach {
                    Text(it.take(3), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = d2m.faint,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (w in 0 until weeks) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (dow in 0 until 7) {
                            val date = gridStart.plus(DatePeriod(days = w * 7 + dow))
                            val outside = date.month.ordinal + 1 != month
                            val sel = date == chosen
                            val isToday = date == today
                            val ch = env.pm.get(date.toString())?.chandrashtamam?.isNotEmpty() == true
                            val shape = RoundedCornerShape(10.dp)
                            Column(
                                Modifier.weight(1f).height(46.dp).clip(shape)
                                    .background(when { sel -> d2m.accent; ch -> p.danger.bg; else -> MaterialTheme.colorScheme.surface })
                                    .border(if (isToday && !sel) BorderStroke(2.dp, d2m.accent) else BorderStroke(1.dp, if (sel) d2m.accent else d2m.borderSoft), shape)
                                    .clickable { onPick(date.toString()) },
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                            ) {
                                Text(date.dayOfMonth.toString(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                    color = when { sel -> d2m.accentOn; outside -> d2m.faint; else -> d2m.textPrimary })
                                if (!sel) PmBadges(date.toString())
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                LinkText(s.calToday, onClick = { onPick(today.toString()) })
                LinkText(s.cancel, onClick = onDismiss)
            }
        }
    }
}

/** "Tap to set" date button + the day's panchangam note underneath. */
@Composable
private fun DateField(label: String, value: String?, onChange: (String?) -> Unit, clearable: Boolean = false) {
    var picking by remember { mutableStateOf(false) }
    val s = LocalWedding.current.s
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = d2m.meta)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            D2MButton(value?.toDate()?.medium(s) ?: s.setDate, { picking = true }, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM)
            if (clearable && value != null) LinkText(s.clear, onClick = { onChange(null) })
        }
        PmNote(value)
    }
    if (picking) DatePickDialog(value, { picking = false }) { picking = false; onChange(it) }
}

/* ------------------------------------------------------------------ task detail */

private fun agoText(iso: String, s: WeddingStrings): String {
    val then = runCatching { Instant.parse(iso).toEpochMilliseconds() }.getOrNull() ?: return ""
    val secs = (Clock.System.now().toEpochMilliseconds() - then) / 1000
    return when {
        secs < 60 -> s.agoJustNow
        secs < 3600 -> s.agoMinutes((secs / 60).toInt())
        secs < 86400 -> s.agoHours((secs / 3600).toInt())
        secs < 604800 -> s.agoDays((secs / 86400).toInt())
        else -> Instant.fromEpochMilliseconds(then).toLocalDateTime(TimeZone.currentSystemDefault()).date.medium(s)
    }
}

private fun sizeText(n: Int) = if (n < 1024) "$n B" else if (n < 1048576) "${n / 1024} KB" else "${(n * 10 / 1048576) / 10.0} MB"

private fun anchorText(t: WeddingTaskOut, s: WeddingStrings): String {
    if (t.dueDateIsCustom) return s.anchorCustom
    val a = s.anchorName(t.anchor)
    val n = t.offsetDays
    return when { n == 0 -> s.anchorOn(a); n < 0 -> s.anchorBefore(-n, a); else -> s.anchorAfter(n, a) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskSheet(c: WeddingController, task: WeddingTaskOut, phases: List<WeddingPhaseOut>, onDismiss: () -> Unit) {
    val env = LocalWedding.current
    val s = env.s
    val scope = rememberCoroutineScope()
    var activity by remember(task.id) { mutableStateOf<WeddingActivityOut?>(null) }
    var err by remember(task.id) { mutableStateOf<String?>(null) }
    var comment by remember(task.id) { mutableStateOf("") }
    var posting by remember(task.id) { mutableStateOf(false) }
    var picks by remember(task.id) { mutableStateOf(listOf<Pair<String, String>>()) } // role to the label typed for it
    var uploading by remember(task.id) { mutableStateOf(false) }
    var viewingId by remember(task.id) { mutableStateOf<String?>(null) }
    var confirmRemove by remember(task.id) { mutableStateOf(false) }
    var opening by remember(task.id) { mutableStateOf<String?>(null) }
    var titleDraft by remember(task.id) { mutableStateOf(task.title) }
    val loader = rememberPhotoImageLoader()
    val done = task.status == "done"

    LaunchedEffect(task.id) {
        runCatching { c.repo.activity(c.primaryId, task.id) }
            .onSuccess { activity = it; c.updateCounts(task.id, it.comments.size, it.attachments.size) }
            .onFailure { err = friendlyError(it, s.couldntLoadComments) }
    }
    fun sync(a: WeddingActivityOut) { activity = a; c.updateCounts(task.id, a.comments.size, a.attachments.size) }

    val pick = rememberMediaAttachLauncher { m ->
        if (m.bytes.size > MAX_ATTACHMENT_BYTES) { err = s.overLimit(m.fileName); return@rememberMediaAttachLauncher }
        scope.launch {
            uploading = true; err = null
            runCatching { c.repo.upload(c.primaryId, task.id, m.fileName, m.mime, m.bytes) }
                .onSuccess { f -> activity?.let { sync(it.copy(attachments = it.attachments + f)) } }
                .onFailure { err = friendlyError(it, s.couldntUpload(m.fileName)) }
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
                MetaText(phases.firstOrNull { it.key == task.phase }?.label ?: s.taskFallback)
                // A custom task's creator can rename it and choose who sees it; template tasks keep their wording.
                if (task.canChangeVisibility) {
                    D2MTextField("", titleDraft, { if (it.length <= 200) titleDraft = it }, placeholder = s.titlePlaceholder)
                    if (titleDraft.trim().isNotEmpty() && titleDraft.trim() != task.title) {
                        D2MButton(s.saveTitle, { c.setTitle(task, titleDraft) }, size = D2MButtonSize.SM, enabled = !c.busy)
                    }
                } else SectionHeading(task.title)
            }

            // Status: only the assignees can change it (and only the signer can reopen). An event is an occasion: nothing to tick off.
            if (task.kind == "event") {
                val pal = LocalD2MStatusPalette.current
                Text("\u25C6 " + s.eventNote, style = MaterialTheme.typography.labelLarge, color = pal.warning.fg,
                    modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(pal.warning.bg).padding(horizontal = 14.dp, vertical = 8.dp))
            } else Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                D2MButton(
                    text = if (done) s.doneReopen else if (task.participants.size > 1) s.markAgreed else s.markDone,
                    onClick = { c.toggle(task) },
                    variant = if (done) D2MButtonVariant.OUTLINE else D2MButtonVariant.SOLID,
                    enabled = !c.busy && task.iAmParticipant && !(done && !task.iConfirmed),
                    modifier = Modifier.fillMaxWidth(),
                )
                when {
                    !task.iAmParticipant -> MetaText(s.assignedOnly(task.participants.joinToString { env.label(it) }))
                    done && !task.iConfirmed -> MetaText(s.signedOffBy(task.confirmedBy.joinToString { env.label(it) }))
                }
            }

            HorizontalDivider(color = d2m.borderSoft)
            if (task.traditions.isNotEmpty()) MetaText(s.tradition + ": " + task.traditions.joinToString(" / ") { s.traditionNames[it] ?: it })
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LabelText(s.assigned)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    task.participants.forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PersonPill(r, size = 28.dp)
                            if (r == task.createdByRole) Text(s.creatorSuffix.trimStart(' ', '·'), style = MaterialTheme.typography.labelMedium, color = d2m.faint)
                        }
                    }
                }
            }
            task.externalParty?.let {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { LabelText(s.groupParty); ConsultChip(it) }
            }
            // Anyone who can see the task may reschedule it.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DateField(s.due, task.dueDate, onChange = { c.setDue(task, it) })
                Text(
                    (if (task.dueDate != null) anchorText(task, s) else s.setKeyDatesToSchedule),
                    style = MaterialTheme.typography.labelSmall, color = d2m.faint,
                )
                if (task.dueDateIsCustom && !task.isCustom) LinkText(s.resetDate, onClick = { c.setDue(task, null) })
            }

            if (task.canChangeVisibility) {
                D2MSelectField(s.visibleTo, task.visibility, VISIBILITY_KEYS, { if (it.isNotEmpty()) c.setVisibility(task, it) }, optionLabel = { k -> s.visibility(k) })
            }
            HorizontalDivider(color = d2m.borderSoft)
            err?.let { Text(it, color = LocalD2MStatusPalette.current.danger.fg, style = MaterialTheme.typography.bodyMedium) }

            // ---- attachments ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.attachments, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    LinkText(if (uploading) s.uploading else s.addFile, onClick = { pick() }, enabled = !uploading)
                }
                val files = activity?.attachments.orEmpty()
                if (files.isEmpty() && !uploading) MetaText(s.attachmentsHint)
                files.forEach { a ->
                    Column(Modifier.border(BorderStroke(1.dp, d2m.border), RoundedCornerShape(6.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth().clickable(enabled = opening == null) {
                            if (a.viewerKind() != null) viewingId = a.id
                            else scope.launch {
                                opening = a.id; err = null
                                runCatching { c.repo.download(c.primaryId, task.id, a.id) }
                                    .onSuccess { bytes -> if (!openDocument(a.filename, a.contentType, bytes)) err = s.noAppCanOpen(a.filename) }
                                    .onFailure { err = friendlyError(it, s.couldntOpen(a.filename)) }
                                opening = null
                            }
                        }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                Text((if (opening == a.id) s.opening + " · " else "") + "${sizeText(a.sizeBytes)} · ${env.label(a.uploadedByRole)} · ${agoText(a.createdAt, s)}", style = MaterialTheme.typography.labelSmall, color = d2m.meta)
                            }
                            if (a.mine) LinkText(s.remove, onClick = {
                                scope.launch {
                                    runCatching { c.repo.deleteAttachment(c.primaryId, task.id, a.id) }
                                        .onSuccess { activity?.let { cur -> sync(cur.copy(attachments = cur.attachments.filter { it.id != a.id })) } }
                                        .onFailure { err = friendlyError(it, s.couldntRemoveFile) }
                                }
                            })
                        }
                    }
                }
                viewingId?.let { id ->
                    val viewable = files.filter { it.viewerKind() != null }
                    if (viewable.any { it.id == id }) WeddingFileViewer(
                        files = viewable, startId = id,
                        imageModel = { f -> c.repo.attachmentUrl(c.primaryId, task.id, f.id) },
                        download = { f -> c.repo.download(c.primaryId, task.id, f.id) },
                        onClose = { viewingId = null },
                    )
                }
                if (files.any { it.viewerKind() == null }) MetaText(s.tapToOpen)
            }

            // ---- comments ----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.comments, style = MaterialTheme.typography.titleSmall)
                activity?.legacyNote?.let { note ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).border(1.dp, d2m.borderSoft, RoundedCornerShape(6.dp)).padding(10.dp)) {
                        Text(s.earlierNote, style = MaterialTheme.typography.labelMedium, color = d2m.meta)
                        Text(note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                val comments = activity?.comments.orEmpty()
                if (activity != null && comments.isEmpty() && activity?.legacyNote == null) MetaText(s.noComments)
                comments.forEach { cm ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(env.label(cm.authorRole), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Text(agoText(cm.createdAt, s), style = MaterialTheme.typography.labelSmall, color = d2m.meta, modifier = Modifier.weight(1f))
                            if (cm.mine) LinkText(s.delete, onClick = {
                                scope.launch {
                                    runCatching { c.repo.deleteComment(c.primaryId, task.id, cm.id) }
                                        .onSuccess { activity?.let { cur -> sync(cur.copy(comments = cur.comments.filter { it.id != cm.id })) } }
                                        .onFailure { err = friendlyError(it, s.couldntDeleteComment) }
                                }
                            })
                        }
                        CommentText(cm.body)
                    }
                }
                val mentionQuery = mentionQueryOf(comment)
                val suggestions = mentionOptions(mentionQuery, ROLES, env.label, s.me)
                if (suggestions.isNotEmpty() && mentionQuery != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestions.forEach { role ->
                            val label = env.label(role)
                            Text(
                                "@$label", style = MaterialTheme.typography.labelLarge, color = d2m.accent,
                                modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(d2m.accentSoft)
                                    .border(1.dp, d2m.accent, RoundedCornerShape(99.dp))
                                    .clickable {
                                        comment = comment.dropLast(mentionQuery.length + 1) + "@$label "
                                        picks = picks.filter { it.first != role } + (role to label)
                                    }.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                D2MTextField(
                    label = "", value = comment, onValueChange = { if (it.length <= 2000) comment = it },
                    placeholder = s.writeComment, singleLine = false, minLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                D2MButton(
                    "@", onClick = { comment = comment + (if (comment.isNotEmpty() && !comment.endsWith(" ") && !comment.endsWith("\n")) " @" else "@") },
                    variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM,
                )
                D2MButton(
                    text = if (posting) s.posting else s.commentButton,
                    onClick = {
                        val body = comment.trim()
                        if (body.isEmpty()) return@D2MButton
                        scope.launch {
                            posting = true; err = null
                            runCatching { c.repo.addComment(c.primaryId, task.id, toStoredMentions(body, picks)) }
                                .onSuccess { cm -> activity?.let { sync(it.copy(comments = it.comments + cm)) }; comment = ""; picks = emptyList() }
                                .onFailure { err = friendlyError(it, s.couldntPost) }
                            posting = false
                        }
                    },
                    enabled = comment.isNotBlank() && !posting, size = D2MButtonSize.SM,
                )
                }
            }

            if (task.canDelete) {
                HorizontalDivider(color = d2m.borderSoft)
                LinkText(s.removeTask, onClick = { confirmRemove = true })
            }
        }
    }
    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(s.removeTaskTitle) }, text = { Text(s.removeTaskBody) },
            confirmButton = { TextButton(onClick = { confirmRemove = false; c.delete(task) { onDismiss() } }) { Text(s.remove) } },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(s.cancel) } },
        )
    }
}

/* ------------------------------------------------------------------ day sheet */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySheet(
    date: String, tasks: List<WeddingTaskOut>, events: List<WeddingTaskOut>, allTasks: List<WeddingTaskOut>, milestone: String?, busy: Boolean,
    onOpen: (String) -> Unit, onToggle: (WeddingTaskOut) -> Unit, onAdd: (String) -> Unit, onAddEvent: (String) -> Unit, onSchedule: (WeddingTaskOut) -> Unit, onDismiss: () -> Unit,
) {
    val s = LocalWedding.current.s
    val d = date.toDate()
    var picking by remember(date) { mutableStateOf(false) }
    var query by remember(date) { mutableStateOf("") }
    // Anything not finished and not already on this day: undated first, then soonest.
    val candidates = remember(allTasks, date, query) {
        val q = query.trim().lowercase()
        allTasks.filter { it.status != "done" && it.dueDate != date && (q.isEmpty() || it.title.lowercase().contains(q)) }
            .sortedWith(compareBy({ it.dueDate != null }, { it.dueDate ?: "" }, { it.title })).take(40)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface, contentColor = d2m.textPrimary,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            // Hero: the big day number, month + weekday, and a milestone tag.
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(d.day.toString(), style = MaterialTheme.typography.displayMedium, color = d2m.accent)
                Column(Modifier.weight(1f)) {
                    Text("${s.monthsLong[d.month.ordinal]} ${d.year}", style = MaterialTheme.typography.titleLarge, color = d2m.ink)
                    MetaText(s.weekdaysLong[d.dayOfWeek.ordinal])
                }
                milestone?.let { D2MBadge(it, D2MBadgeTone.GOLD_OUTLINE) }
            }
            PmCard(date, Modifier.padding(horizontal = 20.dp))
            if (events.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabelText(s.eventsOnThisDay)
                    events.forEach { t ->
                        val pal = LocalD2MStatusPalette.current
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(pal.warning.bg).clickable { onOpen(t.id) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("\u25C6", color = pal.warning.fg, fontSize = 10.sp)
                            Text(t.title, style = MaterialTheme.typography.bodyMedium, color = pal.warning.fg, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LabelText(s.todosOnThisDay, Modifier.weight(1f))
                    if (tasks.isNotEmpty()) MetaText(s.doneOfTotal(tasks.count { it.status == "done" }, tasks.size))
                }
                if (tasks.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).border(BorderStroke(1.dp, d2m.borderStrong), RoundedCornerShape(6.dp)).padding(vertical = 26.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(s.nothingDue, style = MaterialTheme.typography.titleSmall, color = d2m.ink)
                    }
                }
            }
            tasks.forEach { t ->
                Box(Modifier.padding(horizontal = 20.dp).clip(RoundedCornerShape(6.dp)).border(BorderStroke(1.dp, d2m.border), RoundedCornerShape(6.dp))) {
                    TaskRow(t, busy, onOpen = { onOpen(t.id) }, onToggle = { onToggle(t) })
                }
            }
            if (picking) {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LabelText(s.chooseFromList, Modifier.weight(1f))
                        LinkText(s.closeAction, onClick = { picking = false; query = "" })
                    }
                    D2MTextField("", query, { query = it }, placeholder = s.searchYourTodos)
                    if (candidates.isEmpty()) MetaText(s.nothingMatches)
                    candidates.forEach { t ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).border(BorderStroke(1.dp, d2m.border), RoundedCornerShape(6.dp))
                                .clickable(enabled = !busy) { onSchedule(t); picking = false; query = "" }.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(t.title, style = MaterialTheme.typography.bodyMedium, color = d2m.textPrimary)
                            Text((t.dueDate?.let { s.nowDueOn(it.toDate().medium(s)) } ?: s.noDateYet) + " — " + s.moveToThisDay, style = MaterialTheme.typography.labelSmall, color = d2m.meta)
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                D2MButton(s.addOnThisDay, { onAdd(date) }, modifier = Modifier.fillMaxWidth())
                D2MButton(s.addEventHere, { onAddEvent(date) }, variant = D2MButtonVariant.OUTLINE, modifier = Modifier.fillMaxWidth())
                if (!picking) D2MButton(s.chooseFromList, { picking = true }, variant = D2MButtonVariant.OUTLINE, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/* ------------------------------------------------------------------ new task */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewTaskSheet(
    phases: List<WeddingPhaseOut>, myRole: String, preset: NewPreset, busy: Boolean,
    onCreate: (WeddingTaskCreateIn) -> Unit, onDismiss: () -> Unit,
) {
    val env = LocalWedding.current
    val s = env.s
    var title by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf(preset.phase) }
    var participants by remember { mutableStateOf((preset.participants ?: listOf(myRole)).toSet()) }
    var visibility by remember { mutableStateOf("all") }
    var due by remember { mutableStateOf(preset.due) }
    var kind by remember { mutableStateOf(preset.kind) }
    val today = remember { today() }
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, contentColor = d2m.textPrimary,
    ) {
        Column(Modifier.imePadding()) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionHeading(if (kind == "event") s.addEventTitle else s.addTodoTitle)
                    MetaText(if (kind == "event") s.addEventIntro else s.addTodoIntro)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        ChoiceChip(s.kindTodo, selected = kind == "task") { kind = "task" }
                        ChoiceChip(s.kindEvent, selected = kind == "event") { kind = "event" }
                    }
                }
                D2MTextField(s.whatNeedsDoing, title, { if (it.length <= 200) title = it })

                FormSection(s.assignTo, hint = s.ownersHint) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ROLES.forEach { r ->
                            PersonPill(r, size = 28.dp, selected = r in participants, onClick = { participants = if (r in participants) participants - r else participants + r })
                        }
                    }
                    MetaText(s.anyOwnerCanMark)
                }

                FormSection(s.whenLabel) {
                    DateField(s.dueDate, due, onChange = { due = it }, clearable = false)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            s.chipToday to today.toString(),
                            s.chipInWeek to today.plus(DatePeriod(days = 7)).toString(),
                            s.chipInMonth to today.plus(DatePeriod(days = 30)).toString(),
                        ).forEach { (label, value) -> ChoiceChip(label, selected = due == value) { due = value } }
                        if (due != null) LinkText(s.noDateChip, onClick = { due = null })
                    }
                }

                D2MSelectField(s.phase, phase, phases.map { it.key }, { if (it.isNotEmpty()) phase = it }, optionLabel = { k -> phases.firstOrNull { it.key == k }?.label ?: k })

                FormSection(s.visibleTo) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VISIBILITY_KEYS.forEach { k -> ChoiceChip(s.visibility(k), selected = visibility == k) { visibility = k } }
                    }
                    MetaText(when (visibility) { "all" -> s.visibilityEveryoneHint; "side" -> s.visibilitySideHint; else -> s.visibilityOnlyMeHint })
                }
                Spacer(Modifier.height(4.dp))
            }
            HorizontalDivider(color = d2m.border)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                D2MButton(
                    if (kind == "event") s.addEventButton else s.addTodoButton, { onCreate(WeddingTaskCreateIn(title.trim(), phase, visibility, due, ROLES.filter { it in participants }, kind)) },
                    enabled = title.isNotBlank() && participants.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth(),
                )
                D2MButton(s.cancel, onDismiss, variant = D2MButtonVariant.GHOST, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FormSection(label: String, hint: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            LabelText(label, Modifier.weight(1f))
            if (hint != null) Text(hint, style = MaterialTheme.typography.labelSmall, color = d2m.faint)
        }
        content()
    }
}

/** A pill-shaped single choice, selected = accent outline on a soft accent wash (same as the product's filter chips). */
@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = if (selected) d2m.accent else d2m.meta,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier.clip(CircleShape).background(if (selected) d2m.accentSoft else MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, if (selected) d2m.accent else d2m.borderStrong), CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/* ------------------------------------------------------------------ key dates */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyDatesDialog(
    engagement: String?, marriage: String?, honeymoon: String?, tradition: String?, busy: Boolean,
    onSave: (String?, String?, String?, String?) -> Unit, onDismiss: () -> Unit,
) {
    val env = LocalWedding.current
    val s = env.s
    var e by remember { mutableStateOf(engagement) }
    var m by remember { mutableStateOf(marriage) }
    var h by remember { mutableStateOf(honeymoon) }
    var t by remember { mutableStateOf(tradition ?: "") }
    LaunchedEffect(e, m, h) { env.pm.ensure(listOf(e, m, h)) }
    val engagementError = if (e != null && m != null && e!! > m!!) s.engagementBeforeWedding else null
    val honeymoonError = if (h != null && m != null && h!! < m!!) s.honeymoonAfterWedding else null
    val invalid = engagementError != null || honeymoonError != null
    val changed = e != engagement || m != marriage || h != honeymoon || t != (tradition ?: "")
    val ok = LocalD2MStatusPalette.current.success.fg
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.keyDates) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                MetaText(s.keyDatesIntro)
                MilestoneTimeline(if (engagementError == null) e else null, m, if (honeymoonError == null) h else null)
                listOf(
                    Triple(s.engagement, s.milestoneEngagement, d2m.accent) to Triple(e, engagementError) { v: String? -> e = v },
                    Triple(s.marriage, s.milestoneMarriage, d2m.goldStrong) to Triple(m, null as String?) { v: String? -> m = v },
                    Triple(s.honeymoon, s.milestoneHoneymoon, ok) to Triple(h, honeymoonError) { v: String? -> h = v },
                ).forEachIndexed { i, (meta, state) ->
                    val (label, sub, color) = meta
                    val (value, error, set) = state
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).border(BorderStroke(1.dp, if (error != null) LocalD2MStatusPalette.current.danger.fg else d2m.border), RoundedCornerShape(6.dp)).padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(28.dp).clip(CircleShape).border(2.dp, color, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                                Text(label, style = MaterialTheme.typography.titleSmall, color = d2m.ink)
                                Text(sub, style = MaterialTheme.typography.labelSmall, color = d2m.faint)
                            }
                            DateField(s.setDate, value, onChange = set, clearable = true)
                            error?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = LocalD2MStatusPalette.current.danger.fg) }
                            if (value != null && error == null) {
                                val n = daysBetween(env.today, value.toDate())
                                val parts = buildList {
                                    add(if (n >= 0) s.inDays(n) else s.daysAgo(-n))
                                    if (i != 1 && m != null && value != m) {
                                        val w = daysBetween(m!!.toDate(), value.toDate())
                                        add(if (w < 0) s.daysBeforeWedding(-w) else s.daysAfterWedding(w))
                                    }
                                }
                                Text(parts.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = d2m.faint)
                            }
                        }
                    }
                }
                D2MSelectField(s.tradition, t, TRADITION_KEYS.filter { it.isNotEmpty() }, { t = it }, optionLabel = { k -> s.traditionNames[k] ?: k }, placeholderWhenEmpty = s.traditionNames[""] ?: "", hint = s.traditionHint)
                MetaText(s.keyDatesRule)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(e, m, h, t) }, enabled = !busy && !invalid && changed) { Text(s.saveDates) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

/* ------------------------------------------------------------------ filters */

@Composable
private fun Facet(title: String, options: List<Pair<String, String>>, counts: Map<String, Int>, state: FacetState, first: Boolean = false, onChange: (FacetState) -> Unit) {
    if (options.isEmpty()) return
    val s = LocalWedding.current.s
    D2MCheckboxGroup(
        label = title, first = first,
        trailing = { if (state.isActive) LinkText(s.clear, onClick = { onChange(state.copy(values = emptySet())) }) },
    ) {
        if (state.isActive) {
            D2MSegmented(
                options = listOf(s.include, s.exclude), selectedIndex = if (state.mode == FacetMode.EXCLUDE) 1 else 0,
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

/** The filter groups themselves -- shown in a bottom sheet on phones and as a persistent rail on tablets. */
@Composable
fun WeddingFilterContent(
    filters: Filters, onChange: (Filters) -> Unit, phases: List<WeddingPhaseOut>,
    tasks: List<WeddingTaskOut>, resultCount: Int, modifier: Modifier = Modifier,
) {
    val env = LocalWedding.current
    val s = env.s
    val today = env.today
    // Faceted counts: each option is counted over the tasks that pass every OTHER filter.
    fun count(skip: String, values: (WeddingTaskOut) -> List<String>): Map<String, Int> {
        val out = HashMap<String, Int>()
        tasks.forEach { t -> if (passes(t, filters, today, skip)) values(t).forEach { out[it] = (out[it] ?: 0) + 1 } }
        return out
    }
    val owners = count("owners") { it.participants }
    val phase = count("phase") { listOf(it.phase) }
    val status = count("status") { listOf(it.status) }
    val quick = HashMap<String, Int>().also { q -> tasks.forEach { t -> if (passes(t, filters, today, "quick")) QUICK_KEYS.forEach { k -> if (quickMatches(k, t, today)) q[k] = (q[k] ?: 0) + 1 } } }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeading(s.filtersTitle, Modifier.weight(1f))
            LinkText(s.clearAll, onClick = { onChange(Filters(q = filters.q)) }, enabled = filters.quick.isNotEmpty() || listOf(filters.owners, filters.scope, filters.phase, filters.status).any { it.isActive })
        }
        MetaText(s.matchesCount(resultCount, tasks.size), Modifier.padding(bottom = 6.dp))
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            D2MCheckboxGroup(label = s.quickViews, first = true, trailing = { if (filters.quick.isNotEmpty()) LinkText(s.clear, onClick = { onChange(filters.copy(quick = emptySet())) }) }) {
                QUICK_KEYS.forEach { k ->
                    val n = quick[k] ?: 0
                    D2MCheckboxRow(s.quick(k), k in filters.quick, { on -> onChange(filters.copy(quick = if (on) filters.quick + k else filters.quick - k)) }, count = n, dimmed = n == 0 && k !in filters.quick)
                }
            }
            Facet(s.facetAssignedTo, ROLES.map { it to env.label(it) }, owners, filters.owners) { onChange(filters.copy(owners = it)) }
            Facet(s.facetPhase, phases.map { it.key to it.label }, phase, filters.phase) { onChange(filters.copy(phase = it)) }
            Facet(s.facetStatus, listOf("todo" to s.statusTodo, "done" to s.statusDone), status, filters.status) { onChange(filters.copy(status = it)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingFilterSheet(
    filters: Filters, onChange: (Filters) -> Unit, phases: List<WeddingPhaseOut>,
    tasks: List<WeddingTaskOut>, resultCount: Int, onDismiss: () -> Unit,
) {
    val s = LocalWedding.current.s
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, contentColor = d2m.textPrimary,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            WeddingFilterContent(filters, onChange, phases, tasks, resultCount, Modifier.weight(1f, fill = false))
            Spacer(Modifier.height(12.dp))
            D2MButton(s.showTasks(resultCount), onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

/* ------------------------------------------------------------------ milestone timeline */

/** Engagement / marriage / honeymoon on a line with a Today marker; labels of close milestones fan out instead of overlapping. */
@Composable
fun MilestoneTimeline(engagement: String?, marriage: String?, honeymoon: String?) {
    val env = LocalWedding.current
    val s = env.s
    val p = LocalD2MStatusPalette.current
    val marks = listOf(
        Triple(s.engagement, engagement, d2m.accent),
        Triple(s.marriage, marriage, d2m.goldStrong),
        Triple(s.honeymoon, honeymoon, p.success.fg),
    ).mapNotNull { (label, date, color) -> date?.let { Triple(label, it.toDate(), color) } }.sortedBy { it.second }
    if (marks.isEmpty()) return
    val todayDate = env.today
    val all = marks.map { it.second } + todayDate
    val min = all.min()
    val max = all.max()
    val span = maxOf(1, daysBetween(min, max))
    fun frac(d: kotlinx.datetime.LocalDate) = daysBetween(min, d).toFloat() / span

    BoxWithConstraints(Modifier.fillMaxWidth().height(86.dp).padding(horizontal = 36.dp)) {
        val w = maxWidth
        Box(Modifier.fillMaxWidth().padding(top = 14.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(d2m.surfaceSunken))
        if (todayDate >= min && todayDate <= max) {
            Box(Modifier.offset(x = w * frac(todayDate)).width(2.dp).height(34.dp).background(LocalD2MStatusPalette.current.danger.fg))
            Text(s.calToday, fontSize = 9.sp, color = LocalD2MStatusPalette.current.danger.fg, modifier = Modifier.offset(x = w * frac(todayDate) + 4.dp, y = (-1).dp))
        }
        marks.forEachIndexed { i, (label, date, color) ->
            val x = w * frac(date)
            val close = { other: Int -> kotlin.math.abs(frac(marks[other].second) - frac(date)) * 100 < 24 }
            val side = when {
                i + 1 < marks.size && close(i + 1) -> -1 // earlier of a close pair: label ends at its dot
                i > 0 && close(i - 1) -> 1               // later of a close pair: label starts at its dot
                else -> 0
            }
            Box(Modifier.offset(x = x - 8.dp, y = 6.dp).size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).border(3.dp, color, CircleShape))
            val labelWidth = 96.dp
            val lx = when (side) { -1 -> x + 8.dp - labelWidth; 1 -> x - 8.dp; else -> x - labelWidth / 2 }
            val align = when (side) { -1 -> androidx.compose.ui.text.style.TextAlign.End; 1 -> androidx.compose.ui.text.style.TextAlign.Start; else -> androidx.compose.ui.text.style.TextAlign.Center }
            Column(Modifier.offset(x = lx, y = 28.dp).width(labelWidth)) {
                Text(label, Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, textAlign = align, maxLines = 1)
                Text(date.medium(s), Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, color = d2m.meta, textAlign = align, maxLines = 1)
            }
        }
    }
}
