package io.github.drunkendealer.composeautopreview.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.github.drunkendealer.composeautopreview.annotations.AutoPreview
import io.github.drunkendealer.composeautopreview.annotations.Device
import io.github.drunkendealer.composeautopreview.annotations.Theme

data class WellbeingScreenState(
    val isLoading: Boolean = false,
    val items: List<String> = emptyList(),
    val error: String? = null,
)

object PreviewsTest {
    val Loading = WellbeingScreenState(isLoading = true)
    val Empty = WellbeingScreenState()
    val Loaded = WellbeingScreenState(items = listOf("Meditate", "Journal", "Walk"))
    val LoadedLong = WellbeingScreenState(
        items = listOf("Meditate", "Journal", "Walk", "Stretch", "Hydrate", "Sleep early"),
    )
    val Error = WellbeingScreenState(error = "Failed to load")
}

@Composable
fun WellbeingScreen(state: WellbeingScreenState, modifier: Modifier = Modifier) {
    MaterialTheme {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.error != null -> Text("Error: ${state.error}")
                state.items.isEmpty() -> Text("Nothing yet")
                else -> state.items.forEach {
                    Text(it)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@AutoPreview(
    samplesFrom = PreviewsTest::class,
    devices = [Device.Phone, Device.Tablet, Device.Foldable, Device.Tv, Device.Wear],
    themes = [Theme.Light, Theme.Dark],
    navigatesTo = ["SettingsScreen"],
)
@WellbeingScreenAutoPreviews
@Composable
internal fun WellbeingScreenPreview(
    @PreviewParameter(WellbeingScreenPreviewSamplesProvider::class) state: WellbeingScreenState,
) = WellbeingScreen(state)
