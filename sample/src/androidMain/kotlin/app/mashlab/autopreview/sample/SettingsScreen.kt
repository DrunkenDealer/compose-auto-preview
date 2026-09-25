package app.mashlab.autopreview.sample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.mashlab.autopreview.annotations.AutoPreview
import app.mashlab.autopreview.annotations.Device
import app.mashlab.autopreview.annotations.Theme

data class SettingsScreenState(
    val notificationsEnabled: Boolean = true,
    val username: String = "",
)

object SettingsScreenStateSampleData {
    val Default: SettingsScreenState = SettingsScreenState()
    val NotificationsOff: SettingsScreenState = SettingsScreenState(notificationsEnabled = false)
    val Filled: SettingsScreenState = SettingsScreenState(username = "max")
    val FilledNotificationsOff: SettingsScreenState =
        SettingsScreenState(notificationsEnabled = false, username = "max")
    val LongName: SettingsScreenState =
        SettingsScreenState(username = "max-with-an-unreasonably-long-handle")
}

@Composable
fun SettingsScreen(
    state: SettingsScreenState,
    modifier: Modifier = Modifier,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    ) {
        Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Settings",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Username") },
                    supportingContent = { Text(state.username.ifBlank { "(not set)" }) },
                    leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Notifications") },
                    supportingContent = {
                        Text(if (state.notificationsEnabled) "Enabled" else "Disabled")
                    },
                    leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    trailingContent = {
                        Switch(checked = state.notificationsEnabled, onCheckedChange = null)
                    },
                )
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@AutoPreview(
    samplesFrom = SettingsScreenStateSampleData::class,
    devices = [Device.Phone, Device.Tablet],
    themes = [Theme.Light, Theme.Dark],
    backgroundColor = 0xFFF5F5F5,
    navigatesTo = ["ConfirmDialog", "WellbeingScreen"],
)
@SettingsScreenAutoPreviews
@Composable
internal fun SettingsScreenPreview(
    @PreviewParameter(SettingsScreenPreviewSamplesProvider::class) state: SettingsScreenState,
) = SettingsScreen(state)
