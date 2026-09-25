package io.github.drunkendealer.composeautopreview.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AutoPreview(
    val samplesFrom: KClass<*>,
    val locale: String = "en",
    val devices: Array<Device> = [Device.Phone],
    val themes: Array<Theme> = [Theme.Light, Theme.Dark],
    val backgroundColor: Long = 0xFFFFFFFF,
    val showSystemUi: Boolean = false,
)
