package com.calmcoloring.app.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap

// Colors echo CalmPalette's light surface/ink (theme/Color.kt) — this
// bitmap-level card intentionally always renders in the light palette
// regardless of system theme, matching the fixed light "paper" look of a
// printed/shared photo rather than following dark mode.
private const val CardBackground = 0xFFFAF6EE.toInt()
private const val InkColor = 0xFF3A342B.toInt()

actual fun composeWatermarkedBitmap(artwork: ImageBitmap, caption: String, madeWith: String): ImageBitmap {
    val rawSource = artwork.asAndroidBitmap()
    // A `GraphicsLayer.toImageBitmap()` capture (as `ColoringScreen`'s
    // share button produces) comes back as a HARDWARE-config bitmap, which
    // `Canvas.drawBitmap` cannot draw into a software-backed Canvas
    // (`Bitmap.createBitmap(..., ARGB_8888)` below) — throws
    // "Software rendering doesn't support hardware bitmaps". Copying to a
    // software config first is the standard fix; a no-op copy (same
    // instance semantics aside) when the source is already software.
    val source = if (rawSource.config == Bitmap.Config.HARDWARE) {
        rawSource.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        rawSource
    }
    val padding = source.width * 0.06f
    val captionTextSize = source.width * 0.045f
    val madeWithTextSize = source.width * 0.032f
    val textBlockHeight = captionTextSize * 1.5f + madeWithTextSize * 1.6f + padding * 0.5f

    val totalWidth = (source.width + padding * 2).toInt()
    val totalHeight = (source.height + padding * 2 + textBlockHeight).toInt()

    val output = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(CardBackground)
    canvas.drawBitmap(source, padding, padding, null)

    val centerX = totalWidth / 2f
    val maxTextWidth = (totalWidth - padding).toInt()

    val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = InkColor
        textSize = captionTextSize
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    val madeWithPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = InkColor
        textSize = madeWithTextSize
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    val captionLine = TextUtils.ellipsize(caption, captionPaint, maxTextWidth.toFloat(), TextUtils.TruncateAt.END)
    var textY = padding + source.height + padding * 0.85f + captionTextSize
    canvas.drawText(captionLine, 0, captionLine.length, centerX, textY, captionPaint)

    textY += madeWithTextSize * 1.7f
    canvas.drawText(madeWith, centerX, textY, madeWithPaint)

    return output.asImageBitmap()
}
