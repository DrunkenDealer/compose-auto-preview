package app.mashlab.autopreview.sample.feature.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.mashlab.autopreview.annotations.AutoPreview
import app.mashlab.autopreview.annotations.Device
import app.mashlab.autopreview.annotations.Theme
import app.mashlab.autopreview.sample.model.Habit
import app.mashlab.autopreview.sample.model.HabitTint
import app.mashlab.autopreview.sample.model.SampleHabits
import app.mashlab.autopreview.sample.ui.components.HabitIcon
import app.mashlab.autopreview.sample.ui.components.ProgressRing
import app.mashlab.autopreview.sample.ui.theme.BloomTheme

data class WatchState(
    val done: Int,
    val total: Int,
    val next: Habit?,
)

object WatchSamples {
    val InProgress = WatchState(done = 3, total = 5, next = SampleHabits.Run)
    val AlmostThere = WatchState(done = 4, total = 5, next = SampleHabits.Journal)
    val AllDone = WatchState(done = 5, total = 5, next = null)
}

@Composable
fun WatchScreen(
    state: WatchState,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        ProgressRing(
            progress = state.done / state.total.toFloat(),
            modifier = Modifier.fillMaxSize().padding(6.dp),
            strokeWidth = 8.dp,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${state.done}/${state.total}",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "habits today",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                if (state.next != null) {
                    NextHabit(state.next)
                } else {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = "All done",
                        tint = HabitTint.Amber.color,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NextHabit(habit: Habit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HabitIcon(habit.icon, habit.tint, size = 28.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = habit.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// A standalone companion: not reachable from the phone app's entry point, so the graph draws it dashed.
@AutoPreview(
    samplesFrom = WatchSamples::class,
    devices = [Device.Wear],
    themes = [Theme.Dark],
)
@WatchScreenAutoPreviews
@Composable
internal fun WatchScreenPreview(
    @PreviewParameter(WatchScreenPreviewSamplesProvider::class) state: WatchState,
) = BloomTheme(darkTheme = true) { WatchScreen(state) }
