package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.mutedText

/**
 * Shared candidate card, mirrors components/MatchCard.jsx -- one component
 * for Home/Browse/Discover/detail-entry, `detailed` (bigger, sub-lines,
 * shortlist star) vs `compact` (thumbnail row). UnavailableMatchCard's "gone
 * Serious with someone else" state is a separate composable below rather
 * than a third variant flag, matching the web component's own split.
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
) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        when (variant) {
            MatchCardVariant.DETAILED -> Column(modifier = Modifier.padding(12.dp)) {
                Box {
                    AsyncImage(
                        model = suggestion.photoUrl?.let(resolvePhotoUrl),
                        contentDescription = suggestion.candidateName,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(mutedText(0.1f), RoundedCornerShape(D2MRadius.md)),
                    )
                    if (onToggleShortlist != null) {
                        IconButton(onClick = onToggleShortlist, modifier = Modifier.align(Alignment.TopEnd)) {
                            Icon(
                                imageVector = if (isShortlisted) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Shortlist",
                                tint = if (isShortlisted) Color(0xFFD4A24E) else Color.White,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${suggestion.candidateName}${suggestion.age?.let { ", $it" } ?: ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    ScoreBadge(suggestion.compositeScore)
                }
                if (suggestion.city != null) {
                    Text(suggestion.city, style = MaterialTheme.typography.bodyMedium, color = mutedText(0.55f))
                }
                val subline = listOfNotNull(suggestion.gothram, suggestion.sect, suggestion.moonNakshatra).joinToString(" · ")
                if (subline.isNotEmpty()) {
                    Text(subline, style = MaterialTheme.typography.labelMedium, color = mutedText(0.45f))
                }
            }

            MatchCardVariant.COMPACT -> Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = suggestion.photoUrl?.let(resolvePhotoUrl),
                    contentDescription = suggestion.candidateName,
                    modifier = Modifier.size(56.dp).background(mutedText(0.1f), RoundedCornerShape(D2MRadius.sm)),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("${suggestion.candidateName}${suggestion.age?.let { ", $it" } ?: ""}", fontWeight = FontWeight.Bold)
                    if (suggestion.city != null) {
                        Text(suggestion.city, style = MaterialTheme.typography.labelMedium, color = mutedText(0.55f))
                    }
                }
                ScoreBadge(suggestion.compositeScore)
            }
        }
    }
}

/** A tracked candidate who's since gone Serious with someone else -- mirrors UnavailableMatchCard.jsx. */
@Composable
fun UnavailableMatchCard(name: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(name, fontWeight = FontWeight.Bold, color = mutedText(0.45f))
            Text("No longer available -- they've gone Serious with someone else", style = MaterialTheme.typography.labelMedium, color = mutedText(0.45f))
        }
    }
}
