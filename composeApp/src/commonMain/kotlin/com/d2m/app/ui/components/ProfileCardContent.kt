package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText
import org.koin.compose.koinInject

private val CardShape = RoundedCornerShape(22.dp)

/**
 * One whole profile as a single card: full-bleed hero (name / age / city / occupation and the overall rating on a
 * bottom scrim) followed by the scrollable details. The scroll container lives INSIDE the card, so the swipe moves the
 * hero and the details together while a vertical drag only scrolls the profile.
 *
 * The same scroll Column is used for peeking cards (isTop = false) with the details omitted, so the hero's composition
 * (and its already-decoded photo) is preserved when a card is promoted to the top. Nothing here reads drag state.
 *
 * @param topInset status-bar height; the hero is already drawn under it by the caller, this only keeps text clear.
 * @param bottomClearance room kept under the hero text and at the end of the scroll for the floating action bar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileCardContent(
    card: SuggestionOut,
    isTop: Boolean,
    viewerPrimaryId: String?,
    topInset: Dp,
    bottomClearance: Dp,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.discovery
    val apiClient: ApiClient = koinInject()
    val identityRepo: IdentityRepository = koinInject()
    val rating = overallRating(card.scores.compositeScore)
    val firstName = card.candidateName.substringBefore(" ")

    var extraPhotos by remember(card.candidateId) { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(card.candidateId, isTop) {
        if (!isTop) return@LaunchedEffect
        // Cached by the repository (the deck prefetches this for the next cards), so usually instant.
        val photos = runCatching { identityRepo.getPhotos(card.candidateId) }.getOrDefault(emptyList())
        extraPhotos = photos.sortedBy { it.sortOrder }.drop(1).map { apiClient.resolveMediaUrl(it.url) }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().clip(CardShape).background(MaterialTheme.colorScheme.surface)) {
        val heroHeight = maxHeight
        val scroll = rememberScrollState()
        val heroPx = with(androidx.compose.ui.platform.LocalDensity.current) { heroHeight.toPx() }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll, enabled = isTop)) {
            Box(modifier = Modifier.fillMaxWidth().height(heroHeight).background(d2m.surfaceSunken)) {
                if (card.photoUrl != null) {
                    AsyncImage(
                        model = card.photoUrl?.let(apiClient::resolveMediaUrl),
                        imageLoader = rememberPhotoImageLoader(),
                        contentDescription = card.candidateName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PersonGlyph(64.dp) }
                }
                // Top scrim keeps the status bar + progress segments legible over bright photos.
                Box(
                    Modifier.fillMaxWidth().height(topInset + 72.dp).align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent))),
                )
                // Bottom scrim carrying the identity block.
                Column(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                        .padding(start = 20.dp, end = 20.dp, top = 72.dp, bottom = bottomClearance),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "${card.candidateName}${card.age?.let { ", $it" } ?: ""}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subline = listOfNotNull(card.city, card.occupationTitle).joinToString(" · ")
                    if (subline.isNotEmpty()) {
                        Text(subline, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.88f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        D2MLevelPill(rating)
                    }
                }
            }

            if (isTop) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CompatibilityCard(
                        level = rating,
                        breakdown = listOf(
                            strings.astrology to astrologicalCompatibility(card.scores),
                            strings.preferences to preferenceCompatibility(card.scores),
                        ),
                        size = CompatibilityCardSize.Small,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (card.doshaFlags.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            card.doshaFlags.forEach { D2MBadge(it, D2MBadgeTone.WARNING) }
                        }
                    }
                    Text(
                        strings.moreAbout(firstName),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    D2MProfileTabsPanel(candidateId = card.candidateId, modifier = Modifier.fillMaxWidth(), showAboutMe = true, viewerPrimaryId = viewerPrimaryId)
                    if (extraPhotos.isNotEmpty()) {
                        Text(strings.morePhotos, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = mutedText(0.55f))
                        extraPhotos.forEach { url ->
                            AsyncImage(
                                model = url,
                                imageLoader = rememberPhotoImageLoader(),
                                contentDescription = card.candidateName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f).clip(RoundedCornerShape(14.dp)).background(d2m.surfaceSunken),
                            )
                        }
                    }
                    Spacer(Modifier.height(bottomClearance))
                }
            }
        }
        if (isTop) {
            // Keeps the clock/status icons legible once profile text scrolls up underneath them; invisible over the hero.
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().height(topInset + 16.dp)
                    .graphicsLayer { alpha = (scroll.value / (heroPx * 0.6f)).coerceIn(0f, 1f) }
                    .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), Color.Transparent))),
            )
            // Keeps the floating action buttons legible once profile text scrolls underneath them; invisible over the hero.
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bottomClearance + 28.dp)
                    .graphicsLayer { alpha = (scroll.value / (heroPx * 0.6f)).coerceIn(0f, 1f) }
                    .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)))),
            )
        }
    }
}

/** Placeholder with the card's final geometry so the real deck replaces it without a layout jump. */
@Composable
fun SwipeDeckSkeleton(topInset: Dp, bottomClearance: Dp, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().clip(CardShape)) {
        D2MSkeleton(height = maxHeight, radius = 0.dp)
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = bottomClearance),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            D2MSkeleton(width = 220.dp, height = 28.dp)
            D2MSkeleton(width = 160.dp, height = 16.dp)
            D2MSkeleton(width = 90.dp, height = 22.dp, radius = 11.dp)
        }
    }
}

/** Slim segmented progress (counter pill when the batch is long) drawn over the hero under the status bar. */
@Composable
fun DeckProgress(position: Int, total: Int, modifier: Modifier = Modifier) {
    if (total <= 0) return
    if (total <= 12) {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 1..total) {
                Box(
                    Modifier.weight(1f).height(3.dp).clip(CircleShape)
                        .background(if (i <= position) Color.White else Color.White.copy(alpha = 0.35f)),
                )
            }
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            Text(
                LocalStrings.current.discovery.seenOfTotal(position, total),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

/** Floating Pass / Snooze / Accept buttons. Full alternative to the gestures; each carries a content description. */
@Composable
fun SwipeActionBar(
    enabled: Boolean,
    passLabel: String,
    snoozeLabel: String,
    acceptLabel: String,
    onPass: () -> Unit,
    onSnooze: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalD2MStatusPalette.current
    val accentOn = d2m.accentOn
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundAction(60.dp, passLabel, enabled, MaterialTheme.colorScheme.surface, onPass) { drawCross(palette.danger.fg) }
        RoundAction(48.dp, snoozeLabel, enabled, MaterialTheme.colorScheme.surface, onSnooze) { drawClock(palette.info.fg) }
        RoundAction(72.dp, acceptLabel, enabled, d2m.accent, onAccept) { drawCheck(accentOn) }
    }
}

@Composable
private fun RoundAction(
    size: Dp,
    label: String,
    enabled: Boolean,
    background: Color,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.42f)) { icon() }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCross(color: Color) {
    val w = this.size.width; val sw = w * 0.16f
    drawLine(color, Offset(0f, 0f), Offset(w, w), sw, StrokeCap.Round)
    drawLine(color, Offset(w, 0f), Offset(0f, w), sw, StrokeCap.Round)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCheck(color: Color) {
    val w = this.size.width; val sw = w * 0.16f
    drawLine(color, Offset(0f, w * 0.55f), Offset(w * 0.38f, w * 0.92f), sw, StrokeCap.Round)
    drawLine(color, Offset(w * 0.38f, w * 0.92f), Offset(w, w * 0.1f), sw, StrokeCap.Round)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClock(color: Color) {
    val w = this.size.width; val sw = w * 0.12f
    drawCircle(color, radius = w / 2 - sw / 2, style = Stroke(sw))
    drawLine(color, Offset(w / 2, w / 2), Offset(w / 2, w * 0.24f), sw, StrokeCap.Round)
    drawLine(color, Offset(w / 2, w / 2), Offset(w * 0.72f, w * 0.6f), sw, StrokeCap.Round)
}
