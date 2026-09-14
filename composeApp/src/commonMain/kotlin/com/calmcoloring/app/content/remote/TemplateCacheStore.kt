package com.calmcoloring.app.content.remote

import androidx.compose.ui.graphics.Path
import com.calmcoloring.app.db.TemplateCacheDatabase
import com.calmcoloring.app.geometry.toHitPolygon
import com.calmcoloring.app.model.RegionSpec
import com.calmcoloring.app.model.Template
import com.calmcoloring.app.theme.CalmPalette

/**
 * Wraps the SQLDelight-generated queries with the domain-shaped operations
 * [ContentRepository] needs. Regions are stored as [RemoteShape.encode]
 * strings and rebuilt into Compose [Path]s (and hit polygons) on every read
 * — cheap for this app's handful of simple-shape templates, and avoids
 * trying to store a platform [Path] object in a database row.
 */
class TemplateCacheStore(private val database: TemplateCacheDatabase) {
    private val queries = database.templateCacheQueries

    fun cachedContentVersion(id: String): Int? =
        queries.selectTemplate(id).executeAsOneOrNull()?.contentVersion?.toInt()

    /**
     * @param sortIndex this template's position in the manifest's
     *   `templates` array as of this refresh — persisted so gallery order
     *   stays stable across refreshes (SQLite rowids drift on
     *   `INSERT OR REPLACE`; see `TemplateCache.sq`). Defaults to
     *   [Int.MAX_VALUE] (sorts last) for callers — tests, mainly — that
     *   don't care about ordering.
     */
    fun replaceTemplate(
        id: String,
        name: String,
        accent: AccentKey,
        viewBoxWidth: Float,
        viewBoxHeight: Float,
        contentVersion: Int,
        regions: List<RemoteRegion>,
        sortIndex: Int = Int.MAX_VALUE,
    ) {
        queries.transaction {
            queries.upsertTemplate(
                id, name, accent.name, viewBoxWidth.toDouble(), viewBoxHeight.toDouble(),
                contentVersion.toLong(), sortIndex.toLong(),
            )
            queries.deleteRegions(id)
            regions.forEachIndexed { index, region ->
                val encodedRotation = region.rotation?.let { "${it.angleDegrees},${it.cx},${it.cy}" }
                queries.insertRegion(id, index.toLong(), region.id, region.shape.encode(), encodedRotation)
            }
        }
    }

    /** Rollback/removal path (see `docs/content-ota.md`'s "removing content"
     *  section): purges a template this device previously cached, in
     *  response to the manifest listing its id under `removedIds`. */
    fun deleteTemplate(id: String) {
        queries.transaction {
            queries.deleteRegions(id)
            queries.deleteTemplate(id)
        }
    }

    fun allTemplates(): List<Template> =
        queries.selectAllTemplates().executeAsList().map { row ->
            val regions = queries.selectRegions(row.id).executeAsList().map { regionRow ->
                val rotation = regionRow.rotation?.let { encoded ->
                    val (angle, cx, cy) = encoded.split(",").map { it.toFloat() }
                    Rotation(angle, cx, cy)
                }
                val path: Path = decodeShape(regionRow.encodedShape).toPath(rotation)
                RegionSpec(regionRow.regionId, path, path.toHitPolygon())
            }
            Template(
                id = row.id,
                name = row.name,
                accent = AccentKey.valueOf(row.accent).toColor(),
                viewBoxWidth = row.viewBoxWidth.toFloat(),
                viewBoxHeight = row.viewBoxHeight.toFloat(),
                regions = regions,
            )
        }
}

private fun AccentKey.toColor() = when (this) {
    AccentKey.SAGE -> CalmPalette.SageLight
    AccentKey.SKY -> CalmPalette.SkyLight
    AccentKey.CLAY -> CalmPalette.ClayLight
    AccentKey.SAND -> CalmPalette.SandLight
    AccentKey.LILAC -> CalmPalette.LilacLight
    AccentKey.MOSS -> CalmPalette.MossLight
}
