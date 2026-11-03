package com.d2m.app.data.network

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory

actual fun createPublicLinkPhotoImageLoader(): ImageLoader =
    ImageLoader.Builder(PlatformContext.INSTANCE)
        .components { add(KtorNetworkFetcherFactory(httpClient = publicLinkPhotoHttpClient())) }
        .build()
