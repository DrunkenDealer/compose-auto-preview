package io.github.drunkendealer.composeautopreview.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName

internal object PreviewMatrix {

    private val PREVIEW = ClassName("androidx.compose.ui.tooling.preview", "Preview")
    private val CONFIGURATION = ClassName("android.content.res", "Configuration")
    private const val DEFAULT_BACKGROUND_COLOR = 0xFFFFFFFFL

    fun build(args: AutoPreviewArgs): List<AnnotationSpec> =
        args.devices.flatMap { device ->
            args.themes.map { theme -> previewSpec(device, theme, args) }
        }

    private fun previewSpec(
        device: DeviceKind,
        theme: ThemeKind,
        args: AutoPreviewArgs,
    ): AnnotationSpec {
        val builder = AnnotationSpec.builder(PREVIEW)
            .addMember("name = %S", "${args.locale} · ${device.name} · ${theme.name}")
            .addMember("locale = %S", args.locale)
            .addMember("device = %S", device.deviceSpec)
        if (theme == ThemeKind.Dark) {
            builder.addMember("uiMode = %T.UI_MODE_NIGHT_YES", CONFIGURATION)
        }
        if (args.backgroundColor != DEFAULT_BACKGROUND_COLOR) {
            builder.addMember("showBackground = true")
            builder.addMember("backgroundColor = 0x%LL", args.backgroundColor.toString(16).uppercase())
        }
        if (args.showSystemUi) {
            builder.addMember("showSystemUi = true")
        }
        return builder.build()
    }

    private val DeviceKind.deviceSpec: String
        get() = when (this) {
            DeviceKind.Phone    -> "spec:width=411dp,height=891dp,dpi=160"
            DeviceKind.Tablet   -> "spec:width=1280dp,height=800dp,dpi=160"
            DeviceKind.Foldable -> "spec:width=673dp,height=841dp,dpi=160"
            DeviceKind.Desktop  -> "spec:width=1920dp,height=1080dp,dpi=160"
        }
}
