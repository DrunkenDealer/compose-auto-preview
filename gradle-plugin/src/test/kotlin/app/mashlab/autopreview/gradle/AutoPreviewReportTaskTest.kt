package app.mashlab.autopreview.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoPreviewReportTaskTest {
    @get:Rule
    val dir = TemporaryFolder()

    @Test
    fun `report writes page, assets and data with images relative to the page`() {
        val project = ProjectBuilder
            .builder()
            .withProjectDir(dir.root)
            .build()
        val images = dir.newFolder("build", "autopreview", "images")
        images.resolve("manifest.json").writeText("""{"density":2,"screens":[]}""")
        val task = project.tasks
            .register("report", AutoPreviewReportTask::class.java) {
                it.imagesDir.set(images)
                it.reportFile.set(dir.root.resolve("build/autopreview/index.html"))
                it.assetsDir.set(dir.root.resolve("build/autopreview/assets"))
            }.get()

        task.report()

        val out = dir.root.resolve("build/autopreview")
        assertContains(out.resolve("index.html").readText(), """<script src="assets/report.js"></script>""")
        assertTrue(out.resolve("assets/report.css").isFile)
        assertTrue(out.resolve("assets/report.js").isFile)
        assertTrue(out.resolve("assets/frames.js").isFile)
        assertEquals(
            "const DATA = {\"density\":2,\"screens\":[]};\nconst IMAGES = \"images\";\n",
            out.resolve("assets/data.js").readText(),
        )
    }
}
