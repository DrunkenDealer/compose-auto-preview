@file:Suppress("MatchingDeclarationName")

package app.mashlab.autopreview.sample.feature.habit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.mashlab.autopreview.sample.PreviewBloom
import app.mashlab.autopreview.sample.R
import app.mashlab.autopreview.sample.model.Habit
import app.mashlab.autopreview.sample.ui.theme.BloomTheme

object DeleteHabitSamples {
    val WithStreak = HabitDetailSamples.OnStreak
    val NoStreak = HabitDetailSamples.StreakLost
}

@Composable
fun DeleteHabitDialog(
    habit: Habit,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
        title = { Text(stringResource(R.string.habit_delete_title, habit.name)) },
        text = {
            Text(
                if (habit.streak > 0) {
                    pluralStringResource(R.plurals.habit_delete_body_streak, habit.streak, habit.streak)
                } else {
                    stringResource(R.string.habit_delete_body)
                },
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text(stringResource(R.string.habit_delete)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.habit_keep)) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

// The dialog renders in its own window, drawn over the screen it was opened from.
@PreviewBloom(samplesFrom = DeleteHabitSamples::class, navigatesTo = ["TodayScreen"])
@DeleteHabitDialogAutoPreviews
@Composable
internal fun DeleteHabitDialogPreview(
    @PreviewParameter(DeleteHabitDialogPreviewSamplesProvider::class) state: HabitDetailState,
) = BloomTheme {
    HabitDetailScreen(state)
    DeleteHabitDialog(state.habit)
}
