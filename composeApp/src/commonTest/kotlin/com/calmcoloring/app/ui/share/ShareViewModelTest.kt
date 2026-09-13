package com.calmcoloring.app.ui.share

import kotlin.test.Test
import kotlin.test.assertEquals

class ShareViewModelTest {
    @Test
    fun caption_blankName_showsDefaultCopy() {
        assertEquals("A little artist's painting", captionFor(name = ""))
        assertEquals("A little artist's painting", captionFor(name = "   "))
    }

    @Test
    fun caption_withName_showsPaintedByName() {
        assertEquals("Painted by Maya", captionFor(name = "Maya"))
    }
}
