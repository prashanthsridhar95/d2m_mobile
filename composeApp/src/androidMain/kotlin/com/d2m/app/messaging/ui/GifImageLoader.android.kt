package com.d2m.app.messaging.ui

import android.os.Build
import coil3.ImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import com.d2m.app.data.session.AndroidSettingsContextHolder

// AnimatedImageDecoder (Android's ImageDecoder API, faster, also handles
// animated WebP/HEIF) on API 28+; GifDecoder (android.graphics.Movie-based)
// below that -- same SDK-gated pairing Coil's own docs recommend, since
// AnimatedImageDecoder isn't available pre-28 at all. This app's minSdk is
// 26 (see libs.versions.toml), so both branches are actually reachable.
actual fun createGifImageLoader(): ImageLoader =
    ImageLoader.Builder(AndroidSettingsContextHolder.appContext)
        .components {
            if (Build.VERSION.SDK_INT >= 28) add(AnimatedImageDecoder.Factory()) else add(GifDecoder.Factory())
        }
        .build()
