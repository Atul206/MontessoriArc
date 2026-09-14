package com.calmcoloring.app.content.remote

import androidx.compose.ui.graphics.Path
import com.calmcoloring.app.content.generated.svgArcTo

private val PATH_TOKEN = Regex("[MmLlHhVvQqCcAaZz]|-?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?")

/**
 * Parses an SVG `d` attribute into a Compose [Path], for the command subset
 * every svg-source file in this repo actually uses: M/m, L/l, H/h, V/v,
 * Q/q, C/c, A/a, and Z/z — no shorthand S/T curves. Arc flags (`largeArc`,
 * `sweep`) must be whitespace-separated tokens (`"a30,30 0 0 1 ..."`, never
 * the digit-glued `"a30,30 0011"` form) — true of every hand-authored arc in
 * this project (see docs/content-ota.md), and this tokenizer relies on it.
 * The x-axis-rotation argument is parsed but always treated as 0, matching
 * every arc drawn so far; extend if a template ever needs a rotated arc.
 */
internal fun parseSvgPathData(d: String): Path {
    val tokens = PATH_TOKEN.findAll(d.trim()).map { it.value }.toList()
    val path = Path()
    var index = 0
    var command = ' '
    var cx = 0f
    var cy = 0f
    var startX = 0f
    var startY = 0f

    fun nextNumber(): Float = tokens[index++].toFloat()

    while (index < tokens.size) {
        val token = tokens[index]
        if (token.length == 1 && token[0].isLetter()) {
            command = token[0]
            index++
        }
        when (command) {
            'M' -> {
                cx = nextNumber(); cy = nextNumber()
                path.moveTo(cx, cy)
                startX = cx; startY = cy
                command = 'L'
            }
            'm' -> {
                cx += nextNumber(); cy += nextNumber()
                path.moveTo(cx, cy)
                startX = cx; startY = cy
                command = 'l'
            }
            'L' -> { cx = nextNumber(); cy = nextNumber(); path.lineTo(cx, cy) }
            'l' -> { cx += nextNumber(); cy += nextNumber(); path.lineTo(cx, cy) }
            'H' -> { cx = nextNumber(); path.lineTo(cx, cy) }
            'h' -> { cx += nextNumber(); path.lineTo(cx, cy) }
            'V' -> { cy = nextNumber(); path.lineTo(cx, cy) }
            'v' -> { cy += nextNumber(); path.lineTo(cx, cy) }
            'Q' -> {
                val x1 = nextNumber(); val y1 = nextNumber()
                cx = nextNumber(); cy = nextNumber()
                path.quadraticTo(x1, y1, cx, cy)
            }
            'q' -> {
                val x1 = cx + nextNumber(); val y1 = cy + nextNumber()
                val ex = cx + nextNumber(); val ey = cy + nextNumber()
                path.quadraticTo(x1, y1, ex, ey)
                cx = ex; cy = ey
            }
            'C' -> {
                val x1 = nextNumber(); val y1 = nextNumber()
                val x2 = nextNumber(); val y2 = nextNumber()
                cx = nextNumber(); cy = nextNumber()
                path.cubicTo(x1, y1, x2, y2, cx, cy)
            }
            'c' -> {
                val x1 = cx + nextNumber(); val y1 = cy + nextNumber()
                val x2 = cx + nextNumber(); val y2 = cy + nextNumber()
                val ex = cx + nextNumber(); val ey = cy + nextNumber()
                path.cubicTo(x1, y1, x2, y2, ex, ey)
                cx = ex; cy = ey
            }
            'A' -> {
                val rx = nextNumber(); val ry = nextNumber()
                nextNumber()
                val largeArc = nextNumber() != 0f
                val sweep = nextNumber() != 0f
                val ex = nextNumber(); val ey = nextNumber()
                path.svgArcTo(cx, cy, rx, ry, largeArc, sweep, ex, ey)
                cx = ex; cy = ey
            }
            'a' -> {
                val rx = nextNumber(); val ry = nextNumber()
                nextNumber()
                val largeArc = nextNumber() != 0f
                val sweep = nextNumber() != 0f
                val ex = cx + nextNumber(); val ey = cy + nextNumber()
                path.svgArcTo(cx, cy, rx, ry, largeArc, sweep, ex, ey)
                cx = ex; cy = ey
            }
            'Z', 'z' -> {
                path.close()
                cx = startX; cy = startY
            }
            else -> error("Unsupported SVG path command '$command' in: $d")
        }
    }
    return path
}
