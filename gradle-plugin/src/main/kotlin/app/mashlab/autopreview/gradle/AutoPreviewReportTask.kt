package app.mashlab.autopreview.gradle

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Merges the rendered previews of a module and the modules it depends on into one report: screens from every module,
 * with `navigatesTo` resolved across them.
 */
abstract class AutoPreviewReportTask : DefaultTask() {
    /** Each module's render output: `manifest.json` and the PNGs it lists. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val renders: ConfigurableFileCollection

    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    @get:Optional
    abstract val modules: SetProperty<String>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:OutputDirectory
    abstract val assetsDir: DirectoryProperty

    @get:OutputDirectory
    abstract val imagesDir: DirectoryProperty

    @TaskAction
    fun report() {
        val report = reportFile.get().asFile
        val images = imagesDir
            .get()
            .asFile
            .apply { deleteRecursively() }
        val rendered = renders.files
            .filter { it.resolve("manifest.json").isFile }
            .map { RenderedModule(it) }
            .sortedBy { it.path }
        rendered.forEach { module ->
            module.dir.copyRecursively(images.resolve(module.slug))
            images
                .resolve(module.slug)
                .resolve("manifest.json")
                .delete()
        }
        val data = mapOf(
            "density" to (rendered.firstOrNull()?.density ?: 2),
            "modules" to rendered.map { it.path },
            "screens" to merge(rendered),
        )

        val assets = assetsDir.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }
        val files = listOf("report.css", "report.js", "frames.js").associateWith(::resource) +
            // A script rather than JSON: browsers block fetch() from file:// pages.
            (
                "data.js" to "const DATA = ${JsonOutput.toJson(data)};\n" +
                    "const IMAGES = \"${images.relativeTo(report.parentFile).invariantSeparatorsPath}\";\n"
            )
        files.forEach { (name, text) -> assets.resolve(name).writeText(text) }
        // A content hash in each asset URL, so browsers never pair a new page with cached old scripts.
        report.writeText(
            files.entries.fold(resource("index.html")) { html, (name, text) ->
                html.replace("assets/$name\"", "assets/$name?v=${"%x".format(text.hashCode())}\"")
            },
        )
    }

    private fun merge(rendered: List<RenderedModule>): List<MutableMap<String, Any?>> {
        modules.orNull?.minus(rendered.map { it.path }.toSet())?.takeIf { it.isNotEmpty() }?.let { missing ->
            throw GradleException(
                "--modules: no @AutoPreview screens in ${missing.joinToString()} among the modules " +
                    "${projectPath.get()} depends on. Run it on the module that depends on them all, " +
                    "e.g. ./gradlew :app:autoPreview --modules=${modules.get().joinToString(",")}",
            )
        }
        val screens = rendered.flatMap { module ->
            module.screens.onEach { screen ->
                screen["module"] = module.path
                screen.cells().forEach { it["image"] = "${module.slug}/${it["image"]}" }
            }
        }
        if (screens.isEmpty()) {
            logger.warn(
                "Compose Auto Preview: no @AutoPreview screens in ${projectPath.get()} or the modules it depends " +
                    "on. Modules with previews need the KSP plugin applied next to this one.",
            )
        }
        screens.groupBy { it["id"] }.filterValues { it.size > 1 }.forEach { (id, duplicates) ->
            throw GradleException(
                "@AutoPreview: screen id \"$id\" is used in ${duplicates.joinToString { it["module"].toString() }}. " +
                    "Screen ids must be unique across the modules in one report.",
            )
        }
        // Feature modules may mark their own entry point for their own report; this module's wins in a merged one.
        val entryPoints = screens.filter { it["entryPoint"] == true }
        val entry = entryPoints.firstOrNull { it["module"] == projectPath.get() } ?: entryPoints.firstOrNull()
        if (entryPoints.size > 1 && entry?.get("module") != projectPath.get()) {
            logger.warn(
                "@AutoPreview: several entry points (${entryPoints.joinToString { it["id"].toString() }}), " +
                    "using ${entry?.get("id")}. Mark one in ${projectPath.get()} to choose.",
            )
        }
        entryPoints.forEach { it["entryPoint"] = it === entry }

        // With --modules, links to the modules left out are expected.
        if (modules.isPresent) return screens
        val ids = screens.mapTo(HashSet()) { it["id"] }
        screens.forEach { screen ->
            (screen["navigatesTo"] as List<*>).filterNot { it in ids }.forEach {
                logger.warn("@AutoPreview: ${screen["id"]} navigatesTo unknown screen \"$it\"")
            }
        }
        return screens
    }

    private fun resource(name: String) = requireNotNull(javaClass.getResource("/autopreview/$name")).readText()
}

@Suppress("UNCHECKED_CAST")
private class RenderedModule(
    val dir: File,
) {
    private val manifest = JsonSlurper().parse(dir.resolve("manifest.json")) as Map<String, Any?>
    val path = manifest["module"] as String
    val density = manifest["density"]
    val screens = manifest["screens"] as List<MutableMap<String, Any?>>

    /** Where the module's images go in the report: `:feature:home` → `feature/home`. */
    val slug = path
        .removePrefix(":")
        .replace(':', '/')
        .ifEmpty { "root" }
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.cells() = this["cells"] as List<MutableMap<String, Any?>>

/** Never up to date (no outputs), so the link is printed on every run, not only when the report changed. */
abstract class ShowReportTask : DefaultTask() {
    @get:Internal
    abstract val reportFile: RegularFileProperty

    /** Narrows the report to these modules, e.g. `--modules=:feature:home,:feature:settings`. */
    @get:Input
    @get:Optional
    @get:Option(
        option = "modules",
        description = "Comma-separated project paths, e.g. :feature:home,:feature:settings.",
    )
    abstract val modules: Property<String>

    @get:Input
    abstract val open: Property<Boolean>

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun show() {
        val report = reportFile.get().asFile
        logger.lifecycle("Auto preview report: file://${report.absolutePath}")
        if (!open.get()) return
        val os = System.getProperty("os.name").lowercase()
        val command = when {
            "mac" in os -> listOf("open", report.path)
            "windows" in os -> listOf("cmd", "/c", "start", "", report.path)
            else -> listOf("xdg-open", report.path)
        }
        exec.exec { it.commandLine(command) }
    }
}
