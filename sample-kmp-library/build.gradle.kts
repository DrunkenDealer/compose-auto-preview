plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    id("app.mashlab.autopreview")
}

kotlin {
    androidLibrary {
        namespace = "app.mashlab.autopreview.kmp"
        compileSdk = libs.versions.android.compileSdk
            .get()
            .toInt()
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
        }
    }
}

compose.resources {
    packageOfResClass = "app.mashlab.autopreview.kmp.resources"
}

// The plugin adds the published annotations and processor; build them from source here instead.
configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("app.mashlab.autopreview:annotations")).using(project(":annotations"))
        substitute(module("app.mashlab.autopreview:processor")).using(project(":processor"))
    }
}
