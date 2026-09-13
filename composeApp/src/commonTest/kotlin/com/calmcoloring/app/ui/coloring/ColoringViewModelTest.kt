package com.calmcoloring.app.ui.coloring

import com.calmcoloring.app.model.Template
import com.calmcoloring.app.theme.CalmPalette
import kotlin.test.Test
import kotlin.test.assertEquals

class ColoringViewModelTest {
    private fun emptyTemplate() = Template("t", "Test", CalmPalette.SageLight, 320f, 320f, regions = emptyList())

    @Test
    fun initialSelectedColor_isFirstSwatch() {
        val vm = ColoringViewModel(emptyTemplate())
        assertEquals(CalmPalette.SageLight, vm.selectedColor)
    }

    @Test
    fun onRegionTapped_fillsRegionWithSelectedColor() {
        val vm = ColoringViewModel(emptyTemplate())
        vm.selectColor(CalmPalette.ClayLight)
        vm.onRegionTapped("roof")
        assertEquals(CalmPalette.ClayLight, vm.fills["roof"])
    }

    @Test
    fun reset_clearsAllFills() {
        val vm = ColoringViewModel(emptyTemplate())
        vm.onRegionTapped("roof")
        vm.reset()
        assertEquals(emptyMap(), vm.fills)
    }
}
