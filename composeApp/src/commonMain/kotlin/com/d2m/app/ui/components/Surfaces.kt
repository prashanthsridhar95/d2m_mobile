package com.d2m.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.D2MStroke
import com.d2m.app.ui.theme.d2m

/*
 * Surfaces -- cards, rules, rows and callouts.
 *
 * The reference comps separate surfaces with a single hairline rule and
 * near-zero radius, with no elevation anywhere. Material's Card defaults
 * to the opposite (a tonal/shadow elevation, no border, 12dp corners), so
 * every card in this app was drawing a floating rounded panel. D2MCard is
 * the one place that gets corrected, and everything else builds on it.
 */

/**
 * The standard card: white (or, in dark mode, the raised warm surface), a
 * 1dp warm rule, 6dp corners, zero elevation.
 *
 * `accented` marks a card as "read this one". It used to tint the border
 * with the flow accent; now that the accent is a strong maroon, a maroon
 * hairline around an ordinary informational card reads as an error, so
 * accented cards get the comps' gold callout treatment instead -- which is
 * exactly what the comps use the gold for ("Before you match charts",
 * "Rather do this on paper").
 */
@Composable
fun D2MCard(
    modifier: Modifier = Modifier,
    accented: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(D2MRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = if (accented) d2m.noticeBg else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(D2MStroke.hairline, if (accented) d2m.noticeBorder else d2m.border),
        content = content,
    )
}

/**
 * D2MCard plus the comps' internal padding and an optional serif heading,
 * which is how nearly every block in the Profile-detail and Registration
 * comps is built. Exists because that combination was being retyped at
 * every call site, which is how the old UI drifted to five different card
 * heading sizes.
 *
 * `trailing` is the right-hand slot on the heading row -- the comps put a
 * status dot, a count or a small link there ("Profile status   ● Live").
 */
@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    accented: Boolean = false,
    padding: Dp = 18.dp,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    D2MCard(modifier = modifier, accented = accented) {
        Column(Modifier.padding(padding)) {
            if (title != null || trailing != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (title != null) SectionHeading(title, Modifier.weight(1f, fill = false))
                    trailing?.invoke()
                }
            }
            content()
        }
    }
}

/** A hairline rule inside a card. Its own composable so the weight and colour are decided once. */
@Composable
fun D2MDivider(modifier: Modifier = Modifier, soft: Boolean = true) {
    Box(
        modifier
            .fillMaxWidth()
            .height(D2MStroke.hairline)
            .background(if (soft) d2m.borderSoft else d2m.border),
    )
}

/**
 * Label left, value right, unruled -- the browse card's fact block
 * ("Gothram ..... Bharadwaja"). The comps set these tight and let the
 * label/value contrast do the separating instead of a rule per row.
 */
@Composable
fun DataRow(label: String, value: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = d2m.meta,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isNullOrBlank()) d2m.faint else d2m.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
            textAlign = TextAlign.End,
        )
    }
}

/** The same pairing but ruled, for a long list where the rules do real scanning work. */
@Composable
fun FieldRow(label: String, value: String?, last: Boolean = false, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = d2m.meta)
            Text(
                text = value?.takeIf { it.isNotBlank() } ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                color = if (value.isNullOrBlank()) d2m.faint else d2m.textPrimary,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 12.dp).weight(1f, fill = false),
            )
        }
        if (!last) D2MDivider()
    }
}

/*
 * The gold callout.
 *
 * All three comps use one recurring device for "this is guidance, not an
 * error and not a control": a pale gold panel with a slightly deeper gold
 * rule, sometimes with a leading (i), usually ending in a maroon link. It
 * appears as a strip above results, as a card in a column, and as a
 * sidebar block.
 *
 * That's a different thing from D2MErrorBanner (something went wrong, red,
 * transient) and from an accented card (an ordinary card that happens to
 * be highlighted), which is why it's its own composable. The old UI had no
 * equivalent, so advisory copy rendered either as muted body text nobody
 * reads or as an error banner it isn't.
 */
@Composable
fun D2MNotice(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(D2MRadius.lg))
            .background(d2m.noticeBg)
            .border(D2MStroke.hairline, d2m.noticeBorder, RoundedCornerShape(D2MRadius.lg))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (title != null) SectionHeading(title)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = d2m.noticeFg)
        action?.invoke()
    }
}

/**
 * The comps' inverted panel -- "Contact the family": an espresso block
 * carrying a tracked label, a couple of lines, and a gold CTA. Everything
 * about it inverts (its own ground, its own text ramp, its own button
 * variant), so it can't be expressed as a card with different padding.
 *
 * Any D2MButton inside should use variant GOLD -- maroon on espresso has
 * almost no contrast.
 */
@Composable
fun DarkPanel(
    modifier: Modifier = Modifier,
    label: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(D2MRadius.lg))
            .background(d2m.panelBg)
            .border(D2MStroke.hairline, d2m.panelBorder, RoundedCornerShape(D2MRadius.lg))
            .padding(18.dp),
    ) {
        if (label != null) {
            LabelText(label, color = d2m.panelFgMuted, modifier = Modifier.padding(bottom = 12.dp))
        }
        content()
    }
}

/** Muted small print inside a DarkPanel, so call sites don't re-derive the right tone. */
@Composable
fun DarkPanelNote(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = d2m.panelFgMuted, modifier = modifier)
}

/**
 * A small circular status dot -- "● Live". Kept here so the two places
 * that show one (profile status, presence) draw the same 7dp mark.
 */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(7.dp).clip(RoundedCornerShape(D2MRadius.pill)).background(color))
}
