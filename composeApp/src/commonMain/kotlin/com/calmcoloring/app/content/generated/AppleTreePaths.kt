package com.calmcoloring.app.content.generated

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path

/**
 * Hand-authored Compose [Path] data for the "Apple Tree" template
 * (`svg/apple-tree.svg`, mockup id `tree`). See [SunnyDayPaths] for the
 * hand-authored-vs-plugin mechanism note.
 */
object AppleTreePaths {
    val background: Path = backgroundPath()

    // x=140 y=205 width=40 height=100 rx=10
    val trunk: Path = Path().apply {
        addRoundRect(RoundRect(left = 140f, top = 205f, right = 180f, bottom = 305f, radiusX = 10f, radiusY = 10f))
    }

    // cx=160 cy=140 r=95
    val canopy: Path = Path().apply { addOval(Rect(center = Offset(160f, 140f), radius = 95f)) }

    // cx=118 cy=128 r=15
    val apple1: Path = Path().apply { addOval(Rect(center = Offset(118f, 128f), radius = 15f)) }

    // cx=204 cy=162 r=15
    val apple2: Path = Path().apply { addOval(Rect(center = Offset(204f, 162f), radius = 15f)) }
}
