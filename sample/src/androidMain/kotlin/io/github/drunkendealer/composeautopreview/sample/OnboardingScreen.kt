package io.github.drunkendealer.composeautopreview.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.github.drunkendealer.composeautopreview.annotations.AutoPreview
import io.github.drunkendealer.composeautopreview.annotations.Device
import io.github.drunkendealer.composeautopreview.annotations.Theme

sealed interface OnboardingScreenState {
    data object Welcome : OnboardingScreenState
    data class CategorySelection(val selected: Set<String> = emptySet()) : OnboardingScreenState
    data class Complete(val summary: String) : OnboardingScreenState

    companion object Previews {
        val Welcome: OnboardingScreenState = OnboardingScreenState.Welcome
        val CategoriesEmpty: OnboardingScreenState = CategorySelection()
        val CategoriesPicked: OnboardingScreenState = CategorySelection(selected = setOf("Mind", "Body"))
        val CategoriesAll: OnboardingScreenState =
            CategorySelection(selected = setOf("Mind", "Body", "Sleep", "Focus", "Energy"))
        val Complete: OnboardingScreenState = Complete(summary = "3 habits, reminders on")
    }
}

@Composable
fun OnboardingScreen(state: OnboardingScreenState, modifier: Modifier = Modifier) {
    MaterialTheme {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            val label = when (state) {
                OnboardingScreenState.Welcome -> "Welcome"
                is OnboardingScreenState.CategorySelection ->
                    if (state.selected.isEmpty()) "Pick categories" else "Picked: ${state.selected.joinToString()}"
                is OnboardingScreenState.Complete -> "Done — ${state.summary}"
            }
            Text(label)
        }
    }
}

@AutoPreview(
    samplesFrom = OnboardingScreenState::class,
    locale = "fr",
    devices = [Device.Phone, Device.Tablet, Device.Foldable, Device.Desktop],
    themes = [Theme.Light, Theme.Dark],
    showSystemUi = true,
    navigatesTo = ["WellbeingScreen"],
    entryPoint = true,
)
@OnboardingScreenAutoPreviews
@Composable
internal fun OnboardingScreenPreview(
    @PreviewParameter(OnboardingScreenPreviewSamplesProvider::class) state: OnboardingScreenState,
) = OnboardingScreen(state)
