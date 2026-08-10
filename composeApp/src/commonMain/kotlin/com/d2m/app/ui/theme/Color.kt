package com.d2m.app.ui.theme

import androidx.compose.ui.graphics.Color

// ----- Flow accents (see Flow.kt's doc comment for the source values) -----
object D2MAccents {
    val EntryLight = Color(0xFF8A5A3B)
    val EntryDark = Color(0xFFC99A6E)
    val Parent = Color(0xFFD4A24E) // same value both modes, per tokens.css
    val ChildLight = Color(0xFF14877A)
    val ChildDark = Color(0xFF2FB6A5)
    // Neutral slate for guest/shared-link + shared system screens (role
    // picker, login, notification center, astrology chart) -- the third
    // palette from the app-ui.prashanthsridhar.com reference.
    val GuestSystemLight = Color(0xFF44464A)
    val GuestSystemDark = Color(0xFFC5C6CA)
}

// ----- Semantic status tokens, each with a dark-mode override -- ported 1:1
// from tokens.css's --success/--info/--warning/--danger/--neutral bg/fg/border set.
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
    success = StatusColors(bg = Color(0xFFE6F4EA), fg = Color(0xFF1E7A34)),
    info = StatusColors(bg = Color(0xFFE8F0FE), fg = Color(0xFF1A56B0)),
    warning = StatusColors(bg = Color(0xFFFFF4E0), fg = Color(0xFF8A5A00), border = Color(0xFFE0B24D)),
    danger = StatusColors(bg = Color(0xFFFBE7E6), fg = Color(0xFFB3261E), border = Color(0xFFE8A6A2)),
    neutral = StatusColors(bg = Color(0xFFF1F1EF), fg = Color(0xFF5B5B57)),
)

val DarkStatusPalette = D2MStatusPalette(
    success = StatusColors(bg = Color(0xFF163823), fg = Color(0xFF6FCB8E)),
    info = StatusColors(bg = Color(0xFF17233D), fg = Color(0xFF8AB2F5)),
    warning = StatusColors(bg = Color(0xFF3A2E12), fg = Color(0xFFE8C878), border = Color(0xFF6B551F)),
    danger = StatusColors(bg = Color(0xFF3A1E1C), fg = Color(0xFFE89C97), border = Color(0xFF6B3230)),
    neutral = StatusColors(bg = Color(0xFF262624), fg = Color(0xFFB8B8B3)),
)

// ----- Muted-text opacity ladder (--text-muted-15/35/45/55) -- applied as
// alpha over onSurface at call sites rather than baked as fixed colors here,
// see Theme.kt's mutedText() extension.
object MutedAlpha {
    const val L15 = 0.15f
    const val L35 = 0.35f
    const val L45 = 0.45f
    const val L55 = 0.55f
}
