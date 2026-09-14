package com.calmcoloring.app.content.remote

import com.calmcoloring.app.db.DatabaseDriverFactory
import com.calmcoloring.app.db.TemplateCacheDatabase

// This repo's raw-content root. Publishing convention (adding/bumping
// entries in svg/manifest.json, adding new svg/*.svg files) is documented
// in docs/content-ota.md.
private const val CONTENT_BASE_URL = "https://raw.githubusercontent.com/Atul206/MontessoriArc/main/svg"

/**
 * Lazily builds the single app-wide [ContentRepository], wiring together the
 * platform [DatabaseDriverFactory] and [createHttpClient] (both
 * expect/actual). `App.kt` reads [repository] once and calls `refresh()`
 * from a `LaunchedEffect` on launch.
 */
object ContentRepositoryProvider {
    val repository: ContentRepository by lazy {
        val database = TemplateCacheDatabase(DatabaseDriverFactory().createDriver())
        ContentRepository(
            api = ContentApi(createHttpClient(), CONTENT_BASE_URL),
            cache = TemplateCacheStore(database),
        )
    }
}
