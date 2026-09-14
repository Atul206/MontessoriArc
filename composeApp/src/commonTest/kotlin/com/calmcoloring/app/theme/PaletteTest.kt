package com.calmcoloring.app.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class PaletteTest {
    @Test
    fun swatchPalette_hasExactlyTwelveColors() {
        assertEquals(12, CalmPalette.swatches.size)
    }

    @Test
    fun swatchPalette_hasNoDuplicateColors() {
        assertEquals(CalmPalette.swatches.size, CalmPalette.swatches.toSet().size)
    }
}
