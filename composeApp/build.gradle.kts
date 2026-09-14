import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("TemplateCacheDatabase") {
            packageName.set("com.calmcoloring.app.db")
        }
    }
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.animation)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.sqldelight.runtime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
        }
        androidUnitTest.dependencies {
            // See the `robolectric` version note in gradle/libs.versions.toml:
            // TemplateCatalogTest exercises real androidx.compose.ui.graphics.Path
            // geometry, which the plain android.jar test stub can't provide.
            implementation(libs.robolectric)
            implementation(libs.ktor.client.mock)
        }
        androidInstrumentedTest.dependencies {
            // Task 9: the one Compose UI test that exercises real layout +
            // gesture dispatch on a connected device/emulator (everything
            // else is covered by commonTest).
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.junit)
        }
    }
}


// ui-test-manifest provides the ComponentActivity that createComposeRule
// hosts test content in. It must be merged into the APP-under-test's own
// debug manifest (not the androidInstrumentedTest APK's manifest) or the
// self-instrumenting test launches its host activity in the wrong process
// ("com.calmcoloring.app.test" instead of "com.calmcoloring.app") and
// crashes before the test body runs. `androidDebugImplementation` is the
// KGP/AGP-generated configuration for exactly that (the android target's
// debug compilation) — added post-evaluate since AGP only creates it once
// the android {} block and variants are configured.
afterEvaluate {
    dependencies.add("androidDebugImplementation", libs.androidx.compose.ui.test.manifest.get())
}

tasks.withType<Test>().configureEach {
    // Robolectric's default (LEGACY) graphics shadows leave PathMeasure.length
    // at 0 for any path — verified directly. NATIVE mode runs the real Android
    // graphics engine on the host JVM instead, which TemplateCatalogTest needs
    // for genuine hit-polygon sampling. See the `robolectric` note in
    // gradle/libs.versions.toml.
    systemProperty("robolectric.graphicsMode", "NATIVE")
}

android {
    namespace = "com.calmcoloring.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.calmcoloring.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}
