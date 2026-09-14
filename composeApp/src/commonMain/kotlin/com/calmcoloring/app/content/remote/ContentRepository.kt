package com.calmcoloring.app.content.remote

import com.calmcoloring.app.content.TemplateCatalog
import com.calmcoloring.app.model.Template
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Merges the 8 bundled launch templates ([TemplateCatalog.all], compiled
 * into the app binary) with whatever OTA content [TemplateCacheStore] has
 * cached. A remote id that collides with a bundled id overrides the bundled
 * entry — lets a bundled template's art be corrected post-launch without an
 * app-store release. Deleting a cached override this way (see [refresh]'s
 * `removedIds` handling) naturally falls back to the bundled version rather
 * than removing the id entirely, since [mergeTemplates] only excludes a
 * bundled entry when a *cached* one still exists for that id.
 *
 * The constructor does no DB I/O (see [_templates]'s initial value below) —
 * [refresh] does the (possibly first) cache read itself, off the main
 * thread. Call [refresh] from a suspend context (e.g. `LaunchedEffect`)
 * before relying on cached-only content being visible.
 */
class ContentRepository(
    private val api: ContentApi,
    private val cache: TemplateCacheStore,
) {
    // No DB read here — just the bundled catalog, which is free (already
    // compiled in). Reading the cache is deferred to refresh() so
    // constructing a ContentRepository never blocks whatever thread
    // constructs it (previously this called cache.allTemplates()
    // synchronously, which is a DB read + Path parsing + hit-polygon
    // sampling for every cached region — expensive enough to jank
    // composition if run on the main thread).
    private val _templates = MutableStateFlow(mergeTemplates(emptyList()))
    val templates: StateFlow<List<Template>> = _templates

    /**
     * Loads whatever is already cached (first, so it shows up even if the
     * network fetch below fails or is slow), then fetches `manifest.json`,
     * downloads and caches any entry whose `contentVersion` differs from
     * what's cached, purges any id listed in `removedIds` (the rollback
     * path for content that should be deleted outright — see
     * `docs/content-ota.md`), and republishes [templates]. Safe to call
     * repeatedly (e.g. on every app launch) — network or parse failures are
     * swallowed per-entry (or for the whole manifest fetch) so a bad or
     * offline refresh never breaks the templates already on screen. All DB
     * reads/writes and SVG parsing happen on [Dispatchers.Default], never on
     * whatever dispatcher the caller is running on.
     */
    suspend fun refresh() = withContext(Dispatchers.Default) {
        // Publish whatever's already cached first — makes previously
        // downloaded OTA content available immediately without waiting on
        // the network, and means the constructor above doesn't have to.
        _templates.update { mergeTemplates(cache.allTemplates()) }

        val manifest = try {
            api.fetchManifest()
        } catch (e: Exception) {
            println("ContentRepository.refresh: failed to fetch manifest: ${e.message}")
            return@withContext
        }
        var changed = false
        manifest.templates.forEachIndexed { index, entry ->
            if (cache.cachedContentVersion(entry.id) != entry.contentVersion) {
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
                        sortIndex = index,
                    )
                    changed = true
                } catch (e: Exception) {
                    println("ContentRepository.refresh: failed to fetch/parse entry '${entry.id}': ${e.message}")
                }
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
