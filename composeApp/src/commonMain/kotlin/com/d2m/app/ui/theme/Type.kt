package com.d2m.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Type scale, rebuilt for the print-matrimonial theme.
 *
 * The reference comps carry their hierarchy almost entirely in TYPE rather
 * than in colour or elevation: a serif face for anything that names a
 * thing (page title, a person's name, a card heading), a humanist sans for
 * everything else, and one very specific micro-label treatment
 * (~10.5sp, uppercase, wide tracking, label grey) that recurs dozens of
 * times per screen. The old scale here had none of that -- every heading
 * was bold sans at 15/19/22sp, which is why the app read as a generic
 * Material dashboard.
 *
 * Font choice, and the one honest gap vs. web:
 *
 * d2m_web loads Gentium Book Plus (display) and Mulish (UI) from Google
 * Fonts. Here, display styles use `FontFamily.Serif` and everything else
 * the platform default. That keeps the serif/sans distinction -- which is
 * what actually carries the design -- with zero bundled assets and zero
 * build risk on either target, and it renders as each platform's own
 * bookish serif (Noto Serif on Android, New York/Times on iOS) rather than
 * as the exact faces the comps used.
 *
 * Swapping in the real faces later is a change to this file alone: drop
 * the .ttf files into composeApp/src/commonMain/composeResources/font/,
 * then replace `FontFamily.Serif` and the default below with
 * FontFamily(Font(Res.font.gentium_book_plus_regular)) and
 * FontFamily(Font(Res.font.mulish_regular)). `compose.components.resources`
 * is already on the classpath (see composeApp/build.gradle.kts), so
 * nothing else has to change.
 */
private val Display = FontFamily.Serif

/**
 * Display styles are set at weight Normal, not Bold, and that is
 * deliberate -- the comps' display type is never bold. At these sizes a
 * bold serif turns muddy; the size and the face do the work. Bolding these
 * is the fastest way to lose the look.
 */
val D2MTypography = Typography(
    // Page title -- "Browse", "Register a profile". Sized for a phone;
    // web's equivalent is 40px, which is absurd on a 375pt screen.
    displaySmall = TextStyle(
        fontFamily = Display,
        fontSize = 30.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 34.sp,
        letterSpacing = (-0.2).sp,
    ),
    // A person's name on a detail screen.
    headlineLarge = TextStyle(
        fontFamily = Display,
        fontSize = 26.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    ),
    // Card / section heading -- "Basic details", "Family".
    headlineMedium = TextStyle(
        fontFamily = Display,
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 25.sp,
    ),
    // Sub-section heading, and the name on a browse card.
    titleLarge = TextStyle(
        fontFamily = Display,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
    ),
    // Deliberately still sans: titleMedium is used for row titles and list
    // items across this app, which are UI chrome rather than names of
    // things. Making every one of them serif would flatten the distinction
    // the display styles above are drawing.
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),

    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),

    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp),
    // THE micro-label -- every "GOTHRAM" / "DATE OF BIRTH" / "REGISTRATION
    // NUMBER" in the comps. Uppercasing happens at the call site (see
    // components/Typography.kt's LabelText) rather than here, because
    // Compose has no text-transform and callers sometimes pass already-
    // uppercase strings.
    labelSmall = TextStyle(
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 14.sp,
        letterSpacing = 1.0.sp,
    ),
)

/**
 * The italic serif attribution the comps use under content somebody else
 * wrote ("Written by the family at registration. We do not edit these.").
 * Not a Material typography slot -- there is no role for "a caveat in the
 * display face" -- so it lives here as a plain style.
 */
val D2MNoteStyle = TextStyle(
    fontFamily = Display,
    fontSize = 13.sp,
    fontWeight = FontWeight.Normal,
    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
    lineHeight = 19.sp,
)

/**
 * A registration id above a name -- tracked small caps that reads as an
 * identifier rather than as a field label, so it keeps tabular figures.
 */
val D2MRefNoStyle = TextStyle(
    fontSize = 11.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.2.sp,
    lineHeight = 15.sp,
    textAlign = TextAlign.Start,
)
