package app.mashlab.autopreview.sample

import app.mashlab.autopreview.annotations.AutoPreview
import app.mashlab.autopreview.annotations.Device
import app.mashlab.autopreview.annotations.Theme
import kotlin.reflect.KClass

/** Shared matrix for most screens: English, German and Ukrainian on phone and tablet, light and dark. */
@AutoPreview(
    samplesFrom = Unit::class,
    locales = ["en", "de", "uk"],
    devices = [Device.Phone, Device.Tablet],
    themes = [Theme.Light, Theme.Dark],
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class PreviewBloom(
    val samplesFrom: KClass<*>,
    val navigatesTo: Array<String> = [],
    val group: String = "",
)
