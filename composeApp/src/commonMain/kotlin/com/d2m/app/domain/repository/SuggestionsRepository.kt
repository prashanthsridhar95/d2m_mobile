package com.d2m.app.domain.repository

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.model.*
import com.d2m.app.data.network.BufferApi
import com.d2m.app.data.network.SuggestionsApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SuggestionsRepository(
    private val api: SuggestionsApi,
    private val bufferApi: BufferApi,
    private val cache: ApiCache,
    // Optional so existing constructions (tests) keep working; Koin supplies both. Used only by prefetchCandidateDetail,
    // which warms the SAME cache keys the profile panel reads, so they must go through these repositories.
    private val identity: IdentityRepository? = null,
    private val dashboard: DashboardRepository? = null,
) {
    suspend fun getCandidate(primaryId: String, candidateId: String): SuggestionOut =
        api.getCandidate(primaryId, candidateId)

    suspend fun getSuggestions(primaryId: String, forceRefresh: Boolean = false): List<SuggestionOut> {
        val key = "suggestions:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 60_000) { api.getSuggestions(primaryId) }.value
    }

    /** Cached suggestions (even if stale) without suspending, for painting the deck on the first frame. Pair with [getSuggestions] to revalidate. */
    fun peekSuggestions(primaryId: String): List<SuggestionOut>? =
        cache.peekNow<List<SuggestionOut>>("suggestions:$primaryId")?.value

    /** Fire-and-forget cache warm-up of the ranked feed (login / app start / returning to the tab). */
    fun prefetchSuggestions(primaryId: String) {
        cache.prefetch("suggestions:$primaryId", ttlMillis = 60_000) { api.getSuggestions(primaryId) }
    }

    /**
     * Warms everything D2MProfileTabsPanel reads for [candidateId] -- Bio data, About Me, Chart -- plus the viewer's
     * own bio / about-me (used for About-Me trait matching), so swiping to this card never shows a "Loading..." tab.
     * Requests run concurrently, are coalesced with any identical in-flight call, populate the shared ApiCache,
     * and never throw (a 404 chart or missing bio is simply not warmed). Safe to call repeatedly for the same ids.
     */
    suspend fun prefetchCandidateDetail(primaryId: String, candidateId: String) {
        val id = identity
        val dash = dashboard
        coroutineScope {
            if (id != null) {
                launch { runCatching { id.getExtendedBio(candidateId) } }
                launch { runCatching { id.getAboutMe(candidateId) } }
                launch { runCatching { id.getExtendedBio(primaryId) } }
                launch { runCatching { id.getAboutMe(primaryId) } }
            }
            if (dash != null) launch { runCatching { dash.getChart(candidateId) } }
        }
    }

    /** Prefetch detail for the next few cards, in order, a few at a time (don't stampede the 60/min rate limit). */
    suspend fun prefetchUpcomingDetails(primaryId: String, candidateIds: List<String>, limit: Int = 3) {
        candidateIds.take(limit).forEach { prefetchCandidateDetail(primaryId, it) }
    }

    suspend fun getBrowseAll(primaryId: String, forceRefresh: Boolean = false): List<BrowseCandidateOut> {
        val key = "browse-all:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key) { api.getBrowseAll(primaryId) }.value
    }

    suspend fun getReceivedRequests(primaryId: String): List<SuggestionOut> =
        cache.get("requests-received:$primaryId", ttlMillis = 30_000) { api.getReceivedRequests(primaryId) }.value

    suspend fun getSentRequests(primaryId: String): List<SuggestionOut> =
        cache.get("requests-sent:$primaryId", ttlMillis = 30_000) { api.getSentRequests(primaryId) }.value

    suspend fun getShortlist(primaryId: String): List<SuggestionOut> =
        cache.get("shortlist:$primaryId") { api.getShortlist(primaryId) }.value

    suspend fun addToShortlist(primaryId: String, candidateId: String): SuggestionOut {
        val result = api.addToShortlist(primaryId, candidateId)
        cache.invalidateKey("shortlist:$primaryId")
        return result
    }

    suspend fun removeFromShortlist(primaryId: String, candidateId: String) {
        api.removeFromShortlist(primaryId, candidateId)
        cache.invalidateKey("shortlist:$primaryId")
    }

    suspend fun suggestToChild(sponsorId: String, candidateId: String): SuggestToChildOut =
        api.suggestToChild(sponsorId, candidateId)

    /**
     * Accept/Reject/Snooze. On success the acted-on card is dropped from the cached feed IN PLACE (the cache keeps
     * serving the list instantly and revalidates in the background) instead of discarding the whole cache, which
     * used to force a cold, blocking refetch of the entire deck after every swipe. On failure the feed is marked
     * stale so the card reappears after a background refresh.
     *
     * The call is a single POST; for a no-wait UI, advance the deck first and run this in a background scope (see
     * [DelayedActionCommitter] for the undo-able variant).
     */
    suspend fun act(primaryId: String, candidateId: String, action: String): SuggestionActionResponse {
        val result = try {
            bufferApi.act(primaryId, candidateId, action)
        } catch (t: Throwable) {
            cache.softInvalidate("suggestions:$primaryId")
            throw t
        }
        val patched = cache.patchStale<List<SuggestionOut>>("suggestions:$primaryId") { list -> list.filterNot { it.candidateId == candidateId } }
        if (!patched) cache.invalidate("suggestions:$primaryId")
        cache.invalidate("requests-received:$primaryId")
        cache.invalidate("requests-sent:$primaryId")
        // an accept that completes a mutual match creates a thread: without this Matches kept showing the cached
        // "No matches yet" (15s TTL) right after the match happened
        cache.invalidate("threads:$primaryId")
        cache.invalidate("dashboard:")
        return result
    }
}

/**
 * Undo-able, delayed commit for Accept/Reject/Snooze. The backend has NO undo/rewind endpoint
 * (app/routers/buffer.py exposes only POST .../action, and buffer_service.apply_action is final), so the
 * only honest "undo" is to not send the action yet: [schedule] holds it for [delayMillis] (default 4s, i.e. a
 * snackbar's lifetime) and [undo] cancels it before it ever reaches the server. [flush] sends everything
 * pending immediately -- call it when the screen leaves composition / app goes to background so no swipe is lost.
 * If the process is killed inside the window the pending swipe is lost (acceptable for a 4s window; documented).
 */
class DelayedActionCommitter(
    private val repo: SuggestionsRepository,
    private val delayMillis: Long = 4_000,
    // All state is confined to this (single-threaded) dispatcher; the public calls hop onto it, so they are safe from any thread.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1),
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private class Pending(val primaryId: String, val candidateId: String, val action: String, val onResult: (Result<SuggestionActionResponse>) -> Unit)

    private val pending = mutableMapOf<String, Pending>()
    private val jobs = mutableMapOf<String, Job>()

    private fun key(primaryId: String, candidateId: String) = "$primaryId/$candidateId"

    /** Queue [action]; [onResult] fires after the server round-trip (or failure). Re-scheduling the same card replaces the earlier action. */
    suspend fun schedule(primaryId: String, candidateId: String, action: String, onResult: (Result<SuggestionActionResponse>) -> Unit = {}) = withContext(dispatcher) {
        val k = key(primaryId, candidateId)
        jobs.remove(k)?.cancel()
        pending[k] = Pending(primaryId, candidateId, action, onResult)
        jobs[k] = scope.launch {
            delay(delayMillis)
            commit(k)
        }
    }

    /** Cancels a not-yet-sent action. Returns true if it was still pending (i.e. the undo really prevented the send). */
    suspend fun undo(primaryId: String, candidateId: String): Boolean = withContext(dispatcher) {
        val k = key(primaryId, candidateId)
        jobs.remove(k)?.cancel()
        pending.remove(k) != null
    }

    suspend fun hasPending(): Boolean = withContext(dispatcher) { pending.isNotEmpty() }

    /** Sends every pending action now. */
    suspend fun flush() = withContext(dispatcher) {
        jobs.values.toList().forEach { it.cancel() }
        jobs.clear()
        pending.keys.toList().forEach { commit(it) }
    }

    private suspend fun commit(k: String) {
        val p = pending.remove(k) ?: return
        jobs.remove(k)
        p.onResult(runCatching { repo.act(p.primaryId, p.candidateId, p.action) })
    }
}
