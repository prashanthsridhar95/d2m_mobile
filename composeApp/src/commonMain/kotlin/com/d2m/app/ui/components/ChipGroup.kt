package com.d2m.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.mutedText

/**
 * Mirrors components/ChipGroup.jsx -- multi-select pill/chip toggle group for
 * taxonomy list fields (accept_religions, accept_locations pre-CityChipPicker,
 * marital_status_filter, ranked_community_list, other_languages, etc).
 * `disabled` shows-but-doesn't-toggle, same "locked, still visible" contract
 * as ChildProfileDialog's permission-gated view on web.
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
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            options.forEach { opt ->
                val selected = value.contains(opt)
                FilterChip(
                    selected = selected,
                    enabled = !disabled,
                    onClick = {
                        onChange(if (selected) value - opt else value + opt)
                    },
                    label = { Text(optionLabel(opt)) },
                    modifier = Modifier.wrapContentWidth(),
                )
            }
        }
    }
}
