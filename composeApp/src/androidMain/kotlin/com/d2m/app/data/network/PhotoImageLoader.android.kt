package com.d2m.app.data.network

import coil3.ImageLoader
import coil3.PlatformContext
import com.d2m.app.data.session.AndroidSettingsContextHolder
import com.d2m.app.data.session.IdentityStore

actual fun createPhotoImageLoader(identityStore: IdentityStore): ImageLoader =
    buildPhotoImageLoader(photoPlatformContext(), identityStore)

actual fun photoPlatformContext(): PlatformContext = AndroidSettingsContextHolder.appContext
