package com.d2m.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp

val LocalD2MFlow = compositionLocalOf { D2MFlow.PARENT }
val LocalD2MStatusPalette = compositionLocalOf { LightStatusPalette }
val LocalD2MSemantic = compositionLocalOf { LightSemantic }

/**
 * Shorthand for the extended palette -- `d2m.border`, `d2m.label`,
 * `d2m.goldButton`. Sits alongside MaterialTheme.colorScheme rather than
 * replacing it: Material's own slots still drive every stock M3 component
 * (buttons, text fields, navigation bar), and this covers what those slots
 * cannot express. See D2MSemantic's doc comment in Color.kt.
 */
val d2m: D2MSemantic
    @Composable @ReadOnlyComposable
    get() = LocalD2MSemantic.current

/*
 * One scheme, not four.
 *
 * `flow` is still a parameter (every shell passes it, and Flow.kt explains
 * why the type survives) but it no longer selects a palette -- all four
 * values resolve here to the same maroon primary. The four-accent version
 * of this function is what made the app read as four apps.
 *
 * Notable mappings into Material's vocabulary:
 *  - `background` is the warm cream page; `surface` is the white card. M3
 *    defaults them to near-identical values, but this design depends on
 *    the page and a card being visibly different materials.
 *  - `surfaceVariant` / `surfaceContainerHighest` are the sunken warm
 *    panel, which is what stock components (NavigationBar containers,
 *    TextField backgrounds, dividers) reach for.
 *  - `outline` / `outlineVariant` are the two border weights, so a stock
 *    OutlinedTextField or HorizontalDivider draws the right hairline
 *    without being restyled at the call site.
 *  - `secondary` is gold. It is genuinely the design's secondary, and
 *    leaving it as a copy of the primary (what this did before) meant any
 *    component reaching for `secondary` silently produced more maroon.
 *  - `error` comes off the status palette rather than being a fifth red,
 *    and stays a step hotter than the maroon primary on purpose: now that
 *    the primary button colour IS a brick red, an error surface borrowing
 *    the same hue would read as emphasis rather than as a problem.
 */
private fun schemeFor(dark: Boolean): ColorScheme = if (dark) {
    darkColorScheme(
        primary = D2MPalette.DarkAccent,
        onPrimary = D2MPalette.DarkAccentOn,
        primaryContainer = D2MPalette.DarkAccent.copy(alpha = 0.16f).compositeOver(D2MPalette.DarkCardBg),
        onPrimaryContainer = D2MPalette.DarkAccentStrong,
        secondary = D2MPalette.DarkGold,
        onSecondary = D2MPalette.GoldButtonInk,
        secondaryContainer = D2MPalette.DarkSurfaceSunken,
        onSecondaryContainer = D2MPalette.DarkGoldStrong,
        background = D2MPalette.DarkPageBg,
        onBackground = D2MPalette.DarkTextPrimary,
        surface = D2MPalette.DarkCardBg,
        onSurface = D2MPalette.DarkTextPrimary,
        surfaceVariant = D2MPalette.DarkSurfaceSunken,
        onSurfaceVariant = D2MPalette.DarkTextMeta,
        surfaceContainer = D2MPalette.DarkCardBg,
        surfaceContainerHigh = D2MPalette.DarkSurfaceSunken,
        surfaceContainerHighest = D2MPalette.DarkSurfaceSunken,
        surfaceContainerLow = D2MPalette.DarkPageBg,
        surfaceContainerLowest = D2MPalette.DarkPageBg,
        outline = D2MPalette.DarkBorderStrong,
        outlineVariant = D2MPalette.DarkBorder,
        error = DarkStatusPalette.danger.fg,
        onError = D2MPalette.DarkPageBg,
        errorContainer = DarkStatusPalette.danger.bg,
        onErrorContainer = DarkStatusPalette.danger.fg,
        scrim = Color(0xCC0F0B08),
    )
} else {
    lightColorScheme(
        primary = D2MPalette.Accent,
        onPrimary = D2MPalette.AccentOn,
        primaryContainer = D2MPalette.Accent.copy(alpha = 0.07f).compositeOver(D2MPalette.CardBg),
        onPrimaryContainer = D2MPalette.AccentStrong,
        secondary = D2MPalette.Gold,
        onSecondary = D2MPalette.GoldButtonInk,
        secondaryContainer = D2MPalette.SurfaceSunken,
        onSecondaryContainer = D2MPalette.GoldStrong,
        background = D2MPalette.PageBg,
        onBackground = D2MPalette.TextPrimary,
        surface = D2MPalette.CardBg,
        onSurface = D2MPalette.TextPrimary,
        surfaceVariant = D2MPalette.SurfaceSunken,
        onSurfaceVariant = D2MPalette.TextMeta,
        surfaceContainer = D2MPalette.CardBg,
        surfaceContainerHigh = D2MPalette.SurfaceSunken,
        surfaceContainerHighest = D2MPalette.SurfaceSunken,
        surfaceContainerLow = D2MPalette.PageBg,
        surfaceContainerLowest = D2MPalette.PageBg,
        outline = D2MPalette.BorderStrong,
        outlineVariant = D2MPalette.Border,
        error = LightStatusPalette.danger.fg,
        onError = Color.White,
        errorContainer = LightStatusPalette.danger.bg,
        onErrorContainer = LightStatusPalette.danger.fg,
        scrim = Color(0x99251C15),
    )
}

/**
 * Shapes are wired into MaterialTheme too, not just used by this app's own
 * components -- otherwise every stock M3 surface (dialogs, menus, chips,
 * text fields, snackbars) would keep drawing Material's default 4-28dp
 * rounding and quietly undo the whole point of D2MRadius. `extraLarge`
 * stays a little rounder than the rest because it is what bottom sheets
 * and large dialogs use, where a 6dp corner on a full-width sheet reads as
 * a rendering error rather than as a choice.
 */
private val D2MShapes = Shapes(
    extraSmall = RoundedCornerShape(D2MRadius.sm),
    small = RoundedCornerShape(D2MRadius.md),
    medium = RoundedCornerShape(D2MRadius.md),
    large = RoundedCornerShape(D2MRadius.lg),
    extraLarge = RoundedCornerShape(10.dp),
)

/**
 * Root theme wrapper. Callers still pass a flow per-shell (entry / parent /
 * child / guest_system) to keep LocalD2MFlow populated for the handful of
 * screens that branch on it, but it no longer changes any colour.
 *
 * Dark mode follows the system by default, matching d2m_web's ThemeProvider
 * "system" preference. A screen-level override can be threaded through
 * `darkTheme` the same way the web app's 3-way light/dark/system toggle
 * does.
 */
@Composable
fun D2MTheme(
    flow: D2MFlow,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalD2MFlow provides flow,
        LocalD2MStatusPalette provides if (darkTheme) DarkStatusPalette else LightStatusPalette,
        LocalD2MSemantic provides if (darkTheme) DarkSemantic else LightSemantic,
    ) {
        MaterialTheme(
            colorScheme = schemeFor(darkTheme),
            typography = D2MTypography,
            shapes = D2MShapes,
            content = content,
        )
    }
}

/**
 * The four-step ink ladder, by alpha.
 *
 * The canonical four steps (0.15 / 0.35 / 0.45 / 0.55 -- see MutedAlpha)
 * snap to the comps' actual sampled colours rather than compositing an
 * approximation of them, because those four are exactly the design's ramp:
 * a hairline rule, a disabled tone, a micro-label grey, and a meta line.
 * ~60 call sites across this app already ask for one of the four, so
 * snapping here reskins all of them correctly with no edit -- the same
 * trick d2m_web pulled by pointing its --text-muted-NN variables at these
 * same values.
 *
 * Any other alpha still composites, which is the right behaviour for the
 * handful of one-off tints (0.03-0.12 backgrounds, 0.6-0.7 emphasis).
 */
@Composable
fun mutedText(alpha: Float = MutedAlpha.L55): Color {
    val s = LocalD2MSemantic.current
    return when (alpha) {
        MutedAlpha.L15 -> s.border
        MutedAlpha.L35 -> s.faint
        MutedAlpha.L45 -> s.label
        MutedAlpha.L55 -> s.meta
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            .compositeOver(MaterialTheme.colorScheme.surface)
    }
}
