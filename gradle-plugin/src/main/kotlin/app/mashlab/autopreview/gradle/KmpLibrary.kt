package app.mashlab.autopreview.gradle

import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** `com.android.kotlin.multiplatform.library`: the Android target lives in `kotlin { androidLibrary { … } }`. */
internal fun configureKmpLibrary(
    project: Project,
    configure: (AndroidModule) -> Unit,
) {
    val targets = project.extensions
        .getByType(KotlinMultiplatformExtension::class.java)
        .targets
        .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
    // `all`, not `configureEach`: configure() registers tasks and afterEvaluate hooks, which lazy actions may not.
    targets.all { target ->
        target.hostTests().configureEach { it.isIncludeAndroidResources = true }
        val name = target.name.replaceFirstChar(Char::uppercase)
        configure(
            AndroidModule(
                namespace = project.provider {
                    requireNotNull(target.namespace) {
                        "Compose Auto Preview needs `kotlin { androidLibrary { namespace = … } }` to be set."
                    }
                },
                unitTestTask = "test${name}HostTest",
                testImplementation = "${target.name}HostTestImplementation",
                testDependencies = TEST_DEPENDENCIES,
                kmpMainSourceSet = "${target.name}Main",
                kmpTestSourceSet = "${target.name}HostTest",
                kspConfiguration = "ksp$name",
            ),
        )
    }
    // Host tests are opt-in and `withHostTest` may only be called once, so enable them only if the build script didn't.
    project.extensions.getByType(KotlinMultiplatformAndroidComponentsExtension::class.java).finalizeDsl {
        targets
            .filter { it.hostTests().isEmpty() }
            .forEach { it.withHostTest { isIncludeAndroidResources = true } }
    }
}

private fun KotlinMultiplatformAndroidLibraryTarget.hostTests() =
    compilations.withType(KotlinMultiplatformAndroidHostTestCompilation::class.java)
