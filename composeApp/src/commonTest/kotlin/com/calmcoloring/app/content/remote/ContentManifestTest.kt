package com.calmcoloring.app.content.remote

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentManifestTest {

    @Test
    fun decodesManifestJson() {
        val json = """
            {
              "templates": [
                { "id": "sunny-day", "name": "Sunny Day", "accent": "SAND", "file": "sunny-day.svg", "contentVersion": 1 }
              ]
            }
        """.trimIndent()

        val manifest = Json.decodeFromString<ContentManifest>(json)

        assertEquals(1, manifest.templates.size)
        val entry = manifest.templates.first()
        assertEquals("sunny-day", entry.id)
        assertEquals("Sunny Day", entry.name)
        assertEquals(AccentKey.SAND, entry.accent)
        assertEquals("sunny-day.svg", entry.file)
        assertEquals(1, entry.contentVersion)
        assertEquals(emptyList(), manifest.removedIds)
    }

    @Test
    fun decodesRemovedIds() {
        val json = """
            {
              "templates": [],
              "removedIds": ["bad-template"]
            }
        """.trimIndent()

        val manifest = Json.decodeFromString<ContentManifest>(json)

        assertEquals(listOf("bad-template"), manifest.removedIds)
    }
}
