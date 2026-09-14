package com.calmcoloring.app.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.calmcoloring.app.platform.appContext

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(TemplateCacheDatabase.Schema, appContext, "template-cache.db")
}
