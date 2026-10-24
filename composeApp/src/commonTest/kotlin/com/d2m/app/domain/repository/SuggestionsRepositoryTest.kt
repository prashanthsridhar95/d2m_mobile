package com.d2m.app.domain.repository

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.BufferApi
import com.d2m.app.data.network.SuggestionsApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exercises SuggestionsRepository against a MockEngine backend end-to-end
 * through the real ApiCache, rather than mocking the cache -- this is what
 * actually proves the repository's cache-key and invalidate-prefix choices
 * are correct (e.g. that act() invalidates "suggestions:<id>" and not some
 * typo'd variant), which a cache-mocked test would just assume away.
 */
class SuggestionsRepositoryTest {

    private val primaryId = "prim-1"

    private fun repoCountingRequestsTo(
        path: String,
        responseBody: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): Pair<SuggestionsRepository, () -> Int> {
        var count = 0
        val engine = MockEngine { request ->
            if (request.url.encodedPath == path) count++
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val apiClient = ApiClient(engine = HttpClient(engine), baseUrl = "https://example.test")
        val repo = SuggestionsRepository(SuggestionsApi(apiClient), BufferApi(apiClient), ApiCache())
        return repo to { count }
    }

    @Test
    fun getSuggestions_isCached_secondCallDoesNotHitNetwork() = runTest {
        val (repo, count) = repoCountingRequestsTo(
            path = "/primaries/$primaryId/suggestions",
            responseBody = "[]",
        )

        repo.getSuggestions(primaryId)
        repo.getSuggestions(primaryId)

        assertEquals(1, count())
    }

    @Test
    fun getSuggestions_forceRefresh_bypassesCache() = runTest {
        val (repo, count) = repoCountingRequestsTo(
            path = "/primaries/$primaryId/suggestions",
            responseBody = "[]",
        )

        repo.getSuggestions(primaryId)
        repo.getSuggestions(primaryId, forceRefresh = true)

        assertEquals(2, count())
    }

    @Test
    fun act_invalidatesSuggestionsCache_soNextGetRefetches() = runTest {
        var suggestionsRequests = 0
        var actRequests = 0
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == "/primaries/$primaryId/suggestions" -> {
                    suggestionsRequests++
                    respond(SUGGESTIONS_JSON, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
                request.url.encodedPath == "/primaries/$primaryId/suggestions/cand-1/action" -> {
                    actRequests++
                    respond(
                        // snake_case + full required-field set to match
                        // SuggestionActionResponse/ApiClient's Json config
                        // (JsonNamingStrategy.SnakeCase) -- see that DTO's
                        // doc comment for why primary_id/snooze_count/
                        // mutual_match are required, not just candidate_id/status.
                        """{"primary_id":"$primaryId","candidate_id":"cand-1","status":"accepted","snooze_count":0,"mutual_match":false}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                else -> respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val apiClient = ApiClient(engine = HttpClient(engine), baseUrl = "https://example.test")
        val repo = SuggestionsRepository(SuggestionsApi(apiClient), BufferApi(apiClient), ApiCache())

        repo.getSuggestions(primaryId) // caches
        repo.getSuggestions(primaryId) // cache hit, no new request
        assertEquals(1, suggestionsRequests)

        repo.act(primaryId, "cand-1", "accept") // should invalidate "suggestions:$primaryId"
        assertEquals(1, actRequests)

        // The acted-on card is dropped from the cached feed in place: served instantly, no blocking refetch...
        val afterAct = repo.getSuggestions(primaryId)
        assertEquals(listOf("cand-2"), afterAct.map { it.candidateId })
        // ...while a background revalidation brings the authoritative list.
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (suggestionsRequests < 2) delay(10) } }
        assertEquals(2, suggestionsRequests)
    }

    @Test
    fun delayedCommitter_undoPreventsTheSend_andFlushSendsPending() = runTest {
        var actRequests = 0
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/action")) actRequests++
            respond(
                """{"primary_id":"$primaryId","candidate_id":"cand-1","status":"accepted","snooze_count":0,"mutual_match":false}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val apiClient = ApiClient(engine = HttpClient(engine), baseUrl = "https://example.test")
        val repo = SuggestionsRepository(SuggestionsApi(apiClient), BufferApi(apiClient), ApiCache())
        val committer = DelayedActionCommitter(repo, delayMillis = 60_000)

        committer.schedule(primaryId, "cand-1", "accept")
        assertEquals(true, committer.undo(primaryId, "cand-1"))
        assertEquals(false, committer.hasPending())
        committer.flush()
        assertEquals(0, actRequests) // undone before the delay elapsed: never reached the server

        committer.schedule(primaryId, "cand-1", "accept")
        committer.flush()
        assertEquals(1, actRequests)
    }

    private companion object {
        val SUGGESTIONS_JSON = """[{"candidate_id":"cand-1","candidate_name":"A"},{"candidate_id":"cand-2","candidate_name":"B"}]"""
    }
}
