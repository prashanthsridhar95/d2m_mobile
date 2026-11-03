package com.d2m.app.data.network

import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.d2m.app.data.session.AndroidSettingsContextHolder

actual fun createPublicLinkPhotoImageLoader(): ImageLoader =
    ImageLoader.Builder(AndroidSettingsContextHolder.appContext)
        .components { add(KtorNetworkFetcherFactory(httpClient = publicLinkPhotoHttpClient())) }
        .build()
