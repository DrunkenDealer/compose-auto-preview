# Compose Auto Preview

[![Maven Central](https://img.shields.io/maven-central/v/io.github.drunkendealer/compose-auto-preview-annotations.svg)](https://central.sonatype.com/artifact/io.github.drunkendealer/compose-auto-preview-annotations)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Compose Auto Preview isn't a replacement for Android Studio's preview system — it makes living with it cheaper. One annotation generates the full matrix of states, so you stop hand-writing `@Preview` stacks and `PreviewParameterProvider` classes. Previews stay easy to maintain and your screens become genuinely glanceable.

Once set up, every state sits side by side in the preview pane. UI regressions that would normally wait for QA tend to jump out at a glance. It won't replace a QA process — but for solo developers it shortens the feedback loop a lot.

One `@AutoPreview` annotation generates the full Compose preview matrix — every locale × device × theme × sample state.

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

The snippet above produces **16 previews** (2 × 2 × 2 × 2 samples) from a single function. No stacked `@Preview` annotations. No hand-written `PreviewParameterProvider`.

## Why

Without it, a real screen preview file looks like this:

```kotlin
class SettingsSamples : PreviewParameterProvider<SettingsState> { /* 5 states */ }

@Preview(name = "en · Phone · Light", locale = "en", device = "...", ...)
@Preview(name = "en · Phone · Dark",  locale = "en", device = "...", uiMode = ..., ...)
@Preview(name = "en · Tablet · Light", ...)
// …14 more @Preview lines
@Composable
private fun Preview(@PreviewParameter(SettingsSamples::class) s: SettingsState) = SettingsScreen(s)
```

`@AutoPreview` replaces all of that with one annotation.

## Android native implementation

```kotlin
// build.gradle.kts
plugins { alias(libs.plugins.ksp) }

dependencies {
    implementation("io.github.drunkendealer:compose-auto-preview-annotations:2.0.3")
    ksp("io.github.drunkendealer:compose-auto-preview-processor:2.0.3")
}
```

**1. Define your state samples.** Any `object` with properties of the target state type works:

```kotlin
object SettingsSamples {
    val Default          = SettingsState()
    val NotificationsOff = SettingsState(notificationsEnabled = false)
    val Filled           = SettingsState(username = "max")
}
```

**2. Write the preview.** Two names are derived from the source **file name** — for `SettingsScreen.kt`:

| Generated symbol | Type | What it is |
|---|---|---|
| `SettingsScreenSamples` | class | The `PreviewParameterProvider` |
| `@SettingsScreenPreviews` | annotation | The multi-preview |

Reference them now even though they don't exist yet — KSP creates them on the next build:

```kotlin
// SettingsScreen.kt
@AutoPreview(samplesFrom = SettingsSamples::class)
@SettingsScreenPreviews                                              // generated
@Composable
private fun Preview(
    @PreviewParameter(SettingsScreenSamples::class) s: SettingsState // generated
) = SettingsScreen(s)
```

**3. Build.** The Studio preview pane now renders one cell per `locale × device × theme × sample`.

## Kotlin Multiplatform implementation

State and samples go in `commonMain`. The preview function lives in `androidMain` — Compose Preview is Android-only.

```kotlin
plugins { alias(libs.plugins.ksp) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.drunkendealer:compose-auto-preview-annotations:2.0.3")
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
        }
    }
}

dependencies {
    add("kspAndroid", "io.github.drunkendealer:compose-auto-preview-processor:2.0.3")
}
```

```kotlin
// commonMain/.../MoodLogScreen.kt
@Composable
fun MoodLogScreen(state: MoodLogState, onIntent: (MoodLogIntent) -> Unit) { /* … */ }

object MoodLogSamples {
    val Loading = MoodLogState(isLoading = true)
    val Loaded  = MoodLogState(items = sampleItems)
}
```

```kotlin
// androidMain/.../MoodLogScreen.kt
@file:JvmName("MoodLogScreenAndroid")

@AutoPreview(
    samplesFrom = MoodLogSamples::class,
    devices = [Device.Phone, Device.Tablet],
    themes  = [Theme.Light, Theme.Dark],
)
@MoodLogScreenPreviews
@Composable
private fun Preview(@PreviewParameter(MoodLogScreenSamples::class) s: MoodLogState) {
    AppTheme { MoodLogScreen(state = s, onIntent = {}) }
}
```

> `@file:JvmName(...)` is only required when the `androidMain` file shares its name **and** package with a `commonMain` file — without it, the two would compile to the same JVM class name.

## Shared config across screens

Most apps want the same locales, devices and themes for every screen. Hoist the config into a wrapper annotation:

```kotlin
@AutoPreview(
    samplesFrom = Unit::class, // placeholder — overridden at use site
    locales = ["en", "de", "fr", "ja"],
    devices = [Device.Phone, Device.Tablet, Device.Foldable, Device.Desktop],
    themes  = [Theme.Light, Theme.Dark],
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class AppPreview(val samplesFrom: KClass<*>)
```

Now each screen only declares what's specific to it:

```kotlin
@AppPreview(samplesFrom = SettingsSamples::class)
@SettingsScreenPreviews
@Composable
private fun Preview(@PreviewParameter(SettingsScreenSamples::class) s: SettingsState) =
    SettingsScreen(s)
```

Any parameter declared in the wrapper's constructor overrides the meta-annotation's value at the use site. Works the same way in both Android-only and KMP modules.

## `@AutoPreview` parameters

| Parameter         | Type            | Default              |
|-------------------|-----------------|----------------------|
| `samplesFrom`     | `KClass<*>`     | —                    |
| `locales`         | `Array<String>` | `["en"]`             |
| `devices`         | `Array<Device>` | `[Device.Phone]`     |
| `themes`          | `Array<Theme>`  | `[Theme.Light]`      |
| `backgroundColor` | `Long`          | `0xFFFFFFFF` (white) |
| `showSystemUi`    | `Boolean`       | `false`              |

`Device` values: `Phone`, `Tablet`, `Foldable`, `Desktop`. `Theme` values: `Light`, `Dark`.

Total previews per function: `locales × devices × themes × samples.size`.

## Requirements

Kotlin 2.0+ · KSP 2.0+ · Jetpack Compose (Android) or Compose Multiplatform 1.7+ · `minSdk` 28 · JVM 11.

## License

[Apache 2.0](LICENSE)
