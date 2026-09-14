# SVG OTA Content Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let new/updated coloring-page artwork be published to GitHub and picked up by the app at runtime — downloaded once, cached in a local database, and rendered from that cache — without an app-store release.

**Architecture:** The 8 launch templates stay exactly as they are today: hand-authored `svg/*.svg` files, hand-translated into compile-time Kotlin `Path` objects under `content/generated/`, wired through `TemplateCatalog`. This plan adds a *second*, parallel path for everything published after launch: a small runtime SVG-subset parser turns a `svg/*.svg` file's markup directly into a Compose `Path` at runtime (no compile step), a `manifest.json` in this repo tracks what's publishable and at what version, `ContentApi` (Ktor) fetches both over HTTPS, `TemplateCacheStore` (SQLDelight) persists the parsed result so it only has to be downloaded/parsed once, and `ContentRepository` merges the bundled 8 with whatever is cached — a remote id can even override a bundled one. The app calls `refresh()` once per launch; everything already cached renders immediately regardless of network state.

**Tech Stack:** Kotlin Multiplatform (Android + iOS) / Compose Multiplatform, Ktor client (networking), SQLDelight (local DB), kotlinx.serialization (manifest JSON), kotlinx.coroutines (StateFlow).

**Spec:** This plan itself is the spec — it was scoped directly from the existing codebase (see `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/`, `svg/*.svg`) in conversation; there is no separate spec document.

## Global Constraints

- Kotlin is pinned to `2.2.0` and Compose Multiplatform to `1.10.3` (`gradle/libs.versions.toml`) — every new dependency added by this plan must resolve against that pin. Task 1's own verification step (`./gradlew :composeApp:build`) is the check; if a pinned version below fails to resolve, drop to the nearest older stable release of that library before continuing, the same way this repo's `robolectric`/`androidx-navigation3-ui` entries document their own verification.
- Follow this repo's existing style: KDoc only to explain *why*, not *what*; hand-rolled helpers over new heavyweight dependencies where one already exists in-project (e.g. reuse `svgArcTo` rather than re-deriving arc math).
- New Kotlin files use the existing package root `com.calmcoloring.app`.
- Minimum Android SDK is 24, `compileSdk`/`targetSdk` 37 (`composeApp/build.gradle.kts`) — do not change these.
- This repo's GitHub remote is `https://github.com/Atul206/MontessoriArc` — the content base URL baked into Task 8 assumes that stays the origin.

---

## File Structure

New files this plan creates (all under `composeApp/src/`, unless noted):

- `commonMain/kotlin/com/calmcoloring/app/content/remote/SvgPathDataParser.kt` — runtime SVG `d`-attribute → Compose `Path` parser.
- `commonMain/kotlin/com/calmcoloring/app/content/remote/RemoteShape.kt` — the three shape kinds a remote region can be (`path`, `rect`, `circle`), plus text encode/decode for DB storage and `toPath()`.
- `commonMain/kotlin/com/calmcoloring/app/content/remote/SvgDocumentParser.kt` — parses a whole `svg/*.svg` document into `RemoteSvgDocument`.
- `commonMain/kotlin/com/calmcoloring/app/content/remote/ContentManifest.kt` — `manifest.json`'s data shape (`kotlinx.serialization`).
- `commonMain/kotlin/com/calmcoloring/app/content/remote/ContentApi.kt` — Ktor-backed manifest/SVG fetch.
- `commonMain/kotlin/com/calmcoloring/app/content/remote/HttpClientFactory.kt` — `expect fun createHttpClient()`.
- `androidMain/kotlin/com/calmcoloring/app/content/remote/HttpClientFactory.android.kt` / `iosMain/.../HttpClientFactory.ios.kt` — actuals (OkHttp / Darwin engines).
- `commonMain/sqldelight/com/calmcoloring/app/db/TemplateCache.sq` — SQLDelight schema + queries.
- `commonMain/kotlin/com/calmcoloring/app/db/DatabaseDriverFactory.kt` (expect) / `androidMain/.../DatabaseDriverFactory.android.kt` / `iosMain/.../DatabaseDriverFactory.ios.kt` (actuals).
- `commonMain/kotlin/com/calmcoloring/app/content/remote/TemplateCacheStore.kt` — DB read/write, mapped to `Template`/`RegionSpec`.
- `commonMain/kotlin/com/calmcoloring/app/content/remote/ContentRepository.kt` — orchestrates refresh + merge with bundled `TemplateCatalog.all`.
- `commonMain/kotlin/com/calmcoloring/app/content/remote/ContentRepositoryProvider.kt` — single app-wide instance.
- Matching test files under `androidUnitTest/kotlin/com/calmcoloring/app/content/remote/...` (Robolectric — see Task 2's note on why).
- `svg/manifest.json` — the OTA catalog.
- `docs/content-ota.md` — publishing convention for new artwork.

Modified files:
- `gradle/libs.versions.toml`, `build.gradle.kts`, `composeApp/build.gradle.kts` — new dependencies/plugins.
- `composeApp/src/androidMain/AndroidManifest.xml` — `INTERNET` permission.
- `composeApp/src/commonMain/kotlin/com/calmcoloring/app/App.kt` — reads templates from `ContentRepository` instead of the static `TemplateCatalog.all`.

---

### Task 1: Add OTA content dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `composeApp/build.gradle.kts`

**Interfaces:**
- Produces: the version-catalog aliases every later task's `build.gradle.kts` edits reference (`libs.kotlinx.serialization.json`, `libs.kotlinx.coroutines.core`, `libs.ktor.*`, `libs.sqldelight.*`).

- [ ] **Step 1: Add version + library + plugin entries to the catalog**

In `gradle/libs.versions.toml`, add to `[versions]` (after the existing `androidx-test-junit` line):

```toml
kotlinxSerialization = "1.7.3"
kotlinxCoroutines = "1.9.0"
ktor = "3.1.2"
sqldelight = "2.0.2"
```

Add to `[libraries]`:

```toml
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
```

Add to `[plugins]`:

```toml
kotlinxSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

- [ ] **Step 2: Register the new plugins at the root**

In `build.gradle.kts`, add two lines to the `plugins {}` block:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.sqldelight) apply false
}
```

- [ ] **Step 3: Apply the plugins and add dependencies in `composeApp/build.gradle.kts`**

Add to the `plugins {}` block at the top:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
}
```

Add a `sqldelight {}` block below the `plugins {}` block (top level, sibling of `kotlin {}`):

```kotlin
sqldelight {
    databases {
        create("TemplateCacheDatabase") {
            packageName.set("com.calmcoloring.app.db")
        }
    }
}
```

In the `kotlin { sourceSets { ... } }` block, extend `commonMain.dependencies` with:

```kotlin
implementation(libs.kotlinx.serialization.json)
implementation(libs.kotlinx.coroutines.core)
implementation(libs.ktor.client.core)
implementation(libs.ktor.client.content.negotiation)
implementation(libs.ktor.serialization.kotlinx.json)
implementation(libs.sqldelight.runtime)
```

Add an `iosMain.dependencies { ... }` block (none exists yet — add it as a sibling of `androidMain.dependencies`):

```kotlin
iosMain.dependencies {
    implementation(libs.ktor.client.darwin)
    implementation(libs.sqldelight.native.driver)
}
```

Extend `androidMain.dependencies` with:

```kotlin
implementation(libs.ktor.client.okhttp)
implementation(libs.sqldelight.android.driver)
```

Extend `androidUnitTest.dependencies` with:

```kotlin
implementation(libs.ktor.client.mock)
```

- [ ] **Step 4: Verify the new dependencies resolve and the project still builds**

Run: `./gradlew :composeApp:build`

Expected: `BUILD SUCCESSFUL`. If any artifact fails to resolve against Kotlin 2.2.0, drop that library's version in `gradle/libs.versions.toml` to its next older stable release and re-run.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts composeApp/build.gradle.kts
git commit -m "build: add Ktor, SQLDelight, and kotlinx.serialization for OTA content"
```

---

### Task 2: Runtime SVG path-data parser

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/SvgPathDataParser.kt`
- Test: `composeApp/src/androidUnitTest/kotlin/com/calmcoloring/app/content/remote/SvgPathDataParserTest.kt`

**Interfaces:**
- Consumes: `com.calmcoloring.app.content.generated.svgArcTo` (internal, same Gradle module — see `SvgPathUtils.kt`).
- Produces: `internal fun parseSvgPathData(d: String): androidx.compose.ui.graphics.Path`, used by Task 3's `RemoteShape.toPath()`.

This lives in `androidUnitTest`, not `commonTest`, for the same reason `SvgPathUtilsTest.kt` does: the project has no JVM/desktop target, so `commonTest` has no host that can execute real `Path`/`PathMeasure` graphics — only the Android target (via Robolectric's `NATIVE` graphics mode, already configured in `composeApp/build.gradle.kts`) can.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.calmcoloring.app.content.remote

import androidx.compose.ui.geometry.Offset
import com.calmcoloring.app.content.generated.SunnyDayPaths
import com.calmcoloring.app.geometry.toHitPolygon
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SvgPathDataParserTest {

    // Ground truth: SunnyDayPaths.hill is the hand-authored Kotlin Path built
    // directly from svg/sunny-day.svg's `hill` region (see that file's own
    // comment: d="M0,320 L0,235 Q160,175 320,235 L320,320 Z"). Parsing the
    // same d string at runtime must sample to the same hit polygon.
    @Test
    fun parsesLineAndQuadraticCommands_matchingHandAuthoredPath() {
        val parsed = parseSvgPathData("M0,320 L0,235 Q160,175 320,235 L320,320 Z")
        val expected = SunnyDayPaths.hill

        val parsedPolygon = parsed.toHitPolygon(samples = 48)
        val expectedPolygon = expected.toHitPolygon(samples = 48)

        assertEquals(expectedPolygon.size, parsedPolygon.size)
        parsedPolygon.zip(expectedPolygon).forEach { (actual, expectedPoint) ->
            assertTrue(
                (actual - expectedPoint).getDistance() < 0.5f,
                "expected ~$expectedPoint, got $actual",
            )
        }
    }

    // Ground truth: SunnyDayPaths.cloud, from the same file's `cloud` region
    // (d="M55,150 a30,30 0 0 1 58,-10 a26,26 0 0 1 44,8 a24,24 0 0 1 -6,46
    // h-80 a26,26 0 0 1 -16,-44 Z"). Exercises relative moveto, relative arc
    // (lowercase "a"), relative horizontal-line ("h"), and close.
    @Test
    fun parsesRelativeArcAndHorizontalLineCommands_matchingHandAuthoredPath() {
        val parsed = parseSvgPathData(
            "M55,150 a30,30 0 0 1 58,-10 a26,26 0 0 1 44,8 a24,24 0 0 1 -6,46 h-80 a26,26 0 0 1 -16,-44 Z",
        )
        val expected = SunnyDayPaths.cloud

        val parsedPolygon = parsed.toHitPolygon(samples = 96)
        val expectedPolygon = expected.toHitPolygon(samples = 96)

        assertEquals(expectedPolygon.size, parsedPolygon.size)
        var maxOffset = 0f
        parsedPolygon.zip(expectedPolygon).forEach { (actual, expectedPoint) ->
            maxOffset = maxOf(maxOffset, (actual - expectedPoint).getDistance())
        }
        assertTrue(maxOffset < 1.0f, "max sampled-point offset was $maxOffset")
    }

    private operator fun Offset.minus(other: Offset) = Offset(x - other.x, y - other.y)
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.calmcoloring.app.content.remote.SvgPathDataParserTest"`

Expected: FAIL — `parseSvgPathData` is unresolved (doesn't exist yet).

- [ ] **Step 3: Implement the parser**

```kotlin
package com.calmcoloring.app.content.remote

import androidx.compose.ui.graphics.Path
import com.calmcoloring.app.content.generated.svgArcTo

private val PATH_TOKEN = Regex("[MmLlHhVvQqCcAaZz]|-?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?")

/**
 * Parses an SVG `d` attribute into a Compose [Path], for the command subset
 * every `svg/*.svg` source in this repo actually uses: M/m, L/l, H/h, V/v,
 * Q/q, C/c, A/a, and Z/z — no shorthand S/T curves. Arc flags (`largeArc`,
 * `sweep`) must be whitespace-separated tokens (`"a30,30 0 0 1 ..."`, never
 * the digit-glued `"a30,30 0011"` form) — true of every hand-authored arc in
 * this project (see `docs/content-ota.md`), and this tokenizer relies on it.
 * The x-axis-rotation argument is parsed but always treated as 0, matching
 * every arc drawn so far; extend if a template ever needs a rotated arc.
 */
internal fun parseSvgPathData(d: String): Path {
    val tokens = PATH_TOKEN.findAll(d.trim()).map { it.value }.toList()
    val path = Path()
    var index = 0
    var command = ' '
    var cx = 0f
    var cy = 0f
    var startX = 0f
    var startY = 0f

    fun nextNumber(): Float = tokens[index++].toFloat()

    while (index < tokens.size) {
        val token = tokens[index]
        if (token.length == 1 && token[0].isLetter()) {
            command = token[0]
            index++
        }
        when (command) {
            'M' -> {
                cx = nextNumber(); cy = nextNumber()
                path.moveTo(cx, cy)
                startX = cx; startY = cy
                command = 'L'
            }
            'm' -> {
                cx += nextNumber(); cy += nextNumber()
                path.moveTo(cx, cy)
                startX = cx; startY = cy
                command = 'l'
            }
            'L' -> { cx = nextNumber(); cy = nextNumber(); path.lineTo(cx, cy) }
            'l' -> { cx += nextNumber(); cy += nextNumber(); path.lineTo(cx, cy) }
            'H' -> { cx = nextNumber(); path.lineTo(cx, cy) }
            'h' -> { cx += nextNumber(); path.lineTo(cx, cy) }
            'V' -> { cy = nextNumber(); path.lineTo(cx, cy) }
            'v' -> { cy += nextNumber(); path.lineTo(cx, cy) }
            'Q' -> {
                val x1 = nextNumber(); val y1 = nextNumber()
                cx = nextNumber(); cy = nextNumber()
                path.quadraticTo(x1, y1, cx, cy)
            }
            'q' -> {
                val x1 = cx + nextNumber(); val y1 = cy + nextNumber()
                val ex = cx + nextNumber(); val ey = cy + nextNumber()
                path.quadraticTo(x1, y1, ex, ey)
                cx = ex; cy = ey
            }
            'C' -> {
                val x1 = nextNumber(); val y1 = nextNumber()
                val x2 = nextNumber(); val y2 = nextNumber()
                cx = nextNumber(); cy = nextNumber()
                path.cubicTo(x1, y1, x2, y2, cx, cy)
            }
            'c' -> {
                val x1 = cx + nextNumber(); val y1 = cy + nextNumber()
                val x2 = cx + nextNumber(); val y2 = cy + nextNumber()
                val ex = cx + nextNumber(); val ey = cy + nextNumber()
                path.cubicTo(x1, y1, x2, y2, ex, ey)
                cx = ex; cy = ey
            }
            'A' -> {
                val rx = nextNumber(); val ry = nextNumber()
                nextNumber() // x-axis-rotation, unused
                val largeArc = nextNumber() != 0f
                val sweep = nextNumber() != 0f
                val ex = nextNumber(); val ey = nextNumber()
                path.svgArcTo(cx, cy, rx, ry, largeArc, sweep, ex, ey)
                cx = ex; cy = ey
            }
            'a' -> {
                val rx = nextNumber(); val ry = nextNumber()
                nextNumber()
                val largeArc = nextNumber() != 0f
                val sweep = nextNumber() != 0f
                val ex = cx + nextNumber(); val ey = cy + nextNumber()
                path.svgArcTo(cx, cy, rx, ry, largeArc, sweep, ex, ey)
                cx = ex; cy = ey
            }
            'Z', 'z' -> {
                path.close()
                cx = startX; cy = startY
            }
            else -> error("Unsupported SVG path command '$command' in: $d")
        }
    }
    return path
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.calmcoloring.app.content.remote.SvgPathDataParserTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/SvgPathDataParser.kt composeApp/src/androidUnitTest/kotlin/com/calmcoloring/app/content/remote/SvgPathDataParserTest.kt
git commit -m "feat: add runtime SVG path-data parser for OTA content"
```

---

### Task 3: Remote shape model + whole-document SVG parser

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/RemoteShape.kt`
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/SvgDocumentParser.kt`
- Test: `composeApp/src/androidUnitTest/kotlin/com/calmcoloring/app/content/remote/SvgDocumentParserTest.kt`

**Interfaces:**
- Consumes: `parseSvgPathData` (Task 2).
- Produces: `data class RemoteRegion(val id: String, val shape: RemoteShape)`, `sealed interface RemoteShape` with `fun encode(): String` / `fun decodeShape(encoded: String): RemoteShape` / `fun RemoteShape.toPath(): Path` (used by Task 6's `TemplateCacheStore`), and `fun parseSvgDocument(svgText: String): RemoteSvgDocument` where `RemoteSvgDocument(val viewBoxWidth: Float, val viewBoxHeight: Float, val regions: List<RemoteRegion>)` (used by Task 7's `ContentRepository`).

- [ ] **Step 1: Write `RemoteShape.kt` (no test — pure data type + two inverse functions, exercised end-to-end by Step 2 below)**

```kotlin
package com.calmcoloring.app.content.remote

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path

/**
 * One fillable region parsed from a remote `.svg` document. Kept as a shape
 * *descriptor* rather than a [Path] itself so [TemplateCacheStore] can store
 * it as a single TEXT column ([encode]/[decodeShape]) and only materialize a
 * real [Path] ([toPath]) when a [com.calmcoloring.app.model.Template] is
 * actually built for rendering.
 */
data class RemoteRegion(val id: String, val shape: RemoteShape)

sealed interface RemoteShape {
    data class PathData(val d: String) : RemoteShape
    data class RoundRect(val x: Float, val y: Float, val width: Float, val height: Float, val rx: Float) : RemoteShape
    data class Circle(val cx: Float, val cy: Float, val r: Float) : RemoteShape
}

/** Inverse of [decodeShape]. */
fun RemoteShape.encode(): String = when (this) {
    is RemoteShape.PathData -> "d:$d"
    is RemoteShape.RoundRect -> "rect:$x,$y,$width,$height,$rx"
    is RemoteShape.Circle -> "circle:$cx,$cy,$r"
}

/** Inverse of [encode]. */
fun decodeShape(encoded: String): RemoteShape {
    val kind = encoded.substringBefore(':')
    val rest = encoded.substringAfter(':')
    return when (kind) {
        "d" -> RemoteShape.PathData(rest)
        "rect" -> rest.split(",").map { it.toFloat() }.let { (x, y, w, h, rx) -> RemoteShape.RoundRect(x, y, w, h, rx) }
        "circle" -> rest.split(",").map { it.toFloat() }.let { (cx, cy, r) -> RemoteShape.Circle(cx, cy, r) }
        else -> error("Unknown encoded shape kind '$kind' in: $encoded")
    }
}

/** Uses the same builder calls [com.calmcoloring.app.content.generated]'s
 *  `backgroundPath()`/`SunnyDayPaths.sun` already rely on for rects/circles,
 *  and [parseSvgPathData] (Task 2) for raw path data. */
fun RemoteShape.toPath(): Path = when (this) {
    is RemoteShape.PathData -> parseSvgPathData(d)
    is RemoteShape.RoundRect -> Path().apply {
        addRoundRect(RoundRect(left = x, top = y, right = x + width, bottom = y + height, radiusX = rx, radiusY = rx))
    }
    is RemoteShape.Circle -> Path().apply { addOval(Rect(center = Offset(cx, cy), radius = r)) }
}
```

- [ ] **Step 2: Write the failing test for `parseSvgDocument`**

```kotlin
package com.calmcoloring.app.content.remote

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SvgDocumentParserTest {

    // The exact contents of svg/sunny-day.svg — the simplest of the 8
    // existing templates, and one that exercises all three element kinds
    // (rect, path, circle).
    private val sunnyDaySvg = """
        <svg viewBox="0 0 320 320" xmlns="http://www.w3.org/2000/svg">
          <rect id="background" x="1.5" y="1.5" width="317" height="317" rx="18"/>
          <path id="hill" d="M0,320 L0,235 Q160,175 320,235 L320,320 Z"/>
          <path id="cloud" d="M55,150 a30,30 0 0 1 58,-10 a26,26 0 0 1 44,8 a24,24 0 0 1 -6,46 h-80 a26,26 0 0 1 -16,-44 Z"/>
          <circle id="sun" cx="228" cy="92" r="48"/>
        </svg>
    """.trimIndent()

    @Test
    fun parsesViewBoxAndAllElementsInDocumentOrder() {
        val document = parseSvgDocument(sunnyDaySvg)

        assertEquals(320f, document.viewBoxWidth)
        assertEquals(320f, document.viewBoxHeight)
        assertEquals(listOf("background", "hill", "cloud", "sun"), document.regions.map { it.id })

        assertEquals(RemoteShape.RoundRect(1.5f, 1.5f, 317f, 317f, 18f), document.regions[0].shape)
        assertEquals(
            RemoteShape.PathData("M0,320 L0,235 Q160,175 320,235 L320,320 Z"),
            document.regions[1].shape,
        )
        assertEquals(RemoteShape.Circle(228f, 92f, 48f), document.regions[3].shape)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.calmcoloring.app.content.remote.SvgDocumentParserTest"`

Expected: FAIL — `parseSvgDocument` is unresolved.

- [ ] **Step 4: Implement `SvgDocumentParser.kt`**

```kotlin
package com.calmcoloring.app.content.remote

private val VIEWBOX_REGEX = Regex("viewBox=\"[-0-9.]+\\s+[-0-9.]+\\s+([-0-9.]+)\\s+([-0-9.]+)\"")
private val ELEMENT_REGEX = Regex("<(rect|circle|path)\\b([^>]*)/?>")
private val ATTR_REGEX = Regex("([\\w:-]+)=\"([^\"]*)\"")

data class RemoteSvgDocument(
    val viewBoxWidth: Float,
    val viewBoxHeight: Float,
    val regions: List<RemoteRegion>,
)

/**
 * Parses one of this repo's `svg/*.svg` template sources — a flat SVG
 * document with a single `viewBox` and top-level `<rect>`/`<circle>`/
 * `<path>` children, each carrying an `id` — into a [RemoteSvgDocument].
 * This is *not* a general SVG parser: no groups, no `transform`, no style
 * attributes. See `docs/content-ota.md` for the authoring convention this
 * matches.
 */
fun parseSvgDocument(svgText: String): RemoteSvgDocument {
    val viewBoxMatch = VIEWBOX_REGEX.find(svgText) ?: error("No viewBox attribute found in SVG document")
    val width = viewBoxMatch.groupValues[1].toFloat()
    val height = viewBoxMatch.groupValues[2].toFloat()

    val regions = ELEMENT_REGEX.findAll(svgText).map { element ->
        val tag = element.groupValues[1]
        val attrs = ATTR_REGEX.findAll(element.groupValues[2])
            .associate { it.groupValues[1] to it.groupValues[2] }
        val id = attrs["id"] ?: error("<$tag> element missing id attribute: ${element.value}")
        val shape: RemoteShape = when (tag) {
            "rect" -> RemoteShape.RoundRect(
                x = attrs.getValue("x").toFloat(),
                y = attrs.getValue("y").toFloat(),
                width = attrs.getValue("width").toFloat(),
                height = attrs.getValue("height").toFloat(),
                rx = attrs["rx"]?.toFloat() ?: 0f,
            )
            "circle" -> RemoteShape.Circle(
                cx = attrs.getValue("cx").toFloat(),
                cy = attrs.getValue("cy").toFloat(),
                r = attrs.getValue("r").toFloat(),
            )
            "path" -> RemoteShape.PathData(attrs.getValue("d"))
            else -> error("Unsupported element <$tag>")
        }
        RemoteRegion(id, shape)
    }.toList()

    return RemoteSvgDocument(width, height, regions)
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.calmcoloring.app.content.remote.SvgDocumentParserTest"`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/RemoteShape.kt composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/SvgDocumentParser.kt composeApp/src/androidUnitTest/kotlin/com/calmcoloring/app/content/remote/SvgDocumentParserTest.kt
git commit -m "feat: parse whole SVG documents into remote region descriptors"
```

---

### Task 4: Content manifest model

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/ContentManifest.kt`
- Test: `composeApp/src/commonTest/kotlin/com/calmcoloring/app/content/remote/ContentManifestTest.kt`

This is pure `kotlinx.serialization` data-class decoding — no Compose graphics involved — so it's safe in `commonTest` (unlike Tasks 2–3).

**Interfaces:**
- Produces: `@Serializable data class ContentManifest(val templates: List<ManifestEntry>, val removedIds: List<String> = emptyList())`, `@Serializable data class ManifestEntry(val id: String, val name: String, val accent: AccentKey, val file: String, val contentVersion: Int)`, `@Serializable enum class AccentKey { SAGE, SKY, CLAY, SAND, LILAC, MOSS }` — consumed by Task 5 (`ContentApi`), Task 6 (`TemplateCacheStore.replaceTemplate`'s `accent` param and the new `deleteTemplate`), and Task 7 (`ContentRepository`).

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :composeApp:allTests --tests "com.calmcoloring.app.content.remote.ContentManifestTest"`

Expected: FAIL — `ContentManifest` is unresolved.

- [ ] **Step 3: Implement `ContentManifest.kt`**

```kotlin
package com.calmcoloring.app.content.remote

import kotlinx.serialization.Serializable

/**
 * The OTA content catalog served from `svg/manifest.json` in this repo (see
 * `docs/content-ota.md`). One entry per publishable template.
 * `contentVersion` is bumped by hand whenever `file`'s content changes, so
 * [ContentRepository] can tell "already have the latest" apart from "needs a
 * re-download" without hashing file bodies. [removedIds] is the rollback
 * mechanism for content that should be deleted outright (not fixed-in-place):
 * an id listed here gets purged from every device's local cache on next
 * refresh, rather than silently lingering forever just because its entry was
 * dropped from `templates`.
 */
@Serializable
data class ContentManifest(
    val templates: List<ManifestEntry>,
    val removedIds: List<String> = emptyList(),
)

@Serializable
data class ManifestEntry(
    val id: String,
    val name: String,
    val accent: AccentKey,
    val file: String,
    val contentVersion: Int,
)

/** Mirrors the fixed 6-color system palette in [com.calmcoloring.app.theme.CalmPalette] —
 *  remote content picks one of these rather than an arbitrary hex color, so
 *  OTA templates stay on-theme (and dark-mode-aware, once that's wired). */
@Serializable
enum class AccentKey { SAGE, SKY, CLAY, SAND, LILAC, MOSS }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :composeApp:allTests --tests "com.calmcoloring.app.content.remote.ContentManifestTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/ContentManifest.kt composeApp/src/commonTest/kotlin/com/calmcoloring/app/content/remote/ContentManifestTest.kt
git commit -m "feat: add OTA content manifest data model"
```

---

### Task 5: Content API (Ktor fetch)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/ContentApi.kt`
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/HttpClientFactory.kt`
- Create: `composeApp/src/androidMain/kotlin/com/calmcoloring/app/content/remote/HttpClientFactory.android.kt`
- Create: `composeApp/src/iosMain/kotlin/com/calmcoloring/app/content/remote/HttpClientFactory.ios.kt`
- Test: `composeApp/src/commonTest/kotlin/com/calmcoloring/app/content/remote/ContentApiTest.kt`

**Interfaces:**
- Consumes: `ContentManifest` (Task 4).
- Produces: `class ContentApi(httpClient: HttpClient, baseUrl: String) { suspend fun fetchManifest(): ContentManifest; suspend fun fetchSvgText(fileName: String): String }` (consumed by Task 7's `ContentRepository`), `expect fun createHttpClient(): HttpClient` (consumed by Task 8's `ContentRepositoryProvider`).

- [ ] **Step 1: Write the failing test (using `ktor-client-mock`, no platform engine needed — safe in `commonTest`)**

```kotlin
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
```

- [ ] **Step 2: Add the `kotlinx-coroutines-test` dependency this test needs**

Add to `gradle/libs.versions.toml` `[libraries]`:

```toml
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
```

Add to `composeApp/build.gradle.kts`'s `commonTest.dependencies`:

```kotlin
implementation(libs.kotlinx.coroutines.test)
implementation(libs.ktor.client.mock)
```

(`ktor-client-mock` was added to `androidUnitTest` only in Task 1 — Task 7's repository test needs it there too, but this task's `ContentApiTest` runs in `commonTest`, so it needs its own `commonTest` dependency here.)

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :composeApp:allTests --tests "com.calmcoloring.app.content.remote.ContentApiTest"`

Expected: FAIL — `ContentApi` is unresolved.

- [ ] **Step 4: Implement `ContentApi.kt`**

```kotlin
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
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :composeApp:allTests --tests "com.calmcoloring.app.content.remote.ContentApiTest"`

Expected: PASS.

- [ ] **Step 6: Add the platform HTTP client factory (no test — a one-line engine choice per platform, exercised end-to-end by Task 8's manual verification)**

`composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/HttpClientFactory.kt`:

```kotlin
package com.calmcoloring.app.content.remote

import io.ktor.client.HttpClient

/** Platform HTTP engine for [ContentApi] — OkHttp on Android, Darwin (NSURLSession) on iOS. */
expect fun createHttpClient(): HttpClient
```

`composeApp/src/androidMain/kotlin/com/calmcoloring/app/content/remote/HttpClientFactory.android.kt`:

```kotlin
package com.calmcoloring.app.content.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
}
```

`composeApp/src/iosMain/kotlin/com/calmcoloring/app/content/remote/HttpClientFactory.ios.kt`:

```kotlin
package com.calmcoloring.app.content.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
}
```

- [ ] **Step 7: Verify the whole module still compiles for both targets**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/ContentApi.kt composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/HttpClientFactory.kt composeApp/src/androidMain/kotlin/com/calmcoloring/app/content/remote/HttpClientFactory.android.kt composeApp/src/iosMain/kotlin/com/calmcoloring/app/content/remote/HttpClientFactory.ios.kt composeApp/src/commonTest/kotlin/com/calmcoloring/app/content/remote/ContentApiTest.kt
git commit -m "feat: fetch OTA manifest and SVG content over HTTP"
```

---

### Task 6: Local cache (SQLDelight)

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/calmcoloring/app/db/TemplateCache.sq`
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/db/DatabaseDriverFactory.kt`
- Create: `composeApp/src/androidMain/kotlin/com/calmcoloring/app/db/DatabaseDriverFactory.android.kt`
- Create: `composeApp/src/iosMain/kotlin/com/calmcoloring/app/db/DatabaseDriverFactory.ios.kt`
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/TemplateCacheStore.kt`
- Test: `composeApp/src/androidUnitTest/kotlin/com/calmcoloring/app/content/remote/TemplateCacheStoreTest.kt`

**Interfaces:**
- Consumes: `RemoteRegion`/`RemoteShape` (Task 3), `AccentKey` (Task 4).
- Produces: `class TemplateCacheStore(database: TemplateCacheDatabase) { fun cachedContentVersion(id: String): Int?; fun replaceTemplate(id: String, name: String, accent: AccentKey, viewBoxWidth: Float, viewBoxHeight: Float, contentVersion: Int, regions: List<RemoteRegion>); fun deleteTemplate(id: String); fun allTemplates(): List<Template> }` (consumed by Task 7's `ContentRepository`), `expect class DatabaseDriverFactory { fun createDriver(): SqlDriver }` (consumed by Task 8).

- [ ] **Step 1: Write the schema**

```sql
-- composeApp/src/commonMain/sqldelight/com/calmcoloring/app/db/TemplateCache.sq

CREATE TABLE CachedTemplate (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    accent TEXT NOT NULL,
    viewBoxWidth REAL NOT NULL,
    viewBoxHeight REAL NOT NULL,
    contentVersion INTEGER NOT NULL
);

CREATE TABLE CachedRegion (
    templateId TEXT NOT NULL,
    orderIndex INTEGER NOT NULL,
    regionId TEXT NOT NULL,
    encodedShape TEXT NOT NULL,
    PRIMARY KEY (templateId, orderIndex)
);

selectTemplate:
SELECT * FROM CachedTemplate WHERE id = ?;

selectAllTemplates:
SELECT * FROM CachedTemplate;

selectRegions:
SELECT regionId, encodedShape FROM CachedRegion WHERE templateId = ? ORDER BY orderIndex;

upsertTemplate:
INSERT OR REPLACE INTO CachedTemplate(id, name, accent, viewBoxWidth, viewBoxHeight, contentVersion)
VALUES (?, ?, ?, ?, ?, ?);

deleteRegions:
DELETE FROM CachedRegion WHERE templateId = ?;

insertRegion:
INSERT INTO CachedRegion(templateId, orderIndex, regionId, encodedShape)
VALUES (?, ?, ?, ?);

deleteTemplate:
DELETE FROM CachedTemplate WHERE id = ?;
```

`CachedRegion` rows have no foreign-key cascade (SQLDelight/SQLite here isn't configured with `PRAGMA foreign_keys=ON`), so deleting a template's regions is still `deleteRegions` — `TemplateCacheStore.deleteTemplate` (Step 6 below) calls both queries in one transaction.

- [ ] **Step 2: Add the expect/actual driver factory**

`composeApp/src/commonMain/kotlin/com/calmcoloring/app/db/DatabaseDriverFactory.kt`:

```kotlin
package com.calmcoloring.app.db

import app.cash.sqldelight.db.SqlDriver

/** Platform SQLite driver for [TemplateCacheDatabase] — real file-backed
 *  storage on both platforms; tests build their own in-memory driver
 *  directly (see `TemplateCacheStoreTest`) rather than through this. */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
```

`composeApp/src/androidMain/kotlin/com/calmcoloring/app/db/DatabaseDriverFactory.android.kt`:

```kotlin
package com.calmcoloring.app.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.calmcoloring.app.platform.appContext

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(TemplateCacheDatabase.Schema, appContext, "template-cache.db")
}
```

`composeApp/src/iosMain/kotlin/com/calmcoloring/app/db/DatabaseDriverFactory.ios.kt`:

```kotlin
package com.calmcoloring.app.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(TemplateCacheDatabase.Schema, "template-cache.db")
}
```

- [ ] **Step 3: Run codegen and verify it compiles**

Run: `./gradlew :composeApp:generateCommonMainTemplateCacheDatabaseInterface :composeApp:compileDebugKotlinAndroid`

Expected: `BUILD SUCCESSFUL` — confirms `TemplateCacheDatabase`/`TemplateCacheDatabase.Schema` were generated in package `com.calmcoloring.app.db` (from the `sqldelight { databases { create("TemplateCacheDatabase") { packageName.set(...) } } }` block added in Task 1).

- [ ] **Step 4: Write the failing test for `TemplateCacheStore`**

```kotlin
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
```

- [ ] **Step 5: Run the test to verify it fails**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.calmcoloring.app.content.remote.TemplateCacheStoreTest"`

Expected: FAIL — `TemplateCacheStore` is unresolved.

- [ ] **Step 6: Implement `TemplateCacheStore.kt`**

```kotlin
package com.calmcoloring.app.content.remote

import androidx.compose.ui.graphics.Path
import com.calmcoloring.app.db.TemplateCacheDatabase
import com.calmcoloring.app.geometry.toHitPolygon
import com.calmcoloring.app.model.RegionSpec
import com.calmcoloring.app.model.Template
import com.calmcoloring.app.theme.CalmPalette

/**
 * Wraps the SQLDelight-generated queries with the domain-shaped operations
 * [ContentRepository] needs. Regions are stored as [RemoteShape.encode]
 * strings and rebuilt into Compose [Path]s (and hit polygons) on every read
 * — cheap for this app's handful of simple-shape templates, and avoids
 * trying to store a platform [Path] object in a database row.
 */
class TemplateCacheStore(private val database: TemplateCacheDatabase) {
    private val queries = database.templateCacheQueries

    fun cachedContentVersion(id: String): Int? =
        queries.selectTemplate(id).executeAsOneOrNull()?.contentVersion?.toInt()

    fun replaceTemplate(
        id: String,
        name: String,
        accent: AccentKey,
        viewBoxWidth: Float,
        viewBoxHeight: Float,
        contentVersion: Int,
        regions: List<RemoteRegion>,
    ) {
        queries.transaction {
            queries.upsertTemplate(id, name, accent.name, viewBoxWidth.toDouble(), viewBoxHeight.toDouble(), contentVersion.toLong())
            queries.deleteRegions(id)
            regions.forEachIndexed { index, region ->
                queries.insertRegion(id, index.toLong(), region.id, region.shape.encode())
            }
        }
    }

    /** Rollback/removal path (see `docs/content-ota.md`'s "removing content"
     *  section): purges a template this device previously cached, in
     *  response to the manifest listing its id under `removedIds`. */
    fun deleteTemplate(id: String) {
        queries.transaction {
            queries.deleteRegions(id)
            queries.deleteTemplate(id)
        }
    }

    fun allTemplates(): List<Template> =
        queries.selectAllTemplates().executeAsList().map { row ->
            val regions = queries.selectRegions(row.id).executeAsList().map { regionRow ->
                val path: Path = decodeShape(regionRow.encodedShape).toPath()
                RegionSpec(regionRow.regionId, path, path.toHitPolygon())
            }
            Template(
                id = row.id,
                name = row.name,
                accent = AccentKey.valueOf(row.accent).toColor(),
                viewBoxWidth = row.viewBoxWidth.toFloat(),
                viewBoxHeight = row.viewBoxHeight.toFloat(),
                regions = regions,
            )
        }
}

private fun AccentKey.toColor() = when (this) {
    AccentKey.SAGE -> CalmPalette.SageLight
    AccentKey.SKY -> CalmPalette.SkyLight
    AccentKey.CLAY -> CalmPalette.ClayLight
    AccentKey.SAND -> CalmPalette.SandLight
    AccentKey.LILAC -> CalmPalette.LilacLight
    AccentKey.MOSS -> CalmPalette.MossLight
}
```

- [ ] **Step 7: Run the test to verify it passes**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.calmcoloring.app.content.remote.TemplateCacheStoreTest"`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/sqldelight composeApp/src/commonMain/kotlin/com/calmcoloring/app/db composeApp/src/androidMain/kotlin/com/calmcoloring/app/db composeApp/src/iosMain/kotlin/com/calmcoloring/app/db composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/TemplateCacheStore.kt composeApp/src/androidUnitTest/kotlin/com/calmcoloring/app/content/remote/TemplateCacheStoreTest.kt
git commit -m "feat: cache OTA content locally with SQLDelight"
```

---

### Task 7: Content repository (refresh + merge)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/ContentRepository.kt`
- Test: `composeApp/src/androidUnitTest/kotlin/com/calmcoloring/app/content/remote/ContentRepositoryTest.kt`

This test needs both a real `TemplateCacheStore` (Robolectric-backed SQLite, per Task 6) and a mocked `ContentApi`, so it lives in `androidUnitTest` alongside `TemplateCacheStoreTest`.

**Interfaces:**
- Consumes: `ContentApi` (Task 5), `TemplateCacheStore` (Task 6), `TemplateCatalog.all` (existing, `com.calmcoloring.app.content.TemplateCatalog`).
- Produces: `class ContentRepository(api: ContentApi, cache: TemplateCacheStore) { val templates: StateFlow<List<Template>>; suspend fun refresh() }` — consumed by Task 8's `ContentRepositoryProvider` and `App.kt`.

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.calmcoloring.app.content.remote.ContentRepositoryTest"`

Expected: FAIL — `ContentRepository` is unresolved.

- [ ] **Step 3: Implement `ContentRepository.kt`**

```kotlin
package com.calmcoloring.app.content.remote

import com.calmcoloring.app.content.TemplateCatalog
import com.calmcoloring.app.model.Template
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Merges the 8 bundled launch templates ([TemplateCatalog.all], compiled
 * into the app binary) with whatever OTA content [TemplateCacheStore] has
 * cached. A remote id that collides with a bundled id overrides the bundled
 * entry — lets a bundled template's art be corrected post-launch without an
 * app-store release. Deleting a cached override this way (see [refresh]'s
 * `removedIds` handling) naturally falls back to the bundled version rather
 * than removing the id entirely, since [mergeTemplates] only excludes a
 * bundled entry when a *cached* one still exists for that id.
 */
class ContentRepository(
    private val api: ContentApi,
    private val cache: TemplateCacheStore,
) {
    private val _templates = MutableStateFlow(mergeTemplates(cache.allTemplates()))
    val templates: StateFlow<List<Template>> = _templates

    /**
     * Fetches `manifest.json`, downloads and caches any entry whose
     * `contentVersion` differs from what's cached, purges any id listed in
     * `removedIds` (the rollback path for content that should be deleted
     * outright — see `docs/content-ota.md`), and republishes [templates].
     * Safe to call repeatedly (e.g. on every app launch) — network or parse
     * failures are swallowed per-entry (or for the whole manifest fetch) so
     * a bad or offline refresh never breaks the templates already on
     * screen.
     */
    suspend fun refresh() {
        val manifest = try {
            api.fetchManifest()
        } catch (e: Exception) {
            return
        }
        var changed = false
        for (entry in manifest.templates) {
            if (cache.cachedContentVersion(entry.id) == entry.contentVersion) continue
            try {
                val svgText = api.fetchSvgText(entry.file)
                val document = parseSvgDocument(svgText)
                cache.replaceTemplate(
                    id = entry.id,
                    name = entry.name,
                    accent = entry.accent,
                    viewBoxWidth = document.viewBoxWidth,
                    viewBoxHeight = document.viewBoxHeight,
                    contentVersion = entry.contentVersion,
                    regions = document.regions,
                )
                changed = true
            } catch (e: Exception) {
                continue
            }
        }
        for (id in manifest.removedIds) {
            if (cache.cachedContentVersion(id) == null) continue
            cache.deleteTemplate(id)
            changed = true
        }
        if (changed) _templates.update { mergeTemplates(cache.allTemplates()) }
    }

    private fun mergeTemplates(cached: List<Template>): List<Template> {
        val cachedIds = cached.map { it.id }.toSet()
        return TemplateCatalog.all.filterNot { it.id in cachedIds } + cached
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.calmcoloring.app.content.remote.ContentRepositoryTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/ContentRepository.kt composeApp/src/androidUnitTest/kotlin/com/calmcoloring/app/content/remote/ContentRepositoryTest.kt
git commit -m "feat: merge OTA content with the bundled template catalog"
```

---

### Task 8: Wire into the app

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/ContentRepositoryProvider.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/App.kt`
- Modify: `composeApp/src/androidMain/AndroidManifest.xml`

**Interfaces:**
- Consumes: `ContentRepository`, `ContentApi`, `createHttpClient` (Task 5/7), `DatabaseDriverFactory`, `TemplateCacheDatabase` (Task 6).
- Produces: `object ContentRepositoryProvider { val repository: ContentRepository }` — the single app-wide instance `App.kt` reads from.

- [ ] **Step 1: Add `ContentRepositoryProvider.kt`**

```kotlin
package com.calmcoloring.app.content.remote

import com.calmcoloring.app.db.DatabaseDriverFactory
import com.calmcoloring.app.db.TemplateCacheDatabase

// This repo's raw-content root. Publishing convention (adding/bumping
// entries in svg/manifest.json, adding new svg/*.svg files) is documented
// in docs/content-ota.md.
private const val CONTENT_BASE_URL = "https://raw.githubusercontent.com/Atul206/MontessoriArc/main/svg"

/**
 * Lazily builds the single app-wide [ContentRepository], wiring together the
 * platform [DatabaseDriverFactory] and [createHttpClient] (both
 * expect/actual). `App.kt` reads [repository] once and calls `refresh()`
 * from a `LaunchedEffect` on launch.
 */
object ContentRepositoryProvider {
    val repository: ContentRepository by lazy {
        val database = TemplateCacheDatabase(DatabaseDriverFactory().createDriver())
        ContentRepository(
            api = ContentApi(createHttpClient(), CONTENT_BASE_URL),
            cache = TemplateCacheStore(database),
        )
    }
}
```

- [ ] **Step 2: Wire `App.kt` to read from the repository**

In `composeApp/src/commonMain/kotlin/com/calmcoloring/app/App.kt`, replace the `TemplateCatalog` import and usage:

```kotlin
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.calmcoloring.app.content.remote.ContentRepositoryProvider
```

(remove `import com.calmcoloring.app.content.TemplateCatalog`)

Inside `CalmColoringApp()`, before the `when (val current = backStack.last())` block:

```kotlin
val repository = remember { ContentRepositoryProvider.repository }
val templates by repository.templates.collectAsState()
LaunchedEffect(Unit) { repository.refresh() }
```

Change the `Route.Gallery` branch to pass `templates` instead of `TemplateCatalog.all`:

```kotlin
is Route.Gallery -> GalleryScreen(
    templates = templates,
    onTemplateSelected = { id -> backStack.add(Route.Coloring(id)) },
)
```

Change the `Route.Coloring` branch's lookup (was `TemplateCatalog.byId(current.templateId)`):

```kotlin
is Route.Coloring -> {
    val template = remember(current.templateId, templates) {
        templates.first { it.id == current.templateId }
    }
    ...
```

- [ ] **Step 3: Add the `INTERNET` permission**

In `composeApp/src/androidMain/AndroidManifest.xml`, add before the `<application>` tag:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

- [ ] **Step 4: Verify the app builds for both platforms**

Run: `./gradlew :composeApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Manually verify on Android**

Run: `./gradlew :composeApp:installDebug`, launch the app, and confirm the gallery still shows all 8 templates (now served through `ContentRepository` rather than the static catalog) and that `adb logcat` shows no crash from the `LaunchedEffect(Unit) { repository.refresh() }` call (a network failure — e.g. no connectivity — should be silently swallowed per `ContentRepository.refresh()`'s try/catch, not crash the app).

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/ContentRepositoryProvider.kt composeApp/src/commonMain/kotlin/com/calmcoloring/app/App.kt composeApp/src/androidMain/AndroidManifest.xml
git commit -m "feat: wire OTA content repository into the app"
```

---

### Task 9: Publish the manifest and document the workflow

**Files:**
- Create: `svg/manifest.json`
- Create: `docs/content-ota.md`

**Interfaces:**
- Consumes: nothing (data + docs only).
- Produces: the actual content `ContentRepository.refresh()` fetches at `https://raw.githubusercontent.com/Atul206/MontessoriArc/main/svg/manifest.json` once this is pushed to `main`.

- [ ] **Step 1: Write `svg/manifest.json`, one entry per existing template**

```json
{
  "templates": [
    { "id": "sunny-day", "name": "Sunny Day", "accent": "SAND", "file": "sunny-day.svg", "contentVersion": 1 },
    { "id": "little-house", "name": "Little House", "accent": "CLAY", "file": "little-house.svg", "contentVersion": 1 },
    { "id": "apple-tree", "name": "Apple Tree", "accent": "MOSS", "file": "apple-tree.svg", "contentVersion": 1 },
    { "id": "sleepy-cat", "name": "Sleepy Cat", "accent": "LILAC", "file": "sleepy-cat.svg", "contentVersion": 1 },
    { "id": "little-fish", "name": "Little Fish", "accent": "SKY", "file": "little-fish.svg", "contentVersion": 1 },
    { "id": "garden-flower", "name": "Garden Flower", "accent": "SAGE", "file": "garden-flower.svg", "contentVersion": 1 },
    { "id": "sailboat", "name": "Sailboat", "accent": "CLAY", "file": "sailboat.svg", "contentVersion": 1 },
    { "id": "balloon-ride", "name": "Balloon Ride", "accent": "SAND", "file": "balloon-ride.svg", "contentVersion": 1 }
  ],
  "removedIds": []
}
```

(`accent` values match the `CalmPalette` colors each template already uses in `TemplateCatalog.kt` — e.g. `sunny-day` uses `CalmPalette.SandLight` → `"SAND"`. `removedIds` starts empty — see Step 2's "Removing content" section for when to use it.)

- [ ] **Step 2: Write `docs/content-ota.md`**

```markdown
# Publishing new coloring pages (OTA)

New artwork reaches the app without an app-store release. The pipeline:

1. **Draft the SVG.** Ask Claude for a new template by title/subject (e.g.
   "Montessori pouring jug") in the same style as the existing 8 files under
   `svg/`. The output must follow this exact subset of SVG — it's what
   `parseSvgDocument`/`parseSvgPathData` in
   `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/`
   understand:
   - One `<svg viewBox="0 0 W H">` root, no nested groups or `transform`.
   - Direct `<rect>`, `<circle>`, and `<path>` children only, each with an
     `id` (region name — first element is conventionally `"background"`).
   - `<path d="...">` may use `M/m L/l H/h V/v Q/q C/c A/a Z/z`. Arc flags
     (the two `0`/`1` digits before the endpoint) must be space-separated —
     `"a30,30 0 0 1 58,-10"`, never digit-glued.
   - Save it as `svg/<id>.svg`.

2. **Add or bump its manifest entry** in `svg/manifest.json`: a new file
   needs a new `{ id, name, accent, file, contentVersion: 1 }` entry
   (`accent` is one of `SAGE`/`SKY`/`CLAY`/`SAND`/`LILAC`/`MOSS`); editing an
   existing file's art requires bumping that entry's `contentVersion` — the
   app only re-downloads a template when the manifest's `contentVersion`
   differs from what it has cached.

3. **Commit and push to `main`.** The app fetches
   `https://raw.githubusercontent.com/Atul206/MontessoriArc/main/svg/manifest.json`
   (and the `.svg` files it lists) on every launch
   (`ContentRepository.refresh()`, called from `App.kt`), diffs
   `contentVersion` against its local SQLDelight cache, downloads and caches
   whatever changed, and re-renders. No further action needed — nothing to
   run, deploy, or release.

Rendering itself always comes from the local cache (or, for the 8 launch
templates before any refresh has ever completed, the compiled-in
`TemplateCatalog.all`) — a refresh only updates what's cached for next time,
it never blocks what's on screen.

## Fixing a bad release

If a published piece is wrong but should still exist, **never** edit git
history or force-push — devices may have already fetched the bad commit, and
`raw.githubusercontent.com` has no CDN layer to invalidate anyway. Just fix
the `.svg` file and **bump its `contentVersion` upward** (never down — the
app only re-fetches when the manifest's number differs from what it has
cached, so a higher number is what makes every device pick up the fix on its
next launch).

## Removing content entirely

Use this when a piece shouldn't exist at all (wrong subject, quality issue,
whatever) rather than just needing a fix. Deleting its entry from
`templates` is **not enough** — a device that already cached it has no way to
learn it should stop being shown, since `ContentRepository.refresh()` never
prunes a cached id just because it disappeared from the manifest (that could
just as easily mean "no update available" as "delete this").

Instead, add the id to the top-level `removedIds` array:

```json
{
  "templates": [ /* ... entries for everything that should still exist ... */ ],
  "removedIds": ["some-bad-template-id"]
}
```

On next `refresh()`, every device deletes that id from its local
`TemplateCacheStore` and drops it from the gallery. If that id was one of
the 8 launch templates (i.e. it also exists in the compiled-in
`TemplateCatalog.all`), removing the cached override doesn't remove the
template entirely — it falls back to showing the original bundled version
again, since `ContentRepository` only hides a bundled entry while a cached
override for that same id exists. For a template that only ever existed as
OTA content (no bundled fallback), removing it makes it disappear from the
gallery, which is the correct "undo a bad publish" behavior.

Leave an id in `removedIds` permanently (don't clean it up later) — it costs
nothing on devices that never cached it, and removing it from the list would
let a device that's stuck offline for a long time re-download the entry if
it ever reappeared under `templates` by id collision with something new.
```

- [ ] **Step 3: Verify the JSON is valid**

Run: `python3 -c "import json; json.load(open('svg/manifest.json'))"`

Expected: no output (exits 0 — confirms `svg/manifest.json` parses).

- [ ] **Step 4: Commit**

```bash
git add svg/manifest.json docs/content-ota.md
git commit -m "docs: publish the OTA content manifest and authoring workflow"
```

---

## Self-Review

**Spec coverage:** "Generate SVG via Claude" → Task 9 doc (workflow) + reuses the exact hand-authoring convention already proven by the 8 existing templates. "Push to GitHub" → Task 9 (manifest + docs) + Task 8 (`CONTENT_BASE_URL` pointing at `raw.githubusercontent.com`). "App downloads latest SVG + URL" → Task 5 (`ContentApi`). "Save into local database, one-time save" → Task 6 (`TemplateCacheStore`/SQLDelight) + `ContentRepository.refresh()`'s version check (Task 7) ensures a given `contentVersion` is only ever downloaded once. "OTA publish/download/maintain" → Task 7 (`refresh()` + merge) + Task 8 (wired into app launch). "Roll back / delete a bad release" → `ContentManifest.removedIds` (Task 4), `TemplateCacheStore.deleteTemplate` (Task 6), `ContentRepository.refresh()`'s removal pass with automatic fallback-to-bundled (Task 7), documented in Task 9's "Fixing a bad release" / "Removing content entirely" sections. No gaps found.

**Placeholder scan:** every step above has runnable code or an exact shell command; no "TBD"/"handle appropriately"/deferred blocks.

**Type consistency:** `RemoteRegion`/`RemoteShape` (Task 3) flow unchanged into `TemplateCacheStore.replaceTemplate` (Task 6) and `ContentRepository.refresh()` (Task 7); `AccentKey` (Task 4) is used identically in `ManifestEntry.accent`, `TemplateCacheStore.replaceTemplate`'s `accent` param, and `TemplateCacheStore.allTemplates()`'s `AccentKey.valueOf(...).toColor()`; `ContentApi`'s constructor `(HttpClient, String)` matches both its test (Task 5) and its `ContentRepositoryProvider` call site (Task 8).

---

**Plan complete and saved to `docs/superpowers/plans/2026-09-14-svg-ota-content-pipeline.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
