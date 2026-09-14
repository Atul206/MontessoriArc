package com.calmcoloring.app.content.generated

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path

/**
 * Hand-authored Compose [Path] data for the "Sleepy Cat" template
 * (`svg/sleepy-cat.svg`, mockup id `cat`). The mockup's decorative eyes and
 * whiskers (`class="deco"` / `class="deco-line"`, not `data-region`) are
 * intentionally omitted — v1 regions are fillable shapes only, per the
 * task brief. See [SunnyDayPaths] for the hand-authored-vs-plugin
 * mechanism note.
 */
object SleepyCatPaths {
    val background: Path = backgroundPath()

    // d="M235,255 C282,244 302,192 270,152 C259,141 244,150 249,171 C255,202 236,222 219,236 Z"
    val tail: Path = Path().apply {
        moveTo(235f, 255f)
        cubicTo(282f, 244f, 302f, 192f, 270f, 152f)
        cubicTo(259f, 141f, 244f, 150f, 249f, 171f)
        cubicTo(255f, 202f, 236f, 222f, 219f, 236f)
        close()
    }

    // cx=150 cy=222 rx=92 ry=62
    val body: Path = Path().apply {
        addOval(Rect(left = 150f - 92f, top = 222f - 62f, right = 150f + 92f, bottom = 222f + 62f))
    }

    // d="M68,112 L58,58 L102,96 Z"
    val earL: Path = Path().apply {
        moveTo(68f, 112f)
        lineTo(58f, 58f)
        lineTo(102f, 96f)
        close()
    }

    // d="M150,96 L167,48 L192,100 Z"
    val earR: Path = Path().apply {
        moveTo(150f, 96f)
        lineTo(167f, 48f)
        lineTo(192f, 100f)
        close()
    }

    // cx=112 cy=140 r=56
    val head: Path = Path().apply { addOval(Rect(center = Offset(112f, 140f), radius = 56f)) }
}
