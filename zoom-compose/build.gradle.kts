plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

// The Compose half: the state a pinch or a slider moves, the pinch gesture,
// and the wrappers that apply the size to a composition. Storage and the
// Settings controls stay in each app, which hands [FontSizeState] its stored
// value and the callbacks that write one back. Deliberately carrying **no
// resources**: a library with resources forces resource merging on every
// consumer, and the copy around a slider is each app's own.

android {
    namespace = "com.mikelward.androidzoom.compose"
    compileSdk = 37

    defaultConfig {
        // The floor across the consumer fleet -- clothescast. Raising it
        // silently drops a consumer, so it is a migration note in that app
        // rather than a tidy-up here (AGENTS.md).
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // Only `release` is published. A debug variant would double every
    // coordinate for no consumer, and AGP requires the choice to be stated
    // before `components["release"]` exists at all.
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            // `afterEvaluate` is AGP's requirement: the `release` component is
            // created while the Android extension is evaluated.
            afterEvaluate { from(components["release"]) }
            pom {
                name = "androidzoom zoom-compose"
                description = "Text-size state, pinch-to-resize and scaled density for Compose."
                url = "https://github.com/mikelward/androidzoom"
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // `api`: a consumer's call sites name `FontSizeSettings`, `clampFontScale`
    // and the range constants directly, so the core is part of this module's
    // public surface.
    api(project(":zoom-core"))

    // Compose comes from the consumer's own BOM in practice; the BOM here only
    // pins what this module compiles against, and Gradle resolves the newer of
    // the two in the app.
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.activity.compose)
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
