package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MStroke
import com.d2m.app.ui.theme.d2m

/**
 * A tab strip in the reference design's language: sentence-case labels
 * over a hairline, with the active one in maroon above a 2dp maroon
 * underline -- the same treatment the web app's nav links and stepper use,
 * so tabs and navigation read as one system.
 *
 * Material's stock TabRow is close but wrong in three specifics: it fills
 * its own container colour (a tonal surface, so a tab strip inside a white
 * card reads as a separate panel), its indicator is a rounded 3dp bar
 * inset from the tab's edges, and its labels sit at titleSmall in the
 * onSurfaceVariant role regardless of state. This normalises all three,
 * once, so the app's three tab strips (login, the profile bio/chart panel,
 * the child profile editor) can't drift apart -- which they already had.
 */
@Composable
fun D2MTabs(
    titles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        // Transparent, not `surface`: a strip inside a card should take
        // the card, and a strip on the page should take the page. Filling
        // it painted a white band across the cream login screen.
        containerColor = Color.Transparent,
        contentColor = d2m.accent,
        indicator = { positions: List<TabPosition> ->
            val pos = positions.getOrNull(selectedIndex) ?: return@TabRow
            Box(
                Modifier
                    .tabIndicatorOffsetCompat(pos)
                    .height(D2MStroke.emphasis)
                    .background(d2m.accent),
            )
        },
        divider = { D2MDivider(soft = false) },
    ) {
        titles.forEachIndexed { i, title ->
            val active = i == selectedIndex
            Tab(
                selected = active,
                onClick = { onSelect(i) },
                selectedContentColor = d2m.accent,
                unselectedContentColor = d2m.meta,
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                },
            )
        }
    }
}

// Material's own Modifier.tabIndicatorOffset insets the bar and animates
// it to a fixed width; this is the plain version -- the full width of the
// selected tab, aligned to the bottom of the row -- which is what the
// comps draw. wrapContentSize(BottomStart) is load-bearing: TabRow lays
// its indicator out at the row's full size, so without it the offset has
// nothing to move relative to and the bar spans every tab.
private fun Modifier.tabIndicatorOffsetCompat(position: TabPosition): Modifier =
    this
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = position.left)
        .width(position.width)
