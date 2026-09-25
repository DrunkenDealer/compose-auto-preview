package io.github.drunkendealer.composeautopreview.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

// Set by the Gradle plugin; without it only IDE previews are generated.
internal const val REGISTRY_PACKAGE_OPTION = "autopreview.registryPackage"

class AutoPreviewProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        AutoPreviewProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            registryPackage = environment.options[REGISTRY_PACKAGE_OPTION],
        )
}
