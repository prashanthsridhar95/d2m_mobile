package com.d2m.app.ui.screens.parent

import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.session.createSettings
import com.d2m.app.ui.components.FacetState
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get

/*
 * Browse state that must survive opening a profile and coming back (web's browseState.js).
 *
 * Search / sort / pool / facet filters / age range are kept in memory per child profile for the life of the process
 * (web: sessionStorage, per tab). The List | Cards | Compare choice is a long-lived preference, so it goes to the
 * multiplatform Settings store (web: localStorage "d2m:browseViewMode", same "table" | "grid" | "compare" values).
 */

/** The three ways to show the pool. [stored] matches web's saved values so the vocabulary is shared. */
enum class BrowseView(val stored: String) {
    LIST("table"),
    CARDS("grid"),
    COMPARE("compare"),
}

/** Web's SORT_OPTIONS, in the same order. */
enum class BrowseSort(val key: String) {
    BEST_MATCH("best_match"),
    COMPATIBILITY_DESC("compatibility_desc"),
    AGE_ASC("age_asc"),
    AGE_DESC("age_desc"),
    NAME_ASC("name_asc"),
}

internal data class BrowseSavedState(
    val query: String = "",
    val sort: BrowseSort = BrowseSort.BEST_MATCH,
    val mode: Int = 0, // 0 = matching, 1 = all
    val city: FacetState = FacetState(),
    val gothram: FacetState = FacetState(),
    val sect: FacetState = FacetState(),
    val star: FacetState = FacetState(),
    val ageRange: ClosedFloatingPointRange<Float>? = null,
)

internal object BrowseStateHolder {
    private val byChild = HashMap<String, BrowseSavedState>()

    /** Outlives any one screen: a shortlist toggle (and its Undo from the snackbar) must finish even if the user has navigated away. */
    val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    private val settings: Settings by lazy { createSettings() }

    fun load(childPrimaryId: String): BrowseSavedState = byChild[childPrimaryId] ?: BrowseSavedState()

    fun save(childPrimaryId: String, state: BrowseSavedState) {
        byChild[childPrimaryId] = state
    }

    fun loadView(): BrowseView {
        val v = runCatching { settings[VIEW_KEY, ""] }.getOrDefault("")
        return BrowseView.entries.firstOrNull { it.stored == v } ?: BrowseView.LIST
    }

    fun saveView(view: BrowseView) {
        runCatching { settings.putString(VIEW_KEY, view.stored) } // best effort: the choice still holds this session
    }

    private const val VIEW_KEY = "d2m_browse_view_mode"
}

/** Best match = web's default branch: ranked feed order; in the all-profiles pool, matching ones first, then by score. Unknown values always sort last. */
internal fun sortProfiles(list: List<SuggestionOut>, sort: BrowseSort, matchingIds: Set<String>?): List<SuggestionOut> {
    fun score(s: SuggestionOut): Double? = s.compositeScore?.takeIf { it > 0.0 }
    return when (sort) {
        BrowseSort.BEST_MATCH ->
            if (matchingIds == null) list // ranked feed already arrives in rank order
            else list.sortedWith(
                compareBy<SuggestionOut> { if (it.candidateId in matchingIds) 0 else 1 }
                    .thenByDescending { score(it) ?: Double.NEGATIVE_INFINITY },
            )
        BrowseSort.COMPATIBILITY_DESC -> list.sortedByDescending { score(it) ?: Double.NEGATIVE_INFINITY }
        BrowseSort.AGE_ASC -> list.sortedBy { it.age ?: Int.MAX_VALUE }
        BrowseSort.AGE_DESC -> list.sortedByDescending { it.age ?: Int.MIN_VALUE }
        BrowseSort.NAME_ASC -> list.sortedBy { it.candidateName.lowercase() }
    }
}
