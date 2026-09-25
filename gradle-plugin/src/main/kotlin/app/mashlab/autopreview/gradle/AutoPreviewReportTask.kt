package app.mashlab.autopreview.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class AutoPreviewReportTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val imagesDir: DirectoryProperty

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:OutputDirectory
    abstract val assetsDir: DirectoryProperty

    @TaskAction
    fun report() {
        val report = reportFile.get().asFile
        val assets = assetsDir.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }
        val images = imagesDir.get().asFile
        val files = listOf("report.css", "report.js", "frames.js").associateWith(::resource) +
            // A script rather than JSON: browsers block fetch() from file:// pages.
            ("data.js" to "const DATA = ${images.resolve("manifest.json").readText()};\n" +
                "const IMAGES = \"${images.relativeTo(report.parentFile).invariantSeparatorsPath}\";\n")
        files.forEach { (name, text) -> assets.resolve(name).writeText(text) }
        // A content hash in each asset URL, so browsers never pair a new page with cached old scripts.
        report.writeText(
            files.entries.fold(resource("index.html")) { html, (name, text) ->
                html.replace("assets/$name\"", "assets/$name?v=${"%x".format(text.hashCode())}\"")
            },
        )
    }

    private fun resource(name: String) = requireNotNull(javaClass.getResource("/autopreview/$name")).readText()
}

/** Never up to date (no outputs), so the link is printed on every run, not only when the report changed. */
abstract class ShowReportTask : DefaultTask() {
    @get:Internal
    abstract val reportFile: RegularFileProperty

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
