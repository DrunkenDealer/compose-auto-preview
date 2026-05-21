package io.github.drunkendealer.composeautopreview.sample

import io.github.drunkendealer.composeautopreview.annotations.AutoPreview
import io.github.drunkendealer.composeautopreview.annotations.Device
import io.github.drunkendealer.composeautopreview.annotations.Theme
import kotlin.reflect.KClass

@AutoPreview(
    samplesFrom = Unit::class,
    locale = "en",
    devices = [Device.Phone, Device.Tablet, Device.Foldable, Device.Desktop],
    themes = [Theme.Light, Theme.Dark],
    showSystemUi = true,
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class FullMatrixPreview(val samplesFrom: KClass<*>)
