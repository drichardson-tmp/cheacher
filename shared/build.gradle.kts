plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "com.cheacher.app.shared"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        // The 109-test commonTest suite is pure Kotlin, so it runs on the host JVM.
        withHostTestBuilder {}.configure {
            isIncludeAndroidResources = true
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // Without this the linker warns and falls back to the bundle *name*, since
            // a static framework has no package to infer an ID from.
            binaryOption("bundleId", "com.cheacher.app.shared")
        }
    }

    sourceSets {
        commonMain.dependencies {
            // `api`, not `implementation`: :androidApp calls App() and setContent, so
            // Compose has to be on its compile classpath too.
            // The `compose.*` accessors, not direct coordinates: Compose Multiplatform
            // versions material3 on its own line (1.9.0) from runtime/foundation/ui
            // (1.10.3), and the plugin is what keeps that pairing correct. Declaring
            // them by hand trades six deprecation warnings for a silent version skew.
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
