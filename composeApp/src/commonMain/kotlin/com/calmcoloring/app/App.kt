package com.calmcoloring.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.calmcoloring.app.content.TemplateCatalog
import com.calmcoloring.app.navigation.Route
import com.calmcoloring.app.theme.CalmColoringTheme
import com.calmcoloring.app.ui.coloring.ColoringScreen
import com.calmcoloring.app.ui.gallery.GalleryScreen

/**
 * Root composable and navigation host. Uses a minimal hand rolled back
 * stack instead of `androidx.navigation3`/`org.jetbrains.androidx.navigation3`
 * (see `com.calmcoloring.app.navigation.Route` for why that library is
 * ruled out for this project). There is no Android system-back interception
 * here (no `BackHandler`): `androidx.activity` — the module `BackHandler`
 * lives in — is Android-only, not a Compose Multiplatform commonMain API,
 * so wiring it here would need its own `expect`/`actual` split for a single
 * predictable-back gesture. For v1's 2-route graph, `ColoringScreen`'s own
 * in-app back button (Task 4) is sufficient; this can be revisited with an
 * `expect`/`actual` `BackHandler` if system back-gesture support becomes a
 * real requirement.
 */
@Composable
fun CalmColoringApp() {
    CalmColoringTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val backStack = remember { mutableStateListOf<Route>(Route.Gallery) }

            when (val current = backStack.last()) {
                is Route.Gallery -> GalleryScreen(
                    templates = TemplateCatalog.all,
                    onTemplateSelected = { id -> backStack.add(Route.Coloring(id)) },
                )

                is Route.Coloring -> {
                    val template = remember(current.templateId) { TemplateCatalog.byId(current.templateId) }
                    ColoringScreen(
                        template = template,
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                        onShareRequested = { /* wired in Task 8 */ },
                    )
                }
            }
        }
    }
}
