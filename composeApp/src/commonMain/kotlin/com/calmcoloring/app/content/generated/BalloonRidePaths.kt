package com.calmcoloring.app.content.generated

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path

/**
 * Hand-authored Compose [Path] data for the "Balloon Ride" template
 * (`svg/balloon-ride.svg`, mockup id `balloon`). The mockup's decorative
 * basket strings (`class="deco-line"`, not `data-region`) are intentionally
 * omitted — v1 regions are fillable shapes only, per the task brief. See
 * [SunnyDayPaths] for the hand-authored-vs-plugin mechanism note.
 */
object BalloonRidePaths {
    val background: Path = backgroundPath()

    // x=130 y=260 width=60 height=42 rx=8
    val basket: Path = Path().apply {
        addRoundRect(RoundRect(left = 130f, top = 260f, right = 190f, bottom = 302f, radiusX = 8f, radiusY = 8f))
    }

    // d="M144,210 L176,210 L160,231 Z"
    val knot: Path = Path().apply {
        moveTo(144f, 210f)
        lineTo(176f, 210f)
        lineTo(160f, 231f)
        close()
    }

    // cx=160 cy=128 rx=72 ry=86
    val balloon: Path = Path().apply {
        addOval(Rect(left = 160f - 72f, top = 128f - 86f, right = 160f + 72f, bottom = 128f + 86f))
    }
}
