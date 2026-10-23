package com.d2m.app.ui.screens.parity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.BrowseCandidateOut
import com.d2m.app.ui.theme.mutedText
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get

private const val PREF_PREFIX = "d2m:browseTableColumns:v1:"
private const val COLUMN_SEPARATOR = "|"
private val DEFAULT_COLUMNS = listOf("name", "age", "city", "occupation", "gothram", "sect", "height", "score")
private val COLUMN_LABELS = mapOf(
    "name" to "Name", "age" to "Age", "city" to "City", "occupation" to "Occupation",
    "gothram" to "Gothram", "sect" to "Sect", "height" to "Height", "score" to "Score",
)
private const val COLUMN_WIDTH = 120

private fun preferenceKey(scope: String) = PREF_PREFIX + scope

private fun loadColumns(settings: Settings, scope: String): List<String> {
    val valid = DEFAULT_COLUMNS.toSet()
    val saved = settings[preferenceKey(scope), ""]
        .split(COLUMN_SEPARATOR)
        .filter { it in valid }
    return (saved + DEFAULT_COLUMNS.filterNot { it in saved }).ifEmpty { DEFAULT_COLUMNS }
}

private fun saveColumns(settings: Settings, scope: String, columns: List<String>) {
    settings.putString(preferenceKey(scope), columns.joinToString(COLUMN_SEPARATOR))
}

/** Responsive, sortable browse table with account/child-scoped preferences. */
@Composable
fun BrowseTable(
    rows: List<BrowseCandidateOut>,
    onOpenProfile: (String) -> Unit,
    settings: Settings,
    preferenceScope: String,
) {
    var visibleColumns by remember(preferenceScope) { mutableStateOf(loadColumns(settings, preferenceScope)) }
    var menuOpen by remember { mutableStateOf(false) }
    var sortKey by remember { mutableStateOf<String?>(null) }
    var ascending by remember { mutableStateOf(true) }
    val horizontalScrollState = rememberScrollState()

    val resetColumns = {
        visibleColumns = DEFAULT_COLUMNS
        saveColumns(settings, preferenceScope, DEFAULT_COLUMNS)
        menuOpen = false
    }
    val toggleColumn: (String) -> Unit = { key ->
        if (!(key in visibleColumns && visibleColumns.size == 1)) {
            val next = if (key in visibleColumns) visibleColumns - key else visibleColumns + key
            visibleColumns = next
            saveColumns(settings, preferenceScope, next)
        }
    }
    val sortedRows = remember(rows, sortKey, ascending) {
        val key = sortKey
        if (key == null) rows else rows.sortedWith(compareBy { valueFor(it, key) }).let {
            if (ascending) it else it.asReversed()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Button(onClick = { menuOpen = true }) { Text("Columns") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Reset defaults") }, onClick = resetColumns)
                DEFAULT_COLUMNS.forEach { key ->
                    DropdownMenuItem(
                        text = { Text(COLUMN_LABELS.getValue(key)) },
                        onClick = { toggleColumn(key) },
                        leadingIcon = { Checkbox(checked = key in visibleColumns, onCheckedChange = { toggleColumn(key) }) },
                    )
                }
            }
        }

        Row(modifier = Modifier.horizontalScroll(horizontalScrollState).background(mutedText(0.06f))) {
            visibleColumns.forEach { key ->
                val direction = if (sortKey == key) if (ascending) "ascending" else "descending" else "not sorted"
                Text(
                    text = "${COLUMN_LABELS.getValue(key)}${if (sortKey == key) if (ascending) " ↑" else " ↓" else ""}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .width(COLUMN_WIDTH.dp)
                        .clickable {
                            if (sortKey == key) ascending = !ascending else { sortKey = key; ascending = true }
                        }
                        .semantics { role = Role.Button; stateDescription = direction }
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                )
            }
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(sortedRows, key = { it.candidateId }) { candidate ->
                Row(
                    modifier = Modifier
                        .horizontalScroll(horizontalScrollState)
                        .clickable(role = Role.Button) { onOpenProfile(candidate.candidateId) }
                        .padding(vertical = 6.dp),
                ) {
                    visibleColumns.forEach { key ->
                        Text(
                            text = valueFor(candidate, key),
                            modifier = Modifier.width(COLUMN_WIDTH.dp).padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun valueFor(candidate: BrowseCandidateOut, key: String): String = when (key) {
    "name" -> candidate.candidateName
    "age" -> candidate.age?.toString() ?: "--"
    "city" -> candidate.city ?: "--"
    "occupation" -> candidate.occupationTitle ?: "--"
    "gothram" -> candidate.gothram ?: "--"
    "sect" -> candidate.sect ?: "--"
    "height" -> candidate.heightCm?.toString() ?: "--"
    "score" -> candidate.scores?.compositeScore?.toString() ?: "--"
    else -> "--"
}
