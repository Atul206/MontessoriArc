package com.calmcoloring.app.model

import androidx.compose.ui.graphics.Color

/**
 * A single coloring-book template: its display metadata plus the ordered
 * list of fillable [RegionSpec]s that make it up. The first region is
 * always the full-canvas "background".
 */
data class Template(
    val id: String,
    val name: String,
    val accent: Color,
    val viewBoxWidth: Float,
    val viewBoxHeight: Float,
    val regions: List<RegionSpec>,
)
