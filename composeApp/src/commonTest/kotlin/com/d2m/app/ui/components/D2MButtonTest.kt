package com.d2m.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Basic Compose Multiplatform UI test (uses commonTest's `compose.uiTest`
 * artifact, i.e. `runComposeUiTest` -- the common-code equivalent of
 * `createComposeRule`, portable across the targets that support it). Chosen
 * over a screen-level test as the first one written here since D2MButton has
 * no repository/DI dependencies to fake -- it isolates "does Compose UI
 * testing work in this project at all" from "is this specific screen's
 * business logic correct," which is the right thing to prove first.
 */
@OptIn(ExperimentalTestApi::class)
class D2MButtonTest {

    @Test
    fun click_invokesOnClick() = runComposeUiTest {
        var clicked = false
        setContent {
            MaterialTheme {
                D2MButton(text = "Save", onClick = { clicked = true })
            }
        }

        onNodeWithText("Save").assertIsEnabled().performClick()

        assertTrue(clicked, "Expected onClick to have fired.")
    }

    @Test
    fun disabled_doesNotInvokeOnClick() = runComposeUiTest {
        var clicked = false
        setContent {
            MaterialTheme {
                D2MButton(text = "Save", onClick = { clicked = true }, enabled = false)
            }
        }

        onNodeWithText("Save").assertIsNotEnabled().performClick()

        assertFalse(clicked, "Disabled button should not fire onClick.")
    }
}
