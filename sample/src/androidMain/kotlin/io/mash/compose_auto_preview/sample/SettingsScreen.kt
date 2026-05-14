package io.mash.compose_auto_preview.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.mash.compose_auto_preview.annotations.AutoPreview

data class SettingsScreenState(
    val notificationsEnabled: Boolean = true,
    val username: String = "",
)

object SettingsScreenStateSampleData {
    val Default: SettingsScreenState = SettingsScreenState()
    val NotificationsOff: SettingsScreenState = SettingsScreenState(notificationsEnabled = false)
    val Filled: SettingsScreenState = SettingsScreenState(username = "max")
}

@Composable
fun SettingsScreen(state: SettingsScreenState, modifier: Modifier = Modifier) {
    MaterialTheme {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Text("User: ${state.username.ifBlank { "(none)" }}")
            Text("Notifications: ${if (state.notificationsEnabled) "on" else "off"}")
        }
    }
}

@AutoPreview(samples = SettingsScreenStateSampleData::class)
@SettingsScreenPreviews
@Composable
private fun Preview(
    @PreviewParameter(SettingsScreenSamples::class) state: SettingsScreenState,
) = SettingsScreen(state)
