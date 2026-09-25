package app.mashlab.autopreview.processor

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType

internal data class AutoPreviewArgs(
    val samplesType: KSType,
    val locale: String,
    val devices: List<DeviceKind>,
    val themes: List<ThemeKind>,
    val backgroundColor: Long,
    val showSystemUi: Boolean,
    val navigatesTo: List<String>,
    val entryPoint: Boolean,
) {
    companion object {
        fun from(annotation: KSAnnotation): AutoPreviewArgs = fromArgs(annotation.argsMap())

        fun fromArgs(args: Map<String?, Any?>): AutoPreviewArgs = AutoPreviewArgs(
            samplesType = args.getValue("samplesFrom") as KSType,
            locale = (args["locale"] as? String) ?: "en",
            devices = args["devices"].toEnumNames().mapNotNull(DeviceKind::from)
                .ifEmpty { listOf(DeviceKind.Phone) },
            themes = args["themes"].toEnumNames().mapNotNull(ThemeKind::from)
                .ifEmpty { listOf(ThemeKind.Light, ThemeKind.Dark) },
            backgroundColor = (args["backgroundColor"] as? Long) ?: 0xFFFFFFFFL,
            showSystemUi = (args["showSystemUi"] as? Boolean) ?: false,
            navigatesTo = (args["navigatesTo"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
            entryPoint = (args["entryPoint"] as? Boolean) ?: false,
        )
    }
}

internal fun KSAnnotation.argsMap(): Map<String?, Any?> =
    arguments.associate { it.name?.asString() to it.value }

internal enum class DeviceKind(val widthDp: Int, val heightDp: Int, val isRound: Boolean = false, val uiModeType: String? = null) {
    Phone(411, 891),
    Tablet(1280, 800),
    Foldable(673, 841),
    Desktop(1920, 1080),
    Tv(960, 540, uiModeType = "television"),
    Wear(227, 227, isRound = true, uiModeType = "watch");

    val robolectricQualifiers: String
        get() = listOfNotNull("w${widthDp}dp", "h${heightDp}dp", "round".takeIf { isRound }, uiModeType).joinToString("-")

    companion object { fun from(name: String): DeviceKind? = entries.firstOrNull { it.name == name } }
}

internal enum class ThemeKind {
    Light, Dark;
    companion object { fun from(name: String): ThemeKind? = entries.firstOrNull { it.name == name } }
}

private fun Any?.toEnumNames(): List<String> =
    (this as? List<*>)?.mapNotNull { element ->
        when (element) {
            is KSType -> (element.declaration as? KSClassDeclaration)?.simpleName?.asString()
            is KSDeclaration -> element.simpleName.asString()
            else -> null
        }
    } ?: emptyList()
