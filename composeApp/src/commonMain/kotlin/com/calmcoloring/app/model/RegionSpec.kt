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
    /**
     * Extra boundary stroke width drawn on top of [path]'s fill, in the
     * template's viewBox units. Defaults to the app-wide line weight used
     * by every hand-authored template's thin single-width outline. A
     * region whose [path] is itself already a filled ink shape at its own
     * width (e.g. a potrace-traced line) should pass 0f — stroking an
     * already-thick fill's boundary just bloats it and can visually merge
     * closely-spaced neighboring regions.
     */
    val strokeWidth: Float = 3.5f,
    /**
     * When true and the region has no explicit tap color yet, [path] is
     * filled with the app's outline/ink color instead of [unfilledColor] —
     * for a region whose [path] already *is* the ink line art (e.g. a
     * potrace-traced stroke), so it reads as a drawn line by default
     * without needing an extra boundary stroke on top (which would double
     * its edges and bloat/merge closely-spaced lines).
     */
    val fillsWithOutlineByDefault: Boolean = false,
    /**
     * When true, this region is skipped by tap hit-testing whenever some
     * other, non-decorative region at the same point also matches — it's
     * only chosen if it's the *only* match. For thin ink decoration
     * layered on top of a solid fill region that covers the same visual
     * area (e.g. an eye's ring/pupil ink drawn over its solid `eyeL`/`eyeR`
     * fill), this stops the decoration's own (real, valid) hit polygon
     * from winning a tap that should color the solid fill beneath just
     * because draw order — which must put decoration last, on top — would
     * otherwise also make it win hit-test priority (`lastOrNull`) over the
     * solid region drawn earlier. Does not affect rendering/draw order.
     */
    val isDecorative: Boolean = false,
)
