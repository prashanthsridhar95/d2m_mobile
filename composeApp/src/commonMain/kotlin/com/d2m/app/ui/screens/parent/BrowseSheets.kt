package com.d2m.app.ui.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.strings.ParentBrowseStrings
import com.d2m.app.ui.theme.d2m
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch

/* Web's SortSheet / ViewSheet (BrowseSheets.jsx): radio lists in a bottom sheet; picking an option applies it and closes the sheet. */

internal fun BrowseSort.label(s: ParentBrowseStrings): String = when (this) {
    BrowseSort.BEST_MATCH -> s.sortBestMatch
    BrowseSort.COMPATIBILITY_DESC -> s.sortCompatibilityDesc
    BrowseSort.AGE_ASC -> s.sortAgeAsc
    BrowseSort.AGE_DESC -> s.sortAgeDesc
    BrowseSort.NAME_ASC -> s.sortNameAsc
}

internal fun BrowseView.label(s: ParentBrowseStrings): String = when (this) {
    BrowseView.LIST -> s.viewList
    BrowseView.CARDS -> s.viewCards
    BrowseView.COMPARE -> s.viewCompare
}

internal fun BrowseView.hint(s: ParentBrowseStrings): String = when (this) {
    BrowseView.LIST -> s.viewListHint
    BrowseView.CARDS -> s.viewCardsHint
    BrowseView.COMPARE -> s.viewCompareHint
}

internal fun BrowseView.icon(): ImageVector = when (this) {
    BrowseView.LIST -> Icons.AutoMirrored.Filled.ViewList
    BrowseView.CARDS -> Icons.Filled.GridView
    BrowseView.COMPARE -> Icons.Filled.TableRows
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowseSortSheet(
    value: BrowseSort,
    strings: ParentBrowseStrings,
    onChange: (BrowseSort) -> Unit,
    onDismiss: () -> Unit,
) {
    OptionSheet(strings.sortSheetTitle, onDismiss) { pick ->
        BrowseSort.entries.forEach { o ->
            OptionRow(selected = value == o, label = o.label(strings), onClick = { pick { onChange(o) } })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowseViewSheet(
    value: BrowseView,
    strings: ParentBrowseStrings,
    onChange: (BrowseView) -> Unit,
    onDismiss: () -> Unit,
) {
    OptionSheet(strings.viewSheetTitle, onDismiss) { pick ->
        BrowseView.entries.forEach { v ->
            OptionRow(selected = value == v, label = v.label(strings), hint = v.hint(strings), icon = v.icon(), onClick = { pick { onChange(v) } })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionSheet(title: String, onDismiss: () -> Unit, content: @Composable (pick: (() -> Unit) -> Unit) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = d2m.textPrimary,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp).semantics { heading() },
            )
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                content { apply ->
                    apply()
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }
            }
        }
    }
}

/** One radio row: >= 56dp tall so it is an easy thumb target, check mark at the end, optional leading icon and hint line. */
@Composable
private fun OptionRow(selected: Boolean, label: String, onClick: () -> Unit, hint: String? = null, icon: ImageVector? = null) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) d2m.accentSoft else androidx.compose.ui.graphics.Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = if (selected) d2m.accentStrong else d2m.textPrimary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = d2m.textPrimary)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = d2m.meta)
        }
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = d2m.accentStrong, modifier = Modifier.size(22.dp))
    }
}
