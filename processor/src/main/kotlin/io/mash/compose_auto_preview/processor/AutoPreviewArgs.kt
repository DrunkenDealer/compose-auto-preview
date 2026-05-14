package io.mash.compose_auto_preview.processor

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType

internal data class AutoPreviewArgs(
    val samplesType: KSType,
    val locales: List<String>,
    val devices: List<DeviceKind>,
    val themes: List<ThemeKind>,
    val backgroundColor: Long,
    val showSystemUi: Boolean,
) {
    companion object {
        fun from(annotation: KSAnnotation): AutoPreviewArgs {
            val args = annotation.arguments.associateBy { it.name?.asString() }
            return AutoPreviewArgs(
                samplesType = args.getValue("samples").value as KSType,
                locales = args["locales"]?.value.toStringList() ?: listOf("en"),
                devices = args["devices"]?.value.toEnumNames().mapNotNull(DeviceKind::from)
                    .ifEmpty { listOf(DeviceKind.Phone) },
                themes = args["themes"]?.value.toEnumNames().mapNotNull(ThemeKind::from)
                    .ifEmpty { listOf(ThemeKind.Light) },
                backgroundColor = (args["backgroundColor"]?.value as? Long) ?: 0xFFFFFFFFL,
                showSystemUi = (args["showSystemUi"]?.value as? Boolean) ?: false,
            )
        }
    }
}

internal enum class DeviceKind {
    Phone, Tablet, Foldable, Desktop;
    companion object { fun from(name: String): DeviceKind? = entries.firstOrNull { it.name == name } }
}

internal enum class ThemeKind {
    Light, Dark;
    companion object { fun from(name: String): ThemeKind? = entries.firstOrNull { it.name == name } }
}

private fun Any?.toStringList(): List<String>? =
    (this as? List<*>)?.mapNotNull { it as? String }

private fun Any?.toEnumNames(): List<String> =
    (this as? List<*>)?.mapNotNull { element ->
        when (element) {
            is KSType -> (element.declaration as? KSClassDeclaration)?.simpleName?.asString()
            is KSDeclaration -> element.simpleName.asString()
            else -> null
        }
    } ?: emptyList()
