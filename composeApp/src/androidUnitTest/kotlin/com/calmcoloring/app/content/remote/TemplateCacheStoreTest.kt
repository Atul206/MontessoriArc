package com.calmcoloring.app.content.remote

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.calmcoloring.app.db.TemplateCacheDatabase
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TemplateCacheStoreTest {

    // Robolectric provides a real SQLite implementation for AndroidSqliteDriver;
    // name = null opens an in-memory database, fresh per test.
    private fun newStore(): TemplateCacheStore {
        val driver = AndroidSqliteDriver(TemplateCacheDatabase.Schema, RuntimeEnvironment.getApplication(), null)
        return TemplateCacheStore(TemplateCacheDatabase(driver))
    }

    @Test
    fun cachedContentVersion_isNullBeforeAnyWrite() {
        val store = newStore()
        assertNull(store.cachedContentVersion("sunny-day"))
    }

    @Test
    fun replaceTemplate_thenAllTemplates_roundTripsRegionsInOrder() {
        val store = newStore()

        store.replaceTemplate(
            id = "sunny-day",
            name = "Sunny Day",
            accent = AccentKey.SAND,
            viewBoxWidth = 320f,
            viewBoxHeight = 320f,
            contentVersion = 1,
            regions = listOf(
                RemoteRegion("background", RemoteShape.RoundRect(1.5f, 1.5f, 317f, 317f, 18f)),
                RemoteRegion("hill", RemoteShape.PathData("M0,320 L0,235 Q160,175 320,235 L320,320 Z")),
                RemoteRegion("sun", RemoteShape.Circle(228f, 92f, 48f)),
            ),
        )

        assertEquals(1, store.cachedContentVersion("sunny-day"))

        val templates = store.allTemplates()
        assertEquals(1, templates.size)
        val template = templates.first()
        assertEquals("sunny-day", template.id)
        assertEquals("Sunny Day", template.name)
        assertEquals(listOf("background", "hill", "sun"), template.regions.map { it.id })
        template.regions.forEach { region -> assertEquals(true, region.hitPolygon.isNotEmpty()) }
    }

    @Test
    fun replaceTemplate_calledTwice_replacesRegionsRatherThanAppending() {
        val store = newStore()
        val original = listOf(RemoteRegion("a", RemoteShape.Circle(0f, 0f, 10f)))
        val replacement = listOf(
            RemoteRegion("a", RemoteShape.Circle(0f, 0f, 10f)),
            RemoteRegion("b", RemoteShape.Circle(1f, 1f, 5f)),
        )

        store.replaceTemplate("t", "T", AccentKey.SAGE, 100f, 100f, 1, original)
        store.replaceTemplate("t", "T", AccentKey.SAGE, 100f, 100f, 2, replacement)

        val template = store.allTemplates().first { it.id == "t" }
        assertEquals(listOf("a", "b"), template.regions.map { it.id })
        assertEquals(2, store.cachedContentVersion("t"))
    }

    @Test
    fun deleteTemplate_removesTemplateAndItsRegions() {
        val store = newStore()
        store.replaceTemplate(
            id = "bad-template", name = "Bad", accent = AccentKey.CLAY,
            viewBoxWidth = 320f, viewBoxHeight = 320f, contentVersion = 1,
            regions = listOf(RemoteRegion("a", RemoteShape.Circle(0f, 0f, 10f))),
        )
        assertEquals(1, store.allTemplates().size)

        store.deleteTemplate("bad-template")

        assertEquals(0, store.allTemplates().size)
        assertNull(store.cachedContentVersion("bad-template"))
    }
}
