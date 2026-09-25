import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
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
    namespace = "app.mashlab.autopreview.annotations"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    coordinates(
        groupId = "app.mashlab",
        artifactId = "compose-auto-preview-annotations",
        version = libs.versions.autoPreview
            .get(),
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
