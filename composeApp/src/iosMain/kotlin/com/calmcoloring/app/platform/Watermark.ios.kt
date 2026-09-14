package com.calmcoloring.app.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.Font
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

// NOTE (verification caveat, same as Printer.ios.kt/Sharer.ios.kt): this
// file could not be linked/run in this environment (Xcode Command Line
// Tools only, no full Xcode/simulator) — verified to *compile* against the
// iosSimulatorArm64 target only. Runtime behavior (does the drawn text
// actually appear correctly positioned) is UNVERIFIED here. See
// `Watermark.kt` for why this draws directly onto a bitmap rather than
// capturing a Composable.
//
// Built entirely on `org.jetbrains.skia` (the same Skia bindings Compose
// Multiplatform's iOS/desktop targets already render through, exposed via
// `ImageBitmap.asSkiaBitmap()`/`Image.toComposeImageBitmap()`) rather than
// UIKit/CoreGraphics text APIs — one implementation understandable
// alongside Android's Canvas/Paint version above, with no CoreGraphics
// Create-Rule memory-lifetime concerns (unlike `ImageBitmapExtensions.ios.kt`).
actual fun composeWatermarkedBitmap(artwork: ImageBitmap, caption: String, madeWith: String): ImageBitmap {
    val sourceImage = Image.makeFromBitmap(artwork.asSkiaBitmap())
    val sourceWidth = sourceImage.width.toFloat()
    val sourceHeight = sourceImage.height.toFloat()

    val padding = sourceWidth * 0.06f
    val captionFontSize = sourceWidth * 0.045f
    val madeWithFontSize = sourceWidth * 0.032f
    val textBlockHeight = captionFontSize * 1.5f + madeWithFontSize * 1.6f + padding * 0.5f

    val totalWidth = (sourceWidth + padding * 2).toInt()
    val totalHeight = (sourceHeight + padding * 2 + textBlockHeight).toInt()

    val surface = Surface.makeRasterN32Premul(totalWidth, totalHeight)
    val canvas = surface.canvas

    val backgroundColor = Color.makeARGB(0xFF, 0xFA, 0xF6, 0xEE)
    val inkColor = Color.makeARGB(0xFF, 0x3A, 0x34, 0x2B)

    canvas.drawRect(Rect.makeWH(totalWidth.toFloat(), totalHeight.toFloat()), Paint().apply { color = backgroundColor })
    canvas.drawImage(sourceImage, padding, padding)

    val textPaint = Paint().apply {
        color = inkColor
        isAntiAlias = true
    }
    val captionFont = Font().apply { size = captionFontSize }
    val madeWithFont = Font().apply { size = madeWithFontSize }

    val captionWidth = captionFont.measureTextWidth(caption)
    val captionX = (totalWidth - captionWidth) / 2f
    var textY = padding + sourceHeight + padding * 0.85f + captionFontSize
    canvas.drawString(caption, captionX, textY, captionFont, textPaint)

    val madeWithWidth = madeWithFont.measureTextWidth(madeWith)
    val madeWithX = (totalWidth - madeWithWidth) / 2f
    textY += madeWithFontSize * 1.7f
    canvas.drawString(madeWith, madeWithX, textY, madeWithFont, textPaint)

    return surface.makeImageSnapshot().toComposeImageBitmap()
}
