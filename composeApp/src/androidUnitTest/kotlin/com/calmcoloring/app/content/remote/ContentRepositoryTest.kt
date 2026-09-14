package com.calmcoloring.app.content.remote

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.calmcoloring.app.content.TemplateCatalog
import com.calmcoloring.app.db.TemplateCacheDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContentRepositoryTest {

    private fun newCacheStore(): TemplateCacheStore {
        val driver = AndroidSqliteDriver(TemplateCacheDatabase.Schema, RuntimeEnvironment.getApplication(), null)
        return TemplateCacheStore(TemplateCacheDatabase(driver))
    }

    private fun mockApi(manifestJson: String, svgByFile: Map<String, String>): ContentApi {
        val client = HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath.substringAfterLast('/')
                val body = if (path == "manifest.json") manifestJson else svgByFile.getValue(path)
                respond(content = body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return ContentApi(client, baseUrl = "https://example.com/svg")
    }

    @Test
    fun templates_beforeRefresh_isJustTheBundledCatalog() {
        val repository = ContentRepository(mockApi("""{"templates":[]}""", emptyMap()), newCacheStore())
        assertEquals(TemplateCatalog.all.map { it.id }.toSet(), repository.templates.value.map { it.id }.toSet())
    }

    @Test
    fun refresh_downloadsAndCachesANewRemoteTemplate() = runTest {
        val manifest = """
            {"templates":[{"id":"montessori-jug","name":"Pouring Jug","accent":"SKY","file":"montessori-jug.svg","contentVersion":1}]}
        """.trimIndent()
        val svg = """<svg viewBox="0 0 100 100"><circle id="jug" cx="50" cy="50" r="40"/></svg>"""
        val repository = ContentRepository(mockApi(manifest, mapOf("montessori-jug.svg" to svg)), newCacheStore())

        repository.refresh()

        val ids = repository.templates.value.map { it.id }
        assertTrue("montessori-jug" in ids, "expected downloaded template in $ids")
        assertEquals(TemplateCatalog.all.size + 1, repository.templates.value.size)
    }

    @Test
    fun refresh_skipsEntriesAlreadyAtTheCachedVersion() = runTest {
        val cache = newCacheStore()
        cache.replaceTemplate(
            id = "montessori-jug", name = "Pouring Jug", accent = AccentKey.SKY,
            viewBoxWidth = 100f, viewBoxHeight = 100f, contentVersion = 1,
            regions = listOf(RemoteRegion("jug", RemoteShape.Circle(50f, 50f, 40f))),
        )
        val manifest = """
            {"templates":[{"id":"montessori-jug","name":"Pouring Jug","accent":"SKY","file":"montessori-jug.svg","contentVersion":1}]}
        """.trimIndent()
        // No SVG body registered for this file — if refresh() tried to fetch it
        // (i.e. failed to skip the up-to-date entry), mockApi would throw.
        val repository = ContentRepository(mockApi(manifest, emptyMap()), cache)

        repository.refresh()

        assertEquals(1, cache.cachedContentVersion("montessori-jug"))
    }

    @Test
    fun refresh_purgesCachedTemplateListedInRemovedIds() = runTest {
        val cache = newCacheStore()
        cache.replaceTemplate(
            id = "bad-template", name = "Bad", accent = AccentKey.CLAY,
            viewBoxWidth = 100f, viewBoxHeight = 100f, contentVersion = 1,
            regions = listOf(RemoteRegion("a", RemoteShape.Circle(50f, 50f, 40f))),
        )
        val manifest = """
            {"templates":[], "removedIds":["bad-template"]}
        """.trimIndent()
        val repository = ContentRepository(mockApi(manifest, emptyMap()), cache)

        repository.refresh()

        assertEquals(null, cache.cachedContentVersion("bad-template"))
        assertTrue("bad-template" !in repository.templates.value.map { it.id })
    }

    @Test
    fun refresh_removedIds_fallsBackToBundledTemplateIfOneExists() = runTest {
        val cache = newCacheStore()
        val bundledId = TemplateCatalog.all.first().id
        cache.replaceTemplate(
            id = bundledId, name = "Bad Override", accent = AccentKey.CLAY,
            viewBoxWidth = 100f, viewBoxHeight = 100f, contentVersion = 2,
            regions = listOf(RemoteRegion("a", RemoteShape.Circle(50f, 50f, 40f))),
        )
        val manifest = """{"templates":[], "removedIds":["$bundledId"]}"""
        val repository = ContentRepository(mockApi(manifest, emptyMap()), cache)

        repository.refresh()

        // The bad OTA override is gone; the id is back to being served from
        // the compiled-in TemplateCatalog rather than missing entirely.
        val restored = repository.templates.value.first { it.id == bundledId }
        assertEquals(TemplateCatalog.all.first().regions.map { it.id }, restored.regions.map { it.id })
    }
}
