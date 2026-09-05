package com.d2m.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MNoteStyle
import com.d2m.app.ui.theme.D2MRefNoStyle
import com.d2m.app.ui.theme.d2m

/*
 * The reference design's named text roles, as composables.
 *
 * New in the retheme pass, and the reason it exists rather than screens
 * reaching for MaterialTheme.typography directly: each of these is a
 * style PLUS a colour PLUS (for the label) a transform, and re-typing that
 * combination at every call site is how the old UI ended up with six
 * near-identical label treatments at 9, 10, 10.5, 11, 11.5 and 12sp with
 * four different letter-spacings. One composable each.
 */

/**
 * THE micro-label -- every "GOTHRAM" / "DATE OF BIRTH" / "REGISTRATION
 * NUMBER" in the comps. Uppercases its input here rather than in the type
 * scale, since Compose has no text-transform and callers pass ordinary
 * sentence-case strings.
 */
@Composable
fun LabelText(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color ?: d2m.label,
        modifier = modifier,
    )
}

/** Same treatment in gold, for an eyebrow above a heading rather than a field label. */
@Composable
fun EyebrowText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = d2m.goldStrong,
        modifier = modifier,
    )
}

/** Page title -- the display serif, one per screen. */
@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall,
        color = d2m.ink,
        modifier = modifier,
    )
}

/** A person's name at detail scale. */
@Composable
fun PersonName(text: String, modifier: Modifier = Modifier, maxLines: Int = 2) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        color = d2m.ink,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Card / section heading -- "Basic details", "Family", "Serious mode". */
@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = d2m.ink,
        modifier = modifier,
    )
}

/** Sub-section heading, and a name on a browse card. */
@Composable
fun SubHeading(text: String, modifier: Modifier = Modifier, maxLines: Int = 2) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = d2m.ink,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** The meta line directly under a name -- "27 · Chennai". */
@Composable
fun MetaText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = d2m.meta,
        modifier = modifier,
    )
}

/** A registration id above a name. */
@Composable
fun RefNoText(text: String, modifier: Modifier = Modifier) {
    Text(text = text.uppercase(), style = D2MRefNoStyle, color = d2m.label, modifier = modifier)
}

/**
 * The italic serif caveat under content somebody else wrote ("Written by
 * the family at registration. We do not edit these.").
 */
@Composable
fun NoteText(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = D2MNoteStyle, color = d2m.meta, modifier = modifier)
}

/**
 * A maroon inline action -- the comps' "Read our safety note." /
 * "Download the form (PDF) →". A TextButton stripped of Material's own
 * padding and minimum touch inflation would be untappable, so this keeps a
 * small padding but drops the default 64dp minimum width that makes short
 * links sit oddly in a row.
 */
@Composable
fun LinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        colors = ButtonDefaults.textButtonColors(contentColor = d2m.accent),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = TextDecoration.None,
        )
    }
}

/**
 * A micro-label with its value stacked underneath -- the comps' detail
 * grid cell. A missing value renders as an em dash rather than an empty
 * slot: these grids read as a fixed form, and a blank cell reads as "the
 * screen is broken" where a dash reads as "we don't have this".
 */
@Composable
fun DetailField(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        LabelText(label)
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "—",
            style = MaterialTheme.typography.bodyLarge,
            color = if (value.isNullOrBlank()) d2m.faint else d2m.textPrimary,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}
