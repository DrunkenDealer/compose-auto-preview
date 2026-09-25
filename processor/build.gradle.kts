import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.mavenPublish)
}

// KSP loads the processor into the consumer's build JVM, so it must not target a newer JDK than AGP 8 needs (17).
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
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
    testImplementation(libs.kctfork.ksp)
}

mavenPublishing {
    coordinates(
        groupId = "app.mashlab",
        artifactId = "compose-auto-preview-processor",
        version = libs.versions.autoPreview
            .get(),
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
