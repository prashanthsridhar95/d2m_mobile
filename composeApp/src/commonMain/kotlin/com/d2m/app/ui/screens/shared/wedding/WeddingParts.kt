package com.d2m.app.ui.screens.shared.wedding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
class WeddingEnv(val label: (String) -> String, val pm: PanchangamStore, val today: LocalDate, val s: WeddingStrings)

val LocalWedding = compositionLocalOf<WeddingEnv> { error("WeddingEnv not provided") }

@Composable
private fun roleColor(role: String): Color {
    val p = LocalD2MStatusPalette.current
    return when (role) {
        "bride" -> d2m.accent
        "groom" -> p.info.fg
        "bride_parents" -> d2m.goldStrong
        else -> p.success.fg
    }
}

/** Role avatars: filled once that person has signed off, outlined until then; "Me" on my own. */
@Composable
fun Avatars(task: WeddingTaskOut, modifier: Modifier = Modifier, named: Boolean = true) {
    val env = LocalWedding.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            task.participants.take(4).forEachIndexed { i, r ->
                val on = r in task.confirmedBy
                val c = roleColor(r)
                val me = env.label(r) == env.s.me
                Box(
                    Modifier.offset(x = (-6 * i).dp).then(if (me) Modifier.height(22.dp) else Modifier.size(22.dp))
                        .clip(CircleShape).background(if (on) c else MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.5.dp, c), CircleShape)
                        .padding(horizontal = if (me) 6.dp else 0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (me) env.s.me else ROLE_SHORT[r] ?: "",
                        fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        color = if (on) MaterialTheme.colorScheme.surface else c,
                    )
                }
            }
        }
        if (named && task.participants.size == 1) {
            Text(env.label(task.participants[0]), style = MaterialTheme.typography.labelMedium, color = d2m.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

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

@Composable
fun TypeTag(task: WeddingTaskOut) {
    val tone = when (task.scope) {
        "couple" -> D2MBadgeTone.ACCENT
        "parents" -> D2MBadgeTone.WARNING
        "group" -> D2MBadgeTone.INFO
        "family" -> D2MBadgeTone.SUCCESS
        else -> D2MBadgeTone.NEUTRAL
    }
    D2MBadge(LocalWedding.current.s.scope(task.scope), tone)
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

/** Who the task needs outside the families ("Consult | Priest"). */
@Composable
fun ConsultChip(party: String) {
    val p = LocalD2MStatusPalette.current
    TagPill(party.capitalised(), p.info.bg, p.info.fg, lead = LocalWedding.current.s.consult)
}

/** Consult chip plus comment / attachment counts as proper tags (nothing when there is nothing to show). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskTags(task: WeddingTaskOut, modifier: Modifier = Modifier) {
    if (task.externalParty == null && task.commentCount == 0 && task.attachmentCount == 0) return
    val s = LocalWedding.current.s
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        task.externalParty?.let { ConsultChip(it) }
        if (task.commentCount > 0) TagPill("💬 " + s.commentsPill(task.commentCount), d2m.accentSoft, d2m.accent)
        if (task.attachmentCount > 0) TagPill("📎 " + s.filesPill(task.attachmentCount), d2m.surfaceSunken, d2m.textPrimary, border = d2m.border)
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
        if (d.isAmavasya) Box(Modifier.size(9.dp).clip(CircleShape).background(d2m.meta).border(1.dp, d2m.borderStrong, CircleShape))
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
