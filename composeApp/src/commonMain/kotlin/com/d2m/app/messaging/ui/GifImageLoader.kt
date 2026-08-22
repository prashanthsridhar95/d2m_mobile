package com.d2m.app.messaging.ui

import coil3.ImageLoader

/**
 * A Coil [ImageLoader] with animated-GIF decoding registered, on top of
 * everything the app's normal default loader already gets for free via
 * Coil3's automatic service-loader component discovery (the network
 * fetcher from coil-network-ktor3, in particular -- adding components
 * manually here does NOT disable that discovery, it only takes precedence
 * over it, so this still loads http(s) URLs exactly like every other
 * AsyncImage in the app).
 *
 * Reported directly: "GIF sent/received should be autoplaying in the
 * bubble atleast for 2-3 times." Root cause: neither `coil-compose` nor
 * `coil-network-ktor3` (this app's only Coil dependencies before this
 * change) include a GIF decoder at all -- Coil's default `ImageLoader`
 * decodes a GIF as a plain static bitmap (first frame only), same as any
 * other image format it doesn't have a decoder for. There was no animation
 * happening yet to control the loop count of; that's the actual gap this
 * closes, separate from GifBubbleImage's repeatCount/tap-to-replay logic
 * in ChatPane.kt.
 *
 * expect/actual because the decoder classes themselves
 * (`coil3.gif.AnimatedImageDecoder`/`GifDecoder`) are Android-only --
 * `coil-gif` publishes no iOS/Native artifact (ImageDecoder/Movie are
 * Android platform APIs) -- see GifImageLoader.ios.kt for the resulting
 * scope boundary there.
 */
expect fun createGifImageLoader(): ImageLoader
