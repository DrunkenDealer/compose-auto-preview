plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(projects.annotations)
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    testImplementation(libs.kotlin.test)
}
