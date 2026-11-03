package com.d2m.app.data.network

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
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
            // The photo route authenticates the session (get_current_user), same as every other endpoint --
            // without the bearer token every photo request is rejected and cards show no photo at all.
            identity.wedlockAccessToken?.let { header("Authorization", "Bearer $it") }
        }
    }

/**
 * Photo cache policy -- why it looks the way it does.
 *
 * The backend (app/routers/identity.py get_photo_image) decrypts + invisibly
 * watermarks every view (~150-200 ms server-side) and answers
 * `Cache-Control: private, no-store` on purpose, so a decrypted photo never
 * rests on disk. Coil's disk cache honours that header, so it would never
 * store anything anyway; we turn it off explicitly to make that intent
 * unambiguous, and put the speed into the IN-PROCESS memory cache instead
 * (Coil's memory cache is header-agnostic and never touches disk): a large,
 * strongly-referenced bitmap cache that survives screen changes for the whole
 * session. A photo seen once -- or prefetched by PhotoPrefetcher -- is instant
 * on every revisit until the OS reclaims the process.
 *
 * If product later decides on-disk photo caching is acceptable, flip
 * PHOTO_DISK_CACHE_BYTES > 0 AND have the backend send a bounded
 * `Cache-Control: private, max-age=...` for the image route; Coil's default
 * cache strategy will then pick it up with no further client change.
 */
internal const val PHOTO_MEMORY_CACHE_PERCENT = 0.30
internal const val PHOTO_CROSSFADE_MS = 120

/** Shared by both platform actuals. */
internal fun buildPhotoImageLoader(context: PlatformContext, identityStore: IdentityStore): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(KtorNetworkFetcherFactory(httpClient = photoAuthHttpClient(identityStore))) }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, PHOTO_MEMORY_CACHE_PERCENT).build() }
        .diskCache(null)
        .crossfade(PHOTO_CROSSFADE_MS)
        .build()

/**
 * expect/actual purely to resolve each platform's Coil [PlatformContext]
 * (Android needs a real Context, see AndroidSettingsContextHolder; iOS uses
 * PlatformContext.INSTANCE) -- same split as GifImageLoader.kt's
 * createGifImageLoader for the same reason. Built once as a Koin single
 * (see AppModule.kt) and handed to every profile-photo AsyncImage via
 * LocalPhotoImageLoader (ui/components/ProfilePhoto.kt).
 */
expect fun createPhotoImageLoader(identityStore: IdentityStore): ImageLoader

/** The Coil [PlatformContext] the photo loader was built with -- PhotoPrefetcher needs it to build ImageRequests. */
expect fun photoPlatformContext(): PlatformContext
