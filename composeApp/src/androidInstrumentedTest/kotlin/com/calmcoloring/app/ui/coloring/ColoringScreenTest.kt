package com.calmcoloring.app.ui.coloring

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.calmcoloring.app.content.TemplateCatalog
import org.junit.Rule
import org.junit.Test

/**
 * The one instrumented Compose test in this app (Task 9, per
 * android-skills:testing-setup step 9): exercises real Compose layout and
 * gesture dispatch for [ColoringScreen] on a connected device/emulator.
 * Pure logic (hit-testing, view-model state, caption text) is already
 * covered by the `commonTest` suite from Tasks 1-4, 7-8.
 */
class ColoringScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun tappingBack_returnsToCallerCallback() {
        var backCalled = false
        composeRule.setContent {
            ColoringScreen(
                template = TemplateCatalog.byId("little-house"),
                onBack = { backCalled = true },
                onShareRequested = {},
            )
        }
        composeRule.onNodeWithContentDescription("Back to templates").performClick()
        assert(backCalled)
    }
}
