package app.mashlab.autopreview.annotations

import kotlin.reflect.KClass

/**
 * @param navigatesTo ids of screens this one links to in the rendered app graph. A screen id is the
 * preview function name without the `Preview` suffix, e.g. `SettingsScreenPreview` → `"SettingsScreen"`.
 * @param entryPoint marks the app's start screen; the report graph is laid out from it. At most one per module.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AutoPreview(
    val samplesFrom: KClass<*>,
    val locale: String = "en",
    val devices: Array<Device> = [Device.Phone],
    val themes: Array<Theme> = [Theme.Light, Theme.Dark],
    val backgroundColor: Long = 0xFFFFFFFF,
    val showSystemUi: Boolean = false,
    val navigatesTo: Array<String> = [],
    val entryPoint: Boolean = false,
)
