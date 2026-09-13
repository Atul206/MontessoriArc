package com.calmcoloring.app.content.generated

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path

/**
 * Hand-authored Compose [Path] data for the "Little Fish" template
 * (`svg/little-fish.svg`, mockup id `fish`). The mockup's decorative eye
 * dot (`class="deco"`, not `data-region`) is intentionally omitted — v1
 * regions are fillable shapes only, per the task brief. See
 * [SunnyDayPaths] for the hand-authored-vs-plugin mechanism note.
 */
object LittleFishPaths {
    val background: Path = backgroundPath()

    // d="M40,168 L104,124 L104,212 Z"
    val tail: Path = Path().apply {
        moveTo(40f, 168f)
        lineTo(104f, 124f)
        lineTo(104f, 212f)
        close()
    }

    // d="M178,108 L212,58 L232,116 Z"
    val fin: Path = Path().apply {
        moveTo(178f, 108f)
        lineTo(212f, 58f)
        lineTo(232f, 116f)
        close()
    }

    // cx=196 cy=168 rx=92 ry=56
    val body: Path = Path().apply {
        addOval(Rect(left = 196f - 92f, top = 168f - 56f, right = 196f + 92f, bottom = 168f + 56f))
    }
}
