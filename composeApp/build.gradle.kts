import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
        androidUnitTest.dependencies {
            // See the `robolectric` version note in gradle/libs.versions.toml:
            // TemplateCatalogTest exercises real androidx.compose.ui.graphics.Path
            // geometry, which the plain android.jar test stub can't provide.
            implementation(libs.robolectric)
        }
    }
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}
