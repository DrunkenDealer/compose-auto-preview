plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(projects.annotations)
    testImplementation(libs.kotlin.test)
}
