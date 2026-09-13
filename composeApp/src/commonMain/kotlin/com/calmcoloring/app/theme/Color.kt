package com.calmcoloring.app.theme

import androidx.compose.ui.graphics.Color

object CalmPalette {
    // Neutrals — light
    val BgLight = Color(0xFFF1ECE2)
    val SurfaceLight = Color(0xFFFAF6EE)
    val InkLight = Color(0xFF3A342B)
    val InkSoftLight = Color(0xFF7A6F5F)
    val LineLight = Color(0xFFDDD3BF)

    // Neutrals — dark
    val BgDark = Color(0xFF211E19)
    val SurfaceDark = Color(0xFF2A2721)
    val InkDark = Color(0xFFECE5D8)
    val InkSoftDark = Color(0xFFB3A893)
    val LineDark = Color(0xFF423C31)

    // System palette — light (chrome + fill, one set, per PRD §5.2)
    val SageLight = Color(0xFF8FA382)
    val SkyLight = Color(0xFF7E97A6)
    val ClayLight = Color(0xFFC98B7A)
    val SandLight = Color(0xFFD9AE63)
    val LilacLight = Color(0xFFA48FA8)
    val MossLight = Color(0xFF6D8A68)

    // System palette — dark
    val SageDark = Color(0xFF9DB38F)
    val SkyDark = Color(0xFF8FABBA)
    val ClayDark = Color(0xFFD99E8D)
    val SandDark = Color(0xFFE2BE7C)
    val LilacDark = Color(0xFFB49FB8)
    val MossDark = Color(0xFF83A37D)

    val swatches: List<Color> = listOf(SageLight, SkyLight, ClayLight, SandLight, LilacLight, MossLight)
    fun swatchesFor(darkTheme: Boolean): List<Color> =
        if (darkTheme) listOf(SageDark, SkyDark, ClayDark, SandDark, LilacDark, MossDark) else swatches
}
