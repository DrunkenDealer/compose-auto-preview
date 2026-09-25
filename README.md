# Compose Auto Preview

[![Maven Central](https://img.shields.io/maven-central/v/app.mashlab/compose-auto-preview-annotations.svg)](https://central.sonatype.com/artifact/app.mashlab/compose-auto-preview-annotations)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Compose Auto Preview turns one annotation into two outputs:

- **A light live preview in Android Studio** — every device × theme for the first state sample. Cheap enough to keep the preview pane responsive while you edit.
- **The full matrix as PNGs** — every device × theme × state, rendered by Gradle outside the IDE, browsable as an HTML graph of your app's screens.

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

## Setup

Apply the plugin next to KSP — that's the whole setup, for Android and Kotlin Multiplatform modules alike:

```kotlin
plugins {
    alias(libs.plugins.ksp)
    id("app.mashlab.compose-auto-preview") version "3.2.0"
}
```

The plugin adds the annotations (to `androidMain` in KMP) and the KSP processor for you.

After the first build, Studio resolves the generated `@SettingsScreenAutoPreviews` annotation and `SettingsScreenPreviewSamplesProvider` class. Studio renders **4 cells** (2 devices × 2 themes, first sample); `./gradlew autoPreview` renders all **20** (× 5 samples).

## Why two outputs

Android Studio creates and keeps a render session per preview cell for every preview in the open file, so IDE memory grows with cell count — a full `device × theme × state` matrix across a few screens quickly eats gigabytes. Off-screen cells are already rendered at low resolution, so shrinking bitmaps doesn't fix it; rendering fewer cells does.

So the IDE only gets `devices × themes` for the first sample, and the full matrix goes to Gradle, which renders it with Robolectric in a separate JVM and writes PNGs to disk.

`@AutoPreview` takes a single `locale` (default `"en"`) — change the value to check another locale.

## Full matrix report

```
./gradlew :app:autoPreview
…
Auto preview report: file:///…/app/build/autopreview/index.html
```

The report opens on an interactive graph of your app, laid out from its entry point. Each screen is a thumbnail in its device frame, the entry point is tagged, and each hop away from the start sits on the next ring out.

- **Graph:** drag nodes, pan, and zoom with the scroll wheel or a pinch. Hover a screen to highlight its links; press `/` to search.
- **List view:** the same information as a table.
- **Screen page:** every state in light and dark, side by side, with one tab per device. Switch between *Fit* and *Actual size* (1 dp = 1 CSS px). Click a shot for a full-screen viewer: `←`/`→` step, `T` switches theme, `[`/`]` go to the previous/next screen.

The render task is incremental: nothing re-renders if the code didn't change. The task opens the report in your default browser; pass `-PautoPreview.open=false` to skip that (it never opens on CI).

Mark the start screen with `entryPoint`, and declare edges with `navigatesTo`. A screen id is the preview function name without the `Preview` suffix; ids must be unique within a module, and unknown `navigatesTo` targets are reported as build warnings:

```kotlin
@AutoPreview(samplesFrom = OnboardingSamples::class, entryPoint = true, navigatesTo = ["SettingsScreen"])
@AutoPreview(samplesFrom = SettingsSamples::class, navigatesTo = ["ConfirmDialog"])
```

Screens the entry point can't reach are drawn dashed on the outer ring. Unknown ids are listed as a warning in the report.

The plugin adds Robolectric to `testImplementation` and a generated render test that is skipped in regular unit test runs. It renders with your target SDK when unit tests run on **JDK 21** (Android Studio's bundled JBR works); on older JDKs it falls back to SDK 34, since Robolectric needs Java 21 for SDK 35+.

## How it works

KSP generates two declarations per `@AutoPreview` function:

1. **`<UserFn>SamplesProvider`** — a `PreviewParameterProvider<StateType>` returning the samples from your `samplesFrom = ...` object in declaration order.
2. **`@<UserFn>AutoPreviews`** — a multi-preview annotation containing the `device × theme` Cartesian at the declared `locale`.

You write **one** function, decorated with `@AutoPreview` (drives codegen) and the generated `@<UserFn>AutoPreviews` (drives Studio rendering). The function parameter carries `@PreviewParameter(<UserFn>SamplesProvider::class)` so Studio injects each sample.

## Usage

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

In a KMP module, state and samples can live in `commonMain`; the `@AutoPreview` function goes in `androidMain` (where KSP runs), since Compose Preview is Android-only.

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
| `themes`          | `Array<Theme>`  | `[Theme.Light, Theme.Dark]` |
| `backgroundColor` | `Long`          | `0xFFFFFFFF` (white) |
| `showSystemUi`    | `Boolean`       | `false`              |
| `navigatesTo`     | `Array<String>` | `[]`                 |
| `entryPoint`      | `Boolean`       | `false`              |

`Device` values: `Phone`, `Tablet`, `Foldable`, `Desktop`, `Tv`, `Wear` (round). `Theme` values: `Light`, `Dark`.

Cells in the preview pane: `devices × themes` (first sample). Cells in the report: `devices × themes × samples`.

## Caveats

- **First-build red squigglies.** `@<Name>AutoPreviews` and `<Name>SamplesProvider` don't exist until KSP runs. Type them, build once, Studio resolves them.
- **Switching the rendered locale** is a source edit: change the `locale` value and let KSP regenerate.
- **Report vs Studio fidelity.** The report renders with Robolectric, Studio with layoutlib — close, but not always pixel-identical. Infinite animations (progress indicators) are frozen at their first frame; `showSystemUi` isn't drawn.
- **Report is per module.** Each module with the plugin gets its own report; `navigatesTo` edges across modules aren't drawn yet.
- **Keeping the IDE light.** For many screens in one file, *Settings › Editor › UI Tools › Preview Settings › View Mode: Focus* renders one preview at a time.

## Requirements

Kotlin 2.0+ · KSP 2.0+ · Jetpack Compose (Android) or Compose Multiplatform 1.7+ · `minSdk` 28 · JVM 11.

## License

[Apache 2.0](LICENSE)
