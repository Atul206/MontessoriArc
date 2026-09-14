package com.calmcoloring.app.content.remote

private val VIEWBOX_REGEX = Regex("viewBox=\"[-0-9.]+\\s+[-0-9.]+\\s+([-0-9.]+)\\s+([-0-9.]+)\"")
private val ELEMENT_REGEX = Regex("<(rect|circle|path|ellipse)\\b([^>]*)/?>")
private val ATTR_REGEX = Regex("([\\w:-]+)=\"([^\"]*)\"")
private val ROTATE_TRANSFORM_REGEX = Regex("rotate\\(\\s*([-0-9.]+)[,\\s]+([-0-9.]+)[,\\s]+([-0-9.]+)\\s*\\)")

// Any opening tag in the document, used only to detect elements this parser
// doesn't understand (see the loud-failure check in [parseSvgDocument]) — not
// used to actually parse those elements.
private val ANY_OPENING_TAG_REGEX = Regex("<(\\w+)\\b")
private val KNOWN_TAGS = setOf("rect", "circle", "path", "ellipse")

data class RemoteSvgDocument(
    val viewBoxWidth: Float,
    val viewBoxHeight: Float,
    val regions: List<RemoteRegion>,
)

/**
 * Parses one of this repo's `svg` template sources — a flat SVG
 * document with a single `viewBox` and top-level `<rect>`/`<circle>`/
 * `<path>`/`<ellipse>` children, each carrying an `id` — into a
 * [RemoteSvgDocument]. This is *not* a general SVG parser: no groups, no
 * style attributes, and the only `transform` form understood is
 * `rotate(angle,cx,cy)`. See `docs/content-ota.md` for the authoring
 * convention this matches.
 *
 * Any top-level element whose tag isn't one of [KNOWN_TAGS] (a `<g>`,
 * `<defs>`, or anything else this parser can't handle) makes this function
 * throw rather than silently skip it — a document using an unsupported
 * element would otherwise cache a region list quietly missing whatever that
 * element drew. A thrown exception here is caught per-entry by
 * [ContentRepository.refresh], which falls back to the previous/bundled
 * version instead.
 */
fun parseSvgDocument(svgText: String): RemoteSvgDocument {
    val viewBoxMatch = VIEWBOX_REGEX.find(svgText) ?: error("No viewBox attribute found in SVG document")
    val width = viewBoxMatch.groupValues[1].toFloat()
    val height = viewBoxMatch.groupValues[2].toFloat()

    val unknownTag = ANY_OPENING_TAG_REGEX.findAll(svgText)
        .map { it.groupValues[1] }
        .firstOrNull { it != "svg" && it !in KNOWN_TAGS }
    if (unknownTag != null) {
        error("Unsupported SVG element <$unknownTag> — parseSvgDocument only understands ${KNOWN_TAGS.joinToString()}")
    }

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
            "ellipse" -> RemoteShape.Ellipse(
                cx = attrs.getValue("cx").toFloat(),
                cy = attrs.getValue("cy").toFloat(),
                rx = attrs.getValue("rx").toFloat(),
                ry = attrs.getValue("ry").toFloat(),
            )
            "path" -> RemoteShape.PathData(attrs.getValue("d"))
            else -> error("Unsupported element <$tag>")
        }
        val rotation = attrs["transform"]?.let { transform ->
            ROTATE_TRANSFORM_REGEX.find(transform)?.let { m ->
                Rotation(
                    angleDegrees = m.groupValues[1].toFloat(),
                    cx = m.groupValues[2].toFloat(),
                    cy = m.groupValues[3].toFloat(),
                )
            }
        }
        RemoteRegion(id, shape, rotation)
    }.toList()

    return RemoteSvgDocument(width, height, regions)
}
