package com.d2m.app.messaging.ui

import coil3.request.ImageRequest

/**
 * Caps how many times an animated GIF loops before freezing on its last
 * frame -- `coil3.gif.repeatCount(Int)` is an Android-only extension
 * (part of the `coil-gif` artifact, see GifImageLoader.kt), so this is
 * expect/actual for the same reason that whole file is. `repeatCount`
 * itself is Coil's "repeat N times AFTER the first play" -- so
 * [GIF_BUBBLE_REPEAT_COUNT] = 2 plays a GIF 3 times total, matching what
 * was asked for ("autoplaying... atleast for 2-3 times").
 *
 * No-op on iOS: without a GIF decoder registered there at all (see
 * GifImageLoader.ios.kt), there's no animation for a repeat count to apply
 * to in the first place.
 */
expect fun ImageRequest.Builder.applyGifRepeatCount(repeatCount: Int): ImageRequest.Builder

/** Total plays = this + 1 (Coil's repeatCount is "repeats after the first play"). */
const val GIF_BUBBLE_REPEAT_COUNT = 2
