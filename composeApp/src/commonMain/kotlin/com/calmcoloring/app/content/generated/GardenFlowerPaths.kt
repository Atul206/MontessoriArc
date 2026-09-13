package com.calmcoloring.app.content.generated

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path

/**
 * Hand-authored Compose [Path] data for the "Garden Flower" template
 * (`svg/garden-flower.svg`, mockup id `flower`). The mockup rotates each
 * petal ellipse around the flower's stem center (160,160), not its own
 * ellipse center (160,92) — replicated here via [rotatedEllipsePath].
 * See [SunnyDayPaths] for the hand-authored-vs-plugin mechanism note.
 */
object GardenFlowerPaths {
    val background: Path = backgroundPath()

    // x=150 y=195 width=12 height=108 rx=6
    val stem: Path = Path().apply {
        addRoundRect(RoundRect(left = 150f, top = 195f, right = 162f, bottom = 303f, radiusX = 6f, radiusY = 6f))
    }

    // cx=160 cy=92 rx=32 ry=56, no rotation
    val petal1: Path = Path().apply {
        addOval(Rect(left = 160f - 32f, top = 92f - 56f, right = 160f + 32f, bottom = 92f + 56f))
    }

    // cx=160 cy=92 rx=32 ry=56, transform="rotate(72,160,160)"
    val petal2: Path = rotatedEllipsePath(cx = 160f, cy = 92f, rx = 32f, ry = 56f, rotationDegrees = 72f, pivotX = 160f, pivotY = 160f)

    // transform="rotate(144,160,160)"
    val petal3: Path = rotatedEllipsePath(cx = 160f, cy = 92f, rx = 32f, ry = 56f, rotationDegrees = 144f, pivotX = 160f, pivotY = 160f)

    // transform="rotate(216,160,160)"
    val petal4: Path = rotatedEllipsePath(cx = 160f, cy = 92f, rx = 32f, ry = 56f, rotationDegrees = 216f, pivotX = 160f, pivotY = 160f)

    // transform="rotate(288,160,160)"
    val petal5: Path = rotatedEllipsePath(cx = 160f, cy = 92f, rx = 32f, ry = 56f, rotationDegrees = 288f, pivotX = 160f, pivotY = 160f)

    // cx=160 cy=160 r=30
    val center: Path = Path().apply { addOval(Rect(center = Offset(160f, 160f), radius = 30f)) }
}
