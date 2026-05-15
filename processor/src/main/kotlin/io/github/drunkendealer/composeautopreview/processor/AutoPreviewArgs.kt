package io.github.drunkendealer.composeautopreview.processor

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
        fun from(annotation: KSAnnotation): AutoPreviewArgs = fromArgs(annotation.argsMap())

        fun fromArgs(args: Map<String?, Any?>): AutoPreviewArgs = AutoPreviewArgs(
            samplesType = args.getValue("samples") as KSType,
            locales = args["locales"].toStringList() ?: listOf("en"),
            devices = args["devices"].toEnumNames().mapNotNull(DeviceKind::from)
                .ifEmpty { listOf(DeviceKind.Phone) },
            themes = args["themes"].toEnumNames().mapNotNull(ThemeKind::from)
                .ifEmpty { listOf(ThemeKind.Light) },
            backgroundColor = (args["backgroundColor"] as? Long) ?: 0xFFFFFFFFL,
            showSystemUi = (args["showSystemUi"] as? Boolean) ?: false,
        )
    }
}

internal fun KSAnnotation.argsMap(): Map<String?, Any?> =
    arguments.associate { it.name?.asString() to it.value }

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
