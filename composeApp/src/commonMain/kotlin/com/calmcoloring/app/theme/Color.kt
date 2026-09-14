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

    // System palette — light, added later to give the coloring palette more
    // variety once it became scrollable (see PaletteSwatches in
    // ColoringScreen.kt) — same muted/desaturated character as the set above.
    val BlushLight = Color(0xFFD9A0A0)
    val ButterLight = Color(0xFFE6D18A)
    val TealLight = Color(0xFF7FA69C)
    val PlumLight = Color(0xFF9B7BA0)
    val CoralLight = Color(0xFFE0906B)
    val StoneLight = Color(0xFFA6A296)

    // System palette — dark
    val SageDark = Color(0xFF9DB38F)
    val SkyDark = Color(0xFF8FABBA)
    val ClayDark = Color(0xFFD99E8D)
    val SandDark = Color(0xFFE2BE7C)
    val LilacDark = Color(0xFFB49FB8)
    val MossDark = Color(0xFF83A37D)

    // System palette — dark, matching the *Light additions above.
    val BlushDark = Color(0xFFE6B4B4)
    val ButterDark = Color(0xFFF0DE9E)
    val TealDark = Color(0xFF93BAB0)
    val PlumDark = Color(0xFFAF8FB4)
    val CoralDark = Color(0xFFEBA482)
    val StoneDark = Color(0xFFBAB6A8)

    val swatches: List<Color> = listOf(
        SageLight, SkyLight, ClayLight, SandLight, LilacLight, MossLight,
        BlushLight, ButterLight, TealLight, PlumLight, CoralLight, StoneLight,
    )
    fun swatchesFor(darkTheme: Boolean): List<Color> =
        if (darkTheme) {
            listOf(
                SageDark, SkyDark, ClayDark, SandDark, LilacDark, MossDark,
                BlushDark, ButterDark, TealDark, PlumDark, CoralDark, StoneDark,
            )
        } else {
            swatches
        }
}
