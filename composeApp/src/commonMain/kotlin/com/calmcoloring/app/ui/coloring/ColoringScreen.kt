package com.calmcoloring.app.ui.coloring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calmcoloring.app.model.Template
import com.calmcoloring.app.theme.CalmPalette
import com.calmcoloring.app.ui.canvas.RegionCanvas
import kotlinx.coroutines.launch

// 2cm in dp, computed from density so the physical size is correct
// regardless of screen class (PRD §5.3's NN/g-grounded touch-target floor).
private const val CM_PER_INCH = 2.54f

@Composable
private fun minTouchTargetDp(): Dp {
    val density = LocalDensity.current
    val dpPerCm = (density.density * 160f) / CM_PER_INCH / density.density
    return (2f * dpPerCm).dp
}

@Composable
fun ColoringScreen(
    template: Template,
    onBack: () -> Unit,
    onShareRequested: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ColoringViewModel = viewModel(
        factory = viewModelFactory { initializer { ColoringViewModel(template) } },
    )
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val touchTarget = minTouchTargetDp()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(touchTarget)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to templates")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(template.name, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { viewModel.reset() }) { Text("Start over") }
            }
            Row {
                // Print button wired in Task 6; Share button wired in Task 8.
                IconButton(onClick = { }, modifier = Modifier.size(touchTarget)) {
                    Icon(Icons.Filled.Print, contentDescription = "Print or export")
                }
                IconButton(
                    onClick = {
                        scope.launch { onShareRequested(graphicsLayer.toImageBitmap()) }
                    },
                    modifier = Modifier.size(touchTarget),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share this picture")
                }
            }
        }

        Box(modifier = Modifier.weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
            RegionCanvas(
                template = template,
                fills = viewModel.fills,
                unfilledColor = MaterialTheme.colorScheme.surface,
                outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
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
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        ) {
            CalmPalette.swatches.forEach { swatch ->
                val selected = swatch == viewModel.selectedColor
                Box(
                    modifier = Modifier
                        .size(touchTarget)
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
