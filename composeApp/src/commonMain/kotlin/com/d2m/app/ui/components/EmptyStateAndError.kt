package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.D2MStroke
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m

/**
 * "Nothing here yet" -- a mark, a serif heading, supporting text, and an
 * optional action, centred.
 *
 * Retheme note: the mark is the same warm sunken square the comps use for
 * a missing photograph, so an empty section and an unphotographed profile
 * read as the same "nothing here" material rather than as two unrelated
 * decorations. The heading is the display serif, matching every other
 * heading in the app.
 */
@Composable
fun D2MEmptyState(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(D2MRadius.md))
                .background(d2m.surfaceSunken),
            contentAlignment = Alignment.Center,
        ) {
            PersonGlyph(26.dp)
        }
        SubHeading(title, Modifier.padding(top = 6.dp))
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = d2m.meta,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(320.dp),
            )
        }
        if (action != null) {
            Box(Modifier.padding(top = 8.dp)) { action() }
        }
    }
}

/**
 * The shared inline error surface used in every screen's catch block.
 *
 * Retheme note: the tint alone stopped being enough. On the old cool-white
 * ground a pale red block was obvious; on the warm cream page it very
 * nearly disappears, so this now carries a 3dp maroon edge on its leading
 * side -- the same device the comps use to mark anything you have to act
 * on. Also gains a hairline so it reads as a surface rather than a wash.
 */
@Composable
fun D2MErrorBanner(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val danger = LocalD2MStatusPalette.current.danger
    val shape = RoundedCornerShape(D2MRadius.md)
    Row(
        modifier = modifier
            .fillMaxWidth()
            // height(IntrinsicSize.Min) is what lets the 3dp edge below
            // stretch to the banner's own height. Without it a Row's
            // children size themselves independently and the rule would
            // collapse to nothing, or need a guessed fixed height that is
            // wrong for any message longer than one line.
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(danger.bg)
            .border(D2MStroke.hairline, danger.border ?: danger.fg, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(danger.fg),
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(message, color = danger.fg, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) {
                LinkText("Retry", onRetry, Modifier.padding(top = 2.dp))
            }
        }
    }
}
