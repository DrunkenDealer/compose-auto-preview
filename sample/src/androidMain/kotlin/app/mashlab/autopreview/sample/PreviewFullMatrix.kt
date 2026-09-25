package app.mashlab.autopreview.sample

import app.mashlab.autopreview.annotations.AutoPreview
import app.mashlab.autopreview.annotations.Device
import app.mashlab.autopreview.annotations.Theme
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
annotation class PreviewFullMatrix(
    val samplesFrom: KClass<*>,
)
