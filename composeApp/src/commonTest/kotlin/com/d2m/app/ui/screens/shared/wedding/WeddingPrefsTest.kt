package com.d2m.app.ui.screens.shared.wedding

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class WeddingPrefsTest {
    @Test fun defaultsThenRemembersTheLastChoices() {
        val store = MapSettings()
        val first = WeddingPrefs(store)
        assertEquals("list", first.view()); assertEquals("phase", first.listGroup()); assertEquals("due", first.boardGroup())
        first.save("board", "owner", "phase")
        // a fresh instance over the same storage (an app restart) sees them
        val second = WeddingPrefs(store)
        assertEquals("board", second.view()); assertEquals("owner", second.listGroup()); assertEquals("phase", second.boardGroup())
    }

    @Test fun ignoresValuesItDoesNotKnow() {
        val store = MapSettings("wedding.view" to "gantt", "wedding.group.list" to "colour")
        val prefs = WeddingPrefs(store)
        assertEquals("list", prefs.view()); assertEquals("phase", prefs.listGroup())
    }
}
