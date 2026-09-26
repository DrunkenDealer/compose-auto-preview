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

    private fun screen(
        pkg: String,
        name: String,
        extraArgs: String = "",
    ) = SourceFile.kotlin(
        "${pkg.replace('.', '/')}/$name.kt",
        """
        package $pkg

        import androidx.compose.runtime.Composable
        import app.mashlab.autopreview.annotations.AutoPreview

        data class ${name}State(val title: String)

        object ${name}Samples {
            val Default: ${name}State = ${name}State("$name")
        }

        @AutoPreview(samplesFrom = ${name}Samples::class, $extraArgs)
        @Composable
        internal fun ${name}Preview(state: ${name}State) {}
        """.trimIndent(),
    )

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
