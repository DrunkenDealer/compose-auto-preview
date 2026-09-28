package app.mashlab.autopreview.sample.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.mashlab.autopreview.sample.PreviewBloom
import app.mashlab.autopreview.sample.R
import app.mashlab.autopreview.sample.ui.components.Avatar
import app.mashlab.autopreview.sample.ui.components.BloomCard
import app.mashlab.autopreview.sample.ui.components.BloomScaffold
import app.mashlab.autopreview.sample.ui.components.BloomTab
import app.mashlab.autopreview.sample.ui.components.StatTile
import app.mashlab.autopreview.sample.ui.theme.BloomTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class ProfileState(
    val name: String = "Maya Lin",
    val email: String = "maya.lin@example.com",
    val memberSince: YearMonth = YearMonth.of(2025, 3),
    val isPremium: Boolean = false,
    val remindersOn: Boolean = true,
    val weeklyReportOn: Boolean = true,
)

object ProfileSamples {
    val Free = ProfileState()
    val Premium = ProfileState(isPremium = true)
    val RemindersOff = ProfileState(remindersOn = false, weeklyReportOn = false)
    val LongName = ProfileState(
        name = "Maximilian Alexander von Habsburg-Lothringen",
        email = "maximilian.alexander.habsburg@very-long-company-domain.example.com",
    )
}

@Composable
fun ProfileScreen(
    state: ProfileState,
    modifier: Modifier = Modifier,
    onTabSelect: (BloomTab) -> Unit = {},
    onUpgrade: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    BloomScaffold(selectedTab = BloomTab.Profile, modifier = modifier, onTabSelect = onTabSelect) { expanded ->
        Box(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 640.dp).padding(if (expanded) 32.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Header(state)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("6", stringResource(R.string.profile_stat_habits), Modifier.weight(1f))
                    StatTile("34", stringResource(R.string.profile_stat_best_streak), Modifier.weight(1f))
                    StatTile("318", stringResource(R.string.profile_stat_check_ins), Modifier.weight(1f))
                }
                if (!state.isPremium) UpgradeCard(onUpgrade)
                SettingsGroup {
                    SettingRow(
                        Icons.Rounded.Notifications,
                        stringResource(R.string.profile_reminders),
                        stringResource(if (state.remindersOn) R.string.profile_on else R.string.profile_off),
                    ) {
                        Switch(checked = state.remindersOn, onCheckedChange = null)
                    }
                    SettingRow(
                        Icons.Rounded.Mail,
                        stringResource(R.string.profile_weekly_report),
                        stringResource(R.string.profile_weekly_report_schedule),
                    ) {
                        Switch(checked = state.weeklyReportOn, onCheckedChange = null)
                    }
                    SettingRow(
                        Icons.Rounded.DarkMode,
                        stringResource(R.string.profile_appearance),
                        stringResource(R.string.profile_appearance_system),
                    )
                    SettingRow(
                        Icons.Rounded.Language,
                        stringResource(R.string.profile_language),
                        stringResource(R.string.profile_language_current),
                    )
                }
                SettingsGroup {
                    SettingRow(
                        Icons.Rounded.Download,
                        stringResource(R.string.profile_export),
                        stringResource(R.string.profile_export_formats),
                    )
                    SettingRow(Icons.Rounded.Shield, stringResource(R.string.profile_privacy), null)
                }
                TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_sign_out))
                }
                Text(
                    text = "Bloom 2.4.0",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Header(state: ProfileState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(
            state.name
                .split(" ")
                .take(2)
                .joinToString("") { it.take(1) },
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = state.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            if (state.isPremium) {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.profile_premium),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            } else {
                val locale = LocalConfiguration.current.locales[0]
                Text(
                    text = stringResource(
                        R.string.profile_member_since,
                        state.memberSince.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UpgradeCard(onUpgrade: () -> Unit) {
    BloomCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_upgrade_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = stringResource(R.string.profile_upgrade_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                        .copy(alpha = 0.8f),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onUpgrade,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
        ) { Text(stringResource(R.string.profile_upgrade_cta)) }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(vertical = 8.dp)) { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    trailing: @Composable () -> Unit = {
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    },
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}

@PreviewBloom(
    samplesFrom = ProfileSamples::class,
    navigatesTo = ["PremiumScreen", "WelcomeScreen"],
    group = "Bottom navigation",
)
@ProfileScreenAutoPreviews
@Composable
internal fun ProfileScreenPreview(
    @PreviewParameter(ProfileScreenPreviewSamplesProvider::class) state: ProfileState,
) = BloomTheme { ProfileScreen(state) }
