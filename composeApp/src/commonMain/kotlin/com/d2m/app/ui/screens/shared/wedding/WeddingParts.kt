package com.d2m.app.ui.screens.shared.wedding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d2m.app.data.model.WeddingTaskOut
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
import com.d2m.app.ui.strings.WeddingStrings
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m
import kotlinx.datetime.LocalDate

/** What every wedding composable needs: role names ("Me", "Priya's parents"), panchangam flags, today. */
class WeddingEnv(
    val label: (String) -> String, val pm: PanchangamStore, val today: LocalDate, val s: WeddingStrings,
    val who: (String) -> Who = { r -> Who(label(r), label(r), null, r.endsWith("_parents")) },
)

val LocalWedding = compositionLocalOf<WeddingEnv> { error("WeddingEnv not provided") }

/** Round tick-box. Disabled for non-assignees, and for a task someone else already signed off. */
@Composable
fun CheckCircle(task: WeddingTaskOut, busy: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val done = task.status == "done"
    val enabled = !busy && task.iAmParticipant && !(done && !task.iConfirmed)
    val green = LocalD2MStatusPalette.current.success.fg
    Box(
        modifier.size(24.dp).clip(CircleShape)
            .background(if (done) green else MaterialTheme.colorScheme.surface)
            .border(BorderStroke(2.dp, if (done) green else d2m.borderStrong), CircleShape)
            .clickable(enabled = enabled, onClick = onToggle)
            .semantics { contentDescription = task.title },
        contentAlignment = Alignment.Center,
    ) { if (done) Text("✓", color = MaterialTheme.colorScheme.surface, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

private fun String.capitalised() = replaceFirstChar { it.uppercase() }

@Composable
private fun TagPill(text: String, bg: Color, fg: Color, border: Color? = null, lead: String? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(99.dp)).background(bg).then(if (border != null) Modifier.border(1.dp, border, RoundedCornerShape(99.dp)) else Modifier)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (lead != null) {
            Text(lead, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = fg.copy(alpha = 0.75f))
            Box(Modifier.size(width = 1.dp, height = 10.dp).background(fg.copy(alpha = 0.3f)))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

/** Who the to-do involves outside the families ("Priest", "Astrologer"). */
@Composable
fun ConsultChip(party: String) {
    val p = LocalD2MStatusPalette.current
    TagPill(party.capitalised(), p.info.bg, p.info.fg)
}

/** Consult chip plus comment / attachment counts as proper tags (nothing when there is nothing to show). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskTags(task: WeddingTaskOut, modifier: Modifier = Modifier) {
    if (task.externalParty == null && task.commentCount == 0 && task.attachmentCount == 0) return
    val s = LocalWedding.current.s
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        task.externalParty?.let { ConsultChip(it) }
        if (task.commentCount > 0) TagPill("💬 ${task.commentCount}", d2m.accentSoft, d2m.accent)
        if (task.attachmentCount > 0) TagPill("📎 ${task.attachmentCount}", d2m.surfaceSunken, d2m.textPrimary, border = d2m.border)
    }
}

/** "In 5 days" / "3d overdue" plus the day's panchangam markers. */
@Composable
fun DueChip(task: WeddingTaskOut) {
    val env = LocalWedding.current
    if (task.dueDate == null) {
        Text(env.s.noDate, style = MaterialTheme.typography.labelMedium, color = d2m.faint)
        return
    }
    val danger = LocalD2MStatusPalette.current.danger.fg
    val color = when {
        task.status == "done" -> d2m.faint
        task.overdue -> danger
        else -> d2m.meta
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(relDue(task, env.today, env.s), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = if (task.overdue && task.status != "done") FontWeight.SemiBold else FontWeight.Normal)
        PmBadges(task.dueDate)
    }
}

/** A comment's text with its @mentions highlighted, worded for the viewer. */
@Composable
fun CommentText(body: String, modifier: Modifier = Modifier) {
    val env = LocalWedding.current
    val (shown, spans) = displayMentions(body, env.label)
    val style = androidx.compose.ui.text.SpanStyle(color = d2m.accent, fontWeight = FontWeight.SemiBold, background = d2m.accentSoft)
    val text = androidx.compose.ui.text.buildAnnotatedString {
        append(shown)
        spans.forEach { addStyle(style, it.first, it.last + 1) }
    }
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium)
}

/* ---------------------------------------------------------------- panchangam */

/** Compact markers, same vocabulary as the panchangam calendar: muhoortham star, Purnima disc, Amavasya moon, chandrashtamam. */
@Composable
fun PmBadges(date: String?, modifier: Modifier = Modifier) {
    val env = LocalWedding.current
    val d = env.pm.get(date) ?: return
    if (!d.isMuhoortham && !d.isPurnima && !d.isAmavasya && d.chandrashtamam.isEmpty()) return
    val p = LocalD2MStatusPalette.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (d.isMuhoortham) Text("★", color = p.success.fg, fontSize = 11.sp)
        if (d.isPurnima) Box(Modifier.size(9.dp).clip(CircleShape).background(p.warning.fg))
        if (d.isAmavasya) Box(Modifier.size(10.dp).clip(CircleShape).background(d2m.ink).border(1.5.dp, d2m.meta, CircleShape))
        if (d.chandrashtamam.isNotEmpty()) {
            Text("Ch", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = p.danger.fg,
                modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(p.danger.bg).border(0.5.dp, p.danger.fg, RoundedCornerShape(3.dp)).padding(horizontal = 3.dp, vertical = 1.dp))
        }
    }
}

/** Readable panchangam line for places where a date is picked or inspected. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PmNote(date: String?, modifier: Modifier = Modifier) {
    val env = LocalWedding.current
    if (date == null || !env.pm.available) return
    val d = env.pm.get(date)
    if (d == null) { Text(env.s.pmChecking, style = MaterialTheme.typography.labelSmall, color = d2m.faint, modifier = modifier); return }
    val p = LocalD2MStatusPalette.current
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${d.tithi.replace(Regex(" \\(.*\\)"), "")} · ${d.nakshatra}", style = MaterialTheme.typography.labelMedium, color = d2m.meta)
        @Composable fun tag(text: String, bg: Color, fg: Color) =
            Text(text, style = MaterialTheme.typography.labelSmall, color = fg, modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp))
        if (d.isMuhoortham) tag(env.s.pmShubh, p.success.bg, p.success.fg)
        if (d.isPurnima) tag(env.s.pmPurnima, p.warning.bg, p.warning.fg)
        if (d.isAmavasya) tag(env.s.pmAmavasyaAvoid, p.neutral.bg, p.neutral.fg)
        if (d.chandrashtamam.isNotEmpty()) tag(env.s.pmChandrashtamamFor(d.chandrashtamam.joinToString(" & ") { env.label(it) }), p.danger.bg, p.danger.fg)
    }
}

/** The day's panchangam laid out for reading: tithi + nakshatra, then each marker with what it means. */
@Composable
fun PmCard(date: String?, modifier: Modifier = Modifier) {
    val env = LocalWedding.current
    if (date == null || !env.pm.available) return
    val s = env.s
    val d = env.pm.get(date)
    val p = LocalD2MStatusPalette.current
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(d2m.surfaceSunken).border(1.dp, d2m.border, RoundedCornerShape(6.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.panchangam.uppercase(), style = MaterialTheme.typography.labelSmall, color = d2m.label, modifier = Modifier.weight(1f))
            if (d != null) Text("${d.tithi.replace(Regex(" \\(.*\\)"), "")} · ${d.nakshatra}", style = MaterialTheme.typography.labelMedium, color = d2m.meta)
        }
        if (d == null) { Text(s.pmChecking, style = MaterialTheme.typography.labelMedium, color = d2m.faint); return@Column }
        val rows = buildList {
            if (d.isMuhoortham) add(Triple(s.pmShubh, s.pmMuhoortham1, p.success.fg))
            if (d.isPurnima) add(Triple(s.pmPurnima, s.pmPurnima1, p.warning.fg))
            if (d.isAmavasya) add(Triple(s.pmAmavasya, s.pmAmavasya1, d2m.meta))
            if (d.chandrashtamam.isNotEmpty()) add(Triple(s.pmChandrashtamam + " · " + d.chandrashtamam.joinToString(" & ") { env.label(it) }, s.pmChandra1, p.danger.fg))
        }
        if (rows.isEmpty()) Text(s.ordinaryDay, style = MaterialTheme.typography.bodyMedium, color = d2m.meta)
        rows.forEach { (title, text, color) ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = color)
                Text(text, style = MaterialTheme.typography.labelMedium, color = d2m.meta)
            }
        }
    }
}
