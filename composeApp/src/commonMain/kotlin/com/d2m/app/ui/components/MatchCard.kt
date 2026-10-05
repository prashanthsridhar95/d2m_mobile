package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.d2m

/**
 * Shared candidate card. Rebuilt in the retheme pass as the Browse comp's
 * profile card, which is a genuinely different object from what was here.
 *
 * Before: a square AsyncImage over a name row and two muted sub-lines, in
 * a Material Card with its default elevation and 12dp corners. A missing
 * photograph was an empty tinted box.
 *
 * After, following the comp: a full-bleed photograph band at the top of
 * the card (4:3, with the comps' warm captioned placeholder when there is
 * no photo -- see ProfilePhoto.kt for why the missing case gets equal
 * treatment), the registration id in tracked small caps, the name in the
 * display serif, one meta line of "age · place", a rule, the facts as
 * unruled DataRows, and a full-width outline "View profile" action.
 *
 * Two things that reads better for, beyond matching the reference:
 *  - The photograph is what families actually scan by, and it was
 *    previously competing with the name for the same space.
 *  - An explicit action means the card no longer has to teach that it is
 *    tappable. The card stays tappable too, so nothing is lost.
 *
 * DETAILED is the list card; COMPACT stays a row for the contexts where a
 * photograph band would be wasted height (a specific request or
 * suggestion list, which is a different reading task from scanning many
 * candidates at once).
 */
enum class MatchCardVariant { DETAILED, COMPACT }

@Composable
fun MatchCard(
    suggestion: SuggestionOut,
    resolvePhotoUrl: (String) -> String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MatchCardVariant = MatchCardVariant.DETAILED,
    isShortlisted: Boolean = false,
    onToggleShortlist: (() -> Unit)? = null,
    tag: String? = null,
) {
    val strings = LocalStrings.current.sharedComponents
    val photo = suggestion.photoUrl?.let(resolvePhotoUrl)
    // "27 · Chennai" -- assembled from whatever the backend actually sent
    // rather than a fixed template, so a profile with no age renders
    // "Chennai" and not " · Chennai".
    val meta = listOfNotNull(
        suggestion.age?.toString(),
        suggestion.nativity ?: suggestion.city,
    ).joinToString(" · ")

    when (variant) {
        MatchCardVariant.DETAILED -> D2MCard(modifier) {
            Column(Modifier.clickable(onClick = onClick)) {
                ProfilePhoto(
                    photoUrl = photo,
                    contentDescription = strings.photographOf(suggestion.candidateName),
                    ratio = 4f / 3f,
                    caption = if (photo == null) strings.noPhotographOnFile else null,
                    glyphSize = 40.dp,
                    overlay = {
                        if (tag != null) {
                            D2MBadge(
                                tag,
                                D2MBadgeTone.GOLD,
                                Modifier.align(Alignment.TopStart).padding(10.dp),
                            )
                        }
                        if (onToggleShortlist != null) {
                            ShortlistStar(
                                shortlisted = isShortlisted,
                                onToggle = onToggleShortlist,
                                onPhoto = true,
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                            )
                        }
                    },
                )

                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RefNoText(suggestion.candidateId, Modifier.weight(1f))
                        ScoreBadge(suggestion.compositeScore)
                    }
                    SubHeading(suggestion.candidateName, Modifier.padding(top = 3.dp))
                    if (meta.isNotEmpty()) MetaText(meta, Modifier.padding(top = 2.dp))
                }

                val facts = buildList {
                    listOfNotNull(suggestion.gothram, suggestion.sect).joinToString(", ")
                        .takeIf { it.isNotEmpty() }?.let { add(strings.matchRowGothram to it) }
                    suggestion.moonNakshatra?.let { n ->
                        add(strings.matchRowStar to (suggestion.moonPada?.let { "$n, pada $it" } ?: n))
                    }
                    suggestion.occupationTitle?.let { add(strings.matchRowWork to it) }
                    suggestion.employer?.takeIf { it.isNotBlank() }?.let { add(strings.matchRowOrganisation to it) }
                }
                if (facts.isNotEmpty()) {
                    D2MDivider(Modifier.padding(top = 12.dp))
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
                        facts.forEach { (l, v) -> DataRow(l, v) }
                    }
                }

                if (suggestion.doshaFlags.isNotEmpty()) {
                    Text(
                        text = strings.doshaFlagsNoted(suggestion.doshaFlags.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = d2m.label,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    )
                }

                // The comps' card closes on a full-width outline action.
                D2MButton(
                    text = strings.viewProfile,
                    onClick = onClick,
                    variant = D2MButtonVariant.ACCENT_OUTLINE,
                    size = D2MButtonSize.SM,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
                )
            }
        }

        // Mirrors web's MatchCard.jsx compact-row redesign (own doc comment
        // there has the full history): a left accent bar in the rating's
        // own colour (levelColor -- the exact colour the pill below uses,
        // not a second, disagreeing tone system), a bigger circular photo
        // with the star anchored on its own corner instead of adrift in a
        // side column, a place-only line (age already reads in the name),
        // and the rating pushed to the row's actual right edge instead of
        // stacked under the star where a wide row just went empty past it.
        //
        // The accent bar is a drawBehind rect on this Row, not a sibling
        // Box measured via Modifier.height(IntrinsicSize.Min) (an earlier
        // version of this did that). Intrinsic measurement asks every
        // child -- including ProfileThumb's AsyncImage -- for its
        // preferred size in a separate pre-pass before normal layout runs,
        // and Coil's AsyncImage can report a small/zero intrinsic size
        // while an image is still loading; that collapsed the *whole row*
        // (photo included -- Modifier.size() still clamps to whatever
        // height the row's own intrinsic pass settled on) down to a
        // squashed oval on whichever card happened to still be loading its
        // photo at that instant. Confirmed live: one row out of five
        // rendered exactly that way. drawBehind runs in the draw phase
        // against the row's real, already-resolved final size, so it can't
        // be wrong before that size exists.
        MatchCardVariant.COMPACT -> D2MCard(modifier) {
            val level = suggestion.compositeScore?.let { overallRating(it) }
            val place = suggestion.nativity ?: suggestion.city
            val accentColor = level?.let { levelColor(it) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        if (accentColor != null) {
                            drawRect(color = accentColor, size = Size(3.dp.toPx(), size.height))
                        }
                    }
                    .clickable(onClick = onClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProfileThumb(
                    photoUrl = photo,
                    contentDescription = strings.photographOf(suggestion.candidateName),
                    size = 68.dp,
                    shape = RoundedCornerShape(percent = 50),
                    overlay = {
                        if (onToggleShortlist != null) {
                            ShortlistStar(
                                shortlisted = isShortlisted,
                                onToggle = onToggleShortlist,
                                onPhoto = true,
                                modifier = Modifier.align(Alignment.BottomEnd),
                            )
                        }
                    },
                )
                Column(Modifier.weight(1f)) {
                    SubHeading(
                        suggestion.candidateName + (suggestion.age?.let { ", $it" } ?: ""),
                        maxLines = 2,
                    )
                    if (place != null) MetaText(place, Modifier.padding(top = 2.dp))
                    if (suggestion.doshaFlags.isNotEmpty()) {
                        Text(
                            text = strings.doshaFlags(suggestion.doshaFlags.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = d2m.meta,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                if (level != null) D2MLevelPill(level)
            }
        }
    }
}

/**
 * The shortlist toggle. Squared rather than a bare icon so it reads as a
 * control on top of an arbitrary photograph, and gold when set -- gold is
 * this design's "marked/featured" colour, and a maroon star would compete
 * with the card's own primary action.
 *
 * Circular when onPhoto -- mirrors web's own fix for the identical
 * complaint ("the star button next to the profile image doesn't look
 * nice"): a squared badge sitting half on/half off a circular thumb reads
 * as a sticker stuck at an angle. Round echoes the shape it overlays,
 * whether that's a circular thumb (MatchCard's COMPACT row) or a
 * rectangular photo band (DETAILED) -- a round icon-button on a photo is
 * the standard "save"/"favourite" shape either way. Off a photo (the
 * "Profile status" card's inline star, if this app grows one) it stays
 * squared, sitting among the app's other square controls in a plain row.
 */
@Composable
fun ShortlistStar(
    shortlisted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onPhoto: Boolean = false,
) {
    val strings = LocalStrings.current.sharedComponents
    val shape = if (onPhoto) RoundedCornerShape(percent = 50) else RoundedCornerShape(D2MRadius.sm)
    Box(
        modifier
            .size(32.dp)
            .clip(shape)
            // On a photograph the control needs its own ground to stay
            // legible over an arbitrary image; in a text row it can sit on
            // the card.
            .background(if (onPhoto) d2m.surfaceSunken.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surface, shape),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (shortlisted) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (shortlisted) strings.removeFromShortlist else strings.addToShortlist,
                tint = if (shortlisted) d2m.goldStrong else d2m.label,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * A tracked candidate who has since gone Serious with someone else.
 * A sibling shape to MatchCard rather than a third variant: an unavailable
 * card genuinely isn't interactive (no tap, no star, no score worth
 * showing), so folding it in would mean threading a disabled-everything
 * branch through every conditional above.
 */
@Composable
fun UnavailableMatchCard(name: String, modifier: Modifier = Modifier, photoUrl: String? = null) {
    val strings = LocalStrings.current.sharedComponents
    D2MCard(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileThumb(photoUrl, null)
            Column(Modifier.weight(1f)) {
                SubHeading(name)
                MetaText(
                    strings.noLongerAvailable,
                    Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}
