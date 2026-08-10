package com.d2m.app.ui.navigation

/**
 * Mirrors src/App.jsx's route tree. Role/session state (sponsorId/primaryId)
 * comes from IdentityStore (app-wide), not nav args -- only genuinely
 * per-navigation entity ids (candidateId, threadId, invite token, unionId,
 * galleryEntryId) travel as route args, same split as the web app's router
 * state vs. DevIdentity context.
 */
object Routes {
    const val LOGIN = "login"
    const val ROLE_PICKER = "role_picker"

    const val ONBOARDING_WIZARD = "onboarding/parent"
    const val HANDOFF = "onboarding/handoff"

    const val CLAIM = "claim/{token}"
    fun claim(token: String) = "claim/$token"
    const val CLAIM_ARG_TOKEN = "token"
    // Deep link patterns registered against this route -- see D2MNavGraph.kt.
    const val CLAIM_DEEPLINK_HTTPS = "https://d2m.app/claim/{token}"
    const val CLAIM_DEEPLINK_SCHEME = "d2m://claim/{token}"

    const val PARENT_HOME = "parent/home"
    const val PARENT_BROWSE = "parent/browse"
    const val PARENT_MESSAGES = "parent/messages"

    const val CHILD_HOME = "child/home"
    const val DISCOVERY = "child/discover"
    const val MATCHES = "child/matches"

    const val PROFILE_DETAIL = "profile/{candidateId}"
    fun profileDetail(candidateId: String) = "profile/$candidateId"
    const val PROFILE_ARG_CANDIDATE_ID = "candidateId"

    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"

    const val CHILD_PROFILE_DIALOG = "child_profile_dialog"

    // Phase 4 -- parity closeout
    const val SUCCESS_GALLERY = "gallery"
    const val ADMIN_GALLERY_MODERATION = "admin/gallery"
    const val PANCHANGAM_CALENDAR = "panchangam"
}
