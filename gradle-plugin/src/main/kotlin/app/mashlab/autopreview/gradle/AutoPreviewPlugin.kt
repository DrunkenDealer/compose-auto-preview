package app.mashlab.autopreview.gradle

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.util.concurrent.Callable

private const val GROUP = "autopreview"
private const val RENDER_TEST = "AutoPreviewRenderTest"
private const val RENDER_TASK = "autoPreviewRender"
private const val GENERATE_TEST_TASK = "generateAutoPreviewRenderTest"
private const val SHOW_TASK = "autoPreview"
private const val ELEMENTS = "autoPreviewElements"
private const val KSP_PLUGIN = "com.google.devtools.ksp"
private const val KMP_PLUGIN = "org.jetbrains.kotlin.multiplatform"
private const val KOTLIN_ANDROID_PLUGIN = "org.jetbrains.kotlin.android"
private const val KMP_LIBRARY_PLUGIN = "com.android.kotlin.multiplatform.library"
private const val ANNOTATIONS = "app.mashlab.autopreview:annotations:${Versions.AUTO_PREVIEW}"
private const val PROCESSOR = "app.mashlab.autopreview:processor:${Versions.AUTO_PREVIEW}"

internal val TEST_DEPENDENCIES = listOf(
    "org.robolectric:robolectric:${Versions.ROBOLECTRIC}",
    "junit:junit:${Versions.JUNIT}",
    // The render test hosts previews in a ComponentActivity, which a library needn't depend on. The oldest version
    // that works keeps the compileSdk floor low (34); Gradle still resolves the module's own, newer one.
    "androidx.activity:activity-compose:1.9.3",
)

/** What differs between the Android plugins; [configure] wires everything else the same way. */
@Suppress("LongParameterList")
internal class AndroidModule(
    val namespace: Provider<String>,
    val unitTestTask: String,
    val testImplementation: String,
    val testDependencies: List<String>,
    val kmpMainSourceSet: String,
    val kmpTestSourceSet: String,
    val kspConfiguration: String,
    val runtimeClasspath: String,
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
            runtimeClasspath = "debugRuntimeClasspath",
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
            if (!project.pluginManager.hasPlugin(KMP_PLUGIN) && project.pluginManager.hasPlugin(KSP_PLUGIN)) {
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
        // Preview functions are Android-only, so KMP gets the annotations in the Android source set, not commonMain.
        project.pluginManager.withPlugin(KMP_PLUGIN) {
            project.extensions
                .getByType(KotlinMultiplatformExtension::class.java)
                .sourceSets
                .matching { it.name == module.kmpMainSourceSet }
                .configureEach { it.dependencies { implementation(ANNOTATIONS) } }
        }
        val images = project.layout.buildDirectory
            .dir("intermediates/autopreview/images")
        val elements = project.configurations.create(ELEMENTS) {
            it.isCanBeConsumed = true
            it.isCanBeResolved = false
            it.attributes.previewImages(project)
        }
        // Without KSP a module renders nothing of its own, and only merges the modules it depends on (an app module).
        project.pluginManager.withPlugin(KSP_PLUGIN) {
            val render = configureRender(project, module, images)
            elements.outgoing.artifact(images) { it.builtBy(render) }
        }
        configureReport(project, module, images)
    }

    private fun configureRender(
        project: Project,
        module: AndroidModule,
        images: Provider<Directory>,
    ): TaskProvider<Test> {
        project.extensions
            .getByType(KspExtension::class.java)
            .arg(RegistryPackageArgument(module.namespace))

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
        project.pluginManager.withPlugin(KMP_PLUGIN) {
            project.extensions
                .getByType(KotlinMultiplatformExtension::class.java)
                .sourceSets
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

        return project.tasks.register(RENDER_TASK, Test::class.java) { test ->
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
            test.jvmArgumentProviders.add(RenderArguments(project.path, images))
        }
    }

    private fun configureReport(
        project: Project,
        module: AndroidModule,
        images: Provider<Directory>,
    ) {
        val show = project.tasks.register(SHOW_TASK, ShowReportTask::class.java)
        val modules = show.flatMap { it.modules }.map(::parseModules)
        val report = project.tasks.register("autoPreviewReport", AutoPreviewReportTask::class.java) { task ->
            task.group = GROUP
            task.description =
                "Builds the HTML app graph from the rendered previews of this module and the modules it depends on."
            task.projectPath.set(project.path)
            task.modules.set(modules)
            // Every dependency module's rendered images, re-selected off the runtime classpath (so Android and KMP
            // variant attributes still apply) and skipping modules without the plugin. Resolved as late as the task
            // graph, after `--modules` is set, so modules filtered out never render.
            task.renders.from(
                Callable {
                    project.configurations
                        .getByName(module.runtimeClasspath)
                        .incoming
                        .artifactView { view ->
                            view.withVariantReselection()
                            view.lenient(true)
                            view.attributes.previewImages(project)
                            view.componentFilter {
                                it is ProjectComponentIdentifier && modules.orNull.includes(it.projectPath)
                            }
                        }.files
                },
                Callable {
                    if (project.pluginManager.hasPlugin(KSP_PLUGIN) && modules.orNull.includes(project.path)) {
                        project.files(images).builtBy(RENDER_TASK)
                    } else {
                        project.files()
                    }
                },
            )
            task.reportFile.set(
                project.layout.buildDirectory
                    .file("autopreview/index.html"),
            )
            task.assetsDir.set(
                project.layout.buildDirectory
                    .dir("autopreview/assets"),
            )
            task.imagesDir.set(
                project.layout.buildDirectory
                    .dir("autopreview/images"),
            )
        }

        show.configure {
            it.group = GROUP
            it.description =
                "Renders the full preview matrix of this module and every module it depends on, builds an HTML " +
                "app graph and opens it (--modules=:a,:b to narrow, -PautoPreview.open=false to skip opening)."
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
internal class RenderArguments(
    @get:Input val projectPath: String,
    @get:OutputDirectory val dir: Provider<Directory>,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> =
        listOf(
            "-Dautopreview.outputDir=${dir.get().asFile.absolutePath}",
            "-Dautopreview.module=$projectPath",
        )
}

/** `--modules` → project paths to include (a leading `:` is optional); without it, every module is included. */
internal fun parseModules(value: String): Set<String> =
    value
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .mapTo(LinkedHashSet()) { if (it.startsWith(":")) it else ":$it" }

internal fun Set<String>?.includes(path: String) = this == null || path in this

// A custom usage keeps every other variant (jars, AARs, lint) out: an attribute a variant lacks would still match.
private fun AttributeContainer.previewImages(project: Project) {
    attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "app.mashlab.autopreview"))
    attribute(ARTIFACT_ATTRIBUTE, "images")
}

private val ARTIFACT_ATTRIBUTE = Attribute.of("app.mashlab.autopreview.artifact", String::class.java)

internal class RegistryPackageArgument(
    @get:Input val packageName: Provider<String>,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("autopreview.registryPackage=${packageName.get()}")
}
