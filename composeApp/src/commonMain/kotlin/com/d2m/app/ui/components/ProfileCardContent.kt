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
import androidx.compose.material.icons.filled.Favorite
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
import com.d2m.app.data.model.AboutMePromptEntry
import com.d2m.app.data.model.PhotoOut
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
    onLike: (LikeTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.discovery
    val apiClient: ApiClient = koinInject()
    val identityRepo: IdentityRepository = koinInject()
    val rating = overallRating(card.scores.compositeScore)
    val firstName = card.candidateName.substringBefore(" ")

    // Seeded from the repository cache (the deck prefetches these) so a promoted card shows its feed on frame one.
    var photos by remember(card.candidateId) { mutableStateOf<List<PhotoOut>>(emptyList()) }
    var prompts by remember(card.candidateId) {
        mutableStateOf(identityRepo.peekAboutMe(card.candidateId)?.aboutPrompts.orEmpty())
    }
    LaunchedEffect(card.candidateId, isTop) {
        if (!isTop) return@LaunchedEffect
        photos = runCatching { identityRepo.getPhotos(card.candidateId) }.getOrDefault(emptyList()).sortedBy { it.sortOrder }
        prompts = runCatching { identityRepo.getAboutMe(card.candidateId) }.getOrNull()?.aboutPrompts ?: prompts
    }
    val heroPhotoId = photos.firstOrNull()?.id
    val feed = remember(photos, prompts) { buildFeed(photos.drop(1), prompts) }

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
                // Top scrim keeps the status bar legible over bright photos.
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
                    val subline = listOfNotNull(card.city, card.occupationTitle).joinToString(" \u00B7 ")
                    if (subline.isNotEmpty()) {
                        Text(subline, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.88f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        D2MLevelPill(rating)
                    }
                }
                if (isTop) {
                    LikeHeart(
                        label = strings.likeThisPhoto,
                        onClick = { onLike(LikeTarget("photo", heroPhotoId, card.photoUrl)) },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 16.dp),
                    )
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

                    // Photos and prompt answers interleaved, each with its own heart (web: discover/ProfileFeed.jsx).
                    feed.forEach { item ->
                        when (item) {
                            is FeedItem.Photo -> FeedPhotoBlock(
                                url = apiClient.resolveMediaUrl(item.photo.url),
                                name = card.candidateName,
                                likeLabel = strings.likeThisPhoto,
                                onLike = { onLike(LikeTarget("photo", item.photo.id, item.photo.url)) },
                            )
                            is FeedItem.Prompt -> FeedPromptBlock(
                                prompt = item.entry.prompt,
                                answer = item.entry.answer,
                                likeLabel = strings.likeThisAnswer,
                                onLike = { onLike(LikeTarget("prompt", item.entry.prompt, prompt = item.entry.prompt, answer = item.entry.answer)) },
                            )
                        }
                    }

                    Text(
                        strings.moreAbout(firstName),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    D2MProfileTabsPanel(
                        candidateId = card.candidateId,
                        modifier = Modifier.fillMaxWidth(),
                        showAboutMe = true,
                        viewerPrimaryId = viewerPrimaryId,
                        hideAboutPrompts = true,
                    )
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
            // Keeps the floating pass button legible once profile text scrolls underneath it; invisible over the hero.
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bottomClearance + 28.dp)
                    .graphicsLayer { alpha = (scroll.value / (heroPx * 0.6f)).coerceIn(0f, 1f) }
                    .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)))),
            )
        }
    }
}

private sealed interface FeedItem {
    data class Photo(val photo: PhotoOut) : FeedItem
    data class Prompt(val entry: AboutMePromptEntry) : FeedItem
}

/** Prompt, photo, prompt, photo ... with whatever is left of either kind appended (the hero is photo #1). */
private fun buildFeed(photos: List<PhotoOut>, prompts: List<AboutMePromptEntry>): List<FeedItem> {
    val out = ArrayList<FeedItem>(photos.size + prompts.size)
    val ph = photos.iterator()
    val pr = prompts.iterator()
    while (ph.hasNext() || pr.hasNext()) {
        if (pr.hasNext()) out += FeedItem.Prompt(pr.next())
        if (ph.hasNext()) out += FeedItem.Photo(ph.next())
    }
    return out
}

@Composable
private fun FeedPhotoBlock(url: String, name: String, likeLabel: String, onLike: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(4f / 5f).clip(RoundedCornerShape(16.dp)).background(d2m.surfaceSunken),
    ) {
        AsyncImage(
            model = url,
            imageLoader = rememberPhotoImageLoader(),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        LikeHeart(likeLabel, onLike, Modifier.align(Alignment.BottomEnd).padding(14.dp))
    }
}

@Composable
private fun FeedPromptBlock(prompt: String, answer: String, likeLabel: String, onLike: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(d2m.surfaceSunken),
    ) {
        Column(Modifier.padding(start = 20.dp, top = 22.dp, bottom = 26.dp, end = 78.dp)) {
            Text(prompt, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = mutedText(0.55f))
            Text(answer, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
        }
        LikeHeart(likeLabel, onLike, Modifier.align(Alignment.TopEnd).padding(14.dp))
    }
}

/** Per-item like control (photo / prompt), Hinge style: a round white button with an accent heart. */
@Composable
fun LikeHeart(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(48.dp)
            .pressScale(interaction)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) { haptics.light(); onClick() }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            androidx.compose.material.icons.Icons.Filled.Favorite,
            contentDescription = null,
            tint = d2m.accent,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** The single floating X: "pass for now" (not a rejection -- the server snoozes it and it comes back later). */
@Composable
fun PassFab(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val ink = d2m.textPrimary
    Box(
        modifier = modifier
            .size(44.dp)
            .pressScale(interaction)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) { haptics.light(); onClick() }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(15.dp)) { drawCross(ink) }
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCross(color: Color) {
    val w = this.size.width; val sw = w * 0.16f
    drawLine(color, Offset(0f, 0f), Offset(w, w), sw, StrokeCap.Round)
    drawLine(color, Offset(w, 0f), Offset(0f, w), sw, StrokeCap.Round)
}
