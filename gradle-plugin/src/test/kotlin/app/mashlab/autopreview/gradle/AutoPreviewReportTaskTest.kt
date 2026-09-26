package app.mashlab.autopreview.gradle

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AutoPreviewReportTaskTest {
    @get:Rule
    val dir = TemporaryFolder()

    private val out get() = dir.root.resolve("build/autopreview")

    @Test
    fun `report writes page, assets and data with images relative to the page`() {
        report(render(":app"))

        assertContains(
            out.resolve("index.html").readText(),
            Regex("""<script src="assets/report\.js\?v=[0-9a-f]+"></script>"""),
        )
        assertTrue(out.resolve("assets/report.css").isFile)
        assertTrue(out.resolve("assets/report.js").isFile)
        assertTrue(out.resolve("assets/frames.js").isFile)
        assertEquals(
            "const DATA = {\"density\":2,\"modules\":[\":app\"],\"screens\":[]};\nconst IMAGES = \"images\";\n",
            out.resolve("assets/data.js").readText(),
        )
    }

    @Test
    fun `screens from every module are merged with images under the module path`() {
        report(
            render(":app", "Home", navigatesTo = mapOf("Home" to "Settings")),
            render(":feature:settings", "Settings"),
        )

        val data = out.resolve("assets/data.js").readText()
        assertContains(data, "\"modules\":[\":app\",\":feature:settings\"]")
        assertContains(data, "\"image\":\"feature/settings/Settings/Phone/Light/Default.png\"")
        assertContains(data, "\"module\":\":feature:settings\"")
        assertTrue(out.resolve("images/feature/settings/Settings/Phone/Light/Default.png").isFile)
    }

    @Test
    fun `the entry point of the module the report runs on wins`() {
        report(
            render(":app", "Home", entryPoint = "Home"),
            render(":feature:settings", "Settings", entryPoint = "Settings"),
        )

        val data = out.resolve("assets/data.js").readText()
        assertContains(data, "\"id\":\"Home\",\"navigatesTo\":[],\"entryPoint\":true")
        assertContains(data, "\"id\":\"Settings\",\"navigatesTo\":[],\"entryPoint\":false")
    }

    @Test
    fun `a screen id in two modules fails`() {
        val error = assertFailsWith<GradleException> {
            report(
                render(":feature:a", "Home"),
                render(":feature:b", "Home"),
            )
        }
        assertContains(error.message.orEmpty(), "\"Home\" is used in :feature:a, :feature:b")
    }

    @Test
    fun `a requested module without screens fails`() {
        val error = assertFailsWith<GradleException> {
            report(render(":app"), modules = setOf(":app", ":feature:missing"))
        }
        assertContains(error.message.orEmpty(), "no @AutoPreview screens in :feature:missing among the modules :app")
    }

    @Test
    fun `modules parse with or without the leading colon`() {
        assertEquals(setOf(":feature:home", ":app"), parseModules(" feature:home, :app ,"))
    }

    private fun report(
        vararg renders: File,
        modules: Set<String>? = null,
    ) {
        val project = ProjectBuilder
            .builder()
            .withProjectDir(dir.root)
            .build()
        project.tasks
            .register("report", AutoPreviewReportTask::class.java) {
                it.renders.from(renders)
                it.projectPath.set(":app")
                it.modules.set(modules)
                it.reportFile.set(out.resolve("index.html"))
                it.assetsDir.set(out.resolve("assets"))
                it.imagesDir.set(out.resolve("images"))
            }.get()
            .report()
    }

    /** A module's render output: its manifest and one image per screen. */
    private fun render(
        module: String,
        vararg ids: String,
        navigatesTo: Map<String, String> = emptyMap(),
        entryPoint: String? = null,
    ): File {
        val render = dir.newFolder(module.replace(':', '_'))
        val screens = ids.map { id ->
            render
                .resolve("$id/Phone/Light/Default.png")
                .apply { parentFile.mkdirs() }
                .writeText("png")
            """{"id":"$id","navigatesTo":[${navigatesTo[id]?.let { "\"$it\"" }.orEmpty()}],""" +
                """"entryPoint":${id == entryPoint},"cells":[{"device":"Phone","theme":"Light","sample":"Default",""" +
                """"image":"$id/Phone/Light/Default.png"}]}"""
        }
        render.resolve("manifest.json").writeText(
            """{"density":2,"module":"$module","screens":[${screens.joinToString(",")}]}""",
        )
        return render
    }
}
