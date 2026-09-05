package com.d2m.app.ui.theme

import androidx.compose.ui.unit.dp

/** Spacing scale, ported from d2m_web tokens.css's --space-1..8. */
object D2MSpacing {
    val space1 = 4.dp
    val space2 = 8.dp
    val space3 = 12.dp
    val space4 = 16.dp
    val space5 = 20.dp
    val space6 = 26.dp
    val space7 = 32.dp
    val space8 = 44.dp
}

/**
 * Corner radii. Collapsed hard in the retheme pass -- 10/13/18 -> 3/4/6,
 * matching web's --radius-sm/md/lg.
 *
 * This is the single biggest lever on how the app reads. The reference
 * comps' whole character is crisp and print-set; pill-shaped cards and
 * 13dp buttons read as a consumer app rather than as a matrimonial
 * register that has been in Ashok Nagar since 1990. Because every
 * component here already draws its corners from these three values, the
 * change lands everywhere at once.
 *
 * `pill` stays for the genuinely round things -- filter chips, count
 * badges, avatars.
 */
object D2MRadius {
    val sm = 3.dp
    val md = 4.dp
    val lg = 6.dp
    val pill = 999.dp
}

/** Border weights, so a hairline rule is one value and not 1.dp typed 60 times. */
object D2MStroke {
    val hairline = 1.dp
    val emphasis = 2.dp
}
