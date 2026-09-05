package com.d2m.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Palette -- the print-matrimonial theme, ported from d2m_web's retheme.
 *
 * These hexes are the reference comps' actual sampled pixels (Browse
 * profiles / Profile detail / Registration), the same values that now sit
 * in d2m_web/src/styles/tokens.css. Kept numerically identical to web
 * on purpose: this app and that one are the same product, and a "close
 * enough" second palette is how two clients drift apart.
 *
 * The previous version of this file carried three unrelated flow accents
 * (warm brown for entry, gold for parent, teal for child) plus a fourth
 * neutral slate for shared screens. Those are gone -- see Flow.kt. One
 * maroon primary, one gold secondary, everywhere.
 */
object D2MPalette {
    // Ink ramp
    val Ink = Color(0xFF251C15)          // headings, dark bars and panels
    val TextPrimary = Color(0xFF2E2621)  // body copy, field values
    val TextMeta = Color(0xFF5F5954)     // meta line under a name
    val TextLabel = Color(0xFF8C8375)    // UPPERCASE micro-labels
    val TextFaint = Color(0xFFB9AF9F)    // disabled / placeholder-adjacent

    // Surfaces
    val PageBg = Color(0xFFFAF6ED)
    val CardBg = Color(0xFFFFFFFF)
    val SurfaceSunken = Color(0xFFF3EADB) // photo placeholders, inset panels
    val FieldBg = Color(0xFFFCFBF7)
    val Border = Color(0xFFE7E1D6)
    val BorderSoft = Color(0xFFEFE9DE)
    val BorderStrong = Color(0xFFD9D0C1)

    // Maroon: the one primary
    val Accent = Color(0xFF8E2D1D)
    val AccentStrong = Color(0xFF7A2617)
    val AccentOn = Color(0xFFFFF9F2)

    // Gold: the secondary
    val Gold = Color(0xFFB48629)
    val GoldStrong = Color(0xFF8A6318)
    val GoldButton = Color(0xFFE0BE72)
    val GoldButtonInk = Color(0xFF2E1A08)

    // Notice / advisory callout
    val NoticeBg = Color(0xFFFDF4E3)
    val NoticeBorder = Color(0xFFEADABD)
    val NoticeFg = Color(0xFF4A3A20)

    // ----- Dark counterparts. The comps are light-only, so this side is an
    // interpretation rather than a sample: same warm espresso/cream/maroon
    // relationships with the ground rotated dark. The one real adjustment
    // is the accent -- #8E2D1D on a #191410 page fails contrast badly, so
    // maroon lifts to a warmer terracotta and gold brightens, keeping the
    // relationship (deep red primary, gold secondary) rather than the
    // literal hexes.
    val DarkInk = Color(0xFFF4EDE0)
    val DarkTextPrimary = Color(0xFFEFE7D9)
    val DarkTextMeta = Color(0xFFB3A897)
    val DarkTextLabel = Color(0xFF9A8F80)
    val DarkTextFaint = Color(0xFF6B6055)

    val DarkPageBg = Color(0xFF191410)
    val DarkCardBg = Color(0xFF221B15)
    val DarkSurfaceSunken = Color(0xFF2C231B)
    val DarkFieldBg = Color(0xFF1E1813)
    val DarkBorder = Color(0xFF3A3028)
    val DarkBorderSoft = Color(0xFF2F271F)
    val DarkBorderStrong = Color(0xFF473A2E)

    val DarkAccent = Color(0xFFC9563E)
    val DarkAccentStrong = Color(0xFFDB6A50)
    val DarkAccentOn = Color(0xFF1C0F0A)

    val DarkGold = Color(0xFFD9AE55)
    val DarkGoldStrong = Color(0xFFE8C271)

    val DarkNoticeBg = Color(0xFF241D14)
    val DarkNoticeBorder = Color(0xFF4A3C22)
    val DarkNoticeFg = Color(0xFFE4D5B4)

    // The comps' "Contact the family" block. Its own token rather than
    // reusing the top-bar espresso, because in dark mode the two go
    // opposite ways: chrome recedes (darker than the page) while this panel
    // has to stay raised (lighter) to keep reading as a distinct block.
    val PanelDarkBg = Color(0xFF251C15)
    val DarkPanelDarkBg = Color(0xFF2E251C)
}

// ----- Semantic status tokens, each with a dark-mode override. Retuned in
// the retheme pass to sit inside the warm palette: the greens are the
// comps' own #30644A/#406D5B family rather than the material-ish
// #1E7A34/#1A56B0 they were, "warning" is now the same gold as the
// FEATURED tag so a Medium rating and a featured tag read as one family,
// and "info" is a deep teal-green rather than a cobalt blue -- there is no
// blue anywhere in this design.
data class StatusColors(
    val bg: Color,
    val fg: Color,
    val border: Color? = null,
)

data class D2MStatusPalette(
    val success: StatusColors,
    val info: StatusColors,
    val warning: StatusColors,
    val danger: StatusColors,
    val neutral: StatusColors,
)

val LightStatusPalette = D2MStatusPalette(
    success = StatusColors(bg = Color(0xFFE7EFEA), fg = Color(0xFF30644A)),
    info = StatusColors(bg = Color(0xFFE6EFEE), fg = Color(0xFF2F6A63)),
    warning = StatusColors(bg = Color(0xFFF6ECD6), fg = D2MPalette.GoldStrong, border = Color(0xFFE0C88A)),
    danger = StatusColors(bg = Color(0xFFF8E9E6), fg = Color(0xFF96231A), border = Color(0xFFE3BCB6)),
    neutral = StatusColors(bg = D2MPalette.SurfaceSunken, fg = Color(0xFF7A7064)),
)

val DarkStatusPalette = D2MStatusPalette(
    success = StatusColors(bg = Color(0xFF1C2A22), fg = Color(0xFF7CC79E)),
    info = StatusColors(bg = Color(0xFF1A2A28), fg = Color(0xFF6FC3B8)),
    warning = StatusColors(bg = Color(0xFF33291A), fg = Color(0xFFE0B863), border = Color(0xFF5C4A26)),
    danger = StatusColors(bg = Color(0xFF33211E), fg = Color(0xFFE88476), border = Color(0xFF5F332D)),
    neutral = StatusColors(bg = D2MPalette.DarkSurfaceSunken, fg = Color(0xFFA79B8B)),
)

/*
 * Everything Material3's ColorScheme has no slot for.
 *
 * Compose's ColorScheme covers primary/surface/error and friends, but this
 * design leans on a set of things it simply doesn't model: four distinct
 * ink levels, three border weights, a sunken surface, a field fill, a gold
 * secondary that isn't Material's "secondary" role, an advisory-callout
 * triple, and an inverted panel. Those live here and reach call sites via
 * LocalD2MSemantic (see Theme.kt), so a screen writes `d2m.border` next to
 * `MaterialTheme.colorScheme.primary` and both are theme-driven.
 */
data class D2MSemantic(
    val ink: Color,
    val textPrimary: Color,
    val meta: Color,
    val label: Color,
    val faint: Color,
    val border: Color,
    val borderSoft: Color,
    val borderStrong: Color,
    val surfaceSunken: Color,
    val field: Color,
    val accent: Color,
    val accentStrong: Color,
    val accentOn: Color,
    val accentSoft: Color,
    val accentBorder: Color,
    val gold: Color,
    val goldStrong: Color,
    val goldSoft: Color,
    val goldButton: Color,
    val goldButtonInk: Color,
    val noticeBg: Color,
    val noticeBorder: Color,
    val noticeFg: Color,
    val panelBg: Color,
    val panelFg: Color,
    val panelFgMuted: Color,
    val panelBorder: Color,
)

val LightSemantic = D2MSemantic(
    ink = D2MPalette.Ink,
    textPrimary = D2MPalette.TextPrimary,
    meta = D2MPalette.TextMeta,
    label = D2MPalette.TextLabel,
    faint = D2MPalette.TextFaint,
    border = D2MPalette.Border,
    borderSoft = D2MPalette.BorderSoft,
    borderStrong = D2MPalette.BorderStrong,
    surfaceSunken = D2MPalette.SurfaceSunken,
    field = D2MPalette.FieldBg,
    accent = D2MPalette.Accent,
    accentStrong = D2MPalette.AccentStrong,
    accentOn = D2MPalette.AccentOn,
    accentSoft = D2MPalette.Accent.copy(alpha = 0.07f),
    accentBorder = D2MPalette.Accent.copy(alpha = 0.30f),
    gold = D2MPalette.Gold,
    goldStrong = D2MPalette.GoldStrong,
    goldSoft = D2MPalette.Gold.copy(alpha = 0.14f),
    goldButton = D2MPalette.GoldButton,
    goldButtonInk = D2MPalette.GoldButtonInk,
    noticeBg = D2MPalette.NoticeBg,
    noticeBorder = D2MPalette.NoticeBorder,
    noticeFg = D2MPalette.NoticeFg,
    panelBg = D2MPalette.PanelDarkBg,
    panelFg = Color(0xFFF6EEE0),
    panelFgMuted = Color(0xFFF6EEE0).copy(alpha = 0.62f),
    panelBorder = Color.Transparent,
)

val DarkSemantic = D2MSemantic(
    ink = D2MPalette.DarkInk,
    textPrimary = D2MPalette.DarkTextPrimary,
    meta = D2MPalette.DarkTextMeta,
    label = D2MPalette.DarkTextLabel,
    faint = D2MPalette.DarkTextFaint,
    border = D2MPalette.DarkBorder,
    borderSoft = D2MPalette.DarkBorderSoft,
    borderStrong = D2MPalette.DarkBorderStrong,
    surfaceSunken = D2MPalette.DarkSurfaceSunken,
    field = D2MPalette.DarkFieldBg,
    accent = D2MPalette.DarkAccent,
    accentStrong = D2MPalette.DarkAccentStrong,
    accentOn = D2MPalette.DarkAccentOn,
    accentSoft = D2MPalette.DarkAccent.copy(alpha = 0.16f),
    accentBorder = D2MPalette.DarkAccent.copy(alpha = 0.34f),
    gold = D2MPalette.DarkGold,
    goldStrong = D2MPalette.DarkGoldStrong,
    goldSoft = D2MPalette.DarkGold.copy(alpha = 0.16f),
    goldButton = D2MPalette.GoldButton,
    goldButtonInk = D2MPalette.GoldButtonInk,
    noticeBg = D2MPalette.DarkNoticeBg,
    noticeBorder = D2MPalette.DarkNoticeBorder,
    noticeFg = D2MPalette.DarkNoticeFg,
    panelBg = D2MPalette.DarkPanelDarkBg,
    panelFg = D2MPalette.DarkInk,
    panelFgMuted = D2MPalette.DarkInk.copy(alpha = 0.60f),
    panelBorder = Color(0xFF43372B),
)

// ----- Muted-text ladder. Kept as an object of named constants (rather
// than call sites typing 0.55f) because these four steps ARE the design's
// ink ramp -- see Theme.kt's mutedText(), which snaps them to the sampled
// colours instead of compositing an approximation.
object MutedAlpha {
    const val L15 = 0.15f
    const val L35 = 0.35f
    const val L45 = 0.45f
    const val L55 = 0.55f
}
