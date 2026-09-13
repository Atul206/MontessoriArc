package com.calmcoloring.app.content.generated

import androidx.compose.ui.graphics.Path

/**
 * Hand-authored Compose [Path] data for the "Sailboat" template
 * (`svg/sailboat.svg`, mockup id `boat`). See [SunnyDayPaths] for the
 * hand-authored-vs-plugin mechanism note.
 */
object SailboatPaths {
    val background: Path = backgroundPath()

    // d="M56,222 L264,222 L232,272 L88,272 Z"
    val hull: Path = Path().apply {
        moveTo(56f, 222f)
        lineTo(264f, 222f)
        lineTo(232f, 272f)
        lineTo(88f, 272f)
        close()
    }

    // d="M170,62 L170,212 L100,212 Z"
    val sailBig: Path = Path().apply {
        moveTo(170f, 62f)
        lineTo(170f, 212f)
        lineTo(100f, 212f)
        close()
    }

    // d="M180,92 L180,212 L230,212 Z"
    val sailSmall: Path = Path().apply {
        moveTo(180f, 92f)
        lineTo(180f, 212f)
        lineTo(230f, 212f)
        close()
    }

    // d="M170,50 L170,72 L198,61 Z"
    val flag: Path = Path().apply {
        moveTo(170f, 50f)
        lineTo(170f, 72f)
        lineTo(198f, 61f)
        close()
    }
}
