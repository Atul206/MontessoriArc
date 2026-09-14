package com.calmcoloring.app.geometry

import androidx.compose.ui.geometry.Offset

fun pointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    if (polygon.size < 3) return false
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val pi = polygon[i]
        val pj = polygon[j]
        val intersects = (pi.y > point.y) != (pj.y > point.y) &&
            point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x
        if (intersects) inside = !inside
        j = i
    }
    return inside
}
