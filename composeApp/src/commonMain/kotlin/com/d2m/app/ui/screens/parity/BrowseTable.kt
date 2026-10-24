package com.d2m.app.ui.screens.parity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.ui.components.D2MLevelPill
import com.d2m.app.ui.components.PrefetchPhotos
import com.d2m.app.ui.components.ProfileThumb
import com.d2m.app.ui.components.ShortlistHeart
import com.d2m.app.ui.components.overallRating
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.d2m

/**
 * Spreadsheet-style alternative to the card grid (web's BrowseTable.jsx).
 *
 * Layout: a frozen leading pane (photo, name + age, shortlist bookmark) and a
 * set of data columns that scroll horizontally. The header and every row
 * share ONE ScrollState so they can never drift apart, and the header sits
 * above the LazyColumn so it stays put while rows scroll vertically. Tap a
 * header to sort; tap again to reverse.
 */
private enum class Col(val width: Dp, val numeric: Boolean = false) {
    MATCH(112.dp), CITY(128.dp), JOB(168.dp), AGE(64.dp, true), HEIGHT(84.dp, true),
    GOTHRAM(128.dp), SECT(116.dp), STAR(132.dp),
}

private val FROZEN_WIDTH = 204.dp
private val ROW_HEIGHT = 56.dp
private val HEADER_HEIGHT = 44.dp

/** null sortKey = server order. NAME is the frozen column's sort. */
private enum class SortKey { NAME, MATCH, CITY, JOB, AGE, HEIGHT, GOTHRAM, SECT, STAR }

private fun Col.sortKey() = when (this) {
    Col.MATCH -> SortKey.MATCH; Col.CITY -> SortKey.CITY; Col.JOB -> SortKey.JOB; Col.AGE -> SortKey.AGE
    Col.HEIGHT -> SortKey.HEIGHT; Col.GOTHRAM -> SortKey.GOTHRAM; Col.SECT -> SortKey.SECT; Col.STAR -> SortKey.STAR
}

private fun SuggestionOut.score(): Double? = compositeScore?.takeIf { it > 0.0 }

private fun heightLabel(cm: Int?): String? = cm?.let {
    val totalIn = (it / 2.54).let { v -> kotlin.math.round(v).toInt() }
    "${totalIn / 12}'${totalIn % 12}\""
}

@Composable
fun BrowseTable(
    rows: List<SuggestionOut>,
    onOpenProfile: (String) -> Unit,
    shortlistOverrides: Map<String, Boolean>,
    onToggleShortlist: (SuggestionOut, Boolean) -> Unit,
    resolvePhotoUrl: (String) -> String,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.parentBrowse
    val parity = LocalStrings.current.parity
    val scroll = rememberScrollState()
    val listState = rememberLazyListState()
    var sortKey by remember { mutableStateOf<SortKey?>(null) }
    var ascending by remember { mutableStateOf(true) }

    fun onSort(k: SortKey) {
        if (sortKey == k) ascending = !ascending
        else { sortKey = k; ascending = k != SortKey.MATCH } // best match first
    }

    val sorted = remember(rows, sortKey, ascending) {
        val k = sortKey ?: return@remember rows
        val sel: (SuggestionOut) -> Comparable<*>? = when (k) {
            SortKey.NAME -> { s -> s.candidateName.lowercase() }
            SortKey.MATCH -> { s -> s.score() }
            SortKey.CITY -> { s -> (s.city ?: s.nativity)?.lowercase() }
            SortKey.JOB -> { s -> s.occupationTitle?.lowercase() }
            SortKey.AGE -> { s -> s.age }
            SortKey.HEIGHT -> { s -> s.heightCm }
            SortKey.GOTHRAM -> { s -> s.gothram?.lowercase() }
            SortKey.SECT -> { s -> s.sect?.lowercase() }
            SortKey.STAR -> { s -> s.moonNakshatra?.lowercase() }
        }
        @Suppress("UNCHECKED_CAST")
        val cmp = compareBy<SuggestionOut, Comparable<Any>?>(nullsLast<Comparable<Any>>()) { sel(it) as Comparable<Any>? }
        // Missing values always sink to the bottom, whichever way we sort.
        val present = rows.filter { sel(it) != null }
        val missing = rows.filter { sel(it) == null }
        (if (ascending) present.sortedWith(cmp) else present.sortedWith(cmp.reversed())) + missing
    }

    PrefetchPhotos(
        count = sorted.size,
        firstVisibleIndex = { listState.firstVisibleItemIndex },
        urlAt = { i -> sorted.getOrNull(i)?.photoUrl?.let(resolvePhotoUrl) },
        ahead = 8,
        perRow = 1,
    )

    val headerLabels = mapOf(
        Col.MATCH to strings.colMatch, Col.CITY to parity.colCity, Col.JOB to parity.colOccupation,
        Col.AGE to parity.colAge, Col.HEIGHT to parity.colHeight, Col.GOTHRAM to parity.colGothram,
        Col.SECT to parity.colSect, Col.STAR to strings.facetNakshatra,
    )
    val dataCols = Col.entries
    val scrolled = scroll.value > 0

    Column(modifier.fillMaxWidth()) {
        // ---- sticky header -------------------------------------------------
        Row(
            Modifier.fillMaxWidth().height(HEADER_HEIGHT).background(d2m.surfaceSunken),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderCell(
                label = parity.colName, width = FROZEN_WIDTH, active = sortKey == SortKey.NAME,
                ascending = ascending, onClick = { onSort(SortKey.NAME) },
                ascDesc = strings.sortedAscending to strings.sortedDescending,
            )
            Row(Modifier.horizontalScroll(scroll)) {
                dataCols.forEach { c ->
                    HeaderCell(
                        label = headerLabels.getValue(c), width = c.width, active = sortKey == c.sortKey(),
                        ascending = ascending, numeric = c.numeric, onClick = { onSort(c.sortKey()) },
                        ascDesc = strings.sortedAscending to strings.sortedDescending,
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(d2m.borderStrong))

        PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(sorted, key = { _, s -> s.candidateId }, contentType = { _, _ -> "row" }) { index, s ->
                    val shortlisted = shortlistOverrides[s.candidateId] ?: s.isShortlisted
                    val bg = if (index % 2 == 0) MaterialTheme.colorScheme.surface else d2m.surfaceSunken.copy(alpha = 0.45f)
                    Column {
                        Row(
                            Modifier.fillMaxWidth().height(ROW_HEIGHT).background(bg)
                                .clickable(role = Role.Button) { onOpenProfile(s.candidateId) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // frozen pane
                            Row(
                                Modifier.width(FROZEN_WIDTH).fillMaxHeight().padding(start = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                ProfileThumb(
                                    photoUrl = s.photoUrl?.let(resolvePhotoUrl),
                                    contentDescription = null, size = 40.dp, shape = CircleShape,
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        s.candidateName, style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold, color = d2m.textPrimary,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    val sub = listOfNotNull(s.age?.toString(), s.shortId).joinToString(" · ")
                                    if (sub.isNotEmpty()) {
                                        Text(
                                            sub, style = MaterialTheme.typography.bodySmall, color = d2m.meta,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                ShortlistHeart(shortlisted = shortlisted, onToggle = { onToggleShortlist(s, !shortlisted) })
                            }
                            // scrolling data columns (same ScrollState as the header)
                            Row(Modifier.horizontalScroll(scroll).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                                dataCols.forEach { c ->
                                    Box(Modifier.width(c.width).padding(horizontal = 10.dp), contentAlignment = if (c.numeric) Alignment.CenterEnd else Alignment.CenterStart) {
                                        when (c) {
                                            Col.MATCH -> {
                                                val sc = s.score()
                                                if (sc != null) D2MLevelPill(overallRating(sc)) else DashText()
                                            }
                                            Col.CITY -> CellText(s.city ?: s.nativity)
                                            Col.JOB -> CellText(s.occupationTitle)
                                            Col.AGE -> CellText(s.age?.toString())
                                            Col.HEIGHT -> CellText(heightLabel(s.heightCm))
                                            Col.GOTHRAM -> CellText(s.gothram)
                                            Col.SECT -> CellText(s.sect)
                                            Col.STAR -> CellText(s.moonNakshatra?.let { n -> s.moonPada?.let { "$n · $it" } ?: n })
                                        }
                                    }
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(d2m.borderSoft))
                    }
                }
            }
            // Hairline at the frozen pane's edge, only once data has slid under it.
            if (scrolled) {
                Box(Modifier.padding(start = FROZEN_WIDTH - 1.dp).width(1.dp).fillMaxHeight().background(d2m.border))
            }
        }
    }
}

@Composable
private fun HeaderCell(
    label: String,
    width: Dp,
    active: Boolean,
    ascending: Boolean,
    onClick: () -> Unit,
    ascDesc: Pair<String, String>,
    numeric: Boolean = false,
) {
    Row(
        Modifier.width(width).fillMaxHeight().clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = if (width == FROZEN_WIDTH) 12.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (numeric) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            label, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (active) d2m.accentStrong else d2m.label,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (active) {
            Icon(
                if (ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = if (ascending) ascDesc.first else ascDesc.second,
                tint = d2m.accentStrong, modifier = Modifier.padding(start = 4.dp).size(14.dp),
            )
        }
    }
}

@Composable
private fun CellText(value: String?) {
    if (value.isNullOrBlank()) DashText()
    else Text(
        value, style = MaterialTheme.typography.bodyMedium, color = d2m.textPrimary,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun DashText() {
    Text("–", style = MaterialTheme.typography.bodyMedium, color = d2m.meta)
}
