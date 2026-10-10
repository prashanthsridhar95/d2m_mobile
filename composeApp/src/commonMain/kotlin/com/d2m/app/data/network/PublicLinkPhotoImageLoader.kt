package com.d2m.app.data.network

import coil3.ImageLoader
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header

/**
 * Mirrors PhotoImageLoader.kt's own doc comment, for the public share-link
 * viewer's photos (GET /links/{code}/photos/{photo_id} -- see
 * app/routers/links.py's view_shared_photo) instead of the authenticated
 * /primaries/{id}/photos/{id}/image route. A dedicated client/loader for
 * the same reason PhotoImageLoader.kt's is dedicated: scoping the gate
 * headers to a client that ONLY ever talks to this one route keeps them
 * from leaking onto an unrelated request (GifPicker's giphy.com calls,
 * already-decrypted chat media, etc.) structurally, not by convention.
 *
 * Unlike the authenticated loader, the header values here aren't derived
 * from IdentityStore -- they're per-VISIT state (a resolved child viewer's
 * own id, or an anonymous visitor's freshly-minted lead_key), only known
 * once ProfileLinkScreen.kt actually loads. PublicLinkViewerGate below is
 * the small mutable holder that bridges that per-visit state into this
 * client's defaultRequest block, which Ktor evaluates per-request (not
 * once at client construction) -- so it's safe to set these fields after
 * the ImageLoader/HttpClient already exist.
 */
object PublicLinkViewerGate {
    var viewerPrimaryId: String? = null
    var leadKey: String? = null

    fun reset() {
        viewerPrimaryId = null
        leadKey = null
    }
}

internal fun publicLinkPhotoHttpClient(): HttpClient =
    httpEngine().config {
        defaultRequest {
            PublicLinkViewerGate.viewerPrimaryId?.let { header("X-D2M-User-Id", it) }
            PublicLinkViewerGate.leadKey?.let { header("X-D2M-Lead-Key", it) }
        }
    }

/** expect/actual purely to resolve each platform's Coil PlatformContext -- see PhotoImageLoader.kt's createPhotoImageLoader for the identical split. */
expect fun createPublicLinkPhotoImageLoader(): ImageLoader
