package io.mash.compose_auto_preview.sample

import io.mash.compose_auto_preview.annotations.AutoPreview
import io.mash.compose_auto_preview.annotations.Device
import io.mash.compose_auto_preview.annotations.Theme
import kotlin.reflect.KClass

@AutoPreview(
    samples = Unit::class,
    locales = ["en", "de", "fr", "ja"],
    devices = [Device.Phone, Device.Tablet, Device.Foldable, Device.Desktop],
    themes = [Theme.Light, Theme.Dark],
    showSystemUi = true,
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class FullMatrixPreview(val samples: KClass<*>)
