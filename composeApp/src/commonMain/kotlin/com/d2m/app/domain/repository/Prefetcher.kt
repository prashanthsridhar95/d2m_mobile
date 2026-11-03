package com.d2m.app.domain.repository

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.network.PhotoPrefetcher
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * One call that warms the caches the first screens read, so they paint from memory instead of showing
 * skeletons. Fire-and-forget on its own scope, deduped through ApiCache's coalescing (a screen asking for the
 * same key while a warm-up is mid-flight simply joins it), never throws.
 *
 * Call [warmUp]:
 *  - at app start when IdentityStore already holds an identity (returning user), and
 *  - right after a successful login / claim / onboarding finishes setting the identity.
 * Call [reset] on logout (clears every cached response so a different account never sees the last one's data).
 *
 * The keys warmed are exactly the ones ChildHomeScreen / ParentHomeScreen / DiscoveryScreen /
 * NotificationsScreen read, because they all go through the same repositories.
 */
class Prefetcher(
    private val identityStore: IdentityStore,
    private val suggestions: SuggestionsRepository,
    private val dashboard: DashboardRepository,
    private val identity: IdentityRepository,
    private val notifications: NotificationsRepository,
    private val seriousMode: SeriousModeRepository,
    private val photos: PhotoPrefetcher,
    private val cache: ApiCache,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private var job: Job? = null
    private var watcher: Job? = null

    /**
     * Idempotent. Watches IdentityStore: warms when an identity appears (app start for a returning user, or
     * right after login / claim / onboarding) and [reset]s when it is cleared (logout). This is the ONE line a
     * host needs -- `LaunchedEffect(Unit) { koinInject<Prefetcher>().start() }` at the App() root -- no per-screen
     * hooks. Deliberately NOT started from Koin bootstrap: iOS's Notification Service Extension also calls
     * KoinBootstrap.ensureStarted() and must not kick off network warm-ups under its tight memory limit.
     */
    fun start() {
        if (watcher?.isActive == true) return
        watcher = scope.launch {
            identityStore.identity
                .map { Triple(it.role, it.sponsorId ?: it.primaryId, it.childPrimaryId) }
                .distinctUntilChanged()
                .collect { (role, account, _) ->
                    if (role == null || account == null) reset() else warmUp()
                }
        }
    }

    fun warmUp() {
        val id = identityStore.identity.value
        job?.cancel()
        job = scope.launch {
            when (id.role) {
                D2MRole.CHILD -> id.primaryId?.let { warmChild(it) }
                D2MRole.PARENT -> warmParent(id.sponsorId, id.childPrimaryId)
                null -> Unit
            }
            (id.sponsorId ?: id.primaryId)?.let { runCatching { notifications.getNotifications(it) } }
        }
    }

    private suspend fun warmChild(primaryId: String) = coroutineScope {
        launch { runCatching { identity.getPrimaryProfile(primaryId) } }
        launch { runCatching { identity.getPhotos(primaryId) } }
        launch { runCatching { seriousMode.getSponsorStatus(primaryId) } }
        launch { runCatching { seriousMode.getThreads(primaryId) } }
        launch { runCatching { suggestions.getReceivedRequests(primaryId) } }
        launch { runCatching { suggestions.getSentRequests(primaryId) } }
        launch { warmDeck(primaryId) }
    }

    private suspend fun warmParent(sponsorId: String?, childPrimaryId: String?) = coroutineScope {
        if (sponsorId != null) launch { runCatching { dashboard.getSponsorDashboard(sponsorId) } }
        if (childPrimaryId != null) {
            launch { runCatching { suggestions.getShortlist(childPrimaryId) } }
            launch { warmDeck(childPrimaryId) }
        }
    }

    /** Suggestions list, then photos + profile detail for the first few cards (what the user sees first). */
    private suspend fun warmDeck(primaryId: String) {
        val list = runCatching { suggestions.getSuggestions(primaryId) }.getOrNull() ?: return
        photos.prefetch(list.map { it.photoUrl }, limit = 4)
        suggestions.prefetchUpcomingDetails(primaryId, list.map { it.candidateId }, limit = 2)
    }

    /** Logout: stop warming and forget every cached response. */
    suspend fun reset() {
        job?.cancel()
        photos.cancelAll()
        cache.clear()
    }
}
