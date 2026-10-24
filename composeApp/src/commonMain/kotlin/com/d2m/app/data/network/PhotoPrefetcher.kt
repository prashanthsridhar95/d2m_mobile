package com.d2m.app.data.network

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.memory.MemoryCache
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.size.Size
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Warms the photo loader's memory cache for profiles the user is ABOUT to see
 * (next cards in the swipe deck, the matches list below the fold), so the
 * photo is already decoded by the time the card composes. Fire-and-forget,
 * deduped, never throws, and runs on Coil's own dispatchers.
 *
 * Callers pass whatever string they will later hand to ProfilePhoto -- a
 * relative `/primaries/.../image` path or an already-absolute URL; this resolves
 * it with the same [ApiClient.resolveMediaUrl] the screens use, so the
 * memory-cache key (the URL string) matches the later display request.
 * Prefetch decodes to at most [maxDimensionPx] so a 12MP original is never
 * held in memory; any display request at or below that size is a cache hit.
 */
class PhotoPrefetcher(
    private val loader: ImageLoader,
    private val context: PlatformContext,
    private val resolve: (String) -> String,
    private val maxDimensionPx: Int = 1080,
) {
    private val lock = Mutex()
    private val pending = LinkedHashMap<String, Disposable>()

    /** Enqueue up to [limit] of the first non-blank entries; returns how many requests were actually started. */
    suspend fun prefetch(urls: List<String?>, limit: Int = 4): Int = lock.withLock {
        var started = 0
        var considered = 0
        for (raw in urls) {
            if (raw.isNullOrBlank()) continue
            if (considered++ >= limit) break
            val url = resolve(raw)
            if (isWarm(url)) continue
            val existing = pending[url]
            if (existing != null && !existing.job.isCompleted) continue
            pending[url] = loader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .size(Size(maxDimensionPx, maxDimensionPx))
                    .build(),
            )
            started++
        }
        // Don't let the bookkeeping map grow for the life of the process.
        if (pending.size > 64) pending.entries.removeAll { it.value.job.isCompleted }
        started
    }

    private fun isWarm(url: String): Boolean = loader.memoryCache?.get(MemoryCache.Key(url)) != null

    suspend fun cancelAll() = lock.withLock {
        pending.values.forEach { it.dispose() }
        pending.clear()
    }
}
