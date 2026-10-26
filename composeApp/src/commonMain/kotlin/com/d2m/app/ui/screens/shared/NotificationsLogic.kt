package com.d2m.app.ui.screens.shared

import com.d2m.app.data.model.NotificationOut
import com.d2m.app.data.session.D2MRole
import com.d2m.app.ui.navigation.Routes
import com.d2m.app.ui.strings.NotificationsStrings
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Pure copy + routing rules, ported 1:1 from d2m_web's NotificationsScreen.jsx
 * (typeLabel / payloadPreview / ctaFor) so both clients say and do the same
 * thing for every notification type.
 */

internal fun notificationTitle(type: String, s: NotificationsStrings): String = when (type) {
    "new_suggestion" -> s.typeNewSuggestion
    "mutual_match" -> s.typeMutualMatch
    "request_received" -> s.typeRequestReceived
    "serious_mode_request" -> s.typeSeriousModeRequest
    "serious_mode_accepted" -> s.typeSeriousModeAccepted
    "serious_mode_revoked" -> s.typeSeriousModeRevoked
    "consent_request" -> s.typeConsentRequest
    "consent_granted" -> s.typeConsentGranted
    "share_link_viewed" -> s.typeShareLinkViewed
    "mutual_sponsor_interest" -> s.typeMutualSponsorInterest
    "shortlisted_profile_updated" -> s.typeShortlistedProfileUpdated
    // A type this app has no copy for yet (e.g. calls_unlocked): a readable title, never the raw key.
    else -> type.replace('_', ' ').trim().replaceFirstChar { it.uppercase() }
}

private fun NotificationOut.str(key: String): String? =
    (payload?.get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() }

private fun String.id8() = take(8)

internal fun notificationBody(n: NotificationOut, s: NotificationsStrings): String = when (n.type) {
    "mutual_match", "new_suggestion" ->
        n.str("candidate_id")?.let { s.previewCandidate.replace("{id}", it.id8()) } ?: ""
    "request_received" ->
        n.str("sender_id")?.let { s.previewRequestFrom.replace("{id}", it.id8()) } ?: ""
    "serious_mode_request", "serious_mode_accepted", "serious_mode_revoked" ->
        n.str("thread_id")?.let { s.previewThread.replace("{id}", it.id8()) } ?: ""
    "consent_request", "consent_granted" ->
        n.str("prospect_id")?.let { s.previewProspect.replace("{id}", it.id8()) } ?: ""
    "share_link_viewed" ->
        n.str("viewer_name")?.let { s.previewViewerNamed.replace("{name}", it) } ?: s.previewViewerAnonymous
    "mutual_sponsor_interest" -> if (n.str("target_primary_id") != null) s.previewMutualSponsorInterest else ""
    "shortlisted_profile_updated" -> if (n.str("candidate_id") != null) s.previewShortlistedProfileUpdated else ""
    // Unknown type: never dump the raw payload on screen. A counterpart name is the one safe, readable detail.
    else -> n.str("counterpart_name") ?: ""
}

internal enum class CtaKind { ViewMatch, ViewRequest, Respond, ViewConversation, ViewDetails, ViewSuggestion, ViewTheirProfile, ViewShareLinks, ViewProfile }

/** Where tapping goes: a tab/route, or a profile detail for [profileId]. Exactly one is set. */
internal data class NotificationCta(val kind: CtaKind, val route: String? = null, val profileId: String? = null)

internal fun ctaLabel(kind: CtaKind, s: NotificationsStrings): String = when (kind) {
    CtaKind.ViewMatch -> s.ctaViewMatch
    CtaKind.ViewRequest -> s.ctaViewRequest
    CtaKind.Respond -> s.ctaRespond
    CtaKind.ViewConversation -> s.ctaViewConversation
    CtaKind.ViewDetails -> s.ctaViewDetails
    CtaKind.ViewSuggestion -> s.ctaViewSuggestion
    CtaKind.ViewTheirProfile -> s.ctaViewTheirProfile
    CtaKind.ViewShareLinks -> s.ctaViewShareLinks
    CtaKind.ViewProfile -> s.ctaViewProfile
}

/**
 * Same role gating as web's ctaFor. Mobile has no per-thread route state, so
 * thread notifications land on the Matches tab (where the conversation lives).
 * share_link_viewed -> viewer profile needs no `?viewer=` here: the profile
 * screen already resolves the viewer from the signed-in identity.
 */
internal fun notificationCta(n: NotificationOut, role: D2MRole?): NotificationCta? {
    val child = role == D2MRole.CHILD
    val parent = role == D2MRole.PARENT
    return when (n.type) {
        "mutual_match" -> if (child && n.str("candidate_id") != null) NotificationCta(CtaKind.ViewMatch, Routes.MATCHES) else null
        "request_received" -> if (child) NotificationCta(CtaKind.ViewRequest, Routes.CHILD_HOME) else null
        "serious_mode_request" -> if (child) NotificationCta(CtaKind.Respond, Routes.MATCHES) else null
        "serious_mode_accepted" -> if (child) NotificationCta(CtaKind.ViewMatch, Routes.MATCHES) else null
        "serious_mode_revoked" -> if (child) NotificationCta(CtaKind.ViewConversation, Routes.MATCHES) else null
        "consent_request" -> if (child) NotificationCta(CtaKind.Respond, Routes.CHILD_HOME) else null
        "consent_granted" -> if (parent) NotificationCta(CtaKind.ViewDetails, Routes.PARENT_HOME) else null
        "new_suggestion" -> if (child) NotificationCta(CtaKind.ViewSuggestion, Routes.DISCOVERY) else null
        "share_link_viewed" -> {
            val viewer = n.str("viewer_primary_id")
            if (child && viewer != null) NotificationCta(CtaKind.ViewTheirProfile, profileId = viewer)
            else NotificationCta(CtaKind.ViewShareLinks, Routes.SHARE_LINKS)
        }
        "mutual_sponsor_interest" -> {
            val t = n.str("target_primary_id")
            if (parent && t != null) NotificationCta(CtaKind.ViewProfile, profileId = t) else null
        }
        "shortlisted_profile_updated" -> {
            val c = n.str("candidate_id")
            if (child && c != null) NotificationCta(CtaKind.ViewProfile, profileId = c) else null
        }
        else -> null
    }
}

/** Compact relative stamp for the last day; older rows fall back to [clock] (the day header carries the date). */
internal fun relativeTime(diffMs: Long, clock: String, s: NotificationsStrings): String {
    val mins = diffMs / 60_000
    return when {
        mins < 1 -> s.justNow
        mins < 60 -> s.minutesAgo.replace("{n}", mins.toString())
        mins < 24 * 60 -> s.hoursAgo.replace("{n}", (mins / 60).toString())
        else -> clock
    }
}
