package com.calmcoloring.app.ui.coloring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

// Palette swatch size: fixed rather than dividing the available width evenly
// (as it was when the palette had to fit all swatches with no scrolling) —
// now that the palette scrolls (LazyRow/LazyColumn below), a growing color
// count no longer needs to shrink each swatch to fit; whatever doesn't fit
// on screen is reached by swiping instead.
private val SWATCH_SIZE = 56.dp
private val PALETTE_SPACING = 8.dp

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

    val swatches = CalmPalette.swatchesFor(isDark)
    val canvasModifier = Modifier
        .drawWithContent {
            graphicsLayer.record { this@drawWithContent.drawContent() }
            drawLayer(graphicsLayer)
        }

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

        // BoxWithConstraints reads the space actually left under the topbar
        // so portrait (taller-than-wide) and landscape/tablet (wider-than-tall)
        // can each get a deliberately arranged layout instead of one arrangement
        // stretched to fit both. See Finding 1/2, bugfix round 2.
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        RegionCanvas(
                            template = template,
                            fills = viewModel.fills,
                            unfilledColor = unfilledColor,
                            outlineColor = outlineColor,
                            onRegionTapped = viewModel::onRegionTapped,
                            modifier = canvasModifier,
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxHeight().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PaletteSwatches(
                            swatches = swatches,
                            selectedColor = viewModel.selectedColor,
                            onSelect = viewModel::selectColor,
                            arrangement = PaletteArrangement.Column,
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        RegionCanvas(
                            template = template,
                            fills = viewModel.fills,
                            unfilledColor = unfilledColor,
                            outlineColor = outlineColor,
                            onRegionTapped = viewModel::onRegionTapped,
                            modifier = canvasModifier,
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PaletteSwatches(
                            swatches = swatches,
                            selectedColor = viewModel.selectedColor,
                            onSelect = viewModel::selectColor,
                            arrangement = PaletteArrangement.Row,
                        )
                    }
                }
            }
        }
    }
}

private enum class PaletteArrangement { Row, Column }

@Composable
private fun PaletteSwatches(
    swatches: List<androidx.compose.ui.graphics.Color>,
    selectedColor: androidx.compose.ui.graphics.Color,
    onSelect: (androidx.compose.ui.graphics.Color) -> Unit,
    arrangement: PaletteArrangement,
) {
    // LazyRow/LazyColumn so a palette bigger than fits on screen scrolls
    // instead of shrinking every swatch to squeeze in — each swatch stays a
    // fixed, always-legible SWATCH_SIZE regardless of how many colors there
    // are.
    when (arrangement) {
        PaletteArrangement.Row -> LazyRow(
            horizontalArrangement = Arrangement.spacedBy(PALETTE_SPACING),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            items(swatches) { swatch ->
                PaletteSwatch(
                    swatch = swatch,
                    selected = swatch == selectedColor,
                    onSelect = onSelect,
                )
            }
        }
        PaletteArrangement.Column -> LazyColumn(
            verticalArrangement = Arrangement.spacedBy(PALETTE_SPACING),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            items(swatches) { swatch ->
                PaletteSwatch(
                    swatch = swatch,
                    selected = swatch == selectedColor,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    swatch: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    onSelect: (androidx.compose.ui.graphics.Color) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SWATCH_SIZE)
            .clip(CircleShape)
            .background(swatch)
            .border(
                width = if (selected) 2.5.dp else 0.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
            )
            .clickable { onSelect(swatch) },
    )
}
