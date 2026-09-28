package app.mashlab.autopreview.sample.feature.insights

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.mashlab.autopreview.annotations.AutoPreview
import app.mashlab.autopreview.annotations.Device
import app.mashlab.autopreview.annotations.Theme
import app.mashlab.autopreview.sample.R
import app.mashlab.autopreview.sample.model.Habit
import app.mashlab.autopreview.sample.model.HabitTint
import app.mashlab.autopreview.sample.model.SampleHabits
import app.mashlab.autopreview.sample.ui.components.BarChart
import app.mashlab.autopreview.sample.ui.components.BloomCard
import app.mashlab.autopreview.sample.ui.components.BloomScaffold
import app.mashlab.autopreview.sample.ui.components.BloomTab
import app.mashlab.autopreview.sample.ui.components.EmptyState
import app.mashlab.autopreview.sample.ui.components.HabitIcon
import app.mashlab.autopreview.sample.ui.components.LineChart
import app.mashlab.autopreview.sample.ui.components.SkeletonBlock
import app.mashlab.autopreview.sample.ui.components.StatTile
import app.mashlab.autopreview.sample.ui.theme.BloomTheme
import java.time.DayOfWeek
import java.time.format.TextStyle

enum class InsightsRange(
    @StringRes val label: Int,
) {
    Week(R.string.insights_range_week),
    Month(R.string.insights_range_month),
}

data class InsightsState(
    val range: InsightsRange = InsightsRange.Week,
    val completion: List<Float> = emptyList(),
    val firstWeek: Int = 36,
    val mood: List<Float> = emptyList(),
    val topHabits: List<Pair<Habit, Float>> = emptyList(),
    val rate: Int = 0,
    val perfectDays: Int = 0,
    val streak: Int = 0,
    val isLoading: Boolean = false,
)

object InsightsSamples {
    private val Top = listOf(
        SampleHabits.Read to 0.96f,
        SampleHabits.Meditate to 0.88f,
        SampleHabits.Water to 0.74f,
        SampleHabits.Run to 0.52f,
    )

    val Week = InsightsState(
        completion = listOf(0.8f, 1f, 0.6f, 0.8f, 0.4f, 1f, 0.6f),
        mood = listOf(0.5f, 0.62f, 0.45f, 0.7f, 0.66f, 0.85f, 0.8f),
        topHabits = Top,
        rate = 82,
        perfectDays = 2,
        streak = 12,
    )
    val Month = Week.copy(
        range = InsightsRange.Month,
        completion = listOf(0.55f, 0.7f, 0.64f, 0.82f),
        mood = listOf(0.4f, 0.48f, 0.44f, 0.58f, 0.63f, 0.6f, 0.72f, 0.7f, 0.78f, 0.84f),
        rate = 74,
        perfectDays = 9,
    )
    val NotEnoughData = InsightsState()
    val Loading = InsightsState(isLoading = true)
}

@Composable
fun InsightsScreen(
    state: InsightsState,
    modifier: Modifier = Modifier,
    onTabSelect: (BloomTab) -> Unit = {},
) {
    BloomScaffold(selectedTab = BloomTab.Insights, modifier = modifier, onTabSelect = onTabSelect) { expanded ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(if (expanded) 32.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.insights_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    RangePicker(state.range, Modifier.widthIn(max = 220.dp))
                }
            }
            when {
                state.isLoading -> {
                    Loading()
                }

                state.completion.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Rounded.Insights,
                        title = stringResource(R.string.insights_empty_title),
                        body = stringResource(R.string.insights_empty_body),
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    )
                }

                expanded -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Stats(state)
                            CompletionCard(state)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MoodCard(state)
                            TopHabitsCard(state)
                        }
                    }
                }

                else -> {
                    Stats(state)
                    CompletionCard(state)
                    MoodCard(state)
                    TopHabitsCard(state)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePicker(
    range: InsightsRange,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier) {
        InsightsRange.entries.forEachIndexed { index, item ->
            SegmentedButton(
                selected = item == range,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index, InsightsRange.entries.size),
                icon = {},
            ) { Text(stringResource(item.label)) }
        }
    }
}

@Composable
private fun Stats(state: InsightsState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(
            "${state.rate}%",
            stringResource(R.string.insights_completion),
            Modifier.weight(1f),
            Icons.Rounded.CheckCircle,
        )
        StatTile(
            "${state.streak}",
            stringResource(R.string.insights_day_streak),
            Modifier.weight(1f),
            Icons.Rounded.LocalFireDepartment,
            HabitTint.Amber.color,
        )
        StatTile(
            "${state.perfectDays}",
            stringResource(R.string.insights_perfect_days),
            Modifier.weight(1f),
            Icons.Rounded.EmojiEvents,
            MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun CompletionCard(state: InsightsState) {
    BloomCard {
        Text(stringResource(R.string.insights_check_ins), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(
                if (state.range == InsightsRange.Week) R.string.insights_by_day else R.string.insights_by_week,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        BarChart(
            values = state.completion,
            labels = labels(state),
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
    }
}

@Composable
private fun labels(state: InsightsState): List<String> =
    if (state.range == InsightsRange.Week) {
        val locale = LocalConfiguration.current.locales[0]
        DayOfWeek.entries.map { it.getDisplayName(TextStyle.SHORT, locale) }
    } else {
        state.completion.indices
            .map { stringResource(R.string.insights_week_label, state.firstWeek + it) }
    }

@Composable
private fun MoodCard(state: InsightsState) {
    BloomCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.insights_mood), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.insights_mood_trend),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("+18%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
        }
        Spacer(Modifier.height(16.dp))
        LineChart(state.mood, Modifier.fillMaxWidth().height(140.dp))
    }
}

@Composable
private fun TopHabitsCard(state: InsightsState) {
    BloomCard {
        Text(stringResource(R.string.insights_most_consistent), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        state.topHabits.forEach { (habit, rate) ->
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                HabitIcon(habit.icon, habit.tint, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(habit.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            text = "${(rate * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { rate },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = habit.tint.color,
                        trackColor = habit.tint.color
                            .copy(alpha = 0.16f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { SkeletonBlock(Modifier.weight(1f).height(104.dp), MaterialTheme.shapes.medium) }
        }
        SkeletonBlock(Modifier.fillMaxWidth().height(260.dp), MaterialTheme.shapes.large)
        SkeletonBlock(Modifier.fillMaxWidth().height(220.dp), MaterialTheme.shapes.large)
    }
}

@AutoPreview(
    samplesFrom = InsightsSamples::class,
    locales = ["en", "de", "uk"],
    devices = [Device.Phone, Device.Tablet, Device.Foldable],
    themes = [Theme.Light, Theme.Dark],
    group = "Bottom navigation",
)
@InsightsScreenAutoPreviews
@Composable
internal fun InsightsScreenPreview(
    @PreviewParameter(InsightsScreenPreviewSamplesProvider::class) state: InsightsState,
) = BloomTheme { InsightsScreen(state) }
