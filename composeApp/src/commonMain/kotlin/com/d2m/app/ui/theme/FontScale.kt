package com.d2m.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** True when the user's system text size is well above default -- layouts that put a label beside a value should stack instead. */
@Composable
fun isLargeText(threshold: Float = 1.2f): Boolean = LocalDensity.current.fontScale > threshold

/**
 * Caps the font scale for chrome that has a fixed number of slots and cannot wrap (bottom tab labels, uppercase
 * micro-labels, tiny badges). Large text there is still larger than default (up to [max]), but never so large that a
 * word breaks mid-letter. Body content is NOT capped: it keeps the full system scale.
 */
@Composable
fun CappedFontScale(max: Float = 1.3f, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    if (d.fontScale <= max) {
        content()
    } else {
        CompositionLocalProvider(LocalDensity provides Density(d.density, max), content = content)
    }
}
