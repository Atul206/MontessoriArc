package com.calmcoloring.app.content.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Fetches OTA content from `$baseUrl/manifest.json` and `$baseUrl/<file>`.
 * See `docs/content-ota.md` for what's published there and how.
 */
class ContentApi(private val httpClient: HttpClient, private val baseUrl: String) {
    suspend fun fetchManifest(): ContentManifest = httpClient.get("$baseUrl/manifest.json").body()

    suspend fun fetchSvgText(fileName: String): String = httpClient.get("$baseUrl/$fileName").body()
}
