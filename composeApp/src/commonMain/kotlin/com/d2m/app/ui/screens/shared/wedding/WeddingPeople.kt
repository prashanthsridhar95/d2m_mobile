package com.d2m.app.ui.screens.shared.wedding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.d2m.app.data.model.WeddingTaskOut
import com.d2m.app.ui.components.rememberPhotoImageLoader
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m

/** A person's photo (or their initial) in a circle; a ring marks "their parents", a tick marks "signed off". */
@Composable
fun PersonAvatar(role: String, size: Dp = 24.dp, signed: Boolean = false) {
    val who = LocalWedding.current.who(role)
    val loader = rememberPhotoImageLoader()
    val ok = LocalD2MStatusPalette.current.success.fg
    Box(Modifier.size(size)) {
        Box(
            Modifier.size(size).clip(CircleShape).background(d2m.surfaceSunken)
                .then(if (who.parents) Modifier.border(1.5.dp, d2m.borderStrong, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            // The initial sits underneath, so it still shows if the photo is slow or fails.
            Text(who.name.take(1).uppercase(), fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold, color = d2m.meta)
            if (who.photoUrl != null) {
                AsyncImage(model = who.photoUrl, imageLoader = loader, contentDescription = who.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        if (signed) {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(12.dp).clip(CircleShape).background(ok)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("✓", fontSize = 8.sp, color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Avatar + name in one pill; `selected`/`onClick` turn it into a toggle (used where you choose owners). */
@Composable
fun PersonPill(role: String, size: Dp = 24.dp, signed: Boolean = false, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    val env = LocalWedding.current
    val who = env.who(role)
    val me = who.label == env.s.me
    val bg = when { selected -> d2m.accentSoft; me -> d2m.accentSoft; else -> MaterialTheme.colorScheme.surface }
    val borderColor = when { selected -> d2m.accent; me -> d2m.accentBorder; else -> d2m.border }
    Row(
        Modifier.clip(CircleShape).background(bg).border(BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor), CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 3.dp, end = 11.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        PersonAvatar(role, size, signed)
        Text(who.label, style = MaterialTheme.typography.labelMedium, color = if (me || selected) d2m.accent else d2m.textPrimary, fontWeight = if (me) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The owners of a to-do: one or two short names as pills, otherwise overlapped avatars with a one-phrase summary. */
@Composable
fun OwnerPills(task: WeddingTaskOut, size: Dp = 22.dp) {
    val env = LocalWedding.current
    val summary = ownersSummary(task.participants, env.who, env.s)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (summary == null) {
            task.participants.forEach { PersonPill(it, size, signed = it in task.confirmedBy) }
        } else {
            Row {
                task.participants.forEachIndexed { i, r ->
                    Box(Modifier.offset(x = (-8 * i).dp)) { PersonAvatar(r, size, signed = r in task.confirmedBy) }
                }
            }
            Text(summary, style = MaterialTheme.typography.labelMedium, color = d2m.textPrimary, modifier = Modifier.padding(start = (-8 * (task.participants.size - 1)).dp.coerceAtLeast(0.dp)))
        }
    }
}
