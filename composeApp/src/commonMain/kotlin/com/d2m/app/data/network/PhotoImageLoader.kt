package com.d2m.app.data.network

import coil3.ImageLoader
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header

/**
 * A dedicated HttpClient for this API's authenticated photo route (GET
 * /primaries/{id}/photos/{id}/image -- see app/routers/identity.py's
 * get_photo_image / app.auth.get_current_user on the backend, and
 * ApiClient.resolveMediaUrl, which already builds the right absolute URL
 * for it). Deliberately its OWN client/ImageLoader (see
 * createPhotoImageLoader's expect/actual below), not the app-wide Coil
 * singleton every plain AsyncImage uses by default: GifPicker.kt's GIF
 * search hits api.giphy.com directly through that default loader, and
 * chat media (ChatPane.kt/MediaViewerDialog.kt) is already-decrypted
 * local content -- neither should ever receive this app's internal
 * identity header. Scoping the header to a client that ONLY ever talks to
 * this app's own backend keeps that leak structurally impossible, rather
 * than relying on a per-request host-allowlist check that's easy to
 * forget to add somewhere later.
 *
 * X-D2M-User-Id is app.auth.get_current_user's dev-mode fallback on the
 * backend, the same header d2m_web's src/lib/media.js attaches -- there's
 * no bearer token yet (see ApiClient.kt's own doc comment on the missing
 * RBAC layer). Swapping this for a real `Authorization: Bearer <token>`
 * header once the separate auth service exists is the only change this
 * file will need.
 */
internal fun photoAuthHttpClient(identityStore: IdentityStore): HttpClient =
    httpEngine().config {
        defaultRequest {
            val identity = identityStore.identity.value
            val userId = if (identity.role == D2MRole.PARENT) identity.sponsorId else identity.primaryId
            if (userId != null) header("X-D2M-User-Id", userId)
        }
    }

/**
 * expect/actual purely to resolve each platform's Coil [coil3.PlatformContext]
 * (Android needs a real Context, see AndroidSettingsContextHolder; iOS uses
 * PlatformContext.INSTANCE) -- same split as GifImageLoader.kt's
 * createGifImageLoader for the same reason. Built once as a Koin single
 * (see AppModule.kt) and handed to every profile-photo AsyncImage via
 * LocalPhotoImageLoader (ui/components/ProfilePhoto.kt).
 */
expect fun createPhotoImageLoader(identityStore: IdentityStore): ImageLoader
