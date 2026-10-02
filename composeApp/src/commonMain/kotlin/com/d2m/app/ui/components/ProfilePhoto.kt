package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.d2m

/**
 * The authenticated photo ImageLoader (data/network/PhotoImageLoader.kt),
 * provided once at the App() root (App.kt) via CompositionLocalProvider so
 * every ProfilePhoto/ProfileThumb below it -- MatchCard, DiscoveryScreen,
 * ChildHomeScreen's own inlined AsyncImage spots -- picks it up without
 * threading an ImageLoader parameter through every intermediate composable.
 * Falls back to Coil's plain default loader (unauthenticated) rather than
 * crashing if something renders a photo outside that provider, e.g. a
 * preview/test composition.
 */
val LocalPhotoImageLoader = compositionLocalOf<ImageLoader?> { null }

@Composable
fun rememberPhotoImageLoader(): ImageLoader =
    LocalPhotoImageLoader.current ?: SingletonImageLoader.get(LocalPlatformContext.current)

/*
 * Profile photograph, with the comps' treatment for not having one.
 *
 * Worth its own file because "no photograph" is the common case in this
 * product, not the edge case -- plenty of families register without one,
 * or hold it back deliberately -- and the comps take that seriously: a
 * missing photo is a warm sunken panel with a line-art figure at the same
 * aspect ratio as a real one, optionally captioned. Not a grey box, not a
 * collapsed card, not an initial in a circle. The card's geometry is
 * identical either way, so a list of mostly-unphotographed profiles still
 * reads as a tidy register.
 *
 * The old code did this inline in four places with three different
 * fallbacks: an AsyncImage over a mutedText(0.1f) box, an initial letter
 * in a circle, and an empty tinted square.
 */

/**
 * The line-art figure. Drawn with Canvas primitives rather than shipped as
 * a vector asset so it inherits the theme colour and costs nothing in
 * either target's resource table.
 */
@Composable
fun PersonGlyph(size: Dp = 34.dp, color: Color? = null, modifier: Modifier = Modifier) {
    val tint = color ?: d2m.label
    Box(
        modifier.size(size).drawBehind {
            val w = this.size.width
            val h = this.size.height
            val stroke = Stroke(width = w * 0.045f)
            // Head
            drawCircle(
                color = tint,
                radius = w * 0.155f,
                center = Offset(w / 2f, h * 0.355f),
                style = stroke,
            )
            // Shoulders -- an arc rather than a filled blob, matching the
            // comps' outline treatment.
            drawArc(
                color = tint,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.19f, h * 0.58f),
                size = Size(w * 0.62f, h * 0.62f),
                style = stroke,
            )
        },
    )
}

/**
 * A photograph band. `ratio` is width/height -- the comps use 4:3 for a
 * browse card's band and 1:1 for a detail screen's main photo. Expressed
 * as an aspect ratio rather than a fixed height so it scales with the
 * card's own width instead of letterboxing on a narrow phone.
 *
 * `caption` renders under the glyph in the placeholder only: it is
 * information about the profile ("No photograph on file"), not a caption
 * on an image that exists.
 *
 * `overlay` draws inside the frame, for the tag and shortlist star a
 * browse card puts on top of the photograph.
 */
@Composable
fun ProfilePhoto(
    photoUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    ratio: Float = 4f / 3f,
    caption: String? = null,
    glyphSize: Dp = 34.dp,
    shape: Shape = RoundedCornerShape(0.dp),
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    // A hero banner (ChildHomeScreen.kt) needs a fixed height, not an
    // aspect ratio -- its loading skeleton is also a fixed height (see
    // that file's HeroBanner), and an aspect-ratio frame would stop
    // matching that skeleton's height the moment real content (a
    // different rendered width than assumed) replaces it, reintroducing
    // exactly the layout-jump a skeleton exists to prevent. null (the
    // default) keeps every existing caller's aspect-ratio behavior
    // unchanged.
    fixedHeight: Dp? = null,
    // ChildHomeScreen.kt's hero banner shows the peer's initial letter
    // when they have no photo, not this component's own PersonGlyph --
    // a deliberate, pre-existing choice (mirrors HomeScreen.jsx's
    // HeroBanner) this refactor isn't here to overrule. null (the
    // default) keeps every existing caller's PersonGlyph+caption
    // fallback unchanged.
    fallback: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier
            .fillMaxWidth()
            .then(if (fixedHeight != null) Modifier.height(fixedHeight) else Modifier.aspectRatio(ratio))
            .clip(shape)
            .background(d2m.surfaceSunken),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                imageLoader = rememberPhotoImageLoader(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (fallback != null) {
            fallback()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PersonGlyph(glyphSize)
                if (caption != null) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = d2m.label,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        }
        overlay?.invoke(this)
    }
}

/**
 * Fixed-size square variant, for row-shaped compact cards and chat
 * avatars where a fluid aspect-ratio band makes no sense.
 *
 * `overlay` mirrors ProfilePhoto's own slot above -- added so a shortlist
 * star can anchor to this thumb's own corner (MatchCard's COMPACT row)
 * instead of sitting in a separate column disconnected from the photo it's
 * supposedly marking.
 */
@Composable
fun ProfileThumb(
    photoUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: Shape = RoundedCornerShape(D2MRadius.md),
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    // ChildHomeScreen.kt's PhotoCircle shows the peer's initial letter
    // when they have no photo, not this component's own PersonGlyph --
    // same deliberate, pre-existing per-context choice ProfilePhoto's
    // own `fallback` param documents (mirrors PhotoCircle.jsx). null (the
    // default) keeps every existing caller's PersonGlyph fallback
    // unchanged.
    fallback: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier.size(size).clip(shape).background(d2m.surfaceSunken),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                imageLoader = rememberPhotoImageLoader(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (fallback != null) {
            fallback()
        } else {
            PersonGlyph(size * 0.5f)
        }
        overlay?.invoke(this)
    }
}
