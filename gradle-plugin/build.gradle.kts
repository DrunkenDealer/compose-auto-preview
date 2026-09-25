plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.ktlint)
}

ktlint {
    version.set("1.8.0")
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    compileOnly(libs.android.gradle.api)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    testImplementation(libs.kotlin.test)
}

val generateVersions by tasks.registering {
    val robolectric = libs.versions.robolectric
        .get()
    val junit = libs.versions.junit
        .get()
    val autoPreview = libs.versions.autoPreview
        .get()
    val outputDir = layout.buildDirectory.dir("generated/versions")
    inputs.property("versions", listOf(robolectric, junit, autoPreview))
    outputs.dir(outputDir)
    doLast {
        val file = outputDir
            .get()
            .file("app/mashlab/autopreview/gradle/Versions.kt")
            .asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package app.mashlab.autopreview.gradle
            |
            |internal object Versions {
            |    const val ROBOLECTRIC = "$robolectric"
            |    const val JUNIT = "$junit"
            |    const val AUTO_PREVIEW = "$autoPreview"
            |}
            |
            """.trimMargin(),
        )
    }
}
kotlin.sourceSets.main { kotlin.srcDir(generateVersions) }

gradlePlugin {
    plugins {
        create("autoPreview") {
            id = "app.mashlab.compose-auto-preview"
            implementationClass = "app.mashlab.autopreview.gradle.AutoPreviewPlugin"
        }
    }
}

mavenPublishing {
    coordinates(
        groupId = "app.mashlab",
        artifactId = "compose-auto-preview-gradle-plugin",
        version = libs.versions.autoPreview
            .get(),
    )
    pom {
        name.set("Compose Auto Preview — Gradle Plugin")
        description.set("Renders the full Compose Auto Preview matrix off-IDE and builds an HTML app graph report.")
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
