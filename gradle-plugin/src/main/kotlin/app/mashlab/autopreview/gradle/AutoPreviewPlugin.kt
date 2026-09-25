package app.mashlab.autopreview.gradle

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val GROUP = "autopreview"
private const val RENDER_TEST = "AutoPreviewRenderTest"
private const val RENDER_TASK = "autoPreviewRender"
private const val GENERATE_TEST_TASK = "generateAutoPreviewRenderTest"
private const val KSP_PLUGIN = "com.google.devtools.ksp"
private const val KMP_PLUGIN = "org.jetbrains.kotlin.multiplatform"
private const val KOTLIN_ANDROID_PLUGIN = "org.jetbrains.kotlin.android"
private const val KMP_LIBRARY_PLUGIN = "com.android.kotlin.multiplatform.library"
private const val ANNOTATIONS = "app.mashlab:compose-auto-preview-annotations:${Versions.AUTO_PREVIEW}"
private const val PROCESSOR = "app.mashlab:compose-auto-preview-processor:${Versions.AUTO_PREVIEW}"

internal val TEST_DEPENDENCIES = listOf(
    "org.robolectric:robolectric:${Versions.ROBOLECTRIC}",
    "junit:junit:${Versions.JUNIT}",
    // The render test hosts previews in a ComponentActivity, which a library needn't depend on. The oldest version
    // that works keeps the compileSdk floor low (34); Gradle still resolves the module's own, newer one.
    "androidx.activity:activity-compose:1.9.3",
)

/** What differs between the Android plugins; [configure] wires everything else the same way. */
internal class AndroidModule(
    val namespace: Provider<String>,
    val unitTestTask: String,
    val testImplementation: String,
    val testDependencies: List<String>,
    val kmpMainSourceSet: String,
    val kmpTestSourceSet: String,
    val kspConfiguration: String,
)

class AutoPreviewPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("com.android.application") { configureAndroidGradlePlugin(project) }
        project.pluginManager.withPlugin("com.android.library") { configureAndroidGradlePlugin(project) }
        // Kept in its own file so projects without the KMP library plugin never load its classes.
        project.pluginManager.withPlugin(KMP_LIBRARY_PLUGIN) { configureKmpLibrary(project) { configure(project, it) } }
    }

    private fun configureAndroidGradlePlugin(project: Project) {
        val android = project.extensions.getByType(CommonExtension::class.java)
        android.testOptions.unitTests.isIncludeAndroidResources = true
        val module = AndroidModule(
            namespace = project.provider {
                requireNotNull(android.namespace) { "Compose Auto Preview needs `android.namespace` to be set." }
            },
            unitTestTask = "testDebugUnitTest",
            testImplementation = "testImplementation",
            testDependencies = TEST_DEPENDENCIES,
            kmpMainSourceSet = "androidMain",
            kmpTestSourceSet = "androidUnitTest",
            kspConfiguration = "kspAndroid",
        )
        configure(project, module)

        // Plain Android (kotlin-android on AGP 8, built-in Kotlin on AGP 9, which applies no Kotlin plugin);
        // configure() wires KMP's androidTarget() through its source sets instead. Which one is only known once the
        // build script has run, and KSP reads its processors when the variants are created.
        val components = project.extensions.getByType(AndroidComponentsExtension::class.java)
        components.finalizeDsl {
            if (!project.pluginManager.hasPlugin(KMP_PLUGIN)) {
                project.dependencies.add("implementation", ANNOTATIONS)
                addProcessor(project, "ksp")
            }
        }
        components.onVariants(components.selector().withName("debug")) { variant ->
            if (!project.pluginManager.hasPlugin(KMP_PLUGIN)) {
                // The variant API, unlike `android.sourceSets`, carries the generating task's dependency.
                // unitTest's replacement, HasUnitTest, doesn't exist before AGP 8.1.
                @Suppress("DEPRECATION")
                val sources = variant.unitTest?.sources
                // kotlin-android compiles Kotlin from the Java directories only; built-in Kotlin from the Kotlin ones.
                val dirs = when {
                    project.pluginManager.hasPlugin(KOTLIN_ANDROID_PLUGIN) -> sources?.java
                    else -> sources?.kotlin
                }
                dirs?.addGeneratedSourceDirectory(
                    project.tasks.named(GENERATE_TEST_TASK, GenerateRenderTestTask::class.java),
                    GenerateRenderTestTask::outputDir,
                )
            }
        }
    }

    private fun configure(
        project: Project,
        module: AndroidModule,
    ) {
        project.pluginManager.withPlugin(KSP_PLUGIN) {
            project.extensions
                .getByType(KspExtension::class.java)
                .arg(RegistryPackageArgument(module.namespace))
        }
        project.afterEvaluate {
            if (!project.pluginManager.hasPlugin(KSP_PLUGIN)) {
                throw GradleException(
                    "Compose Auto Preview needs the KSP plugin (`$KSP_PLUGIN`) applied to ${project.path}.",
                )
            }
        }

        // Test configurations may be created after this plugin (the KMP library adds them with its host test).
        project.configurations
            .matching { it.name == module.testImplementation }
            .configureEach { configuration ->
                module.testDependencies.forEach { project.dependencies.add(configuration.name, it) }
            }

        project.tasks.register(GENERATE_TEST_TASK, GenerateRenderTestTask::class.java) {
            it.packageName.set(module.namespace)
            // Looked up lazily: AGP registers the unit test task after this task may be realized.
            it.testJavaVersion.set(
                project.provider {
                    (
                        project.tasks.getByName(
                            module.unitTestTask,
                        ) as Test
                    ).javaLauncher.get().metadata.languageVersion.asInt()
                },
            )
            it.outputDir.set(
                project.layout.buildDirectory
                    .dir("generated/autopreview/test"),
            )
        }
        // Preview functions are Android-only, so KMP gets the annotations in the Android source set, not commonMain.
        project.pluginManager.withPlugin(KMP_PLUGIN) {
            val sourceSets = project.extensions
                .getByType(KotlinMultiplatformExtension::class.java)
                .sourceSets
            sourceSets
                .matching { it.name == module.kmpMainSourceSet }
                .configureEach { it.dependencies { implementation(ANNOTATIONS) } }
            sourceSets
                .matching { it.name == module.kmpTestSourceSet }
                .configureEach { it.kotlin.srcDir(project.generatedTestSources()) }
            addProcessor(project, module.kspConfiguration)
        }

        // AGP's lint tasks read the test sources without the task dependency their provider carries: ours and
        // Compose Multiplatform's test resource accessors.
        project.tasks
            .matching { it.name.startsWith("lint") || "Lint" in it.name }
            .configureEach { lint ->
                lint.dependsOn(GENERATE_TEST_TASK)
                lint.dependsOn(
                    project.tasks.matching {
                        it.name.startsWith("generateResourceAccessorsFor") && it.name.endsWith("Test")
                    },
                )
            }

        // The render test also sits in regular unit test runs (skipped), and Robolectric SDK 35+ patches
        // FileDescriptor internals via jdk.internal.access while setting up any test.
        project.tasks.withType(Test::class.java).configureEach {
            it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
        }

        val imagesDir = project.layout.buildDirectory
            .dir("autopreview/images")
        val render = project.tasks.register(RENDER_TASK, Test::class.java) { test ->
            test.group = GROUP
            test.description = "Renders every @AutoPreview screen × device × theme × sample to PNG."
            val unitTest = project.tasks.getByName(module.unitTestTask) as Test
            test.testClassesDirs = unitTest.testClassesDirs
            test.classpath = unitTest.classpath
            test.javaLauncher.set(unitTest.javaLauncher)
            test.useJUnit()
            test.filter.includeTestsMatching("*.$RENDER_TEST")
            test.maxHeapSize = "2g"
            test.jvmArgumentProviders.addAll(unitTest.jvmArgumentProviders)
            test.jvmArgumentProviders.add(OutputDirArgument(imagesDir))
        }

        val report = project.tasks.register("autoPreviewReport", AutoPreviewReportTask::class.java) {
            it.group = GROUP
            it.description = "Builds the HTML app graph from the rendered previews."
            it.dependsOn(render)
            it.imagesDir.set(imagesDir)
            it.reportFile.set(
                project.layout.buildDirectory
                    .file("autopreview/index.html"),
            )
            it.assetsDir.set(
                project.layout.buildDirectory
                    .dir("autopreview/assets"),
            )
        }

        project.tasks.register("autoPreview", ShowReportTask::class.java) {
            it.group = GROUP
            it.description =
                "Renders the full preview matrix, builds an HTML app graph and opens it " +
                "(-PautoPreview.open=false to skip)."
            it.dependsOn(report)
            it.reportFile.set(report.flatMap { task -> task.reportFile })
            // IDE terminals open file:// links in the editor, so the task opens the browser itself; never on CI.
            it.open.set(
                project.providers
                    .gradleProperty("autoPreview.open")
                    .map { value -> value != "false" }
                    .orElse(
                        project.providers
                            .environmentVariable("CI")
                            .map { false },
                    ).orElse(true),
            )
        }
    }
}

private fun Project.generatedTestSources(): Provider<Directory> =
    tasks.named(GENERATE_TEST_TASK, GenerateRenderTestTask::class.java).flatMap { it.outputDir }

// KSP creates its configurations when it (or the target) is set up, possibly after this plugin.
private fun addProcessor(
    project: Project,
    configuration: String,
) {
    project.configurations
        .matching { it.name == configuration }
        .configureEach { project.dependencies.add(it.name, PROCESSOR) }
}

// A provider rather than a system property so the absolute path stays out of the task's cache key.
internal class OutputDirArgument(
    @get:OutputDirectory val dir: Provider<Directory>,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("-Dautopreview.outputDir=${dir.get().asFile.absolutePath}")
}

internal class RegistryPackageArgument(
    @get:Input val packageName: Provider<String>,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("autopreview.registryPackage=${packageName.get()}")
}
