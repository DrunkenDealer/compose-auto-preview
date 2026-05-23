# Compose Auto Preview

[![Maven Central](https://img.shields.io/maven-central/v/io.github.drunkendealer/compose-auto-preview-annotations.svg)](https://central.sonatype.com/artifact/io.github.drunkendealer/compose-auto-preview-annotations)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Compose Auto Preview removes the boilerplate around Compose `@Preview` matrices. One annotation generates the `device × theme × samples` matrix plus the `PreviewParameterProvider` for your state samples — you stop hand-stacking `@Preview` annotations and hand-writing provider classes.

```kotlin
@AutoPreview(
    samplesFrom = SettingsSamples::class,
    devices = [Device.Phone, Device.Tablet],
    themes  = [Theme.Light, Theme.Dark],
)
@SettingsScreenAutoPreviews
@Composable
internal fun SettingsScreenPreview(
    @PreviewParameter(SettingsScreenPreviewSamplesProvider::class) state: SettingsState,
) = SettingsScreen(state)
```

After the first build, Studio resolves the generated `@SettingsScreenAutoPreviews` annotation and `SettingsScreenPreviewSamplesProvider` class. The example above renders **20 cells** — 2 devices × 2 themes × 5 samples.

## How the matrix stays cheap

Compose Preview's bitmap cache scales linearly with cell count. A 20-screen app rendering a full `device × locale × theme` Cartesian accumulates ~800 cells × ~3MB each ≈ **2.4 GB** of bitmaps before any cell ages out. Studio lags and eventually needs Invalidate Caches.

The library leans on one observation: of the obvious axes, **locale is the one you rarely need to multiply cells**. You verify devices side-by-side (phone vs tablet vs foldable) and themes stacked (light vs dark), but locale is usually checked one at a time. So `@AutoPreview` takes a single `locale: String` (default `"en"`) and renders the `device × theme × samples` Cartesian against that one locale.

Switch the rendered locale by editing the `locale` value and rebuilding. To verify a different locale, change the field. No global flag, no Gradle property.

## How it works

KSP generates two declarations per `@AutoPreview` function:

1. **`<UserFn>SamplesProvider`** — a `PreviewParameterProvider<StateType>` returning the samples from your `samplesFrom = ...` object in declaration order.
2. **`@<UserFn>AutoPreviews`** — a multi-preview annotation containing the `device × theme` Cartesian at the declared `locale`.

You write **one** function, decorated with `@AutoPreview` (drives codegen) and the generated `@<UserFn>AutoPreviews` (drives Studio rendering). The function parameter carries `@PreviewParameter(<UserFn>SamplesProvider::class)` so Studio injects each sample.

## Android native implementation

```kotlin
// build.gradle.kts
plugins { alias(libs.plugins.ksp) }

dependencies {
    implementation("io.github.drunkendealer:compose-auto-preview-annotations:3.1.1")
    ksp("io.github.drunkendealer:compose-auto-preview-processor:3.1.1")
}
```

**1. Define your state samples.** Any `object` with vals of the target state type works:

```kotlin
object SettingsSamples {
    val Default          = SettingsState()
    val NotificationsOff = SettingsState(notificationsEnabled = false)
    val Filled           = SettingsState(username = "max")
}
```

**2. Write the preview function.** One `@Composable` taking the state — `internal` (or `public`) so the generated provider is accessible:

```kotlin
@AutoPreview(samplesFrom = SettingsSamples::class)
@SettingsScreenAutoPreviews
@Composable
internal fun SettingsScreenPreview(
    @PreviewParameter(SettingsScreenPreviewSamplesProvider::class) state: SettingsState,
) = SettingsScreen(state)
```

First build resolves `@SettingsScreenAutoPreviews` and `SettingsScreenPreviewSamplesProvider` — both are red until KSP runs once.

## Dialogs and bottom sheets

`AlertDialog`, `ModalBottomSheet` and similar render in a separate window, so the `@Composable` body has no inline content to size the canvas. Wrap your preview in a `Box(Modifier.fillMaxSize())` so the dialog underlay has a canvas to draw into:

```kotlin
@AutoPreview(samplesFrom = ConfirmDialogSamples::class)
@ConfirmDialogAutoPreviews
@Composable
internal fun ConfirmDialogPreview(
    @PreviewParameter(ConfirmDialogPreviewSamplesProvider::class) state: ConfirmDialogState,
) = Box(Modifier.fillMaxSize()) { ConfirmDialog(state) }
```

## Kotlin Multiplatform implementation

State and samples go in `commonMain`. The `@AutoPreview` function lives in `androidMain` (where KSP runs) — Compose Preview is Android-only.

```kotlin
plugins { alias(libs.plugins.ksp) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.drunkendealer:compose-auto-preview-annotations:3.1.1")
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
        }
    }
}

dependencies {
    add("kspAndroid", "io.github.drunkendealer:compose-auto-preview-processor:3.1.1")
}
```

## Shared config across screens

Most apps want the same devices and themes for every screen. Hoist the config into a wrapper annotation:

```kotlin
@AutoPreview(
    samplesFrom = Unit::class, // placeholder — overridden at use site
    devices = [Device.Phone, Device.Tablet, Device.Foldable, Device.Desktop],
    themes  = [Theme.Light, Theme.Dark],
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class AppPreview(val samplesFrom: KClass<*>)
```

Each screen declares only what's specific to it:

```kotlin
@AppPreview(samplesFrom = SettingsSamples::class)
@SettingsScreenAutoPreviews
@Composable
internal fun SettingsScreenPreview(
    @PreviewParameter(SettingsScreenPreviewSamplesProvider::class) state: SettingsState,
) = SettingsScreen(state)
```

Any parameter declared in the wrapper's constructor overrides the meta-annotation's value at the use site.

## `@AutoPreview` parameters

| Parameter         | Type            | Default              |
|-------------------|-----------------|----------------------|
| `samplesFrom`     | `KClass<*>`     | —                    |
| `locale`          | `String`        | `"en"`               |
| `devices`         | `Array<Device>` | `[Device.Phone]`     |
| `themes`          | `Array<Theme>`  | `[Theme.Light]`      |
| `backgroundColor` | `Long`          | `0xFFFFFFFF` (white) |
| `showSystemUi`    | `Boolean`       | `false`              |

`Device` values: `Phone`, `Tablet`, `Foldable`, `Desktop`. `Theme` values: `Light`, `Dark`.

Cells in the preview pane: `devices × themes × samples`. All device specs render at `dpi=160` (mdpi) regardless of physical-device density to keep bitmaps as small as possible.

## Caveats

- **First-build red squigglies.** `@<Name>AutoPreviews` and `<Name>SamplesProvider` don't exist until KSP runs. Type them, build once, Studio resolves them.
- **Switching the rendered locale** is a source edit: change the `locale` value and let KSP regenerate.

## Requirements

Kotlin 2.0+ · KSP 2.0+ · Jetpack Compose (Android) or Compose Multiplatform 1.7+ · `minSdk` 28 · JVM 11.

## License

[Apache 2.0](LICENSE)
