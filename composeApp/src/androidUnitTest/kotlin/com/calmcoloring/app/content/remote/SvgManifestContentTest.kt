package com.calmcoloring.app.content.remote

import com.calmcoloring.app.content.TemplateCatalog
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Exercises [parseSvgDocument] against the real, published `svg/manifest.json`
 * and every `svg/<file>.svg` it references — the final whole-branch review
 * found that no existing test ever did this, which is exactly how the
 * `<ellipse>` element (used by 4 of the 9 published templates) turned out to
 * be silently dropped by [parseSvgDocument]'s element regex, and how
 * `garden-flower.svg`'s `transform="rotate(...)"` petals went unhandled:
 * every other test uses hand-written SVG snippets that happen not to use
 * either feature.
 *
 * Lives in `androidUnitTest` (Robolectric) rather than `commonTest` because
 * it needs real `Path`/`PathMeasure` support to sample hit polygons via
 * [TemplateCatalog] for comparison — see [com.calmcoloring.app.content.TemplateCatalogTest]'s
 * doc comment for why that requires Robolectric in this module.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SvgManifestContentTest {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Finds the repo root by walking up from the JVM's working directory
     * until a directory containing `settings.gradle.kts` (this repo's own
     * root marker) is found, rather than assuming a fixed relative path from
     * the module directory — robust to however Gradle happens to invoke this
     * test's JVM.
     */
    private fun findRepoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")!!).absoluteFile
        var hops = 0
        while (dir != null && hops < 10) {
            if (File(dir, "settings.gradle.kts").isFile && File(dir, "svg/manifest.json").isFile) {
                return dir
            }
            dir = dir.parentFile
            hops++
        }
        fail("Could not locate repo root (directory containing settings.gradle.kts and svg/manifest.json) by walking up from ${System.getProperty("user.dir")}")
    }

    @Test
    fun everyManifestEntry_parsesToExactlyTheSameRegionsAsTheCompiledTemplate() {
        val repoRoot = findRepoRoot()
        val manifestFile = File(repoRoot, "svg/manifest.json")
        val manifest = json.decodeFromString(ContentManifest.serializer(), manifestFile.readText())

        assertEquals(9, manifest.templates.size, "sanity check: expected the 9 published launch templates")

        for (entry in manifest.templates) {
            val svgFile = File(repoRoot, "svg/${entry.file}")
            val document = parseSvgDocument(svgFile.readText())
            val compiled = TemplateCatalog.byId(entry.id)

            assertEquals(
                compiled.regions.map { it.id },
                document.regions.map { it.id },
                "region id list mismatch for '${entry.id}' (${entry.file})",
            )
            assertEquals(
                compiled.viewBoxWidth,
                document.viewBoxWidth,
                "viewBoxWidth mismatch for '${entry.id}'",
            )
            assertEquals(
                compiled.viewBoxHeight,
                document.viewBoxHeight,
                "viewBoxHeight mismatch for '${entry.id}'",
            )
        }
    }
}
