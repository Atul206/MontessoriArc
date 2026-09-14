package com.calmcoloring.app.content.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The OTA content catalog served from `svg/manifest.json` in this repo (see
 * `docs/content-ota.md`). One entry per publishable template.
 * `contentVersion` is bumped by hand whenever `file`'s content changes, so
 * [ContentRepository] can tell "already have the latest" apart from "needs a
 * re-download" without hashing file bodies. [removedIds] is the rollback
 * mechanism for content that should be deleted outright (not fixed-in-place):
 * an id listed here gets purged from every device's local cache on next
 * refresh, rather than silently lingering forever just because its entry was
 * dropped from `templates`.
 */
@Serializable
data class ContentManifest(
    val templates: List<ManifestEntry>,
    val removedIds: List<String> = emptyList(),
)

@Serializable
data class ManifestEntry(
    val id: String,
    val name: String,
    val accent: AccentKey,
    val file: String,
    val contentVersion: Int,
)

/** Mirrors the fixed 6-color system palette in [com.calmcoloring.app.theme.CalmPalette] —
 *  remote content picks one of these rather than an arbitrary hex color, so
 *  OTA templates stay on-theme (and dark-mode-aware, once that's wired). */
@Serializable
enum class AccentKey { SAGE, SKY, CLAY, SAND, LILAC, MOSS }

private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * Decodes a manifest document leniently, entry by entry, rather than
 * atomically via `Json.decodeFromString<ContentManifest>` — an unrecognized
 * [AccentKey] value (or any other single bad field) on one template entry
 * would otherwise fail the whole document's decode, which would also throw
 * away [ContentManifest.removedIds] for that refresh. Since `removedIds` is
 * the rollback mechanism, that would mean one bad manifest entry disables
 * rollback of everything else too — see the final-review finding this
 * function fixes. A template entry that fails to decode is skipped (logged,
 * not silently dropped) rather than failing the whole manifest; `removedIds`
 * is always decoded independently of whether `templates` decoded cleanly.
 */
fun parseContentManifestLenient(json: String): ContentManifest {
    val root = lenientJson.parseToJsonElement(json).jsonObject

    val removedIds = try {
        root["removedIds"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
    } catch (e: Exception) {
        println("ContentManifest.parseContentManifestLenient: failed to decode removedIds: ${e.message}")
        emptyList()
    }

    val templates = root["templates"]?.jsonArray.orEmpty().mapNotNull { element ->
        try {
            lenientJson.decodeFromJsonElement(ManifestEntry.serializer(), element)
        } catch (e: SerializationException) {
            val id = (element.jsonObject["id"] as? JsonPrimitive)?.contentOrNull ?: "<unknown>"
            println("ContentManifest.parseContentManifestLenient: skipping malformed template entry '$id': ${e.message}")
            null
        }
    }

    return ContentManifest(templates = templates, removedIds = removedIds)
}
