package app.mashlab.autopreview.annotations

import kotlin.reflect.KClass

/**
 * @param navigatesTo ids of screens this one links to in the rendered app graph. A screen id is the
 * preview function name without the `Preview` suffix, e.g. `SettingsScreenPreview` → `"SettingsScreen"`. The screen may
 * live in another module; the edge shows in any report that includes both.
 * @param entryPoint marks the app's start screen; the report graph is laid out from it. At most one per module; when a
 * report merges several modules, the one in the module it runs on wins.
 * @param group puts the screen in a named group, such as `"Bottom navigation"` for the tabs of a bottom bar or
 * `"Onboarding"` for a flow. The report draws a group as one column in a labelled box, with its screens reachable from
 * each other, so tabs need no `navigatesTo` between them. Works across modules.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@Suppress("LongParameterList")
annotation class AutoPreview(
    val samplesFrom: KClass<*>,
    val locale: String = "en",
    val devices: Array<Device> = [Device.Phone],
    val themes: Array<Theme> = [Theme.Light, Theme.Dark],
    val backgroundColor: Long = 0xFFFFFFFF,
    val showSystemUi: Boolean = false,
    val navigatesTo: Array<String> = [],
    val entryPoint: Boolean = false,
    val group: String = "",
)
