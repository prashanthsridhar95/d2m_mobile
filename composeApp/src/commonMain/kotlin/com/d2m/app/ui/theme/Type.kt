package com.d2m.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.text.font.FontStyle
import com.d2m.app.resources.Res
import com.d2m.app.resources.gentium_book_plus_bold
import com.d2m.app.resources.gentium_book_plus_italic
import com.d2m.app.resources.gentium_book_plus_regular
import com.d2m.app.resources.mulish_bold
import com.d2m.app.resources.mulish_extrabold
import com.d2m.app.resources.mulish_italic
import com.d2m.app.resources.mulish_medium
import com.d2m.app.resources.mulish_regular
import com.d2m.app.resources.mulish_semibold
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
 * Fonts: the same two faces as d2m_web -- Gentium Book Plus for display
 * (names, page titles, card headings) and Mulish for everything else --
 * bundled under composeResources/font (SIL OFL, licences in docs/fonts).
 * Gentium is subset to Latin; Mulish is cut into static weights from the
 * variable font so both platforms render identical weights. Hindi and
 * Tamil text falls through to the platform's own Devanagari/Tamil face,
 * which is what a bundled Latin face can't cover anyway.
 */
@Immutable
class D2MFonts(val display: FontFamily, val body: FontFamily)

val LocalD2MFonts = compositionLocalOf<D2MFonts> { error("D2MFonts not provided -- wrap in D2MTheme") }

@Composable
fun rememberD2MFonts(): D2MFonts {
    val display = FontFamily(
        Font(Res.font.gentium_book_plus_regular, FontWeight.Normal),
        Font(Res.font.gentium_book_plus_bold, FontWeight.Bold),
        Font(Res.font.gentium_book_plus_italic, FontWeight.Normal, FontStyle.Italic),
    )
    val body = FontFamily(
        Font(Res.font.mulish_regular, FontWeight.Normal),
        Font(Res.font.mulish_medium, FontWeight.Medium),
        Font(Res.font.mulish_semibold, FontWeight.SemiBold),
        Font(Res.font.mulish_bold, FontWeight.Bold),
        Font(Res.font.mulish_extrabold, FontWeight.ExtraBold),
        Font(Res.font.mulish_italic, FontWeight.Normal, FontStyle.Italic),
    )
    return remember(display, body) { D2MFonts(display, body) }
}



/**
 * Display styles are set at weight Normal, not Bold, and that is
 * deliberate -- the comps' display type is never bold. At these sizes a
 * bold serif turns muddy; the size and the face do the work. Bolding these
 * is the fastest way to lose the look.
 */
fun d2mTypography(display: FontFamily, body: FontFamily) = Typography(
    // Page title -- "Browse", "Register a profile". Sized for a phone;
    // web's equivalent is 40px, which is absurd on a 375pt screen.
    displaySmall = TextStyle(
        fontFamily = display,
        fontSize = 30.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 34.sp,
        letterSpacing = (-0.2).sp,
    ),
    // A person's name on a detail screen.
    headlineLarge = TextStyle(
        fontFamily = display,
        fontSize = 26.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    ),
    // Card / section heading -- "Basic details", "Family".
    headlineMedium = TextStyle(
        fontFamily = display,
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 25.sp,
    ),
    // Sub-section heading, and the name on a browse card.
    titleLarge = TextStyle(
        fontFamily = display,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
    ),
    // Deliberately still sans: titleMedium is used for row titles and list
    // items across this app, which are UI chrome rather than names of
    // things. Making every one of them serif would flatten the distinction
    // the display styles above are drawing.
    headlineSmall = TextStyle(
        fontFamily = display,
        fontSize = 22.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 29.sp,
    ),
    titleSmall = TextStyle(fontFamily = body, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    titleMedium = TextStyle(fontFamily = body, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),

    bodyLarge = TextStyle(fontFamily = body, fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = body, fontSize = 14.5.sp, fontWeight = FontWeight.Normal, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = body, fontSize = 13.5.sp, fontWeight = FontWeight.Normal, lineHeight = 19.sp),

    labelLarge = TextStyle(fontFamily = body, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 19.sp),
    labelMedium = TextStyle(fontFamily = body, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp),
    // THE micro-label -- every "GOTHRAM" / "DATE OF BIRTH" / "REGISTRATION
    // NUMBER" in the comps. Uppercasing happens at the call site (see
    // components/Typography.kt's LabelText) rather than here, because
    // Compose has no text-transform and callers sometimes pass already-
    // uppercase strings.
    labelSmall = TextStyle(
        fontFamily = body,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 15.sp,
        letterSpacing = 0.8.sp,
    ),
)

/**
 * The italic serif attribution the comps use under content somebody else
 * wrote ("Written by the family at registration. We do not edit these.").
 * Not a Material typography slot -- there is no role for "a caveat in the
 * display face" -- so it lives here as a plain style.
 */
@Composable
fun d2mNoteStyle(): TextStyle = TextStyle(
    fontFamily = LocalD2MFonts.current.display,
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
