package app.mashlab.autopreview.processor

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.kspWithCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class RenderRegistryTest {
    @Test
    fun `registry is generated for a module without screens`() {
        val (result, registry) = compile(SourceFile.kotlin("Empty.kt", "package app\n\nclass Empty"))

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        assertContains(registry, Regex("""screens: List<AutoPreviewScreen> = listOf\(\s*\)"""))
    }

    @Test
    fun `no registry when one is already visible, as in a unit test compilation`() {
        val (result, registry) = compile(SourceFile.kotlin("Main.kt", "package app\n\nobject AutoPreviewRegistry"))

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        assertEquals("", registry)
    }

    @Test
    fun `registry carries entry point, navigation and group`() {
        val (result, registry) = compile(
            screen("app", "Home", "entryPoint = true, navigatesTo = [\"Details\", \"Missing\"]"),
            screen("app", "Details", "group = \"Tabs\""),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        assertContains(registry, "id = \"Home\",")
        assertContains(
            registry,
            Regex("""navigatesTo = listOf\("Details", "Missing"\),\s*entryPoint = true,\s*group = null,"""),
        )
        assertContains(registry, Regex("""entryPoint = false,\s*group = "Tabs","""))
    }

    @Test
    fun `more than one entry point is an error`() {
        val (result) = compile(
            screen("app", "Home", "entryPoint = true"),
            screen("app", "Details", "entryPoint = true"),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "only one screen can be the entry point, found Details, Home")
    }

    @Test
    fun `duplicate screen ids are an error`() {
        val (result) = compile(screen("app.a", "Home"), screen("app.b", "Home"))

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "screen id \"Home\" is used by app.a.HomePreview, app.b.HomePreview")
    }

    @Test
    fun `locales keep their order without duplicates and multiply the preview matrix`() {
        val (result, registry) = compile(
            screen(
                "app",
                "Home",
                "locales = [\"en\", \"de\", \"en\", \"uk\"], devices = [Device.Phone, Device.Tablet]",
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        assertContains(registry, "locales = listOf(\"en\", \"de\", \"uk\"),")
        assertEquals(1, Regex("""AutoPreviewScreen\(\s*id = """).findAll(registry).count())
        val previews = generated(result, "HomeAutoPreviews.kt")
        assertEquals(3 * 2 * 2, Regex("""@Preview\(""").findAll(previews).count())
        assertContains(previews, "name = \"uk · Tablet · Dark\"")
    }

    @Test
    fun `the deprecated locale still works and locales wins over it`() {
        val (result, registry) = compile(
            screen("app", "Home", "locale = \"uk\""),
            screen("app", "Details", "locale = \"uk\", locales = [\"de\", \"fr\"]"),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        assertContains(registry, Regex("""id = "Home",\s*locales = listOf\("uk"\),"""))
        assertContains(registry, Regex("""id = "Details",\s*locales = listOf\("de", "fr"\),"""))
    }

    @Test
    fun `a wrapper declares locales and the usage site overrides them`() {
        val wrapper = SourceFile.kotlin(
            "app/AppPreview.kt",
            """
            package app

            import app.mashlab.autopreview.annotations.AutoPreview
            import kotlin.reflect.KClass

            @AutoPreview(samplesFrom = Unit::class, locales = ["en", "de"])
            annotation class AppPreview(val samplesFrom: KClass<*>, val locales: Array<String> = ["en", "de"])
            """.trimIndent(),
        )
        val (result, registry) = compile(
            wrapper,
            screen("app", "Home", annotation = "AppPreview"),
            screen("app", "Details", "locales = [\"uk\"]", annotation = "AppPreview"),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        assertContains(registry, Regex("""id = "Home",\s*locales = listOf\("en", "de"\),"""))
        assertContains(registry, Regex("""id = "Details",\s*locales = listOf\("uk"\),"""))
    }

    @Test
    fun `a direct and a wrapper annotation on one function are an error`() {
        val wrapper = SourceFile.kotlin(
            "app/AppPreview.kt",
            """
            package app

            import app.mashlab.autopreview.annotations.AutoPreview
            import kotlin.reflect.KClass

            @AutoPreview(samplesFrom = Unit::class)
            annotation class AppPreview(val samplesFrom: KClass<*>)
            """.trimIndent(),
        )
        val (result) = compile(
            wrapper,
            screen(
                "app",
                "Home",
                annotation = "AppPreview",
                extraAnnotation = "@AutoPreview(samplesFrom = HomeSamples::class)",
            ),
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "HomePreview has 2 @AutoPreview annotations")
    }

    private fun screen(
        pkg: String,
        name: String,
        extraArgs: String = "",
        annotation: String = "AutoPreview",
        extraAnnotation: String = "",
    ) = SourceFile.kotlin(
        "${pkg.replace('.', '/')}/$name.kt",
        """
        package $pkg

        import androidx.compose.runtime.Composable
        import app.mashlab.autopreview.annotations.AutoPreview
        import app.mashlab.autopreview.annotations.Device

        data class ${name}State(val title: String)

        object ${name}Samples {
            val Default: ${name}State = ${name}State("$name")
        }

        $extraAnnotation
        @$annotation(samplesFrom = ${name}Samples::class, $extraArgs)
        @Composable
        internal fun ${name}Preview(state: ${name}State) {}
        """.trimIndent(),
    )

    private fun generated(
        result: JvmCompilationResult,
        name: String,
    ): String =
        result.outputDirectory.parentFile
            .walk()
            .first { it.name == name }
            .readText()

    private fun compile(vararg sources: SourceFile): Pair<JvmCompilationResult, String> {
        val compilation = KotlinCompilation().apply {
            this.sources = COMPOSE_STUBS + sources
            inheritClassPath = true
            languageVersion = "1.9" // KSP1 runs on the K1 frontend.
            kspWithCompilation = true
            configureKsp(useKsp2 = false) {
                symbolProcessorProviders += AutoPreviewProcessorProvider()
                processorOptions[REGISTRY_PACKAGE_OPTION] = "app"
            }
        }
        val result = compilation.compile()
        val registry = compilation.kspSourcesDir
            .walk()
            .firstOrNull {
                it.name == "AutoPreviewRegistry.kt"
            }?.readText()
            .orEmpty()
        return result to registry
    }

    private companion object {
        val COMPOSE_STUBS = listOf(
            SourceFile.kotlin(
                "Composable.kt",
                "package androidx.compose.runtime\n\n@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)\nannotation class Composable",
            ),
            SourceFile.kotlin(
                "Preview.kt",
                """
                package androidx.compose.ui.tooling.preview

                @Repeatable
                annotation class Preview(
                    val name: String = "",
                    val locale: String = "",
                    val device: String = "",
                    val uiMode: Int = 0,
                    val showBackground: Boolean = false,
                    val backgroundColor: Long = 0,
                    val showSystemUi: Boolean = false,
                )

                interface PreviewParameterProvider<T> {
                    val values: Sequence<T>
                }
                """.trimIndent(),
            ),
            SourceFile.kotlin(
                "Configuration.kt",
                "package android.content.res\n\nclass Configuration {\n    companion object {\n        const val UI_MODE_NIGHT_YES = 32\n    }\n}",
            ),
        )
    }
}
