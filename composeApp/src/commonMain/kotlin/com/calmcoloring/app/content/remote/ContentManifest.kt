package com.calmcoloring.app.content.remote

import kotlinx.serialization.Serializable

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
