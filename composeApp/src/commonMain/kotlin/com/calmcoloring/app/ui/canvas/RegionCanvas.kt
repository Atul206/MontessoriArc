package com.calmcoloring.app.ui.canvas

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import com.calmcoloring.app.geometry.pointInPolygon
import com.calmcoloring.app.model.Template

/**
 * Fits [template]'s viewBox into the available draw area (letterboxed,
 * centered, uniform scale) and draws every region's fill + outline. Shared
 * by the interactive [RegionCanvas] and the non-interactive gallery-card
 * preview (`TemplatePreview`) so the fit/scale/draw math can't drift between
 * the two call sites.
 */
fun DrawScope.drawTemplate(
    template: Template,
    fills: Map<String, Color>,
    unfilledColor: Color,
    outlineColor: Color,
) {
    val scale = minOf(size.width / template.viewBoxWidth, size.height / template.viewBoxHeight)
    val offsetX = (size.width - template.viewBoxWidth * scale) / 2f
    val offsetY = (size.height - template.viewBoxHeight * scale) / 2f
    withTransform({
        translate(offsetX, offsetY)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        template.regions.forEach { region ->
            val color = fills[region.id]
                ?: (if (region.fillsWithOutlineByDefault) outlineColor else unfilledColor)
            drawPath(region.path, color = color)
            if (region.strokeWidth > 0f) {
                drawPath(region.path, color = outlineColor, style = Stroke(width = region.strokeWidth))
            }
        }
    }
}

@Composable
fun RegionCanvas(
    template: Template,
    fills: Map<String, Color>,
    unfilledColor: Color,
    outlineColor: Color,
    onRegionTapped: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedFills = template.regions.associate { region ->
        region.id to animateColorAsState(
            targetValue = fills[region.id]
                ?: (if (region.fillsWithOutlineByDefault) outlineColor else unfilledColor),
            animationSpec = spring(dampingRatio = 0.9f, stiffness = 60f),
            label = "region_fill_${region.id}",
        )
    }

    Canvas(
        modifier = modifier
            .aspectRatio(template.viewBoxWidth / template.viewBoxHeight)
            .pointerInput(template.id) {
                detectTapGestures(onTap = { tapOffset ->
                    val scale = minOf(size.width / template.viewBoxWidth, size.height / template.viewBoxHeight)
                    val offsetX = (size.width - template.viewBoxWidth * scale) / 2f
                    val offsetY = (size.height - template.viewBoxHeight * scale) / 2f
                    val local = Offset(
                        (tapOffset.x - offsetX) / scale,
                        (tapOffset.y - offsetY) / scale,
                    )
                    val matches = template.regions.filter { region -> pointInPolygon(local, region.hitPolygon) }
                    val hit = matches.lastOrNull { !it.isDecorative } ?: matches.lastOrNull()
                    hit?.let { onRegionTapped(it.id) }
                })
            },
    ) {
        val resolvedFills = template.regions.associate { region -> region.id to animatedFills.getValue(region.id).value }
        drawTemplate(template = template, fills = resolvedFills, unfilledColor = unfilledColor, outlineColor = outlineColor)
    }
}
