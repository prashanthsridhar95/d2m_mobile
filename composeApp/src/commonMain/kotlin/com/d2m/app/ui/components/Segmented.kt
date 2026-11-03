package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.D2MStroke
import com.d2m.app.ui.theme.d2m

/**
 * A segmented switch -- "Matching profiles / All profiles",
 * "Grid / Table", "Include / Exclude".
 *
 * Replaces Material's SingleChoiceSegmentedButtonRow, which draws
 * fully-rounded end caps, a tonal selected container and a leading check
 * icon on the active segment -- three Material signatures, none of them in
 * this design.
 *
 * The active segment is filled in INK, not in the maroon accent. That is a
 * deliberate rule carried over from web: maroon means "an action you can
 * take", and which view you are currently looking at is state, not an
 * action. (The comps draw their own view toggle and current page number
 * the same way.) `accentActive = true` opts into a maroon fill for the few
 * places where the choice really is an action -- Include/Exclude on a
 * filter, where picking a direction *does* something.
 */
@Composable
fun D2MSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentActive: Boolean = false,
    small: Boolean = false,
) {
    val shape = RoundedCornerShape(D2MRadius.md)
    Row(
        modifier
            .clip(shape)
            .border(D2MStroke.hairline, d2m.borderStrong, shape),
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selectedIndex
            val bg = when {
                !active -> MaterialTheme.colorScheme.surface
                accentActive -> d2m.accent
                else -> d2m.ink
            }
            val fg = when {
                !active -> d2m.meta
                accentActive -> d2m.accentOn
                else -> MaterialTheme.colorScheme.background
            }
            Box(
                Modifier
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(
                        horizontal = if (small) 10.dp else 14.dp,
                        vertical = if (small) 5.dp else 9.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = fg,
                )
            }
        }
    }
}
