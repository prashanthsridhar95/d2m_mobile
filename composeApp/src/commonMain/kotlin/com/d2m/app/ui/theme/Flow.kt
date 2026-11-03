package com.d2m.app.ui.theme

/**
 * Kept as a type, no longer as four palettes.
 *
 * This used to select between four unrelated accent colours -- warm brown
 * on the entry screens, gold in the parent flow, teal in the child flow,
 * neutral slate on shared ones -- mirroring d2m_web's `data-flow`
 * attribute. The retheme collapsed all of that onto one maroon-and-gold
 * identity on both clients, for the same reason: four accents made the
 * product read as four loosely-related apps sharing a shell, and the
 * reference comps are emphatically one house style end to end.
 *
 * The enum survives because every shell passes it to D2MTheme and a few
 * screens branch on LocalD2MFlow for copy rather than colour, and because
 * it is the natural place to hang a *subtle* per-flow signal later
 * (something like an eyebrow tint) without going back to four hues. Today
 * every value resolves to the same scheme -- see Theme.kt.
 */
enum class D2MFlow { ENTRY, PARENT, CHILD, GUEST_SYSTEM }
