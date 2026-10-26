package com.d2m.app.ui.components

import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.strings.LocalStrings
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
    icon: (@Composable () -> Unit)? = null,
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
            if (icon != null) icon() else PersonGlyph(26.dp)
        }
        SubHeading(title, Modifier.padding(top = 6.dp))
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = d2m.meta,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp),
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
fun D2MErrorBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    /** Compact retry case: keeps the small banner (with a Retry link) instead of the full-screen state. */
    inline: Boolean = false,
    title: String? = null,
    /** True while a retry is in flight: Retry becomes a spinner and the banner announces itself as busy. */
    retrying: Boolean = false,
    onDismiss: (() -> Unit)? = null,
) {
    // A failure that offers Retry is a whole-screen failure (the content never loaded): show the full-screen state,
    // not a small red card. Inline banners without a retry -- form and action errors -- keep the compact look below.
    if (onRetry != null && !inline) {
        SomethingWentWrong(onRetry, modifier)
        return
    }
    val danger = LocalD2MStatusPalette.current.danger
    val busyLabel = LocalStrings.current.common.loading
    val dismissLabel = LocalStrings.current.common.close
    val shape = RoundedCornerShape(D2MRadius.md)
    Row(
        modifier = modifier
            // Announced when it appears (form / action errors) without moving focus.
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Assertive
                if (retrying) stateDescription = busyLabel
            }
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
                .background(danger.fg)
                .clearAndSetSemantics {},
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (title != null) Text(title, color = danger.fg, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(message, color = danger.fg, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) {
                if (retrying) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = 6.dp).size(18.dp),
                        strokeWidth = 2.dp,
                        color = danger.fg,
                    )
                } else {
                    LinkText(LocalStrings.current.sharedComponents.retry, onRetry, Modifier.padding(top = 2.dp))
                }
            }
        }
        if (onDismiss != null) {
            Box(
                Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable(role = Role.Button, onClickLabel = dismissLabel, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = dismissLabel, tint = danger.fg, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Full-area failure state with a real Retry button -- use instead of an
 * inline banner when a screen has nothing else to show. [message] is the
 * already-localized reason (usually friendlyError(...)).
 */
@Composable
fun D2MErrorState(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String = LocalStrings.current.common.errorTitle,
) {
    // [message] and [title] are kept so call sites compile unchanged; the screen shows the one standard state.
    SomethingWentWrong(onRetry, modifier)
}
