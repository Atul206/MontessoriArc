package com.calmcoloring.app.platform

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import com.calmcoloring.app.model.Template

/**
 * Rasterizes [template] (with [fills] applied) into a fresh [ImageBitmap] of
 * exactly [widthPx] x [heightPx], fitting the template's viewBox within that
 * box (centered) and drawing each region with the same two-pass
 * fill-then-stroke approach [com.calmcoloring.app.ui.canvas.RegionCanvas]
 * uses on screen.
 *
 * This is plain imperative drawing onto an off-screen [ImageBitmap] backed
 * [Canvas] — it needs no live composition/[androidx.compose.ui.graphics.layer.GraphicsLayer],
 * so it can run at any resolution independent of whatever the on-screen
 * canvas happens to be laid out at. Used by the iOS print actual to render
 * at a resolution proportional to the print page rather than reusing the
 * phone-screen-resolution capture the share flow uses (see Printer.ios.kt
 * for why iOS takes this raster-at-higher-DPI approach instead of Android's
 * true vector PDF draw).
 */
fun rasterizeTemplate(
    template: Template,
    fills: Map<String, Color>,
    unfilledColor: Color,
    outlineColor: Color,
    widthPx: Int,
    heightPx: Int,
): ImageBitmap {
    val bitmap = ImageBitmap(widthPx, heightPx)
    val canvas = Canvas(bitmap)

    val scale = minOf(widthPx / template.viewBoxWidth, heightPx / template.viewBoxHeight)
    val dx = (widthPx - template.viewBoxWidth * scale) / 2f
    val dy = (heightPx - template.viewBoxHeight * scale) / 2f

    canvas.save()
    canvas.translate(dx, dy)
    canvas.scale(scale, scale)

    val fillPaint = Paint().apply { isAntiAlias = true; style = PaintingStyle.Fill }
    val strokePaint = Paint().apply {
        isAntiAlias = true
        style = PaintingStyle.Stroke
        color = outlineColor
    }

    template.regions.forEach { region ->
        fillPaint.color = fills[region.id]
            ?: (if (region.fillsWithOutlineByDefault) outlineColor else unfilledColor)
        canvas.drawPath(region.path, fillPaint)
        if (region.strokeWidth > 0f) {
            strokePaint.strokeWidth = region.strokeWidth
            canvas.drawPath(region.path, strokePaint)
        }
    }

    canvas.restore()
    return bitmap
}
