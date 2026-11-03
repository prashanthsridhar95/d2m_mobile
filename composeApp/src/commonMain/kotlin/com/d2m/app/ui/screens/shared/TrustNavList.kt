package com.d2m.app.ui.screens.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.SettingsCard
import com.d2m.app.ui.theme.D2MStroke
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText

/** One tappable entry in the trust hub's settings list. */
internal data class TrustNavItem(val key: String, val label: String)

/** A titled group of [TrustNavItem]s. */
internal data class TrustNavGroup(val title: String, val items: List<TrustNavItem>)

/**
 * Phone settings list for the trust hub (mirrors d2m_web TrustNav `list`):
 * a search field above grouped cards of 52dp+ rows with chevrons. Filtering
 * is a case-insensitive substring match on the row label; an empty result
 * shows [noMatch].
 */
@Composable
internal fun TrustNavList(
    groups: List<TrustNavGroup>,
    searchLabel: String,
    noMatch: String,
    onSelect: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()
    val shown = groups
        .map { g -> g.copy(items = g.items.filter { q.isEmpty() || it.label.lowercase().contains(q) }) }
        .filter { it.items.isNotEmpty() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        D2MTextField(
            label = "",
            value = query,
            onValueChange = { query = it },
            placeholder = searchLabel,
            modifier = Modifier.semantics { contentDescription = searchLabel },
        )
        if (shown.isEmpty()) {
            Text(
                noMatch,
                style = MaterialTheme.typography.bodyMedium,
                color = mutedText(0.55f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 24.dp),
            )
        }
        shown.forEach { group ->
            Column {
                Text(
                    group.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = d2m.label,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp).semantics { heading() },
                )
                SettingsCard {
                    group.items.forEachIndexed { i, item ->
                        if (i > 0) HorizontalDivider(thickness = D2MStroke.hairline, color = d2m.borderSoft)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { onSelect(item.key) }
                                .heightIn(min = 52.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .semantics(mergeDescendants = true) {},
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = d2m.ink,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = mutedText(0.45f))
                        }
                    }
                }
            }
        }
    }
}

/** The detail pane's "All settings · {tab}" back row. */
@Composable
internal fun TrustBackRow(label: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onBack)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = d2m.accentStrong)
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = d2m.accentStrong,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
