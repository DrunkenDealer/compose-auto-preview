package io.mash.compose_auto_preview.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AutoPreview(
    val samples: KClass<*>,
    val locales: Array<String> = ["en"],
    val devices: Array<Device> = [Device.Phone],
    val themes: Array<Theme> = [Theme.Light],
    val backgroundColor: Long = 0xFFFFFFFF,
    val showSystemUi: Boolean = false,
)
