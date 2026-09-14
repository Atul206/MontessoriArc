package com.calmcoloring.app.db

import app.cash.sqldelight.db.SqlDriver

/** Platform SQLite driver for [TemplateCacheDatabase] — real file-backed
 *  storage on both platforms; tests build their own in-memory driver
 *  directly (see `TemplateCacheStoreTest`) rather than through this. */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
