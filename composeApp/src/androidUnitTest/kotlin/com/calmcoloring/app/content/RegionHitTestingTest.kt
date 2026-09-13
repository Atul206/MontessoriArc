package com.calmcoloring.app.content

import androidx.compose.ui.geometry.Offset
import com.calmcoloring.app.geometry.pointInPolygon
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression coverage for final-review Finding 6: neither
 * [TemplateCatalogTest] (only checks `hitPolygon.size >= 3`) nor
 * [PointInPolygonTest][com.calmcoloring.app.geometry.PointInPolygonTest]
 * (a synthetic square) verifies that tapping a real point on a real
 * template resolves to the expected region — despite every region's
 * hit polygon being hand-rolled Path/PathMeasure data (see the
 * `content.generated` package's Paths files).
 *
 * This exercises [RegionCanvas][com.calmcoloring.app.ui.canvas.RegionCanvas]'s
 * actual hit-test rule — `template.regions.lastOrNull { pointInPolygon(...) }`
 * — against `TemplateCatalog.all`'s real hit polygons for a handful of known
 * interior points, hand-derived from the actual shape data in the
 * `content.generated` package's Paths files (not copied from a review report
 * without verifying against the real coordinates):
 *  - sunny-day: `SunnyDayPaths.sun` is `addOval(center = Offset(228f, 92f), radius = 48f)`
 *    — its own center is trivially interior, and it's the last (topmost)
 *    region in that template's region list, so lastOrNull must resolve to it
 *    rather than falling back to `background`, which also covers this point.
 *  - garden-flower: `GardenFlowerPaths.center` is
 *    `addOval(center = Offset(160f, 160f), radius = 30f)`, the last region.
 *  - little-house: `LittleHousePaths.window` is
 *    `addOval(center = Offset(212f, 196f), radius = 24f)`, the last region.
 *
 * Lives in `androidUnitTest` under Robolectric (`NATIVE` graphics mode) for
 * the same reason as [TemplateCatalogTest] and
 * [SvgPathUtilsTest][com.calmcoloring.app.content.generated.SvgPathUtilsTest]:
 * `toHitPolygon()`'s `PathMeasure` sampling needs a real Path implementation,
 * which the stub android.jar doesn't provide.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RegionHitTestingTest {

    private fun hitRegionId(templateId: String, point: Offset): String? {
        val template = TemplateCatalog.byId(templateId)
        return template.regions.lastOrNull { region -> pointInPolygon(point, region.hitPolygon) }?.id
    }

    @Test
    fun sunnyDay_tapAtSunCenter_hitsSun() {
        assertEquals("sun", hitRegionId("sunny-day", Offset(228f, 92f)))
    }

    @Test
    fun sunnyDay_tapAwayFromShapes_hitsBackground() {
        // Bottom-right corner, inside the 320x320 canvas but well clear of
        // hill/cloud/sun (hill's top edge peaks at y=175 in the middle of
        // the canvas and slopes down to y=235 at the edges; the sun sits at
        // x=228/y=92 with r=48; the cloud is entirely above y=196 and left
        // of x=135). (10, 10) is top-left, clear of all of them too.
        assertEquals("background", hitRegionId("sunny-day", Offset(10f, 10f)))
    }

    @Test
    fun gardenFlower_tapAtCenterCircle_hitsCenter() {
        assertEquals("center", hitRegionId("garden-flower", Offset(160f, 160f)))
    }

    @Test
    fun littleHouse_tapAtWindowCenter_hitsWindow() {
        assertEquals("window", hitRegionId("little-house", Offset(212f, 196f)))
    }

    @Test
    fun littleHouse_tapAtDoorCenter_hitsDoor() {
        // door: RoundRect(left=140, top=212, right=182, bottom=280) -> center (161, 246)
        assertEquals("door", hitRegionId("little-house", Offset(161f, 246f)))
    }

    @Test
    fun littleHouse_tapOutsideCanvas_hitsNothing() {
        assertEquals(null, hitRegionId("little-house", Offset(-5f, -5f)))
    }
}
