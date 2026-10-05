package com.d2m.app.ui.screens.parity

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.BrowseCandidateOut
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.mutedText

/**
 * Mirrors components/BrowseTable.jsx -- the spreadsheet-style alternative to
 * the card grid (Phase 4 parity item, see plan §8). Web's sticky/frozen
 * columns and column-visibility customization aren't ported (Compose has no
 * direct `position: sticky` equivalent on a scrollable row without a custom
 * layout pass) -- this is a plain horizontally-scrollable table with a
 * fixed column set, which covers the "dense, complete data" need the
 * feature exists for without that extra layout work.
 */
private val COLUMN_WIDTH = 120.dp

@Composable
fun BrowseTable(rows: List<BrowseCandidateOut>, onOpenProfile: (String) -> Unit) {
    val strings = LocalStrings.current.parity
    val headers = listOf(
        strings.colName, strings.colAge, strings.colCity, strings.colOccupation,
        strings.colGothram, strings.colSect, strings.colHeight, strings.colScore,
    )
    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Row(modifier = Modifier.background(mutedText(0.06f)).padding(vertical = 8.dp)) {
            headers.forEach { h -> Text(h, fontWeight = FontWeight.Bold, modifier = Modifier.width(COLUMN_WIDTH).padding(horizontal = 8.dp)) }
        }
        LazyColumn {
            items(rows) { c ->
                // Row click-through to onOpenProfile is a small follow-up (needs a
                // clickable Row wrapper matching the header's fixed-width columns).
                Row(modifier = Modifier.padding(vertical = 6.dp)) {
                    listOf(
                        c.candidateName, c.age?.toString() ?: "--", c.city ?: "--", c.occupationTitle ?: "--",
                        c.gothram ?: "--", c.sect ?: "--", c.heightCm?.toString() ?: "--",
                        c.scores?.compositeScore?.toString() ?: "--",
                    ).forEach { cell ->
                        Text(cell, modifier = Modifier.width(COLUMN_WIDTH).padding(horizontal = 8.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
