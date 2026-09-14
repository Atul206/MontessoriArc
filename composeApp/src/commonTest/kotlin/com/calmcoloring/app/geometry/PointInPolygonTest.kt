package com.calmcoloring.app.geometry

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PointInPolygonTest {
    private val square = listOf(
        Offset(0f, 0f), Offset(100f, 0f), Offset(100f, 100f), Offset(0f, 100f),
    )

    @Test
    fun pointInsideSquare_returnsTrue() {
        assertTrue(pointInPolygon(Offset(50f, 50f), square))
    }

    @Test
    fun pointOutsideSquare_returnsFalse() {
        assertFalse(pointInPolygon(Offset(150f, 50f), square))
    }

    @Test
    fun pointJustInsideEdge_returnsTrue() {
        assertTrue(pointInPolygon(Offset(1f, 50f), square))
    }
}
