package com.calmcoloring.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.calmcoloring.app.content.TemplateCatalog
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

            when (val current = backStack.last()) {
                is Route.Gallery -> GalleryScreen(
                    templates = TemplateCatalog.all,
                    onTemplateSelected = { id -> backStack.add(Route.Coloring(id)) },
                )

                is Route.Coloring -> {
                    val template = remember(current.templateId) { TemplateCatalog.byId(current.templateId) }
                    var shareArtwork by remember { mutableStateOf<ImageBitmap?>(null) }
                    ColoringScreen(
                        template = template,
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                        onShareRequested = { bitmap -> shareArtwork = bitmap },
                    )
                    shareArtwork?.let { bitmap ->
                        ShareSheet(artwork = bitmap, onDismiss = { shareArtwork = null })
                    }
                }
            }
        }
    }
}
