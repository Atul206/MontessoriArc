package com.calmcoloring.app.content.remote

import com.calmcoloring.app.content.TemplateCatalog
import com.calmcoloring.app.model.Template
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Merges the 8 bundled launch templates ([TemplateCatalog.all], compiled
 * into the app binary) with whatever OTA content [TemplateCacheStore] has
 * cached. A remote id that collides with a bundled id overrides the bundled
 * entry — lets a bundled template's art be corrected post-launch without an
 * app-store release. Deleting a cached override this way (see [refresh]'s
 * `removedIds` handling) naturally falls back to the bundled version rather
 * than removing the id entirely, since [mergeTemplates] only excludes a
 * bundled entry when a *cached* one still exists for that id.
 */
class ContentRepository(
    private val api: ContentApi,
    private val cache: TemplateCacheStore,
) {
    private val _templates = MutableStateFlow(mergeTemplates(cache.allTemplates()))
    val templates: StateFlow<List<Template>> = _templates

    /**
     * Fetches `manifest.json`, downloads and caches any entry whose
     * `contentVersion` differs from what's cached, purges any id listed in
     * `removedIds` (the rollback path for content that should be deleted
     * outright — see `docs/content-ota.md`), and republishes [templates].
     * Safe to call repeatedly (e.g. on every app launch) — network or parse
     * failures are swallowed per-entry (or for the whole manifest fetch) so
     * a bad or offline refresh never breaks the templates already on
     * screen.
     */
    suspend fun refresh() {
        val manifest = try {
            api.fetchManifest()
        } catch (e: Exception) {
            return
        }
        var changed = false
        for (entry in manifest.templates) {
            if (cache.cachedContentVersion(entry.id) == entry.contentVersion) continue
            try {
                val svgText = api.fetchSvgText(entry.file)
                val document = parseSvgDocument(svgText)
                cache.replaceTemplate(
                    id = entry.id,
                    name = entry.name,
                    accent = entry.accent,
                    viewBoxWidth = document.viewBoxWidth,
                    viewBoxHeight = document.viewBoxHeight,
                    contentVersion = entry.contentVersion,
                    regions = document.regions,
                )
                changed = true
            } catch (e: Exception) {
                continue
            }
        }
        for (id in manifest.removedIds) {
            if (cache.cachedContentVersion(id) == null) continue
            cache.deleteTemplate(id)
            changed = true
        }
        if (changed) _templates.update { mergeTemplates(cache.allTemplates()) }
    }

    private fun mergeTemplates(cached: List<Template>): List<Template> {
        val cachedIds = cached.map { it.id }.toSet()
        return TemplateCatalog.all.filterNot { it.id in cachedIds } + cached
    }
}
