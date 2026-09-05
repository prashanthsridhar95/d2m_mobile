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
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.SuggestionOut
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
                    contentDescription = "Photograph of ${suggestion.candidateName}",
                    ratio = 4f / 3f,
                    caption = if (photo == null) "No photograph on file" else null,
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
                        .takeIf { it.isNotEmpty() }?.let { add("Gothram" to it) }
                    suggestion.moonNakshatra?.let { n ->
                        add("Star" to (suggestion.moonPada?.let { "$n, pada $it" } ?: n))
                    }
                    suggestion.occupationTitle?.let { add("Work" to it) }
                    suggestion.employer?.takeIf { it.isNotBlank() }?.let { add("Organisation" to it) }
                }
                if (facts.isNotEmpty()) {
                    D2MDivider(Modifier.padding(top = 12.dp))
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
                        facts.forEach { (l, v) -> DataRow(l, v) }
                    }
                }

                if (suggestion.doshaFlags.isNotEmpty()) {
                    Text(
                        text = "${suggestion.doshaFlags.size} dosha flag${if (suggestion.doshaFlags.size > 1) "s" else ""} noted",
                        style = MaterialTheme.typography.bodySmall,
                        color = d2m.label,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    )
                }

                // The comps' card closes on a full-width outline action.
                D2MButton(
                    text = "View profile",
                    onClick = onClick,
                    variant = D2MButtonVariant.ACCENT_OUTLINE,
                    size = D2MButtonSize.SM,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
                )
            }
        }

        MatchCardVariant.COMPACT -> D2MCard(modifier) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileThumb(photo, "Photograph of ${suggestion.candidateName}")
                Column(Modifier.weight(1f)) {
                    SubHeading(
                        suggestion.candidateName + (suggestion.age?.let { ", $it" } ?: ""),
                        maxLines = 2,
                    )
                    if (meta.isNotEmpty()) MetaText(meta, Modifier.padding(top = 2.dp))
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScoreBadge(suggestion.compositeScore)
                    if (onToggleShortlist != null) {
                        ShortlistStar(isShortlisted, onToggleShortlist)
                    }
                }
            }
        }
    }
}

/**
 * The shortlist toggle. Squared rather than a bare icon so it reads as a
 * control on top of an arbitrary photograph, and gold when set -- gold is
 * this design's "marked/featured" colour, and a maroon star would compete
 * with the card's own primary action.
 */
@Composable
fun ShortlistStar(
    shortlisted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onPhoto: Boolean = false,
) {
    val shape = RoundedCornerShape(D2MRadius.sm)
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
                contentDescription = if (shortlisted) "Remove from shortlist" else "Add to shortlist",
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
                    "No longer available — they've gone Serious with someone else",
                    Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}
