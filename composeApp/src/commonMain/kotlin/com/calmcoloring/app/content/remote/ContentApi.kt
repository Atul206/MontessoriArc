package com.calmcoloring.app.content.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

/**
 * Fetches OTA content from `$baseUrl/manifest.json` and `$baseUrl/<file>`.
 * See `docs/content-ota.md` for what's published there and how.
 */
class ContentApi(private val httpClient: HttpClient, private val baseUrl: String) {
    /**
     * Fetches the manifest as raw text and decodes it with
     * [parseContentManifestLenient] rather than a typed
     * `HttpResponse.body<ContentManifest>()` call — the latter decodes the
     * whole document atomically via the installed `ContentNegotiation` json
     * converter, so one malformed entry would throw before `removedIds`
     * (the rollback mechanism) is ever read.
     */
    suspend fun fetchManifest(): ContentManifest =
        parseContentManifestLenient(httpClient.get("$baseUrl/manifest.json").bodyAsText())

    suspend fun fetchSvgText(fileName: String): String = httpClient.get("$baseUrl/$fileName").bodyAsText()
}
