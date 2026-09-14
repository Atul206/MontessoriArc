package com.calmcoloring.app.content.remote

private val VIEWBOX_REGEX = Regex("viewBox=\"[-0-9.]+\\s+[-0-9.]+\\s+([-0-9.]+)\\s+([-0-9.]+)\"")
private val ELEMENT_REGEX = Regex("<(rect|circle|path)\\b([^>]*)/?>")
private val ATTR_REGEX = Regex("([\\w:-]+)=\"([^\"]*)\"")

data class RemoteSvgDocument(
    val viewBoxWidth: Float,
    val viewBoxHeight: Float,
    val regions: List<RemoteRegion>,
)

/**
 * Parses one of this repo's `svg` template sources — a flat SVG
 * document with a single `viewBox` and top-level `<rect>`/`<circle>`/
 * `<path>` children, each carrying an `id` — into a [RemoteSvgDocument].
 * This is *not* a general SVG parser: no groups, no `transform`, no style
 * attributes. See `docs/content-ota.md` for the authoring convention this
 * matches.
 */
fun parseSvgDocument(svgText: String): RemoteSvgDocument {
    val viewBoxMatch = VIEWBOX_REGEX.find(svgText) ?: error("No viewBox attribute found in SVG document")
    val width = viewBoxMatch.groupValues[1].toFloat()
    val height = viewBoxMatch.groupValues[2].toFloat()

    val regions = ELEMENT_REGEX.findAll(svgText).map { element ->
        val tag = element.groupValues[1]
        val attrs = ATTR_REGEX.findAll(element.groupValues[2])
            .associate { it.groupValues[1] to it.groupValues[2] }
        val id = attrs["id"] ?: error("<$tag> element missing id attribute: ${element.value}")
        val shape: RemoteShape = when (tag) {
            "rect" -> RemoteShape.RoundRect(
                x = attrs.getValue("x").toFloat(),
                y = attrs.getValue("y").toFloat(),
                width = attrs.getValue("width").toFloat(),
                height = attrs.getValue("height").toFloat(),
                rx = attrs["rx"]?.toFloat() ?: 0f,
            )
            "circle" -> RemoteShape.Circle(
                cx = attrs.getValue("cx").toFloat(),
                cy = attrs.getValue("cy").toFloat(),
                r = attrs.getValue("r").toFloat(),
            )
            "path" -> RemoteShape.PathData(attrs.getValue("d"))
            else -> error("Unsupported element <$tag>")
        }
        RemoteRegion(id, shape)
    }.toList()

    return RemoteSvgDocument(width, height, regions)
}
