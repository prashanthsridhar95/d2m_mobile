package com.d2m.app.data.network

import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.d2m.app.data.session.AndroidSettingsContextHolder
import com.d2m.app.data.session.IdentityStore

actual fun createPhotoImageLoader(identityStore: IdentityStore): ImageLoader =
    ImageLoader.Builder(AndroidSettingsContextHolder.appContext)
        .components { add(KtorNetworkFetcherFactory(httpClient = photoAuthHttpClient(identityStore))) }
        .build()
