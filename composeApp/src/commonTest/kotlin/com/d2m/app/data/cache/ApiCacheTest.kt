package com.d2m.app.data.cache

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the three behaviors ApiCache exists to guarantee (see its own doc
 * comment): TTL-based freshness, in-flight dedup of concurrent cold-miss
 * callers, and prefix-based invalidation after a mutation. Also locks in the
 * stale-while-revalidate fix made in this hardening pass -- a stale hit must
 * return the OLD value immediately, not block on the refresh.
 */
class ApiCacheTest {

    @Test
    fun freshHit_returnsCachedValue_withoutRefetching() = runTest {
        val cache = ApiCache(defaultTtlMillis = 60_000)
        var fetchCount = 0
        val fetch: suspend () -> String = { fetchCount++; "value-$fetchCount" }

        val first = cache.get("k", fetch = fetch)
        val second = cache.get("k", fetch = fetch)

        assertEquals("value-1", first.value)
        assertEquals("value-1", second.value)
        assertFalse(second.stale)
        assertEquals(1, fetchCount)
    }

    @Test
    fun expiredEntry_isTreatedAsStale_andReturnsOldValueImmediately() = runTest {
        // Drive the cache's clock explicitly rather than through
        // advanceTimeBy: ApiCache reads wall-clock time, which the virtual
        // test scheduler does not control (see ApiCache's nowMillis param).
        var clock = 0L
        val cache = ApiCache(defaultTtlMillis = 1_000, nowMillis = { clock })
        var fetchCount = 0
        val fetch: suspend () -> String = { fetchCount++; "value-$fetchCount" }

        val first = cache.get("k", ttlMillis = 1_000, fetch = fetch)
        assertEquals("value-1", first.value)

        clock += 2_000

        // The stale hit must hand back the OLD value right away (this is the
        // ApiCache.kt bug fixed in this pass: triggerBackgroundRefresh() was
        // previously awaited inline, which blocked this call until the
        // refetch completed -- defeating stale-while-revalidate entirely).
        val staleHit = cache.get("k", ttlMillis = 1_000, fetch = fetch)
        assertEquals("value-1", staleHit.value)
        assertTrue(staleHit.stale)
    }

    @Test
    fun concurrentColdMiss_dedupesIntoOneFetch() = runTest {
        val cache = ApiCache()
        var fetchCount = 0
        val fetch: suspend () -> String = {
            fetchCount++
            delay(50)
            "value-$fetchCount"
        }

        val a = async { cache.get("k", fetch = fetch) }
        val b = async { cache.get("k", fetch = fetch) }
        val c = async { cache.get("k", fetch = fetch) }

        val results = listOf(a.await(), b.await(), c.await())

        assertEquals(1, fetchCount)
        assertTrue(results.all { it.value == "value-1" })
    }

    @Test
    fun invalidate_byPrefix_removesOnlyMatchingKeys() = runTest {
        val cache = ApiCache()
        cache.setCached("suggestions:p1", "a")
        cache.setCached("suggestions:p2", "b")
        cache.setCached("dashboard:p1", "c")

        cache.invalidate("suggestions:")

        var suggestionsFetches = 0
        var dashboardFetches = 0
        val suggestions = cache.get("suggestions:p1") { suggestionsFetches++; "refetched" }
        val dashboard = cache.get("dashboard:p1") { dashboardFetches++; "refetched" }

        assertEquals(1, suggestionsFetches) // was invalidated -> refetched
        assertEquals(0, dashboardFetches) // untouched -> still cached
        assertEquals("refetched", suggestions.value)
        assertEquals("c", dashboard.value)
    }

    @Test
    fun invalidateKey_removesSingleEntry() = runTest {
        val cache = ApiCache()
        cache.setCached("shortlist:p1", "a")

        cache.invalidateKey("shortlist:p1")

        var fetches = 0
        val result = cache.get("shortlist:p1") { fetches++; "refetched" }
        assertEquals(1, fetches)
        assertEquals("refetched", result.value)
    }

    @Test
    fun clear_removesEverything() = runTest {
        val cache = ApiCache()
        cache.setCached("a", 1)
        cache.setCached("b", 2)

        cache.clear()

        var fetches = 0
        cache.get("a") { fetches++; 99 }
        assertEquals(1, fetches)
    }

    @Test
    fun staleRefresh_isCoalesced_manyStaleReadsTriggerOneRefetch() = runTest {
        var clock = 0L
        val cache = ApiCache(defaultTtlMillis = 1_000, nowMillis = { clock })
        var fetchCount = 0
        val gate = CompletableDeferred<Unit>()
        val fetch: suspend () -> String = { fetchCount++; if (fetchCount > 1) gate.await(); "value-$fetchCount" }

        cache.get("k", fetch = fetch)
        clock += 5_000
        repeat(5) { assertEquals("value-1", cache.get("k", fetch = fetch).value) }
        withContext(Dispatchers.Default) { delay(100) }
        assertEquals(2, fetchCount) // 1 initial + exactly 1 shared refresh, not 5
        gate.complete(Unit)
    }

    @Test
    fun invalidateDuringFetch_discardsTheStaleResult() = runTest {
        val cache = ApiCache()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val first = async { cache.get("k") { started.complete(Unit); release.await(); "pre-mutation" } }
        started.await()
        cache.invalidateKey("k") // a mutation lands while the GET is in flight
        release.complete(Unit)
        assertEquals("pre-mutation", first.await().value) // the caller that asked still gets its answer

        var fetches = 0
        val next = cache.get("k") { fetches++; "post-mutation" }
        assertEquals(1, fetches) // the old response must NOT have been written into the cache
        assertEquals("post-mutation", next.value)
    }

    @Test
    fun cancellingOneWaiter_doesNotCancelTheSharedFetch() = runTest {
        val cache = ApiCache()
        val release = CompletableDeferred<Unit>()
        var fetchCount = 0
        val fetch: suspend () -> String = { fetchCount++; release.await(); "v" }
        val a = async { cache.get("k", fetch = fetch) }
        val b = async { cache.get("k", fetch = fetch) }
        withContext(Dispatchers.Default) { delay(50) }
        a.cancel()
        release.complete(Unit)
        assertEquals("v", b.await().value)
        assertEquals(1, fetchCount)
    }

    @Test
    fun patchStale_servesPatchedValueInstantly_thenRevalidates() = runTest {
        val cache = ApiCache()
        var fetchCount = 0
        cache.get("k") { fetchCount++; listOf(1, 2, 3) }
        assertTrue(cache.patchStale<List<Int>>("k") { it.filter { n -> n != 2 } })
        val r = cache.get("k") { fetchCount++; listOf(1, 3, 4) }
        assertEquals(listOf(1, 3), r.value)
        assertTrue(r.stale)
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (fetchCount < 2) delay(10) } }
        withContext(Dispatchers.Default) { delay(50) }
        assertEquals(listOf(1, 3, 4), cache.get("k") { listOf(9) }.value)
    }

    @Test
    fun peekNow_returnsCachedValueSynchronously_andHardExpires() = runTest {
        var clock = 0L
        val cache = ApiCache(defaultTtlMillis = 1_000, nowMillis = { clock })
        assertEquals(null, cache.peekNow<String>("k"))
        cache.setCached("k", "v")
        assertEquals("v", cache.peekNow<String>("k")?.value)
        clock += 2_000
        assertTrue(cache.peekNow<String>("k")!!.stale)
        clock += ApiCache.DEFAULT_MAX_STALE_MILLIS
        assertEquals(null, cache.peekNow<String>("k"))
    }
}
