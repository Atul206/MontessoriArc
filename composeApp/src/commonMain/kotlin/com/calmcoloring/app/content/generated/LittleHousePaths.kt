package com.calmcoloring.app.content.generated

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path

/**
 * Hand-authored Compose [Path] data for the "Little House" template
 * (`svg/little-house.svg`, mockup id `house`). See [SunnyDayPaths] for the
 * hand-authored-vs-plugin mechanism note.
 */
object LittleHousePaths {
    val background: Path = backgroundPath()

    // x=60 y=150 width=200 height=130 rx=6
    val wall: Path = Path().apply {
        addRoundRect(RoundRect(left = 60f, top = 150f, right = 260f, bottom = 280f, radiusX = 6f, radiusY = 6f))
    }

    // d="M40,152 L160,58 L280,152 Z"
    val roof: Path = Path().apply {
        moveTo(40f, 152f)
        lineTo(160f, 58f)
        lineTo(280f, 152f)
        close()
    }

    // x=140 y=212 width=42 height=68 rx=6
    val door: Path = Path().apply {
        addRoundRect(RoundRect(left = 140f, top = 212f, right = 182f, bottom = 280f, radiusX = 6f, radiusY = 6f))
    }

    // cx=212 cy=196 r=24
    val window: Path = Path().apply { addOval(Rect(center = Offset(212f, 196f), radius = 24f)) }
}
