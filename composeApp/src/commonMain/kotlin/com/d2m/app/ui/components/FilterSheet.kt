package com.d2m.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.d2m

/*
 * The filter surface.
 *
 * On web the reference comps put filters in a persistent left rail, which
 * is what d2m_web now does. A phone has no room for a rail beside the
 * results, so the same content becomes a modal bottom sheet -- the mobile-
 * native equivalent, and the shape the web app itself uses below its own
 * stacking breakpoint.
 *
 * Everything else about it is the comp: micro-label group headings on
 * hairlines, checkbox rows with a right-aligned count of how many
 * profiles carry each value, a maroon Include/Exclude switch that appears
 * only once a group has something picked, an age range slider, and a
 * "Clear all" in the header.
 *
 * Why the counts matter enough to compute: they are the difference between
 * a list of values and an index of the register. A row tells you what
 * picking it is worth before you spend a tap on it -- which on a phone,
 * where the results are behind the sheet while you filter, is doing the
 * job the web rail does by simply being next to them.
 */

/** One categorical facet: which values are picked, and whether they're included or excluded. */
data class FacetState(
    val mode: FacetMode = FacetMode.INCLUDE,
    val values: Set<String> = emptySet(),
) {
    val isActive: Boolean get() = values.isNotEmpty()

    /** True if `value` passes this facet. An empty selection is a no-op either way. */
    fun allows(value: String?): Boolean {
        if (values.isEmpty()) return true
        val member = value != null && value in values
        return if (mode == FacetMode.EXCLUDE) !member else member
    }

    /** Same, for a candidate field that is itself a list (dosha flags). */
    fun allowsAny(list: List<String>): Boolean {
        if (values.isEmpty()) return true
        val any = list.any { it in values }
        return if (mode == FacetMode.EXCLUDE) !any else any
    }

    fun toggled(value: String): FacetState =
        copy(values = if (value in values) values - value else values + value)
}

enum class FacetMode { INCLUDE, EXCLUDE }

/** One group's worth of what the sheet needs to render it. */
data class FacetGroup(
    val title: String,
    val options: List<String>,
    val counts: Map<String, Int>,
    val state: FacetState,
    val onChange: (FacetState) -> Unit,
)

private const val VISIBLE_ROWS = 6

@Composable
private fun FacetGroupBlock(group: FacetGroup, first: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    if (group.options.isEmpty()) return

    // Selected values are hoisted into the visible set wherever they sit in
    // the list -- otherwise collapsing to six rows could hide the very
    // filter that is making the results look empty.
    val ordered = group.options.sortedByDescending { it in group.state.values }
    val visible = if (expanded) ordered else ordered.take(VISIBLE_ROWS)
    val overflow = ordered.size - visible.size

    D2MCheckboxGroup(
        label = group.title,
        first = first,
        trailing = {
            if (group.state.isActive) {
                LinkText("Clear", onClick = { group.onChange(group.state.copy(values = emptySet())) })
            }
        },
    ) {
        // Include/Exclude only once something is picked: "exclude nothing"
        // and "include nothing" are the same no-op, so before the first
        // pick this control would be six identical decorations down the
        // sheet with nothing to act on.
        if (group.state.isActive) {
            D2MSegmented(
                options = listOf("Include", "Exclude"),
                selectedIndex = if (group.state.mode == FacetMode.EXCLUDE) 1 else 0,
                onSelect = { group.onChange(group.state.copy(mode = if (it == 1) FacetMode.EXCLUDE else FacetMode.INCLUDE)) },
                accentActive = true,
                small = true,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        visible.forEach { opt ->
            val n = group.counts[opt] ?: 0
            D2MCheckboxRow(
                label = opt,
                checked = opt in group.state.values,
                onCheckedChange = { group.onChange(group.state.toggled(opt)) },
                count = n,
                // A value knocked out by another group's filter still shows
                // its row, dimmed with a 0 -- "nothing here right now" is
                // information, an absent row is not.
                dimmed = n == 0 && opt !in group.state.values,
            )
        }
        if (overflow > 0) {
            LinkText(if (expanded) "Show fewer" else "Show all ${ordered.size}", onClick = { expanded = !expanded })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun D2MFilterSheet(
    groups: List<FacetGroup>,
    resultCount: Int,
    totalCount: Int,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
    ageBounds: ClosedFloatingPointRange<Float>? = null,
    ageRange: ClosedFloatingPointRange<Float>? = null,
    onAgeChange: ((ClosedFloatingPointRange<Float>?) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val anyActive = groups.any { it.state.isActive } || ageRange != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = d2m.textPrimary,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeading("Filters")
                if (anyActive) LinkText("Clear all", onClearAll)
            }
            // The live readout the comps put under the rail heading. On a
            // phone it earns its place twice over: while the sheet is open
            // the results are behind it, so this is the only feedback that
            // a tap did anything.
            MetaText("$resultCount of $totalCount profiles match", Modifier.padding(bottom = 6.dp))

            Column(Modifier.verticalScroll(rememberScrollState())) {
                groups.forEachIndexed { i, g -> FacetGroupBlock(g, first = i == 0) }

                if (ageBounds != null && onAgeChange != null && ageBounds.start < ageBounds.endInclusive) {
                    val current = ageRange ?: ageBounds
                    D2MCheckboxGroup(
                        label = "Age",
                        trailing = { if (ageRange != null) LinkText("Clear", onClick = { onAgeChange(null) }) },
                    ) {
                        RangeSlider(
                            value = current,
                            onValueChange = { onAgeChange(it) },
                            valueRange = ageBounds,
                            colors = SliderDefaults.colors(
                                thumbColor = d2m.accent,
                                activeTrackColor = d2m.accent,
                                inactiveTrackColor = d2m.surfaceSunken,
                            ),
                        )
                        Text(
                            text = "${current.start.toInt()} – ${current.endInclusive.toInt()} years",
                            style = MaterialTheme.typography.bodyMedium,
                            color = d2m.meta,
                        )
                    }
                }
            }

            D2MButton(
                text = "Show $resultCount profiles",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
        }
    }
}
