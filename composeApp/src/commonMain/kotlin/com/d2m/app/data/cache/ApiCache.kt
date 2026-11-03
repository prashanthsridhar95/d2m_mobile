package com.d2m.app.data.cache

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.async
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

    private data class Entry<T>(val value: T, val fetchedAt: Long, val forceStale: Boolean = false)

    private val mutex = Mutex()
    private val entries = mutableMapOf<String, Entry<Any?>>()

    // One shared fetch per key, cold OR background refresh. Everyone who
    // wants key K while a fetch for K is running awaits this same Deferred.
    private val inFlight = mutableMapOf<String, Deferred<Any?>>()

    // Bumped by every invalidate*/clear. A fetch remembers the epoch it
    // started under and refuses to write its (now possibly pre-mutation)
    // result into the cache if the epoch moved -- otherwise an act() that
    // invalidates "suggestions:" while a GET is in flight would have the old
    // response repopulate the cache right behind it.
    private var epoch = 0L

    // Owns every fetch -- lives as long as the ApiCache singleton (app
    // process lifetime). Running fetches here, not in the caller's scope,
    // means a screen leaving composition cancels only ITS await, never the
    // shared network call other waiters (or the cache warm-up) still need.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun now(): Long = nowMillis()

    /** Result wrapper mirroring useApiQuery's { data, stale } shape. */
    data class CachedResult<T>(val value: T, val stale: Boolean)

    /**
     * Returns the cached value immediately if fresh, kicks off a (coalesced)
     * background refresh if stale-but-present (stale-while-revalidate), or
     * awaits a coalesced cold fetch if nothing usable is cached.
     *
     * [maxStaleMillis]: an entry older than this is too old to show even
     * while revalidating and is treated as a cold miss.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> get(
        key: String,
        ttlMillis: Long = defaultTtlMillis,
        maxStaleMillis: Long = DEFAULT_MAX_STALE_MILLIS,
        fetch: suspend () -> T,
    ): CachedResult<T> {
        var join: Deferred<Any?>? = null
        var staleValue: Entry<T>? = null
        mutex.withLock {
            val cached = entries[key] as? Entry<T>
            val age = if (cached != null) now() - cached.fetchedAt else 0L
            if (cached != null && !cached.forceStale && age < ttlMillis) return CachedResult(cached.value, stale = false)
            if (cached != null && age < maxStaleMillis) {
                staleValue = cached
                if (inFlight[key] == null) inFlight[key] = startFetch(key, fetch)
            } else {
                join = inFlight[key] ?: startFetch(key, fetch).also { inFlight[key] = it }
            }
        }
        staleValue?.let { return CachedResult(it.value, stale = true) }
        return CachedResult(join!!.await() as T, stale = false)
    }

    /** Must be called with [mutex] held. Lazy start so the Deferred is registered before the fetch can finish. */
    private fun <T> startFetch(key: String, fetch: suspend () -> T): Deferred<Any?> {
        val startedEpoch = epoch
        val d = scope.async(start = CoroutineStart.LAZY) {
            try {
                val value = fetch()
                mutex.withLock { if (epoch == startedEpoch) entries[key] = Entry(value, now()) }
                value
            } finally {
                // Only clear OUR registration: an invalidate may already have replaced it with a newer fetch.
                val me = currentCoroutineContext()[Job]
                withContext(NonCancellable) { mutex.withLock { if (inFlight[key] === me) inFlight.remove(key) } }
            }
        }
        d.start()
        return d
    }

    /**
     * Cached value for [key] regardless of age (null if absent or hard-expired). For cached-first UIs
     * that paint immediately and then call [get] to revalidate.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> peek(key: String, maxStaleMillis: Long = DEFAULT_MAX_STALE_MILLIS): CachedResult<T>? = mutex.withLock {
        val e = entries[key] as? Entry<T> ?: return@withLock null
        val age = now() - e.fetchedAt
        if (age >= maxStaleMillis) null else CachedResult(e.value, stale = e.forceStale || age >= defaultTtlMillis)
    }

    /**
     * Non-suspending [peek] for use directly in composition (`remember { cache.peekNow(key) }`) so a screen
     * can paint cached data on its very first frame instead of flashing a loader. Returns null if the
     * cache lock is momentarily held (the caller just falls back to the normal async path).
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> peekNow(key: String, maxStaleMillis: Long = DEFAULT_MAX_STALE_MILLIS): CachedResult<T>? {
        if (!mutex.tryLock()) return null
        try {
            val e = entries[key] as? Entry<T> ?: return null
            val age = now() - e.fetchedAt
            return if (age >= maxStaleMillis) null else CachedResult(e.value, stale = e.forceStale || age >= defaultTtlMillis)
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Rewrites a cached value in place (e.g. drop the candidate just acted on from the cached suggestion list)
     * and marks it stale so the next [get] returns the patched value instantly AND revalidates in the background.
     * No-op if nothing is cached. Returns whether an entry was patched.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> patchStale(key: String, transform: (T) -> T): Boolean = mutex.withLock {
        val e = entries[key] as? Entry<T> ?: return@withLock false
        epoch++
        inFlight.remove(key)
        entries[key] = Entry(transform(e.value), e.fetchedAt, forceStale = true)
        true
    }

    /** Like [invalidate] but keeps the values: entries under [prefix] are served once more (stale) while a refresh runs. */
    suspend fun softInvalidate(prefix: String) {
        mutex.withLock {
            epoch++
            inFlight.keys.filter { it.startsWith(prefix) }.forEach { inFlight.remove(it) }
            entries.keys.filter { it.startsWith(prefix) }.forEach { k -> entries[k]?.let { entries[k] = it.copy(forceStale = true) } }
        }
    }

    /** Fire-and-forget warm-up: fetches [key] in the cache's own scope unless fresh or already in flight. Never throws. */
    fun <T> prefetch(key: String, ttlMillis: Long = defaultTtlMillis, fetch: suspend () -> T) {
        scope.launch { runCatching { get(key, ttlMillis, fetch = fetch) } }
    }

    /** Optimistic write -- e.g. after a mutation, seed the cache with the known-good result instead of waiting on a refetch. */
    suspend fun <T> setCached(key: String, value: T) {
        mutex.withLock { epoch++; entries[key] = Entry(value, now()); inFlight.remove(key) }
    }

    /** Prefix-based bulk invalidation, e.g. invalidate("suggestions:") after accept/reject/snooze. */
    suspend fun invalidate(prefix: String) {
        mutex.withLock {
            epoch++
            entries.keys.filter { it.startsWith(prefix) }.forEach { entries.remove(it) }
            inFlight.keys.filter { it.startsWith(prefix) }.forEach { inFlight.remove(it) }
        }
    }

    suspend fun invalidateKey(key: String) {
        mutex.withLock { epoch++; entries.remove(key); inFlight.remove(key) }
    }

    suspend fun clear() {
        mutex.withLock { epoch++; entries.clear(); inFlight.clear() }
    }

    companion object {
        /** Beyond this age a cached value is not worth flashing even while revalidating. */
        const val DEFAULT_MAX_STALE_MILLIS = 30 * 60 * 1000L
    }
}
