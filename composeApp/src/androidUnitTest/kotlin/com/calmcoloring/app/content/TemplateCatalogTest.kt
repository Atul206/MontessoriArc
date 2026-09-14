package com.calmcoloring.app.content

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Lives in `androidUnitTest` (not `commonTest`, as the plan's Step 3 literally
 * places it) and runs under Robolectric rather than plain JUnit.
 *
 * Why: this module's androidUnitTest runs on a stub android.jar with no real
 * graphics implementation — verified directly that even a bare
 * `Path().apply { moveTo(0f,0f); lineTo(10f,0f); close() }` throws
 * `RuntimeException: Method ... not mocked` there. Every region in
 * TemplateCatalog is built from real androidx.compose.ui.graphics.Path calls
 * (moveTo/lineTo/cubicTo/arcTo/addOval/addRoundRect) and sampled via
 * PathMeasure, so the test needs a real Path/PathMeasure/RectF
 * implementation to run at all. Robolectric's shadow classes provide that
 * (backed by java.awt.geom), which the stub jar alone does not — this is
 * the JVM-test-only counterpart of the [SunnyDayPaths]-doc'd svg-to-compose
 * fallback, addressing the *test execution* environment rather than the
 * Path-generation mechanism. The `@Config(sdk = [34])` pins a Robolectric
 * has shadows for, independent of this module's `compileSdk`.
 *
 * The assertions themselves are unchanged from the plan's Step 3 listing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TemplateCatalogTest {
    @Test
    fun everyTemplate_hasABackgroundRegionCoveringTheFullCanvas() {
        for (template in TemplateCatalog.all) {
            val background = template.regions.first()
            assertTrue(background.id == "background", "First region in ${template.id} must be the background")
        }
    }

    @Test
    fun everyRegion_hasANonEmptyHitPolygon() {
        for (template in TemplateCatalog.all) {
            for (region in template.regions) {
                assertTrue(region.hitPolygon.size >= 3, "${template.id}/${region.id} needs a polygon with 3+ points")
            }
        }
    }

    @Test
    fun catalog_hasElevenLaunchTemplates() {
        assertTrue(TemplateCatalog.all.size == 11)
    }
}
