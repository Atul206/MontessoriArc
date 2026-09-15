package com.calmcoloring.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.calmcoloring.app.content.remote.ContentRepositoryProvider
import com.calmcoloring.app.navigation.Route
import com.calmcoloring.app.theme.CalmColoringTheme
import com.calmcoloring.app.ui.coloring.ColoringScreen
import com.calmcoloring.app.ui.gallery.GalleryScreen
import com.calmcoloring.app.ui.share.ShareSheet

/**
 * Root composable and navigation host. Uses a minimal hand rolled back
 * stack instead of `androidx.navigation3`/`org.jetbrains.androidx.navigation3`
 * (see `com.calmcoloring.app.navigation.Route` for why that library is
 * ruled out for this project). System back-gesture interception on
 * `ColoringScreen` (Android predictive back / hardware back) is wired via
 * `com.calmcoloring.app.ui.SystemBackHandler`, a small `expect`/`actual`
 * shim over the Android-only `androidx.activity.compose.BackHandler` — see
 * that file for why the shim exists.
 */
@Composable
fun CalmColoringApp() {
    CalmColoringTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val backStack = remember { mutableStateListOf<Route>(Route.Gallery) }
            val repository = remember { ContentRepositoryProvider.repository }
            val templates by repository.templates.collectAsState()
            LaunchedEffect(Unit) { repository.refresh() }

            when (val current = backStack.last()) {
                is Route.Gallery -> GalleryScreen(
                    templates = templates,
                    onTemplateSelected = { id -> backStack.add(Route.Coloring(id)) },
                )

                is Route.Coloring -> {
                    val template = remember(current.templateId, templates) {
                        templates.firstOrNull { it.id == current.templateId }
                    }
                    if (template == null) {
                        // The template being viewed vanished from `templates`
                        // mid-session — e.g. refresh() just processed a
                        // removedIds entry for an OTA-only template with no
                        // bundled fallback. Bail to the gallery instead of
                        // crashing on a missing template.
                        LaunchedEffect(current) { backStack.removeAt(backStack.lastIndex) }
                    } else {
                        var shareArtwork by remember { mutableStateOf<ImageBitmap?>(null) }
                        // Cycling replaces the current back-stack entry rather than
                        // pushing a new one, so the left/right arrows can hop between
                        // templates indefinitely while `onBack` still returns to the
                        // gallery in a single step. Only offered when there's another
                        // template to cycle to.
                        val currentIndex = templates.indexOf(template)
                        val hasNeighbors = templates.size > 1
                        ColoringScreen(
                            template = template,
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onShareRequested = { bitmap -> shareArtwork = bitmap },
                            onPrevious = if (hasNeighbors) {
                                {
                                    val prevIndex = (currentIndex - 1 + templates.size) % templates.size
                                    backStack[backStack.lastIndex] = Route.Coloring(templates[prevIndex].id)
                                }
                            } else null,
                            onNext = if (hasNeighbors) {
                                {
                                    val nextIndex = (currentIndex + 1) % templates.size
                                    backStack[backStack.lastIndex] = Route.Coloring(templates[nextIndex].id)
                                }
                            } else null,
                        )
                        shareArtwork?.let { bitmap ->
                            ShareSheet(artwork = bitmap, onDismiss = { shareArtwork = null })
                        }
                    }
                }
            }
        }
    }
}
