package com.calmcoloring.app.platform

import androidx.compose.ui.graphics.Color
import com.calmcoloring.app.model.Template

// US Letter in points (72pt/inch) by default; PRD leaves paper-size
// selection as a v1.1 detail, so a single fixed size is the correct v1 scope.
//
// PRD §5.4/§6 fidelity: this takes the template's real vector region data
// (the same Template/RegionSpec model RegionCanvas draws from) plus the
// current fill state, rather than a rasterized snapshot, so the Android
// actual can draw genuine vector paths directly onto the PDF page — see
// Printer.android.kt for the true-vector implementation and Printer.ios.kt
// for why iOS takes a narrower (higher-resolution raster) approach instead.
expect suspend fun printArtwork(
    template: Template,
    fills: Map<String, Color>,
    unfilledColor: Color,
    outlineColor: Color,
    pageWidthPoints: Float = 612f,
    pageHeightPoints: Float = 792f,
)
