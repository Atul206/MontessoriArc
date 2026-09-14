package com.calmcoloring.app.content.generated

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path

/**
 * Hand-authored Compose [Path] data for the "Cat" template
 * (`svg/cat.svg`) — a standing/sitting cat, distinct from
 * [SleepyCatPaths]. See [SunnyDayPaths] for the hand-authored-vs-plugin
 * mechanism note. The ears share their outer arc with the head circle
 * (radius 58, center 160,105) per the project's no-overlap construction
 * rule, so `earL`/`earR` and `head` meet on one literal boundary curve.
 */
object CatPaths {
    val background: Path = backgroundPath()

    // d="M225,280 Q295,270 285,180 L268,178 Q262,235 225,235 Z"
    val tail: Path = Path().apply {
        moveTo(225f, 280f)
        quadraticTo(295f, 270f, 285f, 180f)
        lineTo(268f, 178f)
        quadraticTo(262f, 235f, 225f, 235f)
        close()
    }

    // d="M95,285 L95,225 Q95,163 160,163 Q225,163 225,225 L225,285 Z"
    val body: Path = Path().apply {
        moveTo(95f, 285f)
        lineTo(95f, 225f)
        quadraticTo(95f, 163f, 160f, 163f)
        quadraticTo(225f, 163f, 225f, 225f)
        lineTo(225f, 285f)
        close()
    }

    // d="M114,69.7 L120,15 L146,48.7 A58,58 0 0,0 114,69.7 Z"
    // Arc shares the head circle's exact radius/center (160,105).
    val earL: Path = Path().apply {
        moveTo(114f, 69.7f)
        lineTo(120f, 15f)
        lineTo(146f, 48.7f)
        svgArcTo(x1 = 146f, y1 = 48.7f, rx = 58f, ry = 58f, largeArc = false, sweep = false, x2 = 114f, y2 = 69.7f)
        close()
    }

    // d="M174,48.7 L200,15 L206,69.7 A58,58 0 0,0 174,48.7 Z"
    val earR: Path = Path().apply {
        moveTo(174f, 48.7f)
        lineTo(200f, 15f)
        lineTo(206f, 69.7f)
        svgArcTo(x1 = 206f, y1 = 69.7f, rx = 58f, ry = 58f, largeArc = false, sweep = false, x2 = 174f, y2 = 48.7f)
        close()
    }

    // cx=160 cy=105 r=58
    val head: Path = Path().apply { addOval(Rect(center = Offset(160f, 105f), radius = 58f)) }

    // cx=136 cy=103 r=9
    val eyeL: Path = Path().apply { addOval(Rect(center = Offset(136f, 103f), radius = 9f)) }

    // cx=184 cy=103 r=9
    val eyeR: Path = Path().apply { addOval(Rect(center = Offset(184f, 103f), radius = 9f)) }

    // d="M152,119 L168,119 L160,129 Z"
    val nose: Path = Path().apply {
        moveTo(152f, 119f)
        lineTo(168f, 119f)
        lineTo(160f, 129f)
        close()
    }
}
