package com.calmcoloring.app.ui.coloring

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.calmcoloring.app.model.Template
import com.calmcoloring.app.theme.CalmPalette

class ColoringViewModel(private val template: Template) : ViewModel() {
    var selectedColor: Color by mutableStateOf(CalmPalette.swatches.first())
        private set

    private val _fills = mutableStateMapOf<String, Color>()
    val fills: Map<String, Color> get() = _fills

    fun selectColor(color: Color) {
        selectedColor = color
    }

    fun onRegionTapped(regionId: String) {
        _fills[regionId] = selectedColor
    }

    fun reset() {
        _fills.clear()
    }
}
