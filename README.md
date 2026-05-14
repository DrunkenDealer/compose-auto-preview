# Compose Auto Preview

A KSP processor that generates Compose `@Preview` matrices and `PreviewParameterProvider`s for you, from one `@AutoPreview` annotation and a class of sample values.

## Why

Covering a screen properly across locales, devices, themes, and states means stacking a wall of `@Preview` annotations on top of every composable. A screen file balloons — the actual UI code scrolls off-screen under preview config, every new locale or device touches every screen, and the sample data (states, providers) gets copy-pasted next to the `@Preview` block instead of living somewhere reusable.

With Compose Auto Preview you write **one** preview function. The configuration lives outside the screen file as a flat list of locales / devices / themes; the sample states live as plain `val`s on an object. The processor expands the matrix at build time, so the screen file stays small and the previews stay consistent across the whole project.

Stop writing this:

```kotlin
class StateProvider : PreviewParameterProvider<MyState> {
    override val values = sequenceOf(MyState.Loading, MyState.Loaded, MyState.Error)
}

@Preview(name = "en · Phone · Light", locale = "en", device = "spec:width=411dp,height=891dp")
@Preview(name = "en · Phone · Dark",  locale = "en", device = "spec:width=411dp,height=891dp", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "de · Phone · Light", locale = "de", device = "spec:width=411dp,height=891dp")
// …and so on, for every locale × device × theme
@Composable
private fun Preview(@PreviewParameter(StateProvider::class) s: MyState) = MyScreen(s)
```

Write this:

```kotlin
@AutoPreview(
    samples = MyState::class,
    locales = ["en", "de"],
    devices = [Device.Phone, Device.Tablet],
    themes  = [Theme.Light, Theme.Dark],
)
@MyScreenPreviews
@Composable
private fun Preview(@PreviewParameter(MyScreenSamples::class) s: MyState) = MyScreen(s)
```

## Version

Current: **`1.0.0`**

## Install

Two artifacts: an annotations module (KMP — Android / iOS / JVM) and a KSP processor (JVM).

### 1. Add KSP and the Compose preview tooling to your app/feature module

`build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform) // or kotlin("android")
    alias(libs.plugins.androidApplication)  // or androidLibrary
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
}
```

### 2. Add the dependencies

```kotlin
kotlin {
    sourceSets {
        // commonMain or androidMain — depending on where you annotate
        androidMain.dependencies {
            implementation("io.mash:compose-auto-preview-annotations:1.0.0")
            implementation(libs.compose.uiToolingPreview) // PreviewParameter, PreviewParameterProvider
        }
    }
}

dependencies {
    // Only Android variants render `@Preview` in Studio, so wire KSP on the Android target.
    add("kspAndroid", "io.mash:compose-auto-preview-processor:1.0.0")
    // For a single-target Android module use: ksp(...)
    debugImplementation(libs.compose.uiTooling)
}
```

### 3. Repositories

Until the artifacts land on Maven Central, install locally:

```shell
./gradlew :annotations:publishToMavenLocal :processor:publishToMavenLocal
```

and add `mavenLocal()` to the consuming project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}
```

## Usage

### 1. Declare your screen state and samples

The processor finds samples in one of two shapes — pick whichever fits the type you're previewing.

**A class with a `companion object Previews`** — best for sealed/abstract states where you need the parent type explicitly:

```kotlin
sealed interface OnboardingState {
    data object Welcome : OnboardingState
    data class CategorySelection(val selected: Set<String> = emptySet()) : OnboardingState
    data class Complete(val summary: String) : OnboardingState

    companion object Previews {
        val Welcome:           OnboardingState = OnboardingState.Welcome
        val CategoriesEmpty:   OnboardingState = CategorySelection()
        val CategoriesPicked:  OnboardingState = CategorySelection(selected = setOf("Mind", "Body"))
        val Complete:          OnboardingState = Complete(summary = "3 habits, reminders on")
    }
}
```

**A plain `object`** — simplest case for a single data-class state:

```kotlin
data class SettingsState(val notificationsEnabled: Boolean = true, val username: String = "")

object SettingsSamples {
    val Default          = SettingsState()
    val NotificationsOff = SettingsState(notificationsEnabled = false)
    val Filled           = SettingsState(username = "max")
}
```

Sample rules: public, immutable `val`s whose type is assignable to the preview parameter type. The processor picks them up by type, so the property name is free (it shows up in the variant label).

### 2. Annotate one composable per file

```kotlin
@AutoPreview(samples = SettingsSamples::class)
@SettingsScreenPreviews                                   // ← generated
@Composable
private fun Preview(
    @PreviewParameter(SettingsScreenSamples::class) s: SettingsState, // ← generated
) = SettingsScreen(s)
```

The generated names are derived from the **file name**:
- `SettingsScreen.kt` → `SettingsScreenSamples` (the `PreviewParameterProvider`) + `SettingsScreenPreviews` (the multi-preview annotation).
- Reference both up-front; KSP fills them in on the next build.

### 3. Build and open the preview

`./gradlew :app:assembleDebug` (or just let the IDE sync) — the multi-preview annotation expands to one `@Preview` per `locale × device × theme` cell.

## Share config with a custom wrapper annotation

Once you've picked the locales / devices / themes / extras you want across the app, you don't want to repeat that block on every screen. Move it into a custom annotation that wraps `@AutoPreview`:

```kotlin
@AutoPreview(
    samples = Unit::class, // placeholder — overridden by the wrapper's `samples` argument
    locales = ["en", "de", "fr", "ja"],
    devices = [Device.Phone, Device.Tablet, Device.Foldable, Device.Desktop],
    themes  = [Theme.Light, Theme.Dark],
    showSystemUi = true,
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class AppPreview(val samples: KClass<*>)
```

Then each screen uses just:

```kotlin
@AppPreview(samples = OnboardingScreenState::class)
@OnboardingScreenPreviews
@Composable
private fun Preview(@PreviewParameter(OnboardingScreenSamples::class) s: OnboardingScreenState) =
    OnboardingScreen(s)
```

How merging works:
- Config (`locales`, `devices`, `themes`, `backgroundColor`, `showSystemUi`) is read from the `@AutoPreview` on the wrapper class.
- Any property the wrapper itself **declares** in its constructor — typically `samples` — overrides the value from the meta-annotation at the use site.
- Want per-screen overrides for, say, `showSystemUi`? Add it to the wrapper's constructor (`val showSystemUi: Boolean = true`) and it becomes overridable in the same way.

The `samples = Unit::class` in the meta-annotation is just a placeholder to satisfy `@AutoPreview`'s required parameter; the wrapper's `samples` always wins. The same one-`@AutoPreview`-per-file rule still applies — counted by what reaches the function, whether direct or via a wrapper.

## `@AutoPreview` parameters

| Parameter         | Type            | Default                  | Notes |
|-------------------|-----------------|--------------------------|-------|
| `samples`         | `KClass<*>`     | —                        | Class with `companion object Previews`, or a top-level `object` declaring sample vals. |
| `locales`         | `Array<String>` | `["en"]`                 | One preview row per locale. |
| `devices`         | `Array<Device>` | `[Device.Phone]`         | `Phone` / `Tablet` / `Foldable` / `Desktop`. |
| `themes`          | `Array<Theme>`  | `[Theme.Light]`          | `Light` / `Dark`. Dark adds `uiMode = UI_MODE_NIGHT_YES`. |
| `backgroundColor` | `Long`          | `0xFFFFFFFF` (white)     | When non-default, sets `showBackground = true` + `backgroundColor`. |
| `showSystemUi`    | `Boolean`       | `false`                  | Adds `showSystemUi = true` to each `@Preview`. |

Final preview count is `locales × devices × themes × samples.size`.

## Rules and errors

- The annotated function must be `@Composable` and declare **exactly one** `@PreviewParameter` parameter.
- **One `@AutoPreview` function per file** — generated class names come from the file name, so two in the same file would collide. The processor errors out instead of silently overwriting.
- `samples` must reference either an `object` or a class with `companion object Previews`; anything else errors out.
- If no public `val`s of the parameter type are found, the processor errors with the source class name and the expected type — usually a sealed-state case where the property type wasn't annotated explicitly.

## Requirements

- Kotlin **2.3.21**, KSP **2.3.8**
- Compose Multiplatform **1.10.x** (or AndroidX Compose with the matching `ui-tooling-preview`)
- `minSdk` 28+, `compileSdk` 36
- JVM target 11

## Modules in this repo

- `annotations/` — KMP library (Android / iOS / JVM) with `@AutoPreview`, `Device`, `Theme`.
- `processor/` — JVM KSP processor.
- `sample/` — Android sample app exercising `@AutoPreview` (see `WellbeingScreen.kt`, `OnboardingScreen.kt`, `SettingsScreen.kt`, plus `AppPreview.kt` for the wrapper-annotation pattern).

## Build

```shell
./gradlew :sample:assembleDebug
```
