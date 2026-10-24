package com.d2m.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Warms Coil's cache for the photos a list is about to scroll into.
 * `firstVisibleIndex` is read inside snapshotFlow so scrolling never
 * recomposes the caller; `urlAt(i)` returns the resolved photo URL of item
 * i (or null). Prefetch is fire-and-forget through the same authenticated
 * loader the cards use, so cache keys match.
 */
@Composable
fun PrefetchPhotos(
    count: Int,
    firstVisibleIndex: () -> Int,
    urlAt: (Int) -> String?,
    ahead: Int = 6,
    perRow: Int = 1,
) {
    val context = LocalPlatformContext.current
    val loader = rememberPhotoImageLoader()
    val latestUrlAt = androidx.compose.runtime.rememberUpdatedState(urlAt)
    val done = remember { HashSet<Int>() }
    LaunchedEffect(count, loader) {
        snapshotFlow { firstVisibleIndex() }
            .distinctUntilChanged()
            .collect { first ->
                val from = first + perRow
                val to = minOf(count - 1, first + ahead * perRow)
                for (i in from..to) {
                    if (!done.add(i)) continue
                    val url = latestUrlAt.value(i) ?: continue
                    loader.enqueue(ImageRequest.Builder(context).data(url).build())
                }
            }
    }
}
