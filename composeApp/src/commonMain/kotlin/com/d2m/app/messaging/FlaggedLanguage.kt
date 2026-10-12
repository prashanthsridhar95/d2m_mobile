package com.d2m.app.messaging

/*
 * Client-side pre-send screening -- "Profanity filter - may be provide
 * report or flag for media in chat" (reported directly). True server-side
 * filtering is architecturally impossible here: chat text is Signal-
 * protocol end-to-end encrypted (see messaging/crypto/signal/), so the
 * chat server never sees plaintext to filter against -- see
 * ReportMessageDialog.kt's doc comment for the buildable alternative this
 * sits alongside. This runs entirely on-device, BEFORE encryption, same
 * trust boundary the composer's own draft text already lives in.
 *
 * Direct port of d2m_web's lib/messaging/profanityFilter.js -- same word
 * list, same whole-word (word boundary) case-insensitive matching, so a
 * flagged word doesn't false-positive inside an unrelated longer word. A
 * short, illustrative starter list, not an exhaustive moderation system --
 * extend freely, same "illustrative, not exhaustive" spirit as
 * Taxonomy.kt's own lists.
 */
object FlaggedLanguage {
    private val FLAGGED_WORDS = listOf(
        "damn", "hell", "bitch", "bastard", "asshole", "fuck", "fucking", "shit",
        "slut", "whore", "retard", "idiot", "stupid",
    )

    private val FLAGGED_PATTERN = Regex("\\b(${FLAGGED_WORDS.joinToString("|")})\\b", RegexOption.IGNORE_CASE)

    fun containsFlaggedLanguage(text: String): Boolean = text.isNotEmpty() && FLAGGED_PATTERN.containsMatchIn(text)
}
