package com.calmcoloring.app.ui.share

import kotlin.test.Test
import kotlin.test.assertEquals

class ShareViewModelTest {
    private val anonymousCaption = "A little artist's painting"
    private val paintedByTemplate = "Painted by %1\$s"

    @Test
    fun caption_blankName_showsDefaultCopy() {
        assertEquals(anonymousCaption, captionFor(name = "", anonymousCaption, paintedByTemplate))
        assertEquals(anonymousCaption, captionFor(name = "   ", anonymousCaption, paintedByTemplate))
    }

    @Test
    fun caption_withName_showsPaintedByName() {
        assertEquals("Painted by Maya", captionFor(name = "Maya", anonymousCaption, paintedByTemplate))
    }
}
