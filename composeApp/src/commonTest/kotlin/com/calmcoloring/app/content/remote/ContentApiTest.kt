package com.calmcoloring.app.content.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentApiTest {

    private val manifestJson = """
        {"templates":[{"id":"sunny-day","name":"Sunny Day","accent":"SAND","file":"sunny-day.svg","contentVersion":1}]}
    """.trimIndent()

    private fun mockClient(responseBody: String, contentType: String) = HttpClient(
        MockEngine { request ->
            respond(
                content = responseBody,
                headers = headersOf(HttpHeaders.ContentType, contentType),
            )
        },
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    @Test
    fun fetchManifest_decodesManifestFromBaseUrl() = runTest {
        val api = ContentApi(mockClient(manifestJson, "application/json"), baseUrl = "https://example.com/svg")

        val manifest = api.fetchManifest()

        assertEquals(1, manifest.templates.size)
        assertEquals("sunny-day", manifest.templates.first().id)
    }

    @Test
    fun fetchSvgText_returnsRawBody() = runTest {
        val api = ContentApi(mockClient("<svg></svg>", "image/svg+xml"), baseUrl = "https://example.com/svg")

        val text = api.fetchSvgText("sunny-day.svg")

        assertEquals("<svg></svg>", text)
    }
}
