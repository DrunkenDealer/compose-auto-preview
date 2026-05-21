import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

dependencies {
    implementation(projects.annotations)
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    testImplementation(libs.kotlin.test)
}

mavenPublishing {
    coordinates(
        groupId = "io.github.drunkendealer",
        artifactId = "compose-auto-preview-processor",
        version = "3.1.0",
    )
    pom {
        name.set("Compose Auto Preview — KSP Processor")
        description.set("KSP processor that generates Compose @Preview matrices and PreviewParameterProviders.")
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
