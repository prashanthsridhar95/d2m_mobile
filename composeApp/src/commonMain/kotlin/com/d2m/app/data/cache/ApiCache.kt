package com.d2m.app.data.cache

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
// kotlinx-datetime 0.7.x deprecated kotlinx.datetime.Clock in favor of the
// new stdlib kotlin.time.Clock (stable since Kotlin 2.3) -- confirmed
// directly via `klib dump-metadata` that kotlinx.datetime.Clock is now just
// `public typealias Clock = kotlin.time.Clock`. Compose Multiplatform's own
// material3-uikitsimarm64 artifact transitively pulls kotlinx-datetime 0.7.1
// regardless of this project's own (older) 0.6.1 catalog pin, via Gradle's
// normal highest-version-wins conflict resolution -- so this project was
// always actually compiling against 0.7.1, never the 0.6.1 its own catalog
// declared. kotlin.time.Instant (the now() return type) keeps the identical
// toEpochMilliseconds() method, so no call-site changes are needed here,
// only this import.
import kotlin.time.Clock

/**
 * Direct port of d2m_web/src/lib/apiCache.js's design: an in-memory,
 * stale-while-revalidate cache with a 2-minute default TTL, in-flight
 * request dedup for concurrent callers of the same key, and prefix-based
 * bulk invalidation after mutations (e.g. invalidate("suggestions:") after
 * an accept/reject action). Deliberately hand-rolled rather than adopting a
 * query library -- this exact TTL+dedup+invalidate shape is already proven
 * against this API surface on web, so porting it is lower-risk than
 * bringing in something new (see the mobile plan's §2 rationale).
 *
 * Not persisted across process death (matches the web app's in-memory-only
 * apiCache.js) -- see plan §8 Phase 5 for the SQLDelight-backed persistence
 * option if real usage patterns warrant it later.
 */
class ApiCache(
    private val defaultTtlMillis: Long = 2 * 60 * 1000,
    /**
     * Injectable clock, defaulting to the real one so every production call
     * site is unchanged. This exists because TTL expiry is otherwise
     * untestable: `Clock.System.now()` is wall-clock, and
     * kotlinx-coroutines-test's `advanceTimeBy` moves only the virtual
     * scheduler clock, so a test could never actually age an entry past its
     * TTL. ApiCacheTest's stale-while-revalidate case was silently failing
     * for exactly that reason -- it advanced virtual time by 2s against a
     * 1s TTL and the entry stayed fresh, so the assertion that a stale hit
     * returns the OLD value was never really exercised.
     */
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    private data class Entry<T>(val value: T, val fetchedAt: Long)

    private val mutex = Mutex()
    private val entries = mutableMapOf<String, Entry<Any?>>()
    private val inFlight = mutableMapOf<String, CompletableDeferred<Any?>>()

    // Owns background SWR refreshes -- lives as long as the ApiCache singleton
    // itself (app process lifetime), so there's no dangling-scope leak to
    // manage. SupervisorJob so one failed background refresh can't cancel
    // any other in-flight refresh.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun now(): Long = nowMillis()

    /** Result wrapper mirroring useApiQuery's { data, stale } shape. */
    data class CachedResult<T>(val value: T, val stale: Boolean)

    /**
     * Returns the cached value immediately if fresh, kicks off a background
     * refresh if stale-but-present (stale-while-revalidate), or awaits (and
     * dedupes concurrent callers of) a cold fetch if nothing is cached yet.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> get(
        key: String,
        ttlMillis: Long = defaultTtlMillis,
        fetch: suspend () -> T,
    ): CachedResult<T> {
        val cached = mutex.withLock { entries[key] as? Entry<T> }
        val isFresh = cached != null && (now() - cached.fetchedAt) < ttlMillis

        if (cached != null && isFresh) {
            return CachedResult(cached.value, stale = false)
        }

        if (cached != null && !isFresh) {
            // Stale-while-revalidate: return what we have *immediately*, refresh
            // in the background via `scope.launch` (this must not be a plain
            // suspend call -- awaiting it here would block the caller until the
            // refetch completes, defeating the entire point of SWR). Callers
            // that want to know a refresh landed should re-observe via a
            // Flow-backed repository layer (see domain/repository) rather than
            // awaiting this call a second time.
            triggerBackgroundRefresh(key, ttlMillis, fetch)
            return CachedResult(cached.value, stale = true)
        }

        // Cold miss: dedupe concurrent callers onto one in-flight fetch.
        val deferred = mutex.withLock {
            inFlight[key]?.let { return@withLock it as CompletableDeferred<T> }
            val d = CompletableDeferred<Any?>()
            inFlight[key] = d
            null
        }
        if (deferred != null) {
            return CachedResult(deferred.await() as T, stale = false)
        }

        return try {
            val value = fetch()
            mutex.withLock {
                entries[key] = Entry(value, now())
                (inFlight.remove(key) as? CompletableDeferred<Any?>)?.complete(value)
            }
            CachedResult(value, stale = false)
        } catch (t: Throwable) {
            mutex.withLock { (inFlight.remove(key) as? CompletableDeferred<Any?>)?.completeExceptionally(t) }
            throw t
        }
    }

    private fun <T> triggerBackgroundRefresh(key: String, ttlMillis: Long, fetch: suspend () -> T) {
        scope.launch {
            runCatching {
                val value = fetch()
                mutex.withLock { entries[key] = Entry(value, now()) }
            }
        }
    }

    /** Optimistic write -- e.g. after a mutation, seed the cache with the known-good result instead of waiting on a refetch. */
    suspend fun <T> setCached(key: String, value: T) {
        mutex.withLock { entries[key] = Entry(value, now()) }
    }

    /** Prefix-based bulk invalidation, e.g. invalidate("suggestions:") after accept/reject/snooze. */
    suspend fun invalidate(prefix: String) {
        mutex.withLock {
            entries.keys.filter { it.startsWith(prefix) }.forEach { entries.remove(it) }
        }
    }

    suspend fun invalidateKey(key: String) {
        mutex.withLock { entries.remove(key) }
    }

    suspend fun clear() {
        mutex.withLock { entries.clear() }
    }
}
