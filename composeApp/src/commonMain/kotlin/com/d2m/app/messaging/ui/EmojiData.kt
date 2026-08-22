package com.d2m.app.messaging.ui

// Ported verbatim from d2m_web's lib/messaging/emoji.js (EMOJIS/STICKERS) --
// pure data, no UI-specific handling needed on this side beyond rendering a
// grid of these strings. See EmojiGifPanel.kt for the picker UI itself.

val EMOJIS: List<String> = listOf(
    "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
    "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "😣", "😖", "😫",
    "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗", "🤔", "🤭",
    "🤫", "🤥", "😶", "😐", "😑", "😬", "🙄", "😯", "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "🤐", "🥴", "🤢", "🤮",
    "🤧", "😷", "🤒", "🤕", "🤑", "🤠", "😈", "👍", "👎", "👏", "🙏", "💪", "🔥", "🎉", "❤️", "🧡", "💛", "💚", "💙", "💜",
    "🖤", "🤍", "💯", "✅", "⭐", "🌟", "✨", "🚀", "👀", "💀", "🙌", "👌", "✌️", "🤝", "👋", "🎂", "🍕", "☕", "🌈", "⚡",
)

val STICKERS: List<String> = listOf(
    "🥳", "😂", "😍", "👍", "🎉", "❤️", "🔥", "😎", "🙏", "💯", "👏", "😭", "🤔", "🤩", "😴", "🤯", "🙌", "💪", "✨", "🚀", "👀", "🥰", "😅", "🫶",
)

// Ported from d2m_web's emoji.js isEmojiOnly(): "Emoji-only (short) messages
// render large, chat-app style -- also used for stickers." A sticker isn't a
// distinct message type on either side of the wire -- MessageComposer's
// sticker tab just sends one emoji as a plain text message (same as
// EmojiGifPanel.onSendSticker here) -- so the "no bubble, bigger" look web
// gives stickers is really this same emoji-only detection applied to any
// short message that's nothing but emoji. Kotlin's common Regex can't rely
// on JVM-only \p{Extended_Pictographic}/\p{Emoji_Component} (java.util.regex
// vs iOS's NSRegularExpression differ, and this needs to work identically on
// both), so this is a manual codepoint-range port of the same two Unicode
// properties web's regex checks, applied to the same three JS rules: length
// <= 12 UTF-16 units, no ASCII letter/digit anywhere, and every codepoint is
// either "pictographic" (counts) or a "component" (allowed, doesn't count on
// its own -- skin tones, ZWJ, variation selector, regional indicators) or
// plain whitespace.
private fun codePointsOf(s: String): List<Int> {
    val result = mutableListOf<Int>()
    var i = 0
    while (i < s.length) {
        val c1 = s[i]
        if (c1.code in 0xD800..0xDBFF && i + 1 < s.length) {
            val c2 = s[i + 1]
            if (c2.code in 0xDC00..0xDFFF) {
                result.add(0x10000 + (c1.code - 0xD800) * 0x400 + (c2.code - 0xDC00))
                i += 2
                continue
            }
        }
        result.add(c1.code)
        i += 1
    }
    return result
}

private fun isPictographicCodePoint(cp: Int): Boolean =
    cp in 0x2600..0x27BF || // misc symbols, dingbats (☀️✨❤️ etc.)
        cp in 0x2300..0x23FF || // misc technical (⌚⏰)
        cp in 0x25A0..0x25FF || // geometric shapes
        cp in 0x2B00..0x2BFF || // misc symbols and arrows (⭐➡️)
        cp in 0x1F000..0x1FFFF || // emoticons, transport, symbols & pictographs (incl. extended-A)
        cp == 0x2049 || cp == 0x203C || cp == 0x3030 || cp == 0x303D

private fun isEmojiComponentCodePoint(cp: Int): Boolean =
    cp in 0x1F3FB..0x1F3FF || // skin tone modifiers
        cp == 0x200D || // zero-width joiner (multi-part sequences)
        cp == 0xFE0F || // variation selector-16 (emoji presentation)
        cp == 0x20E3 || // combining enclosing keycap
        cp in 0x1F1E6..0x1F1FF // regional indicators (flag sequences)

fun isEmojiOnly(text: String?): Boolean {
    val t = text?.trim() ?: return false
    if (t.isEmpty() || t.length > 12) return false
    if (t.any { c -> c in '0'..'9' || c in 'a'..'z' || c in 'A'..'Z' }) return false
    var hasPictographic = false
    for (cp in codePointsOf(t)) {
        when {
            isPictographicCodePoint(cp) -> hasPictographic = true
            isEmojiComponentCodePoint(cp) -> Unit
            cp == 0x20 || cp == 0x09 || cp == 0x0A || cp == 0x0D -> Unit
            else -> return false
        }
    }
    return hasPictographic
}
