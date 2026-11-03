package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MStroke
import com.d2m.app.ui.theme.d2m

/*
 * The comps' filter row: a square checkbox, a label, and a right-aligned
 * count of how many profiles carry that value ("Iyengar ..... 1,284").
 * New in the retheme pass.
 *
 * The count is the whole reason this shape works as a filter, and the
 * reason it's worth more than the tag pills it replaces on web: a pill
 * tells you a value exists, a row tells you what picking it is worth
 * before you spend a tap on it.
 *
 * Material's own Checkbox is not used: it draws a 20dp rounded box with
 * its own ripple-sized 48dp touch target and Material's tick, none of
 * which matches the comps' 17dp square. The whole row is the touch target
 * instead, which is both closer to the design and a better mobile target
 * than the box alone.
 */

@Composable
fun D2MCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    enabled: Boolean = true,
    dimmed: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Checkbox) { onCheckedChange(!checked) }
            .padding(vertical = 7.dp)
            .alpha(if (!enabled) 0.5f else if (dimmed) 0.45f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CheckMarkBox(checked)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = d2m.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = d2m.label,
            )
        }
    }
}

@Composable
private fun CheckMarkBox(checked: Boolean) {
    val shape = RoundedCornerShape(2.dp)
    // Read in composition, not inside drawBehind -- that lambda runs in the
    // draw phase, where CompositionLocals aren't available.
    val tick = d2m.accentOn
    Box(
        Modifier
            .size(17.dp)
            .background(if (checked) d2m.accent else MaterialTheme.colorScheme.surface, shape)
            .border(D2MStroke.hairline, if (checked) d2m.accent else d2m.borderStrong, shape)
            .drawBehind {
                if (!checked) return@drawBehind
                // Tick drawn rather than an icon font/vector: two lines is
                // less than an imported asset costs, and it inherits the
                // colour without a tint round-trip.
                val w = size.width
                val h = size.height
                drawLine(
                    color = tick,
                    start = Offset(w * 0.24f, h * 0.52f),
                    end = Offset(w * 0.44f, h * 0.72f),
                    strokeWidth = w * 0.13f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tick,
                    start = Offset(w * 0.44f, h * 0.72f),
                    end = Offset(w * 0.78f, h * 0.30f),
                    strokeWidth = w * 0.13f,
                    cap = StrokeCap.Round,
                )
            },
    )
}

/**
 * A titled group of rows -- the comps' "COMMUNITY", "LOCATED IN", "ALSO
 * SHOW" blocks, each a micro-label heading with a hairline above it.
 * `first = true` for the group at the top of a rail, which sits directly
 * under the panel's own header.
 */
@Composable
fun D2MCheckboxGroup(
    label: String?,
    modifier: Modifier = Modifier,
    first: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(top = if (first) 0.dp else 16.dp)) {
        if (!first) {
            D2MDivider(Modifier.padding(bottom = 16.dp))
        }
        if (label != null || trailing != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (label != null) LabelText(label)
                trailing?.invoke()
            }
        }
        content()
    }
}
