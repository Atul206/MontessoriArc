package com.calmcoloring.app.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * One fillable region within a [Template]: its outline as a Compose [Path]
 * (used for both rendering and clipping fill), plus a pre-sampled polygon
 * used for cheap point-in-region hit testing at tap time.
 */
data class RegionSpec(
    val id: String,
    val path: Path,
    val hitPolygon: List<Offset>,
)
