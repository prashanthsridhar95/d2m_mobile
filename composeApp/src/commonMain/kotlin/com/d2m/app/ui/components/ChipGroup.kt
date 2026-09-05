package com.d2m.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.D2MStroke
import com.d2m.app.ui.theme.d2m

/*
 * Chip groups.
 *
 * The reference comps use two distinct shapes for what is mechanically the
 * same control, and the difference is meaningful, so both are here behind
 * a `shape` parameter rather than picking one:
 *
 *   PILL (default)  "Any star" / "Any gothram" -- a filter facet. Round,
 *                   light, sits in a row of many, cheap to toggle.
 *   BOX             "My daughter / My son / Myself / A relative" -- a
 *                   consequential choice that changes the rest of the
 *                   form. Near-rectangular, bigger target, and a selected
 *                   one gets a full maroon rule so it reads as a
 *                   committed answer rather than an applied filter.
 *
 * A selected chip is a maroon rule plus the faintest warm wash, NOT a
 * maroon fill (which is what Material's FilterChip does by default). That
 * keeps the label readable in accent ink and stops a row of five selected
 * facets from shouting louder than the screen's actual primary button.
 */
enum class D2MChipShape { PILL, BOX }

@Composable
private fun chipShape(shape: D2MChipShape): Shape = when (shape) {
    D2MChipShape.PILL -> RoundedCornerShape(D2MRadius.pill)
    D2MChipShape.BOX -> RoundedCornerShape(D2MRadius.md)
}

@Composable
private fun D2MChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    shape: D2MChipShape,
) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        shape = chipShape(shape),
        border = BorderStroke(
            D2MStroke.hairline,
            if (selected) d2m.accent else d2m.borderStrong,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = d2m.meta,
            selectedContainerColor = d2m.accentSoft,
            selectedLabelColor = d2m.accentStrong,
        ),
        // Material draws a leading check on a selected FilterChip. The
        // comps don't -- the rule and the ink carry it -- and the check
        // shifts the label sideways on every toggle, which reads as jitter
        // in a wrapped row.
        leadingIcon = null,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(
                    horizontal = if (shape == D2MChipShape.BOX) 6.dp else 0.dp,
                    vertical = if (shape == D2MChipShape.BOX) 4.dp else 0.dp,
                ),
            )
        },
        modifier = Modifier.wrapContentWidth(),
    )
}

/**
 * Multi-select group -- taxonomy list fields (accept_religions,
 * marital_status_filter, ranked_community_list, other_languages).
 * `disabled` shows-but-doesn't-toggle, the same "locked, still visible"
 * contract as the permission-gated profile view.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun D2MChipGroup(
    label: String?,
    options: List<String>,
    value: List<String>,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    optionLabel: (String) -> String = { it },
    disabled: Boolean = false,
    shape: D2MChipShape = D2MChipShape.PILL,
    hint: String? = null,
) {
    Column(modifier) {
        if (label != null) LabelText(label, Modifier.padding(bottom = 8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { opt ->
                val selected = value.contains(opt)
                D2MChip(
                    label = optionLabel(opt),
                    selected = selected,
                    enabled = !disabled,
                    shape = shape,
                    onClick = { onChange(if (selected) value - opt else value + opt) },
                )
            }
        }
        if (hint != null) FieldHint(hint)
    }
}

/**
 * Single-select sibling -- the comps' "Who is this profile for?" row.
 * Same control, one difference that matters: exactly one option is always
 * selected, so picking a new one replaces rather than adds.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun D2MOptionGroup(
    label: String?,
    options: List<String>,
    value: String?,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    optionLabel: (String) -> String = { it },
    disabled: Boolean = false,
    hint: String? = null,
    shape: D2MChipShape = D2MChipShape.BOX,
) {
    Column(modifier) {
        if (label != null) FieldLabel(label)
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = d2m.meta,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { opt ->
                D2MChip(
                    label = optionLabel(opt),
                    selected = value == opt,
                    enabled = !disabled,
                    shape = shape,
                    onClick = { onChange(opt) },
                )
            }
        }
    }
}
