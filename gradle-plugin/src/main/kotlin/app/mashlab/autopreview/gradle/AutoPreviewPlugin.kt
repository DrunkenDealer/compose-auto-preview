package app.mashlab.autopreview.gradle

import com.android.build.api.dsl.CommonExtension
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
private const val UNIT_TEST_TASK = "testDebugUnitTest"
private const val RENDER_TASK = "autoPreviewRender"
private const val KSP_PLUGIN = "com.google.devtools.ksp"
private const val KMP_PLUGIN = "org.jetbrains.kotlin.multiplatform"
private const val KOTLIN_ANDROID_PLUGIN = "org.jetbrains.kotlin.android"
private const val ANNOTATIONS = "app.mashlab:compose-auto-preview-annotations:${Versions.AUTO_PREVIEW}"
private const val PROCESSOR = "app.mashlab:compose-auto-preview-processor:${Versions.AUTO_PREVIEW}"

class AutoPreviewPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.withPlugin("com.android.application") { configure(project) }
        project.pluginManager.withPlugin("com.android.library") { configure(project) }
    }

    private fun configure(project: Project) {
        val android = project.extensions.getByType(CommonExtension::class.java)
        android.testOptions.unitTests.isIncludeAndroidResources = true
        val registryPackage = project.provider {
            requireNotNull(android.namespace) { "Compose Auto Preview needs `android.namespace` to be set." }
        }

        project.pluginManager.withPlugin(KSP_PLUGIN) {
            project.extensions.getByType(KspExtension::class.java).arg(RegistryPackageArgument(registryPackage))
        }
        project.afterEvaluate {
            if (!project.pluginManager.hasPlugin(KSP_PLUGIN)) {
                throw GradleException("Compose Auto Preview needs the KSP plugin (`$KSP_PLUGIN`) applied to ${project.path}.")
            }
        }
        addLibraryDependencies(project)

        listOf(
            "org.robolectric:robolectric:${Versions.ROBOLECTRIC}",
            "junit:junit:${Versions.JUNIT}",
        ).forEach { project.dependencies.add("testImplementation", it) }

        val generateTest = project.tasks.register("generateAutoPreviewRenderTest", GenerateRenderTestTask::class.java) {
            it.packageName.set(registryPackage)
            // Looked up lazily: AGP registers the unit test task after this task may be realized.
            it.testJavaVersion.set(
                project.provider {
                    (project.tasks.getByName(UNIT_TEST_TASK) as Test).javaLauncher.get().metadata.languageVersion.asInt()
                }
            )
            it.outputDir.set(project.layout.buildDirectory.dir("generated/autopreview/test"))
        }
        val generatedSources = generateTest.flatMap { it.outputDir }
        project.pluginManager.withPlugin(KMP_PLUGIN) {
            project.extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets
                .matching { it.name == "androidUnitTest" }
                .configureEach { it.kotlin.srcDir(generatedSources) }
        }
        project.pluginManager.withPlugin(KOTLIN_ANDROID_PLUGIN) {
            android.sourceSets.getByName("test").kotlin.srcDir(generatedSources)
        }

        // The render test also sits in regular unit test runs (skipped), and Robolectric SDK 35+ patches
        // FileDescriptor internals via jdk.internal.access while setting up any test.
        project.tasks.withType(Test::class.java).configureEach {
            it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
        }

        val imagesDir = project.layout.buildDirectory.dir("autopreview/images")
        val render = project.tasks.register(RENDER_TASK, Test::class.java) { test ->
            test.group = GROUP
            test.description = "Renders every @AutoPreview screen × device × theme × sample to PNG."
            val unitTest = project.tasks.getByName(UNIT_TEST_TASK) as Test
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
            it.reportFile.set(project.layout.buildDirectory.file("autopreview/index.html"))
            it.assetsDir.set(project.layout.buildDirectory.dir("autopreview/assets"))
        }

        project.tasks.register("autoPreview", ShowReportTask::class.java) {
            it.group = GROUP
            it.description = "Renders the full preview matrix, builds an HTML app graph and opens it (-PautoPreview.open=false to skip)."
            it.dependsOn(report)
            it.reportFile.set(report.flatMap { task -> task.reportFile })
            // IDE terminals open file:// links in the editor, so the task opens the browser itself; never on CI.
            it.open.set(
                project.providers.gradleProperty("autoPreview.open").map { value -> value != "false" }
                    .orElse(project.providers.environmentVariable("CI").map { false })
                    .orElse(true)
            )
        }
    }
}

private fun addLibraryDependencies(project: Project) {
    // Preview functions are Android-only, so KMP gets the annotations in androidMain, not commonMain.
    project.pluginManager.withPlugin(KMP_PLUGIN) {
        project.extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets
            .matching { it.name == "androidMain" }
            .configureEach { it.dependencies { implementation(ANNOTATIONS) } }
        addProcessor(project, "kspAndroid")
    }
    project.pluginManager.withPlugin(KOTLIN_ANDROID_PLUGIN) {
        project.dependencies.add("implementation", ANNOTATIONS)
        addProcessor(project, "ksp")
    }
}

// KSP creates its configurations when it (or the target) is set up, possibly after this plugin.
private fun addProcessor(project: Project, configuration: String) {
    project.configurations.matching { it.name == configuration }
        .configureEach { project.dependencies.add(it.name, PROCESSOR) }
}

// A provider rather than a system property so the absolute path stays out of the task's cache key.
internal class OutputDirArgument(
    @get:OutputDirectory val dir: Provider<Directory>,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("-Dautopreview.outputDir=${dir.get().asFile.absolutePath}")
}

internal class RegistryPackageArgument(@get:Input val packageName: Provider<String>) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("autopreview.registryPackage=${packageName.get()}")
}
