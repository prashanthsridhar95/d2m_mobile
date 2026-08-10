package com.d2m.app.ui.theme

/**
 * Mirrors d2m_web's three "flow" palettes (src/theme/ThemeProvider.jsx +
 * styles/tokens.css), switched via `data-flow` on web -- here, via
 * LocalD2MFlow (see Theme.kt). Values below are the accents that file
 * documents directly:
 *   entry:  warm brown  -- #8A5A3B (light) / #C99A6E (dark)
 *   parent: warm gold   -- #D4A24E (same value both modes)
 *   child:  teal        -- #14877A (light) / #2FB6A5 (dark)
 * These also match the celestial-dark (parent) / teal-mint (child) /
 * neutral-slate (guest/system) palettes described in the
 * app-ui.prashanthsridhar.com visual reference (see plan §5) -- adopted as
 * the same three flows rather than a fourth competing palette set.
 */
enum class D2MFlow { ENTRY, PARENT, CHILD, GUEST_SYSTEM }
