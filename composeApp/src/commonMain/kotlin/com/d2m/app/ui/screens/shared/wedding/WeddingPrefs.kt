package com.d2m.app.ui.screens.shared.wedding

import com.russhwolf.settings.Settings

/** Remembers the checklist's layout and grouping across app restarts (web keeps the same choices in localStorage). */
class WeddingPrefs(private val settings: Settings) {
    fun view(): String = settings.getString(KEY_VIEW, "list").takeIf { it in listOf("list", "board", "calendar") } ?: "list"
    fun listGroup(): String = settings.getString(KEY_LIST_GROUP, "phase").takeIf { it in GROUP_KEYS } ?: "phase"
    fun boardGroup(): String = settings.getString(KEY_BOARD_GROUP, "due").takeIf { it in GROUP_KEYS } ?: "due"
    fun save(view: String, listGroup: String, boardGroup: String) {
        settings.putString(KEY_VIEW, view)
        settings.putString(KEY_LIST_GROUP, listGroup)
        settings.putString(KEY_BOARD_GROUP, boardGroup)
    }

    private companion object {
        const val KEY_VIEW = "wedding.view"
        const val KEY_LIST_GROUP = "wedding.group.list"
        const val KEY_BOARD_GROUP = "wedding.group.board"
    }
}
