@file:Suppress("MatchingDeclarationName")

package app.mashlab.autopreview.sample.feature.habit

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.mashlab.autopreview.sample.PreviewBloom
import app.mashlab.autopreview.sample.R
import app.mashlab.autopreview.sample.model.Habit
import app.mashlab.autopreview.sample.ui.theme.BloomTheme

object HabitActionsSamples {
    val OnStreak = HabitDetailSamples.OnStreak
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitActionsSheet(
    habit: Habit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            habit.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        listOf(
            Icons.Rounded.Edit to R.string.habit_action_edit,
            Icons.Rounded.Share to R.string.habit_action_share,
            Icons.Rounded.Archive to R.string.habit_action_archive,
        ).forEach { (icon, label) ->
            ListItem(
                headlineContent = { Text(stringResource(label)) },
                leadingContent = { Icon(icon, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.habit_delete), color = MaterialTheme.colorScheme.error) },
            leadingContent = {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

// The sheet renders in its own window, drawn over the screen it was opened from.
@PreviewBloom(samplesFrom = HabitActionsSamples::class, navigatesTo = ["EditHabitScreen", "DeleteHabitDialog"])
@HabitActionsSheetAutoPreviews
@Composable
internal fun HabitActionsSheetPreview(
    @PreviewParameter(HabitActionsSheetPreviewSamplesProvider::class) state: HabitDetailState,
) = BloomTheme {
    HabitDetailScreen(state)
    HabitActionsSheet(state.habit)
}
