package com.calmcoloring.app.content.remote

import androidx.compose.ui.geometry.Offset
import com.calmcoloring.app.content.generated.SunnyDayPaths
import com.calmcoloring.app.geometry.toHitPolygon
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SvgPathDataParserTest {

    // Ground truth: SunnyDayPaths.hill is the hand-authored Kotlin Path built
    // directly from svg/sunny-day.svg's `hill` region (see that file's own
    // comment: d="M0,320 L0,235 Q160,175 320,235 L320,320 Z"). Parsing the
    // same d string at runtime must sample to the same hit polygon.
    @Test
    fun parsesLineAndQuadraticCommands_matchingHandAuthoredPath() {
        val parsed = parseSvgPathData("M0,320 L0,235 Q160,175 320,235 L320,320 Z")
        val expected = SunnyDayPaths.hill

        val parsedPolygon = parsed.toHitPolygon(samples = 48)
        val expectedPolygon = expected.toHitPolygon(samples = 48)

        assertEquals(expectedPolygon.size, parsedPolygon.size)
        parsedPolygon.zip(expectedPolygon).forEach { (actual, expectedPoint) ->
            assertTrue(
                (actual - expectedPoint).getDistance() < 0.5f,
                "expected ~$expectedPoint, got $actual",
            )
        }
    }

    // Ground truth: SunnyDayPaths.cloud, from the same file's `cloud` region
    // (d="M55,150 a30,30 0 0 1 58,-10 a26,26 0 0 1 44,8 a24,24 0 0 1 -6,46
    // h-80 a26,26 0 0 1 -16,-44 Z"). Exercises relative moveto, relative arc
    // (lowercase "a"), relative horizontal-line ("h"), and close.
    @Test
    fun parsesRelativeArcAndHorizontalLineCommands_matchingHandAuthoredPath() {
        val parsed = parseSvgPathData(
            "M55,150 a30,30 0 0 1 58,-10 a26,26 0 0 1 44,8 a24,24 0 0 1 -6,46 h-80 a26,26 0 0 1 -16,-44 Z",
        )
        val expected = SunnyDayPaths.cloud

        val parsedPolygon = parsed.toHitPolygon(samples = 96)
        val expectedPolygon = expected.toHitPolygon(samples = 96)

        assertEquals(expectedPolygon.size, parsedPolygon.size)
        var maxOffset = 0f
        parsedPolygon.zip(expectedPolygon).forEach { (actual, expectedPoint) ->
            maxOffset = maxOf(maxOffset, (actual - expectedPoint).getDistance())
        }
        assertTrue(maxOffset < 1.0f, "max sampled-point offset was $maxOffset")
    }

    private operator fun Offset.minus(other: Offset) = Offset(x - other.x, y - other.y)
}
