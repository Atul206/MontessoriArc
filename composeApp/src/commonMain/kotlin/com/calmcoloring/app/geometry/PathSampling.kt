package com.calmcoloring.app.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure

/**
 * Samples this [Path]'s outline into a closed polygon of [samples] points,
 * suitable for cheap point-in-polygon hit testing (see Task 3's
 * `pointInPolygon`). Returns an empty list for a path with zero length
 * (e.g. an empty/degenerate path).
 */
fun Path.toHitPolygon(samples: Int = 96): List<Offset> {
    val measure = PathMeasure()
    measure.setPath(this, forceClosed = true)
    val length = measure.length
    if (length <= 0f) return emptyList()
    val points = ArrayList<Offset>(samples)
    for (i in 0 until samples) {
        val distance = length * (i.toFloat() / samples)
        points += measure.getPosition(distance)
    }
    return points
}
