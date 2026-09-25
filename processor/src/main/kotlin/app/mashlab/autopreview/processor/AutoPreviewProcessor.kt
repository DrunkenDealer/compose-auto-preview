package app.mashlab.autopreview.processor

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
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
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

private const val AUTO_PREVIEW_FQN = "app.mashlab.autopreview.annotations.AutoPreview"
private const val COMPOSABLE_FQN = "androidx.compose.runtime.Composable"

private val PREVIEW_PARAMETER_PROVIDER =
    ClassName("androidx.compose.ui.tooling.preview", "PreviewParameterProvider")
private val SEQUENCE = ClassName("kotlin.sequences", "Sequence")

// Annotations that can appear on @Composable functions but are never @AutoPreview wrappers.
// Filtering by short name avoids the expensive annotationType.resolve() call.
private val NON_WRAPPER_SHORT_NAMES = setOf(
    "AutoPreview",
    "Composable",
    "Preview",
    "PreviewParameter",
    "NonRestartableComposable",
    "ReadOnlyComposable",
    "DisallowComposableCalls",
    "Stable",
    "Immutable",
    "Suppress",
    "OptIn",
    "Deprecated",
    "JvmStatic",
    "JvmOverloads",
    "JvmName",
)

class AutoPreviewProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val registryPackage: String?,
) : SymbolProcessor {
    private val registryEntries = mutableListOf<RegistryEntry>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val direct: List<Pair<KSFunctionDeclaration, AutoPreviewArgs>> = resolver
            .getSymbolsWithAnnotation(AUTO_PREVIEW_FQN)
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull { fn ->
                val ann = fn.annotations.firstOrNull { it.fqn == AUTO_PREVIEW_FQN } ?: return@mapNotNull null
                fn to AutoPreviewArgs.from(ann)
            }.toList()

        // Scan @Composable functions in the current module for wrapper annotations meta-annotated with
        // @AutoPreview. Wrappers can be declared in other modules, so we still resolve the annotation
        // type from the usage — but only after a cheap short-name filter, and we memoize the wrapper
        // lookup per round so a wrapper used N times costs one resolve, not N.
        val wrapperCache = HashMap<KSClassDeclaration, WrapperInfo?>()
        val viaMeta: List<Pair<KSFunctionDeclaration, AutoPreviewArgs>> = resolver
            .getSymbolsWithAnnotation(COMPOSABLE_FQN)
            .filterIsInstance<KSFunctionDeclaration>()
            .flatMap { fn ->
                fn.annotations.mapNotNull { usage ->
                    if (usage.shortName.asString() in NON_WRAPPER_SHORT_NAMES) return@mapNotNull null
                    val metaClass = usage.annotationType
                        .resolve()
                        .declaration as? KSClassDeclaration
                        ?: return@mapNotNull null
                    if (metaClass.classKind != ClassKind.ANNOTATION_CLASS) return@mapNotNull null
                    val info = wrapperCache.getOrPut(metaClass) { WrapperInfo.from(metaClass) }
                        ?: return@mapNotNull null
                    val merged = info.baseArgs.toMutableMap().apply {
                        usage.argsMap().forEach { (name, value) -> if (name in info.overridable) this[name] = value }
                    }
                    fn to AutoPreviewArgs.fromArgs(merged)
                }
            }.toList()

        (direct + viaMeta).forEach { (fn, args) -> processFunction(fn, args) }
        return emptyList()
    }

    override fun finish() {
        val entryPoints = registryEntries.filter { it.args.entryPoint }
        if (entryPoints.size > 1) {
            logger.error(
                "@AutoPreview: only one screen can be the entry point, found ${entryPoints.joinToString { it.id }}",
            )
        }
        // Only with the Gradle plugin is every screen reprocessed on each run (the registry is aggregating),
        // so cross-screen checks are reliable. The registry is written even when empty: the render test uses it.
        if (registryPackage == null) return
        registryEntries.groupBy { it.id }.filterValues { it.size > 1 }.forEach { (id, entries) ->
            logger.error(
                "@AutoPreview: screen id \"$id\" is used by ${entries.joinToString {
                    it.previewFunction.canonicalName
                }}",
            )
        }
        val ids = registryEntries.mapTo(HashSet()) { it.id }
        registryEntries.forEach { entry ->
            (entry.args.navigatesTo - ids).forEach {
                logger.warn(
                    "@AutoPreview: ${entry.id} navigatesTo unknown screen \"$it\"",
                )
            }
        }
        RenderRegistry.write(codeGenerator, registryPackage, registryEntries)
    }

    @Suppress("ReturnCount")
    private fun processFunction(
        fn: KSFunctionDeclaration,
        args: AutoPreviewArgs,
    ) {
        val file = fn.containingFile ?: return
        if (!fn.hasAnnotation(COMPOSABLE_FQN)) {
            logger.error("@AutoPreview can only be applied to @Composable functions.", fn)
            return
        }

        val visibility = fn.getVisibility()
        if (visibility != Visibility.INTERNAL && visibility != Visibility.PUBLIC) {
            logger.error(
                "@AutoPreview function must be `internal` or `public`.",
                fn,
            )
            return
        }

        val valueParams = fn.parameters
        if (valueParams.size != 1) {
            logger.error(
                "@AutoPreview function must declare exactly one value parameter (the state), " +
                    "found ${valueParams.size}.",
                fn,
            )
            return
        }
        val stateType: KSType = valueParams
            .single()
            .type
            .resolve()

        val samplesClass = args.samplesType.declaration as? KSClassDeclaration ?: run {
            logger.error("samplesFrom must reference a class or object.", fn)
            return
        }

        val companion = samplesClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.isCompanionObject && it.simpleName.asString() == "Previews" }
        val source = companion ?: samplesClass

        val stateSimpleName = stateType.declaration.simpleName
            .asString()
        if (companion == null && samplesClass.classKind != ClassKind.OBJECT) {
            logger.error(
                "samplesFrom must reference either a class with a `companion object Previews { ... }`, " +
                    "or an `object` declaring sample vals directly. " +
                    "${samplesClass.qualifiedName?.asString()} is neither.",
                fn,
            )
            return
        }

        val samples = collectSamples(source, stateType)
        if (samples.isEmpty()) {
            logger.error(
                "No samples of type $stateSimpleName found in ${source.qualifiedName?.asString()}. " +
                    "Declare public vals like `val Foo: $stateSimpleName = ...`. " +
                    "For sealed/abstract states, the explicit parent type is required.",
                fn,
            )
            return
        }

        val userFnName = fn.simpleName.asString()
        val annotationBaseName = userFnName.removeSuffix("Preview")
        val packageName = fn.packageName.asString()
        val multiPreviewClassName = ClassName(packageName, "${annotationBaseName}AutoPreviews")
        val providerClassName = ClassName(packageName, "${userFnName}SamplesProvider")
        val sourceClassName = source.toClassName()
        val stateTypeName = stateType.toTypeName()

        val providerSpec = buildSamplesProvider(
            providerClassName = providerClassName,
            stateTypeName = stateTypeName,
            samplesSource = sourceClassName,
            firstSample = samples.first(),
        )

        val multiPreviewSpec = TypeSpec
            .annotationBuilder(multiPreviewClassName)
            .also { spec -> PreviewMatrix.build(args).forEach(spec::addAnnotation) }
            .build()

        val outputFileName = "${annotationBaseName}AutoPreviews"
        FileSpec
            .builder(packageName, outputFileName)
            .addType(providerSpec)
            .addType(multiPreviewSpec)
            .build()
            .writeTo(codeGenerator, aggregating = false, originatingKSFiles = listOf(file))

        registryEntries += RegistryEntry(
            id = annotationBaseName,
            previewFunction = MemberName(packageName, userFnName),
            samplesSource = sourceClassName,
            samples = samples,
            args = args,
            file = file,
        )
    }

    private fun buildSamplesProvider(
        providerClassName: ClassName,
        stateTypeName: TypeName,
        samplesSource: ClassName,
        firstSample: String,
    ): TypeSpec {
        // The IDE renders only the first sample to keep the preview pane light; the full matrix is
        // rendered off-IDE by the Gradle plugin.
        val valuesProperty = PropertySpec
            .builder("values", SEQUENCE.parameterizedBy(stateTypeName))
            .addModifiers(KModifier.OVERRIDE)
            .initializer("sequenceOf(%T.%N)", samplesSource, firstSample)
            .build()
        return TypeSpec
            .classBuilder(providerClassName)
            .addModifiers(KModifier.INTERNAL)
            .addSuperinterface(PREVIEW_PARAMETER_PROVIDER.parameterizedBy(stateTypeName))
            .addProperty(valuesProperty)
            .build()
    }

    private fun collectSamples(
        source: KSClassDeclaration,
        stateType: KSType,
    ): List<String> =
        source
            .getDeclaredProperties()
            .filter { it.isPublic() && !it.isMutable }
            .filter { stateType.isAssignableFrom(it.type.resolve()) }
            .map { it.simpleName.asString() }
            .toList()
}

private data class WrapperInfo(
    val baseArgs: Map<String?, Any?>,
    val overridable: Set<String>,
) {
    companion object {
        fun from(metaClass: KSClassDeclaration): WrapperInfo? {
            if (metaClass.qualifiedName?.asString() == AUTO_PREVIEW_FQN) return null
            val baseAnn = metaClass.annotations.firstOrNull { it.fqn == AUTO_PREVIEW_FQN } ?: return null
            val overridable = metaClass.primaryConstructor
                ?.parameters
                ?.mapNotNull { it.name?.asString() }
                ?.toSet()
                .orEmpty()
            return WrapperInfo(baseAnn.argsMap(), overridable)
        }
    }
}

private fun KSAnnotated.hasAnnotation(fqn: String): Boolean = annotations.any { it.fqn == fqn }

private val KSAnnotation.fqn: String
    get() = annotationType
        .resolve()
        .declaration.qualifiedName
        ?.asString()
        .orEmpty()
