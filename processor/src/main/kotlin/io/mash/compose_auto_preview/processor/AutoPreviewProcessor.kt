package io.mash.compose_auto_preview.processor

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

private const val AUTO_PREVIEW_FQN = "io.mash.compose_auto_preview.annotations.AutoPreview"
private const val COMPOSABLE_FQN = "androidx.compose.runtime.Composable"
private const val PREVIEW_PARAMETER_FQN = "androidx.compose.ui.tooling.preview.PreviewParameter"

private val PREVIEW_PARAMETER_PROVIDER =
    ClassName("androidx.compose.ui.tooling.preview", "PreviewParameterProvider")
private val SEQUENCE = ClassName("kotlin.sequences", "Sequence")
private val SEQUENCE_OF = MemberName("kotlin.sequences", "sequenceOf")

class AutoPreviewProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val tagged = resolver.getSymbolsWithAnnotation(AUTO_PREVIEW_FQN).toList()

        val direct: List<Pair<KSFunctionDeclaration, AutoPreviewArgs>> = tagged
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull { fn ->
                val ann = fn.annotations.firstOrNull { it.fqn == AUTO_PREVIEW_FQN } ?: return@mapNotNull null
                fn to AutoPreviewArgs.from(ann)
            }

        val viaMeta: List<Pair<KSFunctionDeclaration, AutoPreviewArgs>> = tagged
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.ANNOTATION_CLASS }
            .flatMap { metaClass ->
                val metaFqn = metaClass.qualifiedName?.asString() ?: return@flatMap emptyList()
                val baseAnn = metaClass.annotations.firstOrNull { it.fqn == AUTO_PREVIEW_FQN }
                    ?: return@flatMap emptyList()
                val baseArgs = baseAnn.argsMap()
                val overridable = metaClass.primaryConstructor?.parameters
                    ?.mapNotNull { it.name?.asString() }
                    ?.toSet()
                    .orEmpty()

                resolver.getSymbolsWithAnnotation(metaFqn)
                    .filterIsInstance<KSFunctionDeclaration>()
                    .mapNotNull { fn ->
                        val usage = fn.annotations.firstOrNull { it.fqn == metaFqn } ?: return@mapNotNull null
                        val merged = baseArgs.toMutableMap().apply {
                            usage.argsMap().forEach { (name, value) -> if (name in overridable) this[name] = value }
                        }
                        fn to AutoPreviewArgs.fromArgs(merged)
                    }
                    .toList()
            }

        val all = direct + viaMeta
        val byFile = all.groupBy { it.first.containingFile }
        byFile.forEach { (file, group) ->
            if (file != null && group.size > 1) {
                group.forEach { (fn, _) ->
                    logger.error(
                        "Multiple @AutoPreview-annotated functions in ${file.fileName}. " +
                            "Generated names are derived from the file; rename the file or split into separate files.",
                        fn
                    )
                }
            }
        }

        byFile.values.filter { it.size == 1 }.flatten().forEach { (fn, args) -> processFunction(fn, args) }
        return emptyList()
    }

    private fun processFunction(fn: KSFunctionDeclaration, args: AutoPreviewArgs) {
        val file = fn.containingFile ?: return
        if (!fn.hasAnnotation(COMPOSABLE_FQN)) {
            logger.error("@AutoPreview can only be applied to @Composable functions.", fn)
            return
        }

        val previewParams = fn.parameters.filter { it.hasAnnotation(PREVIEW_PARAMETER_FQN) }
        if (previewParams.size != 1) {
            logger.error(
                "@AutoPreview-annotated function must declare exactly one @PreviewParameter parameter (found ${previewParams.size}).",
                fn
            )
            return
        }
        val stateType: KSType = previewParams.single().type.resolve()

        val samplesClass = args.samplesType.declaration as? KSClassDeclaration ?: run {
            logger.error("samples must reference a class or object.", fn)
            return
        }

        val companion = samplesClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.isCompanionObject && it.simpleName.asString() == "Previews" }
        val source = companion ?: samplesClass

        val stateSimpleName = stateType.declaration.simpleName.asString()
        if (companion == null && samplesClass.classKind != ClassKind.OBJECT) {
            logger.error(
                "samples must reference either a class with a `companion object Previews { ... }`, " +
                    "or an `object` declaring sample vals directly. " +
                    "${samplesClass.qualifiedName?.asString()} is neither.",
                fn
            )
            return
        }

        val (sourceQualifier, samples) = collectSamples(source, stateType)
        if (samples.isEmpty()) {
            logger.error(
                "No samples of type $stateSimpleName found in ${source.qualifiedName?.asString()}. " +
                    "Declare public vals like `val Foo: $stateSimpleName = ...`. " +
                    "For sealed/abstract states, the explicit parent type is required.",
                fn
            )
            return
        }

        val fileSimpleName = file.fileName.removeSuffix(".kt")
        val packageName = fn.packageName.asString()
        val providerClassName = ClassName(packageName, "${fileSimpleName}Samples")
        val multiPreviewClassName = ClassName(packageName, "${fileSimpleName}Previews")
        val stateTypeName = stateType.toTypeName()

        val sequenceOfArgs = samples.joinToString(separator = ",\n    ", prefix = "\n    ", postfix = ",\n") {
            "$sourceQualifier.$it"
        }

        val providerSpec = TypeSpec.classBuilder(providerClassName)
            .addSuperinterface(PREVIEW_PARAMETER_PROVIDER.parameterizedBy(stateTypeName))
            .addProperty(
                PropertySpec.builder("values", SEQUENCE.parameterizedBy(stateTypeName))
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%M($sequenceOfArgs)", SEQUENCE_OF)
                    .build()
            )
            .build()

        val multiPreviewSpec = TypeSpec.annotationBuilder(multiPreviewClassName)
            .also { spec -> PreviewMatrix.build(args).forEach(spec::addAnnotation) }
            .build()

        FileSpec.builder(packageName, multiPreviewClassName.simpleName)
            .addType(providerSpec)
            .addType(multiPreviewSpec)
            .build()
            .writeTo(codeGenerator, aggregating = false, originatingKSFiles = listOf(file))
    }

    private fun collectSamples(
        source: KSClassDeclaration,
        stateType: KSType,
    ): Pair<String, List<String>> {
        val sourceQualifier = source.qualifiedName?.asString() ?: source.simpleName.asString()
        val matches = source.getDeclaredProperties()
            .filter { it.isPublic() && !it.isMutable }
            .filter { stateType.isAssignableFrom(it.type.resolve()) }
            .map { it.simpleName.asString() }
            .toList()
        return sourceQualifier to matches
    }
}

private fun KSAnnotated.hasAnnotation(fqn: String): Boolean =
    annotations.any { it.fqn == fqn }

private val KSAnnotation.fqn: String
    get() = annotationType.resolve().declaration.qualifiedName?.asString().orEmpty()
