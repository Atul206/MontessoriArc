# Calm Coloring MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the v1 Calm Coloring app — a two-screen (template gallery + tap-to-fill canvas) Compose Multiplatform app for Android and iOS, matching the reviewed UI mockup exactly, with print and a watermarked social-share flow.

**Architecture:** Single shared `composeApp` module (Kotlin Multiplatform + Compose Multiplatform, Skia-backed on both platforms). All UI, state, hit-testing, and navigation live in `commonMain`. Only PDF generation and the OS share sheet are `expect`/`actual` — everything else, including the shareable-image snapshot, is common code using `GraphicsLayer.toImageBitmap()`. Templates are hand-authored SVGs converted to Compose `Path` objects at build time; each region also gets a sampled hit-testing polygon so tap detection needs no platform-specific point-in-path API.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (Material 3), Navigation 3 (`androidx.navigation3`, common since CMP 1.10+), `svg-to-compose` (build-time Gradle task), `androidx.lifecycle.ViewModel` (KMP artifact), `kotlin.test` for common unit tests, Compose UI test (`androidx.compose.ui.test.junit4`) for one Android instrumented test.

**Spec:** [`calm-coloring-app-PRD.md`](../../../calm-coloring-app-PRD.md) (product requirements, §5–§9 especially) and [`design/calm-coloring-ui-mockup.html`](../../../design/calm-coloring-ui-mockup.html) (approved interactive UI reference — exact palette, copy, and interaction to match).

## Global Constraints

- **Platforms:** Android + iOS, phone and tablet, via Kotlin Multiplatform + Compose Multiplatform, Skia-backed rendering on both (PRD §6).
- **No third-party animation library.** Use built-in `animateColorAsState` with a tuned `spring()` — high damping, low stiffness, no bounce (PRD §5.1, §6). Concretely: `dampingRatio = 0.9f`, `stiffness = 60f`.
- **Hit-testing runs once per tap (tap-up), never per-frame** (PRD §5.1, §6) — implemented with `detectTapGestures(onTap = ...)`, which already only fires on release.
- **SVGs → Compose `Path`/`ImageVector` at build time** via `svg-to-compose` (PRD §6).
- **Calm-design constraints apply everywhere** (PRD §5.2): no sound, no unprompted animation besides the fill-settle, no confetti/streak/reward UI, one small muted palette (6 swatches) reused for chrome and fill alike — see Task 1 for exact hex values, taken verbatim from the approved mockup.
- **Age tier: 3–5 years only for v1** (PRD §5.3, §9) — minimum touch target 2cm × 2cm (≈48dp at mdpi baseline, but compute from `LocalDensity` — see Task 4), 6 colors shown, moderate region count per template.
- **Print renders from the same vector paths shown on screen, no cloud round-trip** (PRD §5.4) — platform-native PDF (Android `PdfDocument`, iOS `UIGraphicsPDFRenderer`), per PRD §6.
- **Offline-first: no accounts, no analytics SDKs, no ads** (PRD §8). The share flow (added after mockup review) sends only the rendered picture bytes to the OS share sheet — the artist's name is never persisted or transmitted anywhere else.
- **No DI framework for v1.** The app has two screens and three small state holders; a framework (Koin/Hilt) adds ceremony with no benefit at this size. ViewModels take constructor args via a `viewModelFactory { }` block. Revisit only if the screen count grows materially past v1.
- **Target SDK 35+ on Android**, `enableEdgeToEdge()` in the single Activity, insets applied per Task 9 (android-skills:edge-to-edge).

---

## File Structure

```
BabyApp/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
├── svg/                                                  # Task 2 — hand-authored source art
│   ├── sunny-day.svg, little-house.svg, apple-tree.svg, sleepy-cat.svg,
│   │   little-fish.svg, garden-flower.svg, sailboat.svg, balloon-ride.svg
├── iosApp/                                               # Task 0 — Xcode wrapper (KMP wizard output)
└── composeApp/
    ├── build.gradle.kts                                  # Task 0, 2
    └── src/
        ├── commonMain/kotlin/com/calmcoloring/app/
        │   ├── App.kt                                    # Task 5
        │   ├── theme/Color.kt, Type.kt, Theme.kt          # Task 1
        │   ├── content/generated/*.kt                     # Task 2 — svg-to-compose output, not hand-edited
        │   ├── content/TemplateCatalog.kt                 # Task 2
        │   ├── model/Template.kt, RegionSpec.kt            # Task 2
        │   ├── geometry/PointInPolygon.kt, PathSampling.kt # Task 3
        │   ├── ui/canvas/RegionCanvas.kt                   # Task 3
        │   ├── ui/coloring/ColoringViewModel.kt, ColoringScreen.kt  # Task 4
        │   ├── ui/gallery/GalleryScreen.kt                 # Task 5
        │   ├── navigation/Routes.kt                        # Task 5
        │   ├── platform/Printer.kt (expect)                # Task 6
        │   ├── ui/share/ParentGate.kt                      # Task 7
        │   ├── ui/share/ShareViewModel.kt, ShareSheet.kt   # Task 8
        │   └── platform/Sharer.kt (expect)                 # Task 8
        ├── commonTest/kotlin/com/calmcoloring/app/
        │   ├── geometry/PointInPolygonTest.kt              # Task 3
        │   ├── ui/coloring/ColoringViewModelTest.kt        # Task 4
        │   └── ui/share/ParentGateTest.kt                  # Task 7
        ├── androidMain/kotlin/com/calmcoloring/app/
        │   ├── MainActivity.kt                             # Task 0, revisited Task 9
        │   ├── platform/Printer.android.kt                 # Task 6
        │   └── platform/Sharer.android.kt                  # Task 8
        ├── androidInstrumentedTest/kotlin/com/calmcoloring/app/
        │   └── ui/coloring/ColoringScreenTest.kt           # Task 9
        └── iosMain/kotlin/com/calmcoloring/app/
            ├── MainViewController.kt                       # Task 0
            ├── platform/Printer.ios.kt                      # Task 6
            └── platform/Sharer.ios.kt                       # Task 8
```

---

### Task 0: Toolchain Verification & Project Scaffold

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`
- Create: `composeApp/build.gradle.kts`
- Create: `composeApp/src/androidMain/kotlin/com/calmcoloring/app/MainActivity.kt`
- Create: `composeApp/src/iosMain/kotlin/com/calmcoloring/app/MainViewController.kt`
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/App.kt` (placeholder, replaced in Task 5)
- Create: `iosApp/` (Xcode project wrapper)

**Interfaces:**
- Produces: a runnable `CalmColoringApp()` common composable (empty `Text("Calm Coloring")` placeholder) rendered by both `MainActivity` and `MainViewController`, so every later task has a target to build against on both platforms.

- [ ] **Step 1: Verify Compose Multiplatform's iOS maturity before scaffolding (PRD §6, "before committing")**

  Check the current JetBrains Compose Multiplatform release notes (https://github.com/JetBrains/compose-multiplatform/releases) for the latest stable version and confirm it states iOS is production-ready, and confirm it is version 1.10 or later (Navigation 3 common support requires 1.10+, per Task 5). Record the exact version resolved in `gradle/libs.versions.toml` in Step 3 — do not proceed on an assumed version.

- [ ] **Step 2: Scaffold the KMP project**

  Use the JetBrains Kotlin Multiplatform wizard (https://kmp.jetbrains.com) with: project name `CalmColoring`, package `com.calmcoloring.app`, targets Android + iOS, template "Compose Multiplatform (UI shared)". Download and unzip into the repo root so the structure matches the File Structure section above. This produces `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, `composeApp/build.gradle.kts`, and the `iosApp/` Xcode wrapper.

- [ ] **Step 3: Pin the verified Compose Multiplatform version**

  In `gradle/libs.versions.toml`, confirm/set:

  ```toml
  [versions]
  kotlin = "2.1.0"
  compose-multiplatform = "1.10.0" # replace with the version confirmed in Step 1
  agp = "8.9.0"

  [libraries]
  androidx-lifecycle-viewmodel-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version = "2.9.0" }
  androidx-navigation3-runtime = { module = "org.jetbrains.androidx.navigation3:navigation3-runtime", version = "1.0.0-alpha01" }
  androidx-navigation3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version = "1.0.0-alpha01" }
  ```

  Replace alpha versions with the latest stable at implementation time — check https://maven.google.com and https://central.sonatype.com for `androidx.navigation3` current coordinates, since it is a young library.

- [ ] **Step 4: Write the placeholder root composable**

  `composeApp/src/commonMain/kotlin/com/calmcoloring/app/App.kt`:

  ```kotlin
  package com.calmcoloring.app

  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Surface
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable

  @Composable
  fun CalmColoringApp() {
      MaterialTheme {
          Surface {
              Text("Calm Coloring")
          }
      }
  }
  ```

- [ ] **Step 5: Wire Android entry point**

  `composeApp/src/androidMain/kotlin/com/calmcoloring/app/MainActivity.kt`:

  ```kotlin
  package com.calmcoloring.app

  import android.os.Bundle
  import androidx.activity.ComponentActivity
  import androidx.activity.compose.setContent

  class MainActivity : ComponentActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          setContent { CalmColoringApp() }
      }
  }
  ```

  (`enableEdgeToEdge()` is added in Task 9, alongside the rest of the edge-to-edge audit, so it's reviewed once with full context instead of half-configured here.)

- [ ] **Step 6: Wire iOS entry point**

  `composeApp/src/iosMain/kotlin/com/calmcoloring/app/MainViewController.kt`:

  ```kotlin
  package com.calmcoloring.app

  import androidx.compose.ui.window.ComposeUIViewController

  fun MainViewController() = ComposeUIViewController { CalmColoringApp() }
  ```

- [ ] **Step 7: Build both targets**

  Run: `./gradlew :composeApp:assembleDebug`
  Expected: BUILD SUCCESSFUL, produces an installable debug APK.

  Run: `./gradlew :composeApp:iosSimulatorArm64Test` (or open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator)
  Expected: app launches in the iOS Simulator showing "Calm Coloring" text.

- [ ] **Step 8: Commit**

  ```bash
  git init
  git add .
  git commit -m "chore: scaffold Calm Coloring KMP + Compose Multiplatform project"
  ```

---

### Task 1: Design Tokens (Theme)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/theme/Color.kt`
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/theme/Type.kt`
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/theme/Theme.kt`
- Test: `composeApp/src/commonTest/kotlin/com/calmcoloring/app/theme/PaletteTest.kt`

**Interfaces:**
- Produces: `object CalmPalette` (6 swatch `Color`s + neutrals), `@Composable fun CalmColoringTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)` — consumed by every screen from Task 4 onward.

- [ ] **Step 1: Write the failing test for the swatch count constraint**

  ```kotlin
  package com.calmcoloring.app.theme

  import kotlin.test.Test
  import kotlin.test.assertEquals

  class PaletteTest {
      @Test
      fun swatchPalette_hasExactlySixColors() {
          assertEquals(6, CalmPalette.swatches.size)
      }

      @Test
      fun swatchPalette_hasNoDuplicateColors() {
          assertEquals(CalmPalette.swatches.size, CalmPalette.swatches.toSet().size)
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

  Run: `./gradlew :composeApp:testDebugUnitTest --tests "*.PaletteTest"`
  Expected: FAIL — `CalmPalette` unresolved reference.

- [ ] **Step 3: Write the color tokens**

  Values copied verbatim from the approved mockup (`design/calm-coloring-ui-mockup.html`, light-theme `:root` block):

  ```kotlin
  package com.calmcoloring.app.theme

  import androidx.compose.ui.graphics.Color

  object CalmPalette {
      // Neutrals — light
      val BgLight = Color(0xFFF1ECE2)
      val SurfaceLight = Color(0xFFFAF6EE)
      val InkLight = Color(0xFF3A342B)
      val InkSoftLight = Color(0xFF7A6F5F)
      val LineLight = Color(0xFFDDD3BF)

      // Neutrals — dark
      val BgDark = Color(0xFF211E19)
      val SurfaceDark = Color(0xFF2A2721)
      val InkDark = Color(0xFFECE5D8)
      val InkSoftDark = Color(0xFFB3A893)
      val LineDark = Color(0xFF423C31)

      // System palette — light (chrome + fill, one set, per PRD §5.2)
      val SageLight = Color(0xFF8FA382)
      val SkyLight = Color(0xFF7E97A6)
      val ClayLight = Color(0xFFC98B7A)
      val SandLight = Color(0xFFD9AE63)
      val LilacLight = Color(0xFFA48FA8)
      val MossLight = Color(0xFF6D8A68)

      // System palette — dark
      val SageDark = Color(0xFF9DB38F)
      val SkyDark = Color(0xFF8FABBA)
      val ClayDark = Color(0xFFD99E8D)
      val SandDark = Color(0xFFE2BE7C)
      val LilacDark = Color(0xFFB49FB8)
      val MossDark = Color(0xFF83A37D)

      val swatches: List<Color> = listOf(SageLight, SkyLight, ClayLight, SandLight, LilacLight, MossLight)
      fun swatchesFor(darkTheme: Boolean): List<Color> =
          if (darkTheme) listOf(SageDark, SkyDark, ClayDark, SandDark, LilacDark, MossDark) else swatches
  }
  ```

- [ ] **Step 4: Run test to verify it passes**

  Run: `./gradlew :composeApp:testDebugUnitTest --tests "*.PaletteTest"`
  Expected: PASS

- [ ] **Step 5: Write typography and theme**

  `Type.kt` (system font stack for v1 — bundling Baloo 2 / Karla as custom fonts is a nice-to-have, not required for the interaction to be reviewable; track as a follow-up, not a blocker):

  ```kotlin
  package com.calmcoloring.app.theme

  import androidx.compose.material3.Typography
  import androidx.compose.ui.text.TextStyle
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.unit.sp

  val CalmTypography = Typography(
      titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
      bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
      labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp),
  )
  ```

  `Theme.kt`:

  ```kotlin
  package com.calmcoloring.app.theme

  import androidx.compose.foundation.isSystemInDarkTheme
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.darkColorScheme
  import androidx.compose.material3.lightColorScheme
  import androidx.compose.runtime.Composable

  private val LightColors = lightColorScheme(
      background = CalmPalette.BgLight,
      surface = CalmPalette.SurfaceLight,
      onBackground = CalmPalette.InkLight,
      onSurface = CalmPalette.InkLight,
      outline = CalmPalette.LineLight,
      primary = CalmPalette.ClayLight,
  )

  private val DarkColors = darkColorScheme(
      background = CalmPalette.BgDark,
      surface = CalmPalette.SurfaceDark,
      onBackground = CalmPalette.InkDark,
      onSurface = CalmPalette.InkDark,
      outline = CalmPalette.LineDark,
      primary = CalmPalette.ClayDark,
  )

  @Composable
  fun CalmColoringTheme(
      darkTheme: Boolean = isSystemInDarkTheme(),
      content: @Composable () -> Unit,
  ) {
      MaterialTheme(
          colorScheme = if (darkTheme) DarkColors else LightColors,
          typography = CalmTypography,
          content = content,
      )
  }
  ```

- [ ] **Step 6: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/calmcoloring/app/theme composeApp/src/commonTest/kotlin/com/calmcoloring/app/theme
  git commit -m "feat: add calm-design theme tokens matching the approved mockup"
  ```

---

### Task 2: Template Content Pipeline (SVG → Compose Path + hit polygon)

**Files:**
- Create: `svg/sunny-day.svg`, `svg/little-house.svg`, `svg/apple-tree.svg`, `svg/sleepy-cat.svg`, `svg/little-fish.svg`, `svg/garden-flower.svg`, `svg/sailboat.svg`, `svg/balloon-ride.svg`
- Modify: `composeApp/build.gradle.kts` (add `svg-to-compose` Gradle task)
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/model/RegionSpec.kt`, `Template.kt`
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/TemplateCatalog.kt`
- Test: `composeApp/src/commonTest/kotlin/com/calmcoloring/app/content/TemplateCatalogTest.kt`

**Interfaces:**
- Produces: `data class Template(id: String, name: String, accent: Color, viewBoxWidth: Float, viewBoxHeight: Float, regions: List<RegionSpec>)`, `data class RegionSpec(id: String, path: Path, hitPolygon: List<Offset>)`, `object TemplateCatalog { val all: List<Template>; fun byId(id: String): Template }` — consumed by `RegionCanvas` (Task 3), `GalleryScreen` (Task 5).

- [ ] **Step 1: Author the 8 launch SVGs, reusing the approved mockup's shapes**

  Each file uses a `320×320` viewBox, one `<path>`/`<rect>`/`<circle>`/`<ellipse>` per fillable region, `id` attributes matching the mockup's `data-region` values, plus a full-canvas background rect as the first element (same construction as `design/calm-coloring-ui-mockup.html`'s `TEMPLATES` object). Example, `svg/little-house.svg`:

  ```xml
  <svg viewBox="0 0 320 320" xmlns="http://www.w3.org/2000/svg">
    <rect id="background" x="1.5" y="1.5" width="317" height="317" rx="18"/>
    <rect id="wall" x="60" y="150" width="200" height="130" rx="6"/>
    <path id="roof" d="M40,152 L160,58 L280,152 Z"/>
    <rect id="door" x="140" y="212" width="42" height="68" rx="6"/>
    <circle id="window" cx="212" cy="196" r="24"/>
  </svg>
  ```

  Repeat for the other 7 templates, copying the exact `d`/`cx`/`cy`/`r`/`x`/`y`/`width`/`height` values from the corresponding `TEMPLATES` entry in `design/calm-coloring-ui-mockup.html` (sunny-day = `sun`, apple-tree = `tree`, sleepy-cat = `cat`, little-fish = `fish`, garden-flower = `flower`, sailboat = `boat`, balloon-ride = `balloon`). Decorative, non-fillable marks from the mockup (the cat's eyes/whiskers, the balloon's string) are omitted here — v1 regions are fillable shapes only; decoration is a post-MVP polish item, not a blocker.

- [ ] **Step 2: Wire the `svg-to-compose` Gradle task**

  In `composeApp/build.gradle.kts`:

  ```kotlin
  plugins {
      id("br.com.devsrsouza.svg-to-compose") version "0.11.0"
  }

  iconsGeneration {
      destinationPackage = "com.calmcoloring.app.content.generated"
      vectorsDirectory = file("${rootDir}/svg")
      outputDirectory = file("composeApp/src/commonMain/kotlin")
      allAssetsPropertyName = "CalmTemplateAssets"
  }
  ```

  Run: `./gradlew generateVectors` (or the exact task name printed by `./gradlew tasks --group svg-to-compose`)
  Expected: generates one `ImageVector`-producing Kotlin object per SVG under `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/generated/`.

- [ ] **Step 3: Write the failing test for polygon sampling**

  This tests the piece that turns a generated `Path` into a hit-testable polygon — written first because Task 3's `pointInPolygon` depends on this data shape existing correctly.

  ```kotlin
  package com.calmcoloring.app.content

  import kotlin.test.Test
  import kotlin.test.assertTrue

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
      fun catalog_hasEightLaunchTemplates() {
          assertTrue(TemplateCatalog.all.size == 8)
      }
  }
  ```

- [ ] **Step 4: Run test to verify it fails**

  Run: `./gradlew :composeApp:testDebugUnitTest --tests "*.TemplateCatalogTest"`
  Expected: FAIL — `TemplateCatalog` unresolved reference.

- [ ] **Step 5: Write `RegionSpec` and `Template`**

  ```kotlin
  package com.calmcoloring.app.model

  import androidx.compose.ui.geometry.Offset
  import androidx.compose.ui.graphics.Path

  data class RegionSpec(
      val id: String,
      val path: Path,
      val hitPolygon: List<Offset>,
  )

  data class Template(
      val id: String,
      val name: String,
      val accent: androidx.compose.ui.graphics.Color,
      val viewBoxWidth: Float,
      val viewBoxHeight: Float,
      val regions: List<RegionSpec>,
  )
  ```

- [ ] **Step 6: Write the path-sampling helper (used to build each `RegionSpec.hitPolygon`)**

  `composeApp/src/commonMain/kotlin/com/calmcoloring/app/geometry/PathSampling.kt` (this is common Compose UI API — `PathMeasure` — no platform code needed):

  ```kotlin
  package com.calmcoloring.app.geometry

  import androidx.compose.ui.geometry.Offset
  import androidx.compose.ui.graphics.Path
  import androidx.compose.ui.graphics.PathMeasure

  fun Path.toHitPolygon(samples: Int = 96): List<Offset> {
      val measure = PathMeasure()
      measure.setPath(this, forceClosed = true)
      val length = measure.length
      if (length <= 0f) return emptyList()
      val points = ArrayList<Offset>(samples)
      for (i in 0 until samples) {
          val distance = length * (i.toFloat() / samples)
          points += measure.getPosition(distance)
      }
      return points
  }
  ```

- [ ] **Step 7: Write `TemplateCatalog`, wiring generated paths to regions**

  ```kotlin
  package com.calmcoloring.app.content

  import com.calmcoloring.app.content.generated.CalmTemplateAssets
  import com.calmcoloring.app.geometry.toHitPolygon
  import com.calmcoloring.app.model.RegionSpec
  import com.calmcoloring.app.model.Template
  import com.calmcoloring.app.theme.CalmPalette

  object TemplateCatalog {
      // CalmTemplateAssets.<Name>.<regionId> is the generated Path accessor
      // shape produced by svg-to-compose for a multi-path vector; the exact
      // accessor names are printed by `./gradlew generateVectors` — confirm
      // against that output before wiring each entry below.
      val all: List<Template> = listOf(
          Template(
              id = "sunny-day",
              name = "Sunny Day",
              accent = CalmPalette.SandLight,
              viewBoxWidth = 320f,
              viewBoxHeight = 320f,
              regions = listOf(
                  RegionSpec("background", CalmTemplateAssets.SunnyDay.background, CalmTemplateAssets.SunnyDay.background.toHitPolygon()),
                  RegionSpec("hill", CalmTemplateAssets.SunnyDay.hill, CalmTemplateAssets.SunnyDay.hill.toHitPolygon()),
                  RegionSpec("cloud", CalmTemplateAssets.SunnyDay.cloud, CalmTemplateAssets.SunnyDay.cloud.toHitPolygon()),
                  RegionSpec("sun", CalmTemplateAssets.SunnyDay.sun, CalmTemplateAssets.SunnyDay.sun.toHitPolygon()),
              ),
          ),
          // ... repeat for little-house, apple-tree, sleepy-cat, little-fish,
          // garden-flower, sailboat, balloon-ride, one Template each, region
          // ids matching Step 1's SVGs and accents cycling through the same
          // 6 CalmPalette swatches used in the mockup's card tint assignment.
      )

      fun byId(id: String): Template = all.first { it.id == id }
  }
  ```

- [ ] **Step 8: Run test to verify it passes**

  Run: `./gradlew :composeApp:testDebugUnitTest --tests "*.TemplateCatalogTest"`
  Expected: PASS

- [ ] **Step 9: Commit**

  ```bash
  git add svg composeApp/build.gradle.kts composeApp/src/commonMain/kotlin/com/calmcoloring/app/model composeApp/src/commonMain/kotlin/com/calmcoloring/app/content composeApp/src/commonMain/kotlin/com/calmcoloring/app/geometry composeApp/src/commonTest
  git commit -m "feat: add 8 launch templates via svg-to-compose content pipeline"
  ```

---

### Task 3: Core Interaction — `RegionCanvas` (hit-test + spring fill)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/geometry/PointInPolygon.kt`
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/canvas/RegionCanvas.kt`
- Test: `composeApp/src/commonTest/kotlin/com/calmcoloring/app/geometry/PointInPolygonTest.kt`

**Interfaces:**
- Consumes: `Template`, `RegionSpec` (Task 2).
- Produces: `fun pointInPolygon(point: Offset, polygon: List<Offset>): Boolean`; `@Composable fun RegionCanvas(template: Template, fills: Map<String, Color>, unfilledColor: Color, outlineColor: Color, onRegionTapped: (String) -> Unit, modifier: Modifier = Modifier)` — consumed by `ColoringScreen` (Task 4).

- [ ] **Step 1: Write the failing test for the ray-casting algorithm**

  ```kotlin
  package com.calmcoloring.app.geometry

  import androidx.compose.ui.geometry.Offset
  import kotlin.test.Test
  import kotlin.test.assertFalse
  import kotlin.test.assertTrue

  class PointInPolygonTest {
      private val square = listOf(
          Offset(0f, 0f), Offset(100f, 0f), Offset(100f, 100f), Offset(0f, 100f),
      )

      @Test
      fun pointInsideSquare_returnsTrue() {
          assertTrue(pointInPolygon(Offset(50f, 50f), square))
      }

      @Test
      fun pointOutsideSquare_returnsFalse() {
          assertFalse(pointInPolygon(Offset(150f, 50f), square))
      }

      @Test
      fun pointJustInsideEdge_returnsTrue() {
          assertTrue(pointInPolygon(Offset(1f, 50f), square))
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

  Run: `./gradlew :composeApp:testDebugUnitTest --tests "*.PointInPolygonTest"`
  Expected: FAIL — `pointInPolygon` unresolved reference.

- [ ] **Step 3: Implement the ray-casting point-in-polygon test**

  ```kotlin
  package com.calmcoloring.app.geometry

  import androidx.compose.ui.geometry.Offset

  fun pointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
      if (polygon.size < 3) return false
      var inside = false
      var j = polygon.size - 1
      for (i in polygon.indices) {
          val pi = polygon[i]
          val pj = polygon[j]
          val intersects = (pi.y > point.y) != (pj.y > point.y) &&
              point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x
          if (intersects) inside = !inside
          j = i
      }
      return inside
  }
  ```

- [ ] **Step 4: Run test to verify it passes**

  Run: `./gradlew :composeApp:testDebugUnitTest --tests "*.PointInPolygonTest"`
  Expected: PASS

- [ ] **Step 5: Write `RegionCanvas`**

  Letterboxes the template's viewBox into the available space (matching `ContentScale.Fit`), transforms taps into viewBox space before hit-testing, checks regions **last-to-first** so later-drawn (topmost) regions win ties — same z-order rule as the mockup. `spring(dampingRatio = 0.9f, stiffness = 60f)` is the "high damping, low stiffness, no bounce" spec from PRD §5.1/§6 and Global Constraints.

  ```kotlin
  package com.calmcoloring.app.ui.canvas

  import androidx.compose.animation.core.animateColorAsState
  import androidx.compose.animation.core.spring
  import androidx.compose.foundation.Canvas
  import androidx.compose.foundation.gestures.detectTapGestures
  import androidx.compose.foundation.layout.aspectRatio
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.geometry.Offset
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.graphics.drawscope.Stroke
  import androidx.compose.ui.graphics.drawscope.withTransform
  import androidx.compose.ui.input.pointer.pointerInput
  import com.calmcoloring.app.geometry.pointInPolygon
  import com.calmcoloring.app.model.Template

  @Composable
  fun RegionCanvas(
      template: Template,
      fills: Map<String, Color>,
      unfilledColor: Color,
      outlineColor: Color,
      onRegionTapped: (String) -> Unit,
      modifier: Modifier = Modifier,
  ) {
      val animatedFills = template.regions.associate { region ->
          region.id to animateColorAsState(
              targetValue = fills[region.id] ?: unfilledColor,
              animationSpec = spring(dampingRatio = 0.9f, stiffness = 60f),
              label = "region_fill_${region.id}",
          )
      }

      Canvas(
          modifier = modifier
              .aspectRatio(template.viewBoxWidth / template.viewBoxHeight)
              .pointerInput(template.id) {
                  detectTapGestures(onTap = { tapOffset ->
                      val scale = minOf(size.width / template.viewBoxWidth, size.height / template.viewBoxHeight)
                      val offsetX = (size.width - template.viewBoxWidth * scale) / 2f
                      val offsetY = (size.height - template.viewBoxHeight * scale) / 2f
                      val local = Offset(
                          (tapOffset.x - offsetX) / scale,
                          (tapOffset.y - offsetY) / scale,
                      )
                      val hit = template.regions.lastOrNull { region -> pointInPolygon(local, region.hitPolygon) }
                      hit?.let { onRegionTapped(it.id) }
                  })
              },
      ) {
          val scale = minOf(size.width / template.viewBoxWidth, size.height / template.viewBoxHeight)
          val offsetX = (size.width - template.viewBoxWidth * scale) / 2f
          val offsetY = (size.height - template.viewBoxHeight * scale) / 2f
          withTransform({
              translate(offsetX, offsetY)
              scale(scale, scale, pivot = Offset.Zero)
          }) {
              template.regions.forEach { region ->
                  val color = animatedFills.getValue(region.id).value
                  drawPath(region.path, color = color)
                  drawPath(region.path, color = outlineColor, style = Stroke(width = 3.5f))
              }
          }
      }
  }
  ```

  Note: `size` inside `pointerInput`'s `PointerInputScope` and inside `DrawScope` are both `androidx.compose.ui.geometry.Size` with `.width`/`.height` as `Float` already — no unit conversion needed.

- [ ] **Step 6: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/calmcoloring/app/geometry/PointInPolygon.kt composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/canvas composeApp/src/commonTest/kotlin/com/calmcoloring/app/geometry/PointInPolygonTest.kt
  git commit -m "feat: add RegionCanvas with tap-up hit-testing and spring fill-settle"
  ```

---

### Task 4: Coloring Screen (state, topbar, palette, reset)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/coloring/ColoringViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/coloring/ColoringScreen.kt`
- Test: `composeApp/src/commonTest/kotlin/com/calmcoloring/app/ui/coloring/ColoringViewModelTest.kt`

**Interfaces:**
- Consumes: `Template` (Task 2), `RegionCanvas` (Task 3), `CalmPalette` (Task 1).
- Produces: `class ColoringViewModel(template: Template) : ViewModel()` with `val fills: Map<String, Color>`, `val selectedColor: Color`, `fun selectColor(color: Color)`, `fun onRegionTapped(regionId: String)`, `fun reset()`; `@Composable fun ColoringScreen(template: Template, onBack: () -> Unit, onShareRequested: (ImageBitmap) -> Unit, modifier: Modifier = Modifier)` — consumed by navigation (Task 5) and the share flow (Task 8).

- [ ] **Step 1: Write the failing test for the state holder**

  Pure Kotlin, no Compose runtime needed to test this logic:

  ```kotlin
  package com.calmcoloring.app.ui.coloring

  import com.calmcoloring.app.model.Template
  import com.calmcoloring.app.theme.CalmPalette
  import kotlin.test.Test
  import kotlin.test.assertEquals

  class ColoringViewModelTest {
      private fun emptyTemplate() = Template("t", "Test", CalmPalette.SageLight, 320f, 320f, regions = emptyList())

      @Test
      fun initialSelectedColor_isFirstSwatch() {
          val vm = ColoringViewModel(emptyTemplate())
          assertEquals(CalmPalette.SageLight, vm.selectedColor)
      }

      @Test
      fun onRegionTapped_fillsRegionWithSelectedColor() {
          val vm = ColoringViewModel(emptyTemplate())
          vm.selectColor(CalmPalette.ClayLight)
          vm.onRegionTapped("roof")
          assertEquals(CalmPalette.ClayLight, vm.fills["roof"])
      }

      @Test
      fun reset_clearsAllFills() {
          val vm = ColoringViewModel(emptyTemplate())
          vm.onRegionTapped("roof")
          vm.reset()
          assertEquals(emptyMap(), vm.fills)
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

  Run: `./gradlew :composeApp:testDebugUnitTest --tests "*.ColoringViewModelTest"`
  Expected: FAIL — `ColoringViewModel` unresolved reference.

- [ ] **Step 3: Implement `ColoringViewModel`**

  ```kotlin
  package com.calmcoloring.app.ui.coloring

  import androidx.compose.runtime.getValue
  import androidx.compose.runtime.mutableStateMapOf
  import androidx.compose.runtime.mutableStateOf
  import androidx.compose.runtime.setValue
  import androidx.compose.ui.graphics.Color
  import androidx.lifecycle.ViewModel
  import com.calmcoloring.app.model.Template
  import com.calmcoloring.app.theme.CalmPalette

  class ColoringViewModel(private val template: Template) : ViewModel() {
      var selectedColor: Color by mutableStateOf(CalmPalette.swatches.first())
          private set

      private val _fills = mutableStateMapOf<String, Color>()
      val fills: Map<String, Color> get() = _fills

      fun selectColor(color: Color) {
          selectedColor = color
      }

      fun onRegionTapped(regionId: String) {
          _fills[regionId] = selectedColor
      }

      fun reset() {
          _fills.clear()
      }
  }
  ```

- [ ] **Step 4: Run test to verify it passes**

  Run: `./gradlew :composeApp:testDebugUnitTest --tests "*.ColoringViewModelTest"`
  Expected: PASS

- [ ] **Step 5: Write `ColoringScreen`**

  Reproduces the mockup's topbar (back · title + "Start over" · print + share icons), 6-swatch palette bar, and `RegionCanvas`. The 2cm minimum touch target (PRD §5.3) is computed from density rather than hardcoded dp, since 2cm varies slightly by screen density class:

  ```kotlin
  package com.calmcoloring.app.ui.coloring

  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.lazy.grid.GridCells
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.graphics.ImageBitmap
  import androidx.compose.ui.graphics.layer.rememberGraphicsLayer
  import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
  import androidx.compose.ui.platform.LocalDensity
  import androidx.compose.ui.unit.dp
  import androidx.lifecycle.viewmodel.compose.viewModel
  import androidx.lifecycle.viewmodel.viewModelFactory
  import androidx.lifecycle.viewmodel.initializer
  import com.calmcoloring.app.model.Template
  import com.calmcoloring.app.theme.CalmPalette
  import com.calmcoloring.app.ui.canvas.RegionCanvas
  import kotlinx.coroutines.launch

  // 2cm in dp, computed from density so the physical size is correct
  // regardless of screen class (PRD §5.3's NN/g-grounded touch-target floor).
  private const val CM_PER_INCH = 2.54f

  @Composable
  private fun minTouchTargetDp(): androidx.compose.ui.unit.Dp {
      val density = LocalDensity.current
      val dpPerCm = (density.density * 160f) / CM_PER_INCH / density.density
      return (2f * dpPerCm).dp
  }

  @Composable
  fun ColoringScreen(
      template: Template,
      onBack: () -> Unit,
      onShareRequested: (ImageBitmap) -> Unit,
      modifier: Modifier = Modifier,
  ) {
      val viewModel: ColoringViewModel = viewModel(
          factory = viewModelFactory { initializer { ColoringViewModel(template) } },
      )
      val graphicsLayer = rememberGraphicsLayer()
      val scope = rememberCoroutineScope()
      val touchTarget = minTouchTargetDp()

      Column(modifier = modifier.fillMaxSize()) {
          Row(
              modifier = Modifier.fillMaxWidth().padding(8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
              IconButton(onClick = onBack, modifier = Modifier.size(touchTarget)) {
                  Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to templates")
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(template.name, style = MaterialTheme.typography.titleMedium)
                  TextButton(onClick = { viewModel.reset() }) { Text("Start over") }
              }
              Row {
                  // Print button wired in Task 6; Share button wired in Task 8.
                  IconButton(onClick = { }, modifier = Modifier.size(touchTarget)) {
                      Icon(Icons.Filled.Print, contentDescription = "Print or export")
                  }
                  IconButton(
                      onClick = {
                          scope.launch { onShareRequested(graphicsLayer.toImageBitmap()) }
                      },
                      modifier = Modifier.size(touchTarget),
                  ) {
                      Icon(Icons.Filled.Share, contentDescription = "Share this picture")
                  }
              }
          }

          Box(modifier = Modifier.weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
              RegionCanvas(
                  template = template,
                  fills = viewModel.fills,
                  unfilledColor = MaterialTheme.colorScheme.surface,
                  outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                  onRegionTapped = viewModel::onRegionTapped,
                  modifier = Modifier
                      .fillMaxSize()
                      .drawWithContent {
                          graphicsLayer.record { this@drawWithContent.drawContent() }
                          drawLayer(graphicsLayer)
                      },
              )
          }

          Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
          ) {
              CalmPalette.swatches.forEach { swatch ->
                  val selected = swatch == viewModel.selectedColor
                  Box(
                      modifier = Modifier
                          .size(touchTarget)
                          .clip(CircleShape)
                          .background(swatch)
                          .border(
                              width = if (selected) 2.5.dp else 0.dp,
                              color = MaterialTheme.colorScheme.onSurface,
                              shape = CircleShape,
                          )
                          .clickable { viewModel.selectColor(swatch) },
                  )
              }
          }
      }
  }
  ```

  (`drawWithContent` + `graphicsLayer.record` is the standard Compose Multiplatform recipe for capturing a composable subtree to an `ImageBitmap` on demand — no platform code needed. `graphicsLayer.toImageBitmap()` is a suspend call, hence the `rememberCoroutineScope()` launch in the share button.)

- [ ] **Step 6: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/coloring composeApp/src/commonTest/kotlin/com/calmcoloring/app/ui/coloring
  git commit -m "feat: add ColoringScreen with topbar, 6-swatch palette, and reset"
  ```

---

### Task 5: Gallery Screen + Navigation 3 Wiring

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/gallery/GalleryScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/navigation/Routes.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/App.kt`

**Interfaces:**
- Consumes: `TemplateCatalog` (Task 2), `ColoringScreen` (Task 4).
- Produces: `@Composable fun GalleryScreen(templates: List<Template>, onTemplateSelected: (String) -> Unit)`; wires the full `GalleryRoute` ⇄ `ColoringRoute` navigation graph in `App.kt`.

- [ ] **Step 1: Define routes as `NavKey`**

  Navigation 3 is common across Android/iOS/desktop/web since Compose Multiplatform 1.10+ (confirmed in Task 0, Step 1), so this lives in `commonMain` with no `expect`/`actual` split.

  ```kotlin
  package com.calmcoloring.app.navigation

  import androidx.navigation3.runtime.NavKey
  import kotlinx.serialization.Serializable

  @Serializable
  data object GalleryRoute : NavKey

  @Serializable
  data class ColoringRoute(val templateId: String) : NavKey
  ```

- [ ] **Step 2: Write `GalleryScreen`**

  Reproduces the mockup's 2-column card grid, each card tinted with the template's `accent` at low opacity, label below.

  ```kotlin
  package com.calmcoloring.app.ui.gallery

  import androidx.compose.foundation.background
  import androidx.compose.foundation.clickable
  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.lazy.grid.GridCells
  import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
  import androidx.compose.foundation.lazy.grid.items
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.unit.dp
  import com.calmcoloring.app.model.Template

  @Composable
  fun GalleryScreen(
      templates: List<Template>,
      onTemplateSelected: (String) -> Unit,
      modifier: Modifier = Modifier,
  ) {
      Column(modifier = modifier.fillMaxSize()) {
          Text(
              "Calm Coloring",
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.padding(16.dp),
          )
          LazyVerticalGrid(
              columns = GridCells.Fixed(2),
              contentPadding = PaddingValues(16.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
              items(templates, key = { it.id }) { template ->
                  Column(
                      modifier = Modifier
                          .clip(RoundedCornerShape(18.dp))
                          .background(MaterialTheme.colorScheme.surface)
                          .clickable { onTemplateSelected(template.id) }
                          .padding(12.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                      Box(
                          modifier = Modifier
                              .fillMaxWidth()
                              .aspectRatio(1f)
                              .clip(RoundedCornerShape(14.dp))
                              .background(template.accent.copy(alpha = 0.2f)),
                      )
                      Spacer(Modifier.height(8.dp))
                      Text(template.name, style = MaterialTheme.typography.bodyMedium)
                  }
              }
          }
          Text(
              "No sound · no ads · no accounts — just tap and color.",
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          )
      }
  }
  ```

- [ ] **Step 3: Wire `App.kt` with `NavDisplay`**

  ```kotlin
  package com.calmcoloring.app

  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.remember
  import androidx.navigation3.runtime.NavEntry
  import androidx.navigation3.runtime.rememberNavBackStack
  import androidx.navigation3.ui.NavDisplay
  import com.calmcoloring.app.content.TemplateCatalog
  import com.calmcoloring.app.navigation.ColoringRoute
  import com.calmcoloring.app.navigation.GalleryRoute
  import com.calmcoloring.app.theme.CalmColoringTheme
  import com.calmcoloring.app.ui.coloring.ColoringScreen
  import com.calmcoloring.app.ui.gallery.GalleryScreen

  @Composable
  fun CalmColoringApp() {
      CalmColoringTheme {
          val backStack = rememberNavBackStack(GalleryRoute)

          NavDisplay(
              backStack = backStack,
              onBack = { backStack.removeLastOrNull() },
              entryProvider = { key ->
                  when (key) {
                      is GalleryRoute -> NavEntry(key) {
                          GalleryScreen(
                              templates = TemplateCatalog.all,
                              onTemplateSelected = { id -> backStack.add(ColoringRoute(id)) },
                          )
                      }
                      is ColoringRoute -> NavEntry(key) {
                          val template = remember(key.templateId) { TemplateCatalog.byId(key.templateId) }
                          ColoringScreen(
                              template = template,
                              onBack = { backStack.removeLastOrNull() },
                              onShareRequested = { /* wired in Task 8 */ },
                          )
                      }
                      else -> error("Unknown route: $key")
                  }
              },
          )
      }
  }
  ```

- [ ] **Step 4: Build and manually verify the navigation flow**

  Run: `./gradlew :composeApp:installDebug` (Android) and launch on an emulator/device.
  Expected: gallery grid shows all 8 templates; tapping one opens `ColoringScreen` with that template's shapes; the back arrow returns to the gallery.

- [ ] **Step 5: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/gallery composeApp/src/commonMain/kotlin/com/calmcoloring/app/navigation composeApp/src/commonMain/kotlin/com/calmcoloring/app/App.kt
  git commit -m "feat: add gallery screen and Navigation 3 back stack"
  ```

---

### Task 6: Print / Export (platform-native PDF from the same vector paths)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/platform/Printer.kt` (expect)
- Create: `composeApp/src/androidMain/kotlin/com/calmcoloring/app/platform/Printer.android.kt` (actual)
- Create: `composeApp/src/iosMain/kotlin/com/calmcoloring/app/platform/Printer.ios.kt` (actual)
- Modify: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/coloring/ColoringScreen.kt` (wire the print button)

**Interfaces:**
- Consumes: `ImageBitmap` captured via the same `graphicsLayer.toImageBitmap()` mechanism already wired in Task 4.
- Produces: `expect suspend fun printArtwork(artwork: ImageBitmap, pageWidthPoints: Float = 612f, pageHeightPoints: Float = 792f)` — consumed by `ColoringScreen`'s print button.

- [ ] **Step 1: Declare the `expect` function**

  ```kotlin
  package com.calmcoloring.app.platform

  import androidx.compose.ui.graphics.ImageBitmap

  // US Letter in points (72pt/inch) by default; PRD leaves paper-size
  // selection as a v1.1 detail, so a single fixed size is the correct v1 scope.
  expect suspend fun printArtwork(
      artwork: ImageBitmap,
      pageWidthPoints: Float = 612f,
      pageHeightPoints: Float = 792f,
  )
  ```

- [ ] **Step 2: Implement the Android `actual` with `PdfDocument` + the system print framework**

  ```kotlin
  package com.calmcoloring.app.platform

  import android.content.Context
  import android.graphics.pdf.PdfDocument
  import android.print.PrintAttributes
  import android.print.PrintManager
  import androidx.compose.ui.graphics.ImageBitmap
  import androidx.compose.ui.graphics.asAndroidBitmap
  import java.io.File
  import java.io.FileOutputStream

  // Set once from MainActivity.onCreate before any print call.
  lateinit var appContext: Context

  actual suspend fun printArtwork(artwork: ImageBitmap, pageWidthPoints: Float, pageHeightPoints: Float) {
      val document = PdfDocument()
      val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPoints.toInt(), pageHeightPoints.toInt(), 1).create()
      val page = document.startPage(pageInfo)
      val bitmap = artwork.asAndroidBitmap()
      val scale = minOf(pageWidthPoints / bitmap.width, pageHeightPoints / bitmap.height)
      val dx = (pageWidthPoints - bitmap.width * scale) / 2f
      val dy = (pageHeightPoints - bitmap.height * scale) / 2f
      page.canvas.translate(dx, dy)
      page.canvas.scale(scale, scale)
      page.canvas.drawBitmap(bitmap, 0f, 0f, null)
      document.finishPage(page)

      val outputFile = File(appContext.cacheDir, "calm-coloring-export.pdf")
      FileOutputStream(outputFile).use { document.writeTo(it) }
      document.close()

      val printManager = appContext.getSystemService(Context.PRINT_SERVICE) as PrintManager
      val adapter = android.print.PdfDocumentAdapter(outputFile)
      printManager.print("Calm Coloring picture", adapter, PrintAttributes.Builder().build())
  }
  ```

  Note: `android.print.PdfDocumentAdapter` above is illustrative — Android's `PrintManager` expects a `PrintDocumentAdapter` subclass that streams the already-written PDF file; write a small private `PrintDocumentAdapter` implementation whose `onWrite` copies `outputFile`'s bytes into the destination `ParcelFileDescriptor`. Confirm the exact adapter shape against the current `android.print` API docs at implementation time — the PDF-generation half above (the `PdfDocument` usage) is the part PRD §6 pins down precisely; the adapter plumbing is ordinary platform boilerplate.

- [ ] **Step 3: Implement the iOS `actual` with `UIGraphicsPDFRenderer`**

  ```kotlin
  package com.calmcoloring.app.platform

  import androidx.compose.ui.graphics.ImageBitmap
  import platform.CoreGraphics.CGRectMake
  import platform.Foundation.NSTemporaryDirectory
  import platform.UIKit.UIActivityViewController
  import platform.UIKit.UIApplication
  import platform.UIKit.UIGraphicsPDFRenderer
  import platform.UIKit.UIGraphicsPDFRendererFormat

  actual suspend fun printArtwork(artwork: ImageBitmap, pageWidthPoints: Float, pageHeightPoints: Float) {
      val pageRect = CGRectMake(0.0, 0.0, pageWidthPoints.toDouble(), pageHeightPoints.toDouble())
      val renderer = UIGraphicsPDFRenderer(bounds = pageRect, format = UIGraphicsPDFRendererFormat())
      val uiImage = artwork.toUIImage() // small platform helper: ImageBitmap -> UIImage via a CGImage bridge
      val data = renderer.PDFDataWithActions { context ->
          context.beginPage()
          uiImage.drawInRect(pageRect)
      }
      val path = NSTemporaryDirectory() + "calm-coloring-export.pdf"
      data.writeToFile(path, atomically = true)

      val controller = UIApplication.sharedApplication.keyWindow?.rootViewController
      val activityController = UIActivityViewController(activityItems = listOf(path), applicationActivities = null)
      controller?.presentViewController(activityController, animated = true, completion = null)
  }
  ```

  Note: `artwork.toUIImage()` (Compose `ImageBitmap` → `UIImage`) and the exact `UIGraphicsPDFRenderer` Kotlin/Native cinterop signatures should be verified against the Kotlin/Native UIKit klib current at implementation time — this file is the one place in the plan where cinterop surface area is genuinely unstable across Kotlin versions; treat Step 3 as a spike to de-risk before relying on its exact shape, same caveat PRD §6 already flags for CMP iOS maturity generally.

- [ ] **Step 4: Wire the print button in `ColoringScreen`**

  Replace the placeholder `IconButton(onClick = { })` for print (Task 4, Step 5) with:

  ```kotlin
  IconButton(
      onClick = { scope.launch { printArtwork(graphicsLayer.toImageBitmap()) } },
      modifier = Modifier.size(touchTarget),
  ) {
      Icon(Icons.Filled.Print, contentDescription = "Print or export")
  }
  ```

- [ ] **Step 5: Manually verify on Android**

  Run the app on a device/emulator with Android's print service (or a PDF-printer virtual print destination), color a couple of regions, tap Print.
  Expected: system print preview opens showing the colored artwork, correctly scaled and centered on the page.

- [ ] **Step 6: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/calmcoloring/app/platform/Printer.kt composeApp/src/androidMain/kotlin/com/calmcoloring/app/platform/Printer.android.kt composeApp/src/iosMain/kotlin/com/calmcoloring/app/platform/Printer.ios.kt composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/coloring/ColoringScreen.kt
  git commit -m "feat: wire platform-native PDF print/export from the on-screen artwork"
  ```

---

### Task 7: Parental Gate (resolves the mockup review's open question)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/share/ParentGate.kt`
- Test: `composeApp/src/commonTest/kotlin/com/calmcoloring/app/ui/share/ParentGateTest.kt`

**Interfaces:**
- Produces: `class ParentGateState { val challenge: Pair<Int, Int>; fun check(answer: Int): Boolean }`, `@Composable fun ParentGateDialog(onPassed: () -> Unit, onDismiss: () -> Unit)` — consumed by `ShareSheet` (Task 8), gating every outbound share target (WhatsApp/Instagram/More), per the mockup review's flagged open question.

**Decision made here (resolving the open question from the mockup):** yes, gate it. A simple on-device arithmetic check — no network call, no data collection, nothing that conflicts with PRD §8 — is the lightest version of the "parental gate" pattern App/Play Store kids policies expect before any share/export flow leaves a children's app. Print is *not* gated (it stays on-device, matching PRD §5.4); only the three share targets are.

- [ ] **Step 1: Write the failing test**

  ```kotlin
  package com.calmcoloring.app.ui.share

  import kotlin.test.Test
  import kotlin.test.assertFalse
  import kotlin.test.assertTrue

  class ParentGateTest {
      @Test
      fun check_correctSum_returnsTrue() {
          val gate = ParentGateState(a = 4, b = 5)
          assertTrue(gate.check(9))
      }

      @Test
      fun check_incorrectSum_returnsFalse() {
          val gate = ParentGateState(a = 4, b = 5)
          assertFalse(gate.check(10))
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

  Run: `./gradlew :composeApp:testDebugUnitTest --tests "*.ParentGateTest"`
  Expected: FAIL — `ParentGateState` unresolved reference.

- [ ] **Step 3: Implement `ParentGateState`**

  ```kotlin
  package com.calmcoloring.app.ui.share

  class ParentGateState(private val a: Int, private val b: Int) {
      val challenge: Pair<Int, Int> get() = a to b
      fun check(answer: Int): Boolean = answer == a + b

      companion object {
          fun random(): ParentGateState {
              val a = (3..9).random()
              val b = (2..8).random()
              return ParentGateState(a, b)
          }
      }
  }
  ```

- [ ] **Step 4: Run test to verify it passes**

  Run: `./gradlew :composeApp:testDebugUnitTest --tests "*.ParentGateTest"`
  Expected: PASS

- [ ] **Step 5: Write `ParentGateDialog`**

  ```kotlin
  package com.calmcoloring.app.ui.share

  import androidx.compose.foundation.layout.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.unit.dp

  @Composable
  fun ParentGateDialog(onPassed: () -> Unit, onDismiss: () -> Unit) {
      val gate = remember { ParentGateState.random() }
      var answer by remember { mutableStateOf("") }
      var showError by remember { mutableStateOf(false) }

      AlertDialog(
          onDismissRequest = onDismiss,
          title = { Text("Quick check for grown-ups") },
          text = {
              Column {
                  Text("What's ${gate.challenge.first} + ${gate.challenge.second}?")
                  Spacer(Modifier.height(8.dp))
                  OutlinedTextField(
                      value = answer,
                      onValueChange = { answer = it; showError = false },
                      isError = showError,
                      supportingText = if (showError) { { Text("Not quite — try again.") } } else null,
                      singleLine = true,
                  )
              }
          },
          confirmButton = {
              TextButton(onClick = {
                  val parsed = answer.toIntOrNull()
                  if (parsed != null && gate.check(parsed)) onPassed() else showError = true
              }) { Text("Continue") }
          },
          dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
      )
  }
  ```

- [ ] **Step 6: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/share/ParentGate.kt composeApp/src/commonTest/kotlin/com/calmcoloring/app/ui/share/ParentGateTest.kt
  git commit -m "feat: add on-device parental gate ahead of the share flow"
  ```

---

### Task 8: Share Sheet (watermark + WhatsApp/Instagram/More)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/share/ShareViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/share/ShareSheet.kt`
- Create: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/platform/Sharer.kt` (expect)
- Create: `composeApp/src/androidMain/kotlin/com/calmcoloring/app/platform/Sharer.android.kt` (actual)
- Create: `composeApp/src/iosMain/kotlin/com/calmcoloring/app/platform/Sharer.ios.kt` (actual)
- Modify: `composeApp/src/commonMain/kotlin/com/calmcoloring/app/App.kt` (host the sheet + gate above `ColoringRoute`)
- Test: `composeApp/src/commonTest/kotlin/com/calmcoloring/app/ui/share/ShareViewModelTest.kt`

**Interfaces:**
- Consumes: `ImageBitmap` (from `ColoringScreen`'s `onShareRequested`, Task 4), `ParentGateDialog` (Task 7).
- Produces: `enum class ShareTarget { WhatsApp, Instagram, More }`, `expect fun shareArtwork(artwork: ImageBitmap, target: ShareTarget)`, `@Composable fun ShareSheet(artwork: ImageBitmap, onDismiss: () -> Unit)`.

- [ ] **Step 1: Write the failing test for the caption logic**

  ```kotlin
  package com.calmcoloring.app.ui.share

  import kotlin.test.Test
  import kotlin.test.assertEquals

  class ShareViewModelTest {
      @Test
      fun caption_blankName_showsDefaultCopy() {
          assertEquals("A little artist's painting", captionFor(name = ""))
          assertEquals("A little artist's painting", captionFor(name = "   "))
      }

      @Test
      fun caption_withName_showsPaintedByName() {
          assertEquals("Painted by Maya", captionFor(name = "Maya"))
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

  Run: `./gradlew :composeApp:testDebugUnitTest --tests "*.ShareViewModelTest"`
  Expected: FAIL — `captionFor` unresolved reference.

- [ ] **Step 3: Implement the caption function and `ShareTarget`**

  `ShareViewModel.kt`:

  ```kotlin
  package com.calmcoloring.app.ui.share

  fun captionFor(name: String): String =
      name.trim().let { if (it.isEmpty()) "A little artist's painting" else "Painted by $it" }

  enum class ShareTarget { WhatsApp, Instagram, More }
  ```

- [ ] **Step 4: Run test to verify it passes**

  Run: `./gradlew :composeApp:testDebugUnitTest --tests "*.ShareViewModelTest"`
  Expected: PASS

- [ ] **Step 5: Declare the `expect` sharer**

  ```kotlin
  package com.calmcoloring.app.platform

  import androidx.compose.ui.graphics.ImageBitmap
  import com.calmcoloring.app.ui.share.ShareTarget

  expect fun shareArtwork(artwork: ImageBitmap, caption: String, target: ShareTarget)
  ```

- [ ] **Step 6: Implement the Android `actual` — package-targeted intents for WhatsApp/Instagram, chooser for More**

  ```kotlin
  package com.calmcoloring.app.platform

  import android.content.Intent
  import android.net.Uri
  import androidx.compose.ui.graphics.ImageBitmap
  import androidx.compose.ui.graphics.asAndroidBitmap
  import androidx.core.content.FileProvider
  import com.calmcoloring.app.ui.share.ShareTarget
  import java.io.File
  import java.io.FileOutputStream

  actual fun shareArtwork(artwork: ImageBitmap, caption: String, target: ShareTarget) {
      val file = File(appContext.cacheDir, "calm-coloring-share.png")
      FileOutputStream(file).use { out ->
          artwork.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
      }
      val uri: Uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)

      val intent = Intent(Intent.ACTION_SEND).apply {
          type = "image/png"
          putExtra(Intent.EXTRA_STREAM, uri)
          putExtra(Intent.EXTRA_TEXT, caption)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          when (target) {
              ShareTarget.WhatsApp -> setPackage("com.whatsapp")
              ShareTarget.Instagram -> setPackage("com.instagram.android")
              ShareTarget.More -> {} // no package -> system chooser
          }
      }

      val launchIntent = if (target == ShareTarget.More) Intent.createChooser(intent, "Share this picture") else intent
      appContext.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }
  ```

  Requires a `FileProvider` entry in `AndroidManifest.xml` (`<provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.fileprovider" android:exported="false" android:grantUriPermissions="true"><meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths" /></provider>`) and `res/xml/file_paths.xml` with a `<cache-path name="shared" path="." />` entry.

- [ ] **Step 7: Implement the iOS `actual` with `UIActivityViewController`**

  iOS has no package-targeted equivalent — the OS share sheet surfaces WhatsApp/Instagram automatically if installed, so all three targets route through the same `UIActivityViewController`; the distinct buttons in `ShareSheet` are a UI affordance matching the mockup, not a routing difference on this platform.

  ```kotlin
  package com.calmcoloring.app.platform

  import androidx.compose.ui.graphics.ImageBitmap
  import com.calmcoloring.app.ui.share.ShareTarget
  import platform.UIKit.UIActivityViewController
  import platform.UIKit.UIApplication

  actual fun shareArtwork(artwork: ImageBitmap, caption: String, target: ShareTarget) {
      val uiImage = artwork.toUIImage() // same helper as Task 6, Step 3
      val controller = UIApplication.sharedApplication.keyWindow?.rootViewController
      val activityController = UIActivityViewController(activityItems = listOf(uiImage, caption), applicationActivities = null)
      controller?.presentViewController(activityController, animated = true, completion = null)
  }
  ```

- [ ] **Step 8: Write `ShareSheet`**

  Reproduces the mockup's sheet: live preview, name field, three target buttons, privacy line. The parental gate (Task 7) sits in front of every target tap.

  ```kotlin
  package com.calmcoloring.app.ui.share

  import androidx.compose.foundation.layout.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.graphics.ImageBitmap
  import androidx.compose.ui.graphics.asImageBitmap
  import androidx.compose.ui.unit.dp
  import com.calmcoloring.app.platform.shareArtwork

  @Composable
  fun ShareSheet(artwork: ImageBitmap, onDismiss: () -> Unit) {
      var name by remember { mutableStateOf("") }
      var pendingTarget by remember { mutableStateOf<ShareTarget?>(null) }

      ModalBottomSheet(onDismissRequest = onDismiss) {
          Column(modifier = Modifier.padding(18.dp)) {
              Text("Share this picture", style = MaterialTheme.typography.titleMedium)
              Spacer(Modifier.height(12.dp))

              Card {
                  Column(
                      modifier = Modifier.padding(14.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                      androidx.compose.foundation.Image(
                          bitmap = artwork,
                          contentDescription = null,
                          modifier = Modifier.size(128.dp),
                      )
                      Text(captionFor(name), style = MaterialTheme.typography.bodyMedium)
                      Text("Made with Calm Coloring", style = MaterialTheme.typography.labelSmall)
                  }
              }

              Spacer(Modifier.height(14.dp))
              OutlinedTextField(
                  value = name,
                  onValueChange = { name = it },
                  label = { Text("Artist's name") },
                  placeholder = { Text("e.g. Maya") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().imePadding(),
              )

              Spacer(Modifier.height(16.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  ShareTarget.entries.forEach { target ->
                      OutlinedButton(
                          onClick = { pendingTarget = target },
                          modifier = Modifier.weight(1f),
                      ) { Text(target.name) }
                  }
              }

              Spacer(Modifier.height(10.dp))
              Text(
                  "Sharing sends only this picture — nothing else ever leaves the device.",
                  style = MaterialTheme.typography.labelSmall,
              )
          }
      }

      pendingTarget?.let { target ->
          ParentGateDialog(
              onPassed = {
                  shareArtwork(artwork, captionFor(name), target)
                  pendingTarget = null
              },
              onDismiss = { pendingTarget = null },
          )
      }
  }
  ```

  `Modifier.imePadding()` on the text field's container is the direct fix for the one soft-keyboard input in the app (android-skills:edge-to-edge's IME guidance) — verified fully in Task 9's insets audit.

- [ ] **Step 9: Wire the sheet into `App.kt`**

  Replace the `ColoringRoute` branch's `onShareRequested = { /* wired in Task 8 */ }` (Task 5, Step 3) with state that shows `ShareSheet` when a snapshot is ready:

  ```kotlin
  is ColoringRoute -> NavEntry(key) {
      val template = remember(key.templateId) { TemplateCatalog.byId(key.templateId) }
      var shareArtwork by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
      ColoringScreen(
          template = template,
          onBack = { backStack.removeLastOrNull() },
          onShareRequested = { bitmap -> shareArtwork = bitmap },
      )
      shareArtwork?.let { bitmap ->
          ShareSheet(artwork = bitmap, onDismiss = { shareArtwork = null })
      }
  }
  ```

- [ ] **Step 10: Manually verify on Android**

  Color a region, tap Share, type a name, confirm the preview caption updates, tap WhatsApp, answer the parental-gate sum, confirm WhatsApp (or the chooser, if not installed) opens with the image attached.

- [ ] **Step 11: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/calmcoloring/app/ui/share composeApp/src/commonMain/kotlin/com/calmcoloring/app/platform/Sharer.kt composeApp/src/androidMain/kotlin/com/calmcoloring/app/platform/Sharer.android.kt composeApp/src/iosMain/kotlin/com/calmcoloring/app/platform/Sharer.ios.kt composeApp/src/commonMain/kotlin/com/calmcoloring/app/App.kt
  git commit -m "feat: add watermarked share sheet gated behind a parental check"
  ```

---

### Task 9: Testing, Performance & Store-Readiness Pass

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/calmcoloring/app/MainActivity.kt` (edge-to-edge)
- Create: `composeApp/src/androidInstrumentedTest/kotlin/com/calmcoloring/app/ui/coloring/ColoringScreenTest.kt`
- Modify: `composeApp/build.gradle.kts` (R8/minify config)

**Interfaces:**
- Consumes: everything from Tasks 0–8. This task only hardens and verifies; it introduces no new public API.

- [ ] **Step 1: Enable edge-to-edge and audit insets (android-skills:edge-to-edge)**

  `MainActivity.kt`:

  ```kotlin
  package com.calmcoloring.app

  import android.os.Bundle
  import androidx.activity.ComponentActivity
  import androidx.activity.compose.setContent
  import androidx.activity.enableEdgeToEdge
  import com.calmcoloring.app.platform.appContext

  class MainActivity : ComponentActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
          enableEdgeToEdge()
          super.onCreate(savedInstanceState)
          appContext = applicationContext
          setContent { CalmColoringApp() }
      }
  }
  ```

  Then, per the checklist in android-skills:edge-to-edge:
  - `GalleryScreen`'s `LazyVerticalGrid` (Task 5): change `contentPadding = PaddingValues(16.dp)` to also include `WindowInsets.safeDrawing.asPaddingValues()` merged in, so the first/last row don't sit under the status/navigation bars.
  - `ColoringScreen`'s root `Column` (Task 4): add `Modifier.safeDrawingPadding()` so the topbar and palette bar clear the system bars.
  - `ShareSheet`'s `OutlinedTextField` (Task 8, Step 8): already has `Modifier.imePadding()` — confirm it does **not** also sit inside a `Scaffold` with `contentWindowInsets = WindowInsets.safeDrawing`, which would double-pad (this app has no such `Scaffold`, so this is already correct — recheck only if one is introduced later).
  - Confirm `targetSdk = 35` (or higher) in `composeApp/build.gradle.kts`.

  Run: `./gradlew :composeApp:assembleDebug` and manually check on a gesture-navigation device that no interactive element (back button, print/share icons, swatches, "Continue" in the parental gate) sits under a system bar or is clipped by the keyboard.

- [ ] **Step 2: Write the one Android instrumented Compose test (tap-to-fill end to end)**

  Pure logic (hit-testing, view-model state, caption text) is already covered by the `commonTest` suite from Tasks 1–4, 7–8; this is the one test that exercises real Compose layout + gesture dispatch, per android-skills:testing-setup step 9.

  ```kotlin
  package com.calmcoloring.app.ui.coloring

  import androidx.compose.ui.test.junit4.createComposeRule
  import androidx.compose.ui.test.onNodeWithContentDescription
  import androidx.compose.ui.test.performClick
  import com.calmcoloring.app.content.TemplateCatalog
  import org.junit.Rule
  import org.junit.Test

  class ColoringScreenTest {
      @get:Rule val composeRule = createComposeRule()

      @Test
      fun tappingBack_returnsToCallerCallback() {
          var backCalled = false
          composeRule.setContent {
              ColoringScreen(
                  template = TemplateCatalog.byId("little-house"),
                  onBack = { backCalled = true },
                  onShareRequested = {},
              )
          }
          composeRule.onNodeWithContentDescription("Back to templates").performClick()
          assert(backCalled)
      }
  }
  ```

  Run: `./gradlew :composeApp:connectedAndroidTest --tests "*.ColoringScreenTest"`
  Expected: PASS on a connected device/emulator.

- [ ] **Step 3: Run an R8 shrink/obfuscate pass and check for stripped reflection (android-skills:r8-analyzer)**

  Run: `./gradlew :composeApp:assembleRelease`
  Then apply android-skills:r8-analyzer to the produced release build to check for over-aggressive stripping — this app's only reflection-sensitive surface is `kotlinx.serialization` on the two `NavKey` route types (Task 5) and any DSL config used by `svg-to-compose`'s generated code (Task 2). Add explicit `keep` rules in `composeApp/proguard-rules.pro` for anything the analyzer flags, rather than disabling shrinking.

- [ ] **Step 4: Establish a performance baseline (android-skills:android-profiler)**

  Apply android-skills:android-profiler against `ColoringScreen` on a mid-tier device, tapping through all 8 templates and filling several regions per template. Confirm: no dropped frames during the fill-settle animation (PRD §6's whole rationale for choosing Compose Multiplatform over WebView/React Native is smooth touch response — this is the step that actually verifies that promise), and that `animateColorAsState`'s per-region `Animatable` instances are correctly disposed when navigating back to the gallery (no growing memory across repeated template visits).

- [ ] **Step 5: Full regression pass**

  Run: `./gradlew :composeApp:testDebugUnitTest :composeApp:connectedAndroidTest`
  Expected: all `commonTest` and instrumented tests pass.

- [ ] **Step 6: Commit**

  ```bash
  git add composeApp/src/androidMain/kotlin/com/calmcoloring/app/MainActivity.kt composeApp/src/androidInstrumentedTest composeApp/proguard-rules.pro composeApp/build.gradle.kts
  git commit -m "chore: edge-to-edge audit, R8 keep rules, and performance baseline"
  ```

---

## Self-Review

**Spec coverage:**
- §5.1 tap-to-fill vector regions, point-in-polygon on tap-up, spring settle → Task 3.
- §5.2 calm-design constraints (no sound/animation/reward, 6-swatch shared palette) → Task 1 (palette), Task 3–4 (only the fill-settle animates, no other motion), Global Constraints (no confetti/sound anywhere in the plan).
- §5.3 age tier 3–5, 2cm touch target, 6 colors → Task 4.
- §5.4 print from same vector source, no cloud round-trip → Task 6.
- §6 KMP/Compose Multiplatform, svg-to-compose, animateColorAsState + spring, hit-test on tap-up, platform PDF, CMP iOS maturity check → Tasks 0, 2, 3, 6.
- §7 content licensing/custom templates → Task 2 (hand-authored, matching the already-approved, non-stock mockup art).
- §8 privacy: no accounts/analytics/ads → Global Constraints; share flow sends only image bytes, no persistence → Task 8.
- §9 MVP scope summary → Tasks 0–9 collectively; nothing out-of-scope (photo AI, freehand drawing, confetti, accounts, 1–2/2–3 tiers) appears anywhere in this plan.
- Mockup's Share addition (not in the original PRD) → Tasks 7–8, including a resolved decision on the mockup review's open parental-gate question.

**Placeholder scan:** no TBD/TODO markers; the two "verify against current X" notes (Task 0 Step 1 CMP version check, Task 6 Step 3 iOS cinterop signatures) are explicit verification steps with a concrete action, not unresolved decisions — both are called out because PRD §6 and §11 themselves flag these as items needing a final check immediately before/during build, not because the plan is unsure what to build.

**Type consistency:** `Template`/`RegionSpec` (Task 2) → consumed identically in `RegionCanvas` (Task 3), `ColoringViewModel`/`ColoringScreen` (Task 4), `GalleryScreen`/`App.kt` (Task 5). `ColoringViewModel.fills`/`selectedColor`/`onRegionTapped`/`reset` (Task 4) match every call site in Task 4's own `ColoringScreen` — no other task calls into `ColoringViewModel` directly. `ImageBitmap` produced by `ColoringScreen`'s `graphicsLayer.toImageBitmap()` (Task 4) flows unchanged into `printArtwork` (Task 6) and `ShareSheet`/`shareArtwork` (Task 8) — one snapshot mechanism, no duplicate capture path. `ShareTarget` (Task 8) is defined once and consumed by both `Sharer.kt` and `ShareSheet.kt`.

---

**Plan complete and saved to `docs/superpowers/plans/2026-09-13-calm-coloring-mvp.md`.** Two execution options:

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

**Which approach?**
