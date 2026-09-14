package com.calmcoloring.app.ui.coloring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calmcoloring.app.model.Template
import com.calmcoloring.app.theme.CalmPalette
import com.calmcoloring.app.ui.SystemBackHandler
import com.calmcoloring.app.ui.canvas.RegionCanvas
import kotlinx.coroutines.launch

// Standard Material/platform touch-target minimum for toolbar icon buttons.
// (The PRD's 2cm physical-size floor is intended for the coloring canvas's
// region tap-precision, not toolbar chrome — 48dp is both correct platform
// convention and what actually fits here.)
private val TOPBAR_ICON_SIZE = 48.dp

// Palette swatch size bounds: weighted to divide available row width evenly
// across all swatches (guaranteed to fit at any screen width), clamped so
// they don't become illegibly tiny on very narrow screens or absurdly large
// on tablets.
private val SWATCH_MIN_SIZE = 40.dp
private val SWATCH_MAX_SIZE = 64.dp

@Composable
fun ColoringScreen(
    template: Template,
    onBack: () -> Unit,
    onShareRequested: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val viewModel: ColoringViewModel = viewModel(
        key = template.id,
        factory = viewModelFactory {
            initializer { ColoringViewModel(template, CalmPalette.swatchesFor(isDark)) }
        },
    )
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val unfilledColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

    // System back gesture/button returns to the gallery, same as the in-app
    // back arrow below (Finding 5, final review).
    SystemBackHandler(onBack = onBack)

    // safeDrawingPadding keeps the topbar's back/share icons and the
    // palette swatches clear of the status/navigation bars in edge-to-edge
    // mode (android-skills:edge-to-edge).
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(TOPBAR_ICON_SIZE)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to templates")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(template.name, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { viewModel.reset() }) { Text("Start over") }
            }
            IconButton(
                onClick = {
                    scope.launch { onShareRequested(graphicsLayer.toImageBitmap()) }
                },
                modifier = Modifier.size(TOPBAR_ICON_SIZE),
            ) {
                Icon(Icons.Filled.Share, contentDescription = "Share this picture")
            }
        }

        Box(modifier = Modifier.weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
            RegionCanvas(
                template = template,
                fills = viewModel.fills,
                unfilledColor = unfilledColor,
                outlineColor = outlineColor,
                onRegionTapped = viewModel::onRegionTapped,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CalmPalette.swatchesFor(isDark).forEach { swatch ->
                val selected = swatch == viewModel.selectedColor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .sizeIn(
                            minWidth = SWATCH_MIN_SIZE,
                            minHeight = SWATCH_MIN_SIZE,
                            maxWidth = SWATCH_MAX_SIZE,
                            maxHeight = SWATCH_MAX_SIZE,
                        )
                        .clip(CircleShape)
                        .background(swatch)
                        .border(
                            width = if (selected) 2.5.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                        )
                        .clickable { viewModel.selectColor(swatch) },
                )
            }
        }
    }
}
