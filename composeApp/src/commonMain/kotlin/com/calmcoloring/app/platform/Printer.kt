package com.calmcoloring.app.platform

import androidx.compose.ui.graphics.ImageBitmap

// US Letter in points (72pt/inch) by default; PRD leaves paper-size
// selection as a v1.1 detail, so a single fixed size is the correct v1 scope.
//
// NOTE (PRD §5.4 fidelity): this takes a rasterized ImageBitmap snapshot of
// the on-screen canvas (the same snapshot the share flow already captures
// via graphicsLayer.toImageBitmap()), not the raw vector Path data the
// template/region model holds internally. See Printer.android.kt/Printer.ios.kt
// for how each platform turns that bitmap into a PDF.
expect suspend fun printArtwork(
    artwork: ImageBitmap,
    pageWidthPoints: Float = 612f,
    pageHeightPoints: Float = 792f,
)
