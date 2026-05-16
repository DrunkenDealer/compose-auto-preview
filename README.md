# Compose Auto Preview

[![Maven Central](https://img.shields.io/maven-central/v/io.github.drunkendealer/compose-auto-preview-annotations.svg)](https://central.sonatype.com/artifact/io.github.drunkendealer/compose-auto-preview-annotations)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

One `@AutoPreview` → the full Compose preview matrix (locales × devices × themes × sample states).

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

Replaces the wall of stacked `@Preview` annotations and the hand-written `PreviewParameterProvider`.

## Android module

```kotlin
// build.gradle.kts
plugins { alias(libs.plugins.ksp) }

dependencies {
    implementation("io.github.drunkendealer:compose-auto-preview-annotations:2.0.2")
    ksp("io.github.drunkendealer:compose-auto-preview-processor:2.0.2")
}
```

**1. Declare state and samples.**

```kotlin
data class SettingsState(val notificationsEnabled: Boolean = true, val username: String = "")

object SettingsSamples {
    val Default          = SettingsState()
    val NotificationsOff = SettingsState(notificationsEnabled = false)
    val Filled           = SettingsState(username = "max")
}
```

**2. Write the `@AutoPreview` function.** Two names are derived from the source file name — for `SettingsScreen.kt`:

- `SettingsScreenSamples` — the generated `PreviewParameterProvider`
- `@SettingsScreenPreviews` — the generated multi-preview annotation

Reference them up-front. They don't exist yet; KSP creates them on the next build:

```kotlin
// SettingsScreen.kt
@AutoPreview(samplesFrom = SettingsSamples::class)
@SettingsScreenPreviews                                              // generated
@Composable
private fun Preview(
    @PreviewParameter(SettingsScreenSamples::class) s: SettingsState // generated
) = SettingsScreen(s)
```

**3. Build.** KSP writes `SettingsScreenPreviews.kt` into `build/generated/ksp/…/`:

```kotlin
public class SettingsScreenSamples : PreviewParameterProvider<SettingsState> {
    override val values = sequenceOf(
        SettingsSamples.Default,
        SettingsSamples.NotificationsOff,
        SettingsSamples.Filled,
    )
}

@Preview(name = "en · Phone · Light", /* … */)
@Preview(name = "en · Phone · Dark",  /* … */)
// …one per locale × device × theme
public annotation class SettingsScreenPreviews
```

The Studio preview pane renders one cell per `locale × device × theme × sample`.

## Kotlin Multiplatform module

State and samples live in `commonMain`. The preview function (and any wrapper annotation) is `androidMain`-only — Compose Preview is Android.

```kotlin
plugins { alias(libs.plugins.ksp) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.drunkendealer:compose-auto-preview-annotations:2.0.2")
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
        }
    }
}

dependencies {
    add("kspAndroid", "io.github.drunkendealer:compose-auto-preview-processor:2.0.2")
}
```

```kotlin
// commonMain/.../MoodLogScreen.kt
@Composable
fun MoodLogScreen(state: MoodLogState, onIntent: (MoodLogIntent) -> Unit) { /* … */ }

// commonMain/.../MoodLogStateSamples.kt
object MoodLogStateSamples {
    val Loading = MoodLogState(isLoading = true)
    val Loaded  = MoodLogState(items = sampleItems)
}
```

```kotlin
// androidMain/.../MoodLogScreen.kt
@file:JvmName("MoodLogScreenAndroid") // disambiguates JVM class name from the commonMain file

@AutoPreview(
    samplesFrom = MoodLogStateSamples::class,
    devices = [Device.Phone, Device.Tablet],
    themes  = [Theme.Light, Theme.Dark],
)
@MoodLogScreenPreviews
@Composable
private fun Preview(@PreviewParameter(MoodLogScreenSamples::class) s: MoodLogState) {
    AppTheme { MoodLogScreen(state = s, onIntent = {}) }
}
```

The `@file:JvmName` is required only when the `androidMain` preview file shares its filename with a `commonMain` file in the same package.

## Shared configuration

Hoist app-wide config into a wrapper that meta-annotates `@AutoPreview`:

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

Any property the wrapper declares in its constructor overrides the meta-annotation at the use site.

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

Kotlin 2.0+ · KSP 2.0+ · Jetpack Compose (Android) or Compose Multiplatform 1.7+ · `minSdk` 28 · JVM 11.

## License

[Apache 2.0](LICENSE)
