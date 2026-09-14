package com.calmcoloring.app.content.generated

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import com.calmcoloring.app.geometry.toHitPolygon
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Focused regression tests for [svgArcTo] and [rotatedEllipsePath] — the two
 * non-trivial trigonometry helpers the `content/generated` package uses to
 * hand-translate SVG arc/rotate commands into Compose [Path] calls (see the
 * mechanism note on [SunnyDayPaths]). `TemplateCatalogTest`'s
 * `hitPolygon.size >= 3` assertion would still pass even if these produced a
 * geometrically *wrong* but non-degenerate shape, so this file checks actual
 * sampled coordinates against hand-computed expected values instead.
 *
 * Lives in `androidUnitTest` (not `commonTest`) and runs under Robolectric
 * with `NATIVE` graphics mode for the same reason as `TemplateCatalogTest`:
 * every assertion here needs a real `Path`/`PathMeasure`, which the stub
 * `android.jar` cannot provide (see the note on `TemplateCatalogTest` and the
 * `robolectric` entry in `gradle/libs.versions.toml`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SvgPathUtilsTest {

    private fun assertNear(actual: Offset, expected: Offset, epsilon: Float, label: String) {
        val dx = actual.x - expected.x
        val dy = actual.y - expected.y
        val dist = sqrt(dx * dx + dy * dy)
        assertTrue(dist <= epsilon, "$label: expected ~$expected, got $actual (off by $dist)")
    }

    @Test
    fun svgArcTo_quarterCircle_sweepTrue_matchesHandComputedPoints() {
        // Arc from (10,0) to (0,10), rx=ry=10, largeArc=false, sweep=true — the exact
        // flag combination ("a30,30 0 0 1 ...") every arc in sunny-day's cloud region uses.
        // This traces a quarter circle centered at the origin. Hand-solving the SVG
        // endpoint-to-center formula for these inputs gives center=(0,0), sweeping from
        // angle 0deg to 90deg — so the expected points below are plain unit-circle math,
        // not a re-derivation of svgArcTo's own formula.
        val path = Path().apply {
            moveTo(10f, 0f)
            svgArcTo(x1 = 10f, y1 = 0f, rx = 10f, ry = 10f, largeArc = false, sweep = true, x2 = 0f, y2 = 10f)
        }
        val measure = PathMeasure().apply { setPath(path, forceClosed = false) }
        val length = measure.length

        // Expected arc length for a 90-degree arc of radius 10: (pi/2)*10 ~= 15.708.
        assertTrue(length in 15.0f..16.4f, "quarter-circle arc length should be ~15.71, was $length")

        assertNear(measure.getPosition(0f), Offset(10f, 0f), 0.3f, "start")
        assertNear(measure.getPosition(length), Offset(0f, 10f), 0.3f, "end")
        // Midpoint of a 90-degree arc of radius 10 centered at the origin sits at 45
        // degrees: (10*cos45, 10*sin45) = (7.071, 7.071). A sign error flipping the sweep
        // direction would instead land here at (7.071, -7.071), ~14 units away.
        assertNear(measure.getPosition(length / 2f), Offset(7.071f, 7.071f), 0.5f, "45-degree midpoint")
    }

    @Test
    fun rotatedEllipsePath_zeroRotation_isIdentity() {
        // rotationDegrees=0 must leave every point exactly where the un-rotated ellipse
        // would put it, regardless of the external pivot — sanity-checks rotateAround's
        // base case before the actually-used rotated cases below.
        val path = rotatedEllipsePath(cx = 160f, cy = 92f, rx = 32f, ry = 56f, rotationDegrees = 0f, pivotX = 160f, pivotY = 160f)
        val measure = PathMeasure().apply { setPath(path, forceClosed = true) }
        // The path's first point (distance 0) is exactly the moveTo target: the
        // un-rotated ellipse's rightmost vertex, (cx+rx, cy) = (192, 92).
        assertNear(measure.getPosition(0f), Offset(192f, 92f), 0.05f, "unrotated right vertex")
    }

    @Test
    fun rotatedEllipsePath_72Degrees_matchesHandComputedPivotRotation() {
        // garden-flower's petal2 is rotatedEllipsePath(..., rotationDegrees = 72f, pivotX =
        // 160f, pivotY = 160f). Hand-computed via the standard 2D rotation-about-a-point
        // formula (SVG rotate() convention, y-axis down): rightmost vertex (192,92) is
        // (dx,dy)=(32,-68) relative to pivot (160,160).
        //   nx = dx*cos72 - dy*sin72 + 160 = 32*0.309017 + 68*0.951057 + 160 ~= 234.56
        //   ny = dy*cos72 + dx*sin72 + 160 = -68*0.309017 + 32*0.951057 + 160 ~= 169.42
        val path = rotatedEllipsePath(cx = 160f, cy = 92f, rx = 32f, ry = 56f, rotationDegrees = 72f, pivotX = 160f, pivotY = 160f)
        val measure = PathMeasure().apply { setPath(path, forceClosed = true) }
        assertNear(measure.getPosition(0f), Offset(234.56f, 169.42f), 0.05f, "72-degree rotated right vertex")
    }

    @Test
    fun rotatedEllipsePath_144Degrees_matchesHandComputedPivotRotation() {
        // garden-flower's petal3 is rotatedEllipsePath(..., rotationDegrees = 144f, pivotX
        // = 160f, pivotY = 160f). Hand-computed: (dx,dy)=(32,-68); cos144=-0.809017,
        // sin144=0.587785.
        //   nx = 32*-0.809017 + 68*0.587785 + 160 ~= 174.08
        //   ny = -68*-0.809017 + 32*0.587785 + 160 ~= 233.82
        val path = rotatedEllipsePath(cx = 160f, cy = 92f, rx = 32f, ry = 56f, rotationDegrees = 144f, pivotX = 160f, pivotY = 160f)
        val measure = PathMeasure().apply { setPath(path, forceClosed = true) }
        assertNear(measure.getPosition(0f), Offset(174.08f, 233.82f), 0.05f, "144-degree rotated right vertex")
    }

    @Test
    fun rotatedEllipsePath_72Degrees_topVertexAlsoMatchesHandComputedRotation() {
        // Beyond the exact moveTo anchor (the right vertex, checked above), also confirm a
        // second known anchor — the "top" vertex (cx, cy-ry) before rotation — appears in
        // the sampled outline at its hand-computed rotated position. This catches a bug
        // isolated to one axis/quadrant that the single right-vertex check above could miss.
        // Unrotated top vertex = (160, 36) -> (dx,dy)=(0,-124) relative to pivot (160,160).
        //   nx = 0*cos72 - (-124)*sin72 + 160 = 124*0.951057 + 160 ~= 277.93
        //   ny = -124*cos72 + 0*sin72 + 160 = -124*0.309017 + 160 ~= 121.68
        val path = rotatedEllipsePath(cx = 160f, cy = 92f, rx = 32f, ry = 56f, rotationDegrees = 72f, pivotX = 160f, pivotY = 160f)
        val polygon = path.toHitPolygon(samples = 720)
        val expected = Offset(277.93f, 121.68f)
        val nearest = polygon.minByOrNull { p ->
            val dx = p.x - expected.x
            val dy = p.y - expected.y
            dx * dx + dy * dy
        }!!
        assertNear(nearest, expected, 1.5f, "72-degree rotated top vertex (nearest sampled point)")
    }
}
