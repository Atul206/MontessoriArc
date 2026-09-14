package com.calmcoloring.app.content.remote

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path

/**
 * One fillable region parsed from a remote `.svg` document. Kept as a shape
 * *descriptor* rather than a [Path] itself so [TemplateCacheStore] can store
 * it as a single TEXT column ([encode]/[decodeShape]) and only materialize a
 * real [Path] ([toPath]) when a [com.calmcoloring.app.model.Template] is
 * actually built for rendering.
 *
 * [rotation] carries this repo's one supported `transform="rotate(a,cx,cy)"`
 * form (see `svg/garden-flower.svg`'s petals 2-5) as a per-region property
 * rather than folding it into [RemoteShape] itself — [RemoteShape] stays a
 * pure geometry descriptor, and [toPath] applies the rotation to the
 * resulting [Path] afterward.
 */
data class RemoteRegion(val id: String, val shape: RemoteShape, val rotation: Rotation? = null)

/** `transform="rotate(angleDegrees,cx,cy)"` — the only SVG transform form
 *  this repo's parser understands. */
data class Rotation(val angleDegrees: Float, val cx: Float, val cy: Float)

sealed interface RemoteShape {
    data class PathData(val d: String) : RemoteShape
    data class RoundRect(val x: Float, val y: Float, val width: Float, val height: Float, val rx: Float) : RemoteShape
    data class Circle(val cx: Float, val cy: Float, val r: Float) : RemoteShape
    data class Ellipse(val cx: Float, val cy: Float, val rx: Float, val ry: Float) : RemoteShape
}

/** Inverse of [decodeShape]. */
fun RemoteShape.encode(): String = when (this) {
    is RemoteShape.PathData -> "d:$d"
    is RemoteShape.RoundRect -> "rect:$x,$y,$width,$height,$rx"
    is RemoteShape.Circle -> "circle:$cx,$cy,$r"
    is RemoteShape.Ellipse -> "ellipse:$cx,$cy,$rx,$ry"
}

/** Inverse of [encode]. */
fun decodeShape(encoded: String): RemoteShape {
    val kind = encoded.substringBefore(':')
    val rest = encoded.substringAfter(':')
    return when (kind) {
        "d" -> RemoteShape.PathData(rest)
        "rect" -> rest.split(",").map { it.toFloat() }.let { (x, y, w, h, rx) -> RemoteShape.RoundRect(x, y, w, h, rx) }
        "circle" -> rest.split(",").map { it.toFloat() }.let { (cx, cy, r) -> RemoteShape.Circle(cx, cy, r) }
        "ellipse" -> rest.split(",").map { it.toFloat() }.let { (cx, cy, rx, ry) -> RemoteShape.Ellipse(cx, cy, rx, ry) }
        else -> error("Unknown encoded shape kind '$kind' in: $encoded")
    }
}

/** Uses the same builder calls [com.calmcoloring.app.content.generated]'s
 *  `backgroundPath()`/`SunnyDayPaths.sun` already rely on for rects/circles,
 *  and [parseSvgPathData] (Task 2) for raw path data. [rotation], when
 *  present, is applied to the built [Path] afterward — pivoting around
 *  (cx, cy) via a plain [Path.translate] dance rather than a translate/
 *  rotateZ/translate [Matrix] combo, so the result doesn't depend on
 *  [Matrix]'s pre-/post-multiply convention. */
fun RemoteShape.toPath(rotation: Rotation? = null): Path {
    val path = when (this) {
        is RemoteShape.PathData -> parseSvgPathData(d)
        is RemoteShape.RoundRect -> Path().apply {
            addRoundRect(RoundRect(left = x, top = y, right = x + width, bottom = y + height, radiusX = rx, radiusY = rx))
        }
        is RemoteShape.Circle -> Path().apply { addOval(Rect(center = Offset(cx, cy), radius = r)) }
        is RemoteShape.Ellipse -> Path().apply {
            addOval(Rect(left = cx - rx, top = cy - ry, right = cx + rx, bottom = cy + ry))
        }
    }
    if (rotation != null) {
        path.translate(Offset(-rotation.cx, -rotation.cy))
        path.transform(Matrix().apply { rotateZ(rotation.angleDegrees) })
        path.translate(Offset(rotation.cx, rotation.cy))
    }
    return path
}
