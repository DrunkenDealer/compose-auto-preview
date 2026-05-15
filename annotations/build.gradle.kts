import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "io.github.drunkendealer.composeautopreview.annotations"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    coordinates(
        groupId = "io.github.drunkendealer",
        artifactId = "compose-auto-preview-annotations",
        version = "2.0.0",
    )
    pom {
        name.set("Compose Auto Preview — Annotations")
        description.set("Annotations for generating Compose @Preview matrices via KSP.")
        inceptionYear.set("2026")
        url.set("https://github.com/DrunkenDealer/compose-auto-preview")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("DrunkenDealer")
                name.set("Max Shwed")
                url.set("https://github.com/DrunkenDealer")
            }
        }
        scm {
            url.set("https://github.com/DrunkenDealer/compose-auto-preview")
            connection.set("scm:git:git://github.com/DrunkenDealer/compose-auto-preview.git")
            developerConnection.set("scm:git:ssh://git@github.com/DrunkenDealer/compose-auto-preview.git")
        }
    }
    publishToMavenCentral()
    if (providers.gradleProperty("signingEnabled").orNull != "false") {
        signAllPublications()
    }
}
