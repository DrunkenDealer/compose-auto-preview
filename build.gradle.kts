plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

val composeRulesVersion = libs.versions.composeRules.get()

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    dependencies {
        "ktlintRuleset"("io.nlopez.compose.rules:ktlint:$composeRulesVersion")
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.8.0")
        android.set(true)
        outputToConsole.set(true)
        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
        }
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        // KMP source sets live outside the default src/main/kotlin.
        source.setFrom(files("src"))
    }

    tasks.register("detektAll") {
        group = "verification"
        description = "Run detekt on all source sets"
        dependsOn("detekt")
    }
}

// The gradle-plugin included build isn't a subproject, so the root task pulls it in.
tasks.register("detektAll") {
    group = "verification"
    description = "Run detekt on all modules, including the gradle-plugin build"
    dependsOn(gradle.includedBuild("gradle-plugin").task(":detekt"))
}
