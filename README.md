# Compose Auto Preview

[![Maven Central](https://img.shields.io/maven-central/v/io.github.drunkendealer/compose-auto-preview-annotations.svg)](https://central.sonatype.com/artifact/io.github.drunkendealer/compose-auto-preview-annotations)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A KSP processor that expands a single `@AutoPreview` into the full Compose preview matrix — locales × devices × themes × sample states — so your screen file stays small.

```kotlin
@AutoPreview(
    samplesFrom = SettingsSamples::class,
    locales = ["en", "de"],
    devices = [Device.Phone, Device.Tablet],
    themes  = [Theme.Light, Theme.Dark],
)
@SettingsScreenPreviews
@Composable
private fun Preview(@PreviewParameter(SettingsScreenSamples::class) s: SettingsState) =
    SettingsScreen(s)
```

That one function replaces the wall of stacked `@Preview` annotations and the hand-written `PreviewParameterProvider`.

## Install

Apply KSP and add both artifacts. The annotations are KMP; the processor runs at build time only.

```kotlin
plugins {
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.drunkendealer:compose-auto-preview-annotations:2.0.0")
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
        }
    }
}

dependencies {
    add("kspAndroid", "io.github.drunkendealer:compose-auto-preview-processor:2.0.0")
}
```

Single-target Android module? Replace the `kotlin { }` block with a plain `implementation(...)` of the annotations and use `ksp(...)` instead of `add("kspAndroid", ...)`.

## Usage

**1.** Declare your state and a source of samples — either a plain `object` of `val`s, or a class with `companion object Previews`:

```kotlin
data class SettingsState(val notificationsEnabled: Boolean = true, val username: String = "")

object SettingsSamples {
    val Default          = SettingsState()
    val NotificationsOff = SettingsState(notificationsEnabled = false)
    val Filled           = SettingsState(username = "max")
}
```

**2.** Write one `@AutoPreview` function per file. The generated names — `SettingsScreenSamples` (the `PreviewParameterProvider`) and `@SettingsScreenPreviews` (the multi-preview annotation) — come from the file name. Reference them up-front; KSP fills them in on the next build.

**3.** Build the module. The preview pane shows one cell per `locale × device × theme × sample`.

## Sharing config

Move app-wide config into a wrapper that meta-annotates `@AutoPreview`:

```kotlin
@AutoPreview(
    samplesFrom = Unit::class, // placeholder, overridden at use site
    locales = ["en", "de", "fr", "ja"],
    devices = [Device.Phone, Device.Tablet, Device.Foldable, Device.Desktop],
    themes  = [Theme.Light, Theme.Dark],
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class AppPreview(val samplesFrom: KClass<*>)
```

Any property the wrapper declares in its constructor (typically `samplesFrom`) overrides the meta-annotation at the use site.

## `@AutoPreview` parameters

| Parameter         | Type            | Default              |
|-------------------|-----------------|----------------------|
| `samplesFrom`     | `KClass<*>`     | —                    |
| `locales`         | `Array<String>` | `["en"]`             |
| `devices`         | `Array<Device>` | `[Device.Phone]`     |
| `themes`          | `Array<Theme>`  | `[Theme.Light]`      |
| `backgroundColor` | `Long`          | `0xFFFFFFFF` (white) |
| `showSystemUi`    | `Boolean`       | `false`              |

Final preview count: `locales × devices × themes × samples.size`.

## Requirements

Kotlin 2.3.21 · KSP 2.3.8 · Compose Multiplatform 1.10.x · `minSdk` 28 · JVM 11.

## License

[Apache 2.0](LICENSE)
