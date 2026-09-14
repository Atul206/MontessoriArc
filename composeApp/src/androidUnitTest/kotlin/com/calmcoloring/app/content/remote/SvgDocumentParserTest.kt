package com.calmcoloring.app.content.remote

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SvgDocumentParserTest {

    // The exact contents of svg/sunny-day.svg — the simplest of the 8
    // existing templates, and one that exercises all three element kinds
    // (rect, path, circle).
    private val sunnyDaySvg = """
        <svg viewBox="0 0 320 320" xmlns="http://www.w3.org/2000/svg">
          <rect id="background" x="1.5" y="1.5" width="317" height="317" rx="18"/>
          <path id="hill" d="M0,320 L0,235 Q160,175 320,235 L320,320 Z"/>
          <path id="cloud" d="M55,150 a30,30 0 0 1 58,-10 a26,26 0 0 1 44,8 a24,24 0 0 1 -6,46 h-80 a26,26 0 0 1 -16,-44 Z"/>
          <circle id="sun" cx="228" cy="92" r="48"/>
        </svg>
    """.trimIndent()

    @Test
    fun parsesViewBoxAndAllElementsInDocumentOrder() {
        val document = parseSvgDocument(sunnyDaySvg)

        assertEquals(320f, document.viewBoxWidth)
        assertEquals(320f, document.viewBoxHeight)
        assertEquals(listOf("background", "hill", "cloud", "sun"), document.regions.map { it.id })

        assertEquals(RemoteShape.RoundRect(1.5f, 1.5f, 317f, 317f, 18f), document.regions[0].shape)
        assertEquals(
            RemoteShape.PathData("M0,320 L0,235 Q160,175 320,235 L320,320 Z"),
            document.regions[1].shape,
        )
        assertEquals(RemoteShape.Circle(228f, 92f, 48f), document.regions[3].shape)
    }
}
