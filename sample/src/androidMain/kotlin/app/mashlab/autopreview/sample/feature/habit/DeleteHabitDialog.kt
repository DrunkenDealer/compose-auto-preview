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
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.mashlab.autopreview.sample.PreviewBloom
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
        title = { Text("Delete “${habit.name}”?") },
        text = {
            Text(
                if (habit.streak > 0) {
                    "You'll lose your ${habit.streak}-day streak and all check-in history. This can't be undone."
                } else {
                    "All check-in history for this habit will be removed. This can't be undone."
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
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep habit") } },
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
