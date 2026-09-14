package com.calmcoloring.app.content.generated

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Small hand-rolled helpers used by the templates in this package to
 * translate raw SVG path/shape data (`d`, `transform="rotate(...)"`) into
 * Compose [Path] builder calls. See the mechanism note in [SunnyDayPaths]
 * for why this is hand-authored rather than plugin-generated.
 */

private const val KAPPA = 0.5522847498307936f

/**
 * Appends an SVG elliptical-arc ("A"/"a" command) segment from the path's
 * current point (x1, y1) to (x2, y2), per the SVG 1.1 endpoint-to-center
 * arc parameterization (spec F.6.5), assuming zero x-axis rotation (true
 * for every arc used in this template set). Delegates to Compose's
 * [Path.arcTo] once the center/angles are recovered.
 */
internal fun Path.svgArcTo(
    x1: Float,
    y1: Float,
    rx: Float,
    ry: Float,
    largeArc: Boolean,
    sweep: Boolean,
    x2: Float,
    y2: Float,
) {
    if (rx == 0f || ry == 0f) {
        lineTo(x2, y2)
        return
    }
    var rxAbs = abs(rx)
    var ryAbs = abs(ry)
    val x1p = (x1 - x2) / 2f
    val y1p = (y1 - y2) / 2f
    val lambda = (x1p * x1p) / (rxAbs * rxAbs) + (y1p * y1p) / (ryAbs * ryAbs)
    if (lambda > 1f) {
        val s = sqrt(lambda)
        rxAbs *= s
        ryAbs *= s
    }
    val sign = if (largeArc != sweep) 1f else -1f
    val num = rxAbs * rxAbs * ryAbs * ryAbs - rxAbs * rxAbs * y1p * y1p - ryAbs * ryAbs * x1p * x1p
    val den = rxAbs * rxAbs * y1p * y1p + ryAbs * ryAbs * x1p * x1p
    val co = sign * sqrt(max(0f, num) / den)
    val cxp = co * (rxAbs * y1p / ryAbs)
    val cyp = co * (-ryAbs * x1p / rxAbs)
    val cx = cxp + (x1 + x2) / 2f
    val cy = cyp + (y1 + y2) / 2f

    fun angleBetween(ux: Float, uy: Float, vx: Float, vy: Float): Float {
        val dot = ux * vx + uy * vy
        val len = sqrt(ux * ux + uy * uy) * sqrt(vx * vx + vy * vy)
        var ang = acos((dot / len).coerceIn(-1f, 1f))
        if (ux * vy - uy * vx < 0f) ang = -ang
        return ang
    }

    val theta1 = angleBetween(1f, 0f, (x1p - cxp) / rxAbs, (y1p - cyp) / ryAbs)
    var dTheta = angleBetween((x1p - cxp) / rxAbs, (y1p - cyp) / ryAbs, (-x1p - cxp) / rxAbs, (-y1p - cyp) / ryAbs)
    if (!sweep && dTheta > 0f) dTheta -= (2 * PI).toFloat()
    if (sweep && dTheta < 0f) dTheta += (2 * PI).toFloat()

    val rect = Rect(cx - rxAbs, cy - ryAbs, cx + rxAbs, cy + ryAbs)
    arcTo(rect, radToDeg(theta1), radToDeg(dTheta), forceMoveTo = false)
}

private fun radToDeg(rad: Float): Float = rad * (180f / PI.toFloat())

/**
 * Rotates (px, py) by [angleDegrees] around ([pivotX], [pivotY]) — the same
 * transform as an SVG `transform="rotate(angle, pivotX, pivotY)"`.
 */
internal fun rotateAround(px: Float, py: Float, pivotX: Float, pivotY: Float, angleDegrees: Float): Offset {
    val rad = angleDegrees * (PI.toFloat() / 180f)
    val cosA = cos(rad)
    val sinA = sin(rad)
    val dx = px - pivotX
    val dy = py - pivotY
    return Offset(dx * cosA - dy * sinA + pivotX, dy * cosA + dx * sinA + pivotY)
}

/**
 * Builds a closed ellipse path (center [cx],[cy], radii [rx]/[ry]) as four
 * cubic-Bezier quadrants, with every anchor/control point rotated by
 * [rotationDegrees] around ([pivotX], [pivotY]). Used for the flower
 * template's petals, which SVG rotates around the flower's center rather
 * than each petal's own ellipse center — a transform `Path.addOval` alone
 * can't express relative to an external pivot.
 */
internal fun rotatedEllipsePath(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    rotationDegrees: Float,
    pivotX: Float,
    pivotY: Float,
): Path {
    fun p(x: Float, y: Float) = rotateAround(x, y, pivotX, pivotY, rotationDegrees)

    val right = p(cx + rx, cy)
    val bottom = p(cx, cy + ry)
    val left = p(cx - rx, cy)
    val top = p(cx, cy - ry)

    val rightToBottomC1 = p(cx + rx, cy + ry * KAPPA)
    val rightToBottomC2 = p(cx + rx * KAPPA, cy + ry)
    val bottomToLeftC1 = p(cx - rx * KAPPA, cy + ry)
    val bottomToLeftC2 = p(cx - rx, cy + ry * KAPPA)
    val leftToTopC1 = p(cx - rx, cy - ry * KAPPA)
    val leftToTopC2 = p(cx - rx * KAPPA, cy - ry)
    val topToRightC1 = p(cx + rx * KAPPA, cy - ry)
    val topToRightC2 = p(cx + rx, cy - ry * KAPPA)

    return Path().apply {
        moveTo(right.x, right.y)
        cubicTo(rightToBottomC1.x, rightToBottomC1.y, rightToBottomC2.x, rightToBottomC2.y, bottom.x, bottom.y)
        cubicTo(bottomToLeftC1.x, bottomToLeftC1.y, bottomToLeftC2.x, bottomToLeftC2.y, left.x, left.y)
        cubicTo(leftToTopC1.x, leftToTopC1.y, leftToTopC2.x, leftToTopC2.y, top.x, top.y)
        cubicTo(topToRightC1.x, topToRightC1.y, topToRightC2.x, topToRightC2.y, right.x, right.y)
        close()
    }
}

/** Builds the shared full-canvas "background" rounded-rect region path. */
internal fun backgroundPath(): Path = Path().apply {
    addRoundRect(
        androidx.compose.ui.geometry.RoundRect(
            left = 1.5f,
            top = 1.5f,
            right = 1.5f + 317f,
            bottom = 1.5f + 317f,
            radiusX = 18f,
            radiusY = 18f,
        )
    )
}
