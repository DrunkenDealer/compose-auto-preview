package app.mashlab.autopreview.sample.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.mashlab.autopreview.annotations.AutoPreview
import app.mashlab.autopreview.annotations.Device
import app.mashlab.autopreview.annotations.Theme
import app.mashlab.autopreview.sample.model.Habit
import app.mashlab.autopreview.sample.model.Mood
import app.mashlab.autopreview.sample.model.SampleHabits
import app.mashlab.autopreview.sample.ui.components.BloomCard
import app.mashlab.autopreview.sample.ui.components.BloomScaffold
import app.mashlab.autopreview.sample.ui.components.BloomTab
import app.mashlab.autopreview.sample.ui.components.EmptyState
import app.mashlab.autopreview.sample.ui.components.HabitRow
import app.mashlab.autopreview.sample.ui.components.InfoBanner
import app.mashlab.autopreview.sample.ui.components.ProgressRing
import app.mashlab.autopreview.sample.ui.components.SectionHeader
import app.mashlab.autopreview.sample.ui.components.SkeletonBlock
import app.mashlab.autopreview.sample.ui.theme.BloomTheme

data class TodayState(
    val name: String = "Maya",
    val date: String = "Thursday, 25 September",
    val habits: List<Habit> = emptyList(),
    val streak: Int = 0,
    val week: List<Boolean> = emptyList(),
    val mood: Mood? = null,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
)

object TodaySamples {
    private val Week = listOf(true, true, false, true, false, false, false)

    val InProgress = TodayState(habits = SampleHabits.Today, streak = 12, week = Week, mood = Mood.Good)
    val AllDone = InProgress.copy(
        habits = SampleHabits.Today.map { it.copy(done = true) },
        week = Week.take(3) + true + Week.drop(4),
        mood = Mood.Great,
    )
    val FirstDay = TodayState()
    val Loading = TodayState(isLoading = true)
    val Offline = InProgress.copy(isOffline = true, mood = null)
}

@Composable
fun TodayScreen(
    state: TodayState,
    modifier: Modifier = Modifier,
    onTabSelect: (BloomTab) -> Unit = {},
    onHabitClick: (Habit) -> Unit = {},
    onAddHabit: () -> Unit = {},
) {
    BloomScaffold(
        selectedTab = BloomTab.Today,
        modifier = modifier,
        onTabSelect = onTabSelect,
        floatingActionButton = { expanded ->
            if (state.habits.isNotEmpty()) AddHabitButton(expanded, onAddHabit)
        },
    ) { expanded ->
        when {
            state.isLoading -> LoadingContent(expanded)

            state.habits.isEmpty() -> EmptyContent(state, onAddHabit)

            expanded -> Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(Modifier.weight(0.42f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Greeting(state)
                    if (state.isOffline) OfflineBanner()
                    SummaryCard(state)
                    MoodCard(state.mood)
                }
                Column(Modifier.weight(0.58f).padding(top = 8.dp)) {
                    HabitList(state.habits, onHabitClick)
                }
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Greeting(state)
                if (state.isOffline) OfflineBanner()
                SummaryCard(state)
                HabitList(state.habits, onHabitClick)
                MoodCard(state.mood)
                Spacer(Modifier.height(64.dp))
            }
        }
    }
}

@Composable
private fun AddHabitButton(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    if (expanded) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) { Icon(Icons.Rounded.Add, contentDescription = "New habit") }
        return
    }
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        text = { Text("New habit") },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    )
}

@Composable
private fun Greeting(state: TodayState) {
    Column {
        Text(
            text = state.date.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text("Good morning, ${state.name}", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun OfflineBanner() {
    InfoBanner(
        icon = Icons.Rounded.CloudOff,
        text = "You're offline. Check-ins will sync when you reconnect.",
    ) {
        TextButton(onClick = {}) { Text("Retry") }
    }
}

@Composable
private fun SummaryCard(state: TodayState) {
    val done = state.habits.count { it.done }
    val total = state.habits.size
    val allDone = done == total
    BloomCard(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = done / total.toFloat(),
                modifier = Modifier.size(96.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer
                    .copy(alpha = 0.12f),
            ) {
                if (allDone) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                } else {
                    Text("$done/$total", style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(
                    text = if (allDone) "Perfect day!" else "${total - done} habits to go",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${state.streak}-day streak. Keep it growing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                        .copy(alpha = 0.8f),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        WeekStrip(state.week)
    }
}

@Composable
private fun WeekStrip(week: List<Boolean>) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val today = 3
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { index, day ->
            val done = week.getOrElse(index) { false }
            val colors = MaterialTheme.colorScheme
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onPrimaryContainer.copy(alpha = if (index == today) 1f else 0.6f),
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                done -> colors.primary
                                index == today -> colors.onPrimaryContainer.copy(alpha = 0.2f)
                                else -> colors.onPrimaryContainer.copy(alpha = 0.08f)
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun HabitList(
    habits: List<Habit>,
    onHabitClick: (Habit) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Today's habits", trailing = "${habits.count { it.done }} of ${habits.size}")
        habits.forEach { habit ->
            HabitRow(habit, onClick = { onHabitClick(habit) })
        }
    }
}

private val MoodIcons: Map<Mood, ImageVector> = mapOf(
    Mood.Awful to Icons.Rounded.SentimentVeryDissatisfied,
    Mood.Low to Icons.Rounded.SentimentDissatisfied,
    Mood.Okay to Icons.Rounded.SentimentNeutral,
    Mood.Good to Icons.Rounded.SentimentSatisfied,
    Mood.Great to Icons.Rounded.SentimentVerySatisfied,
)

@Composable
private fun MoodCard(mood: Mood?) {
    BloomCard {
        Text("How are you feeling?", style = MaterialTheme.typography.titleMedium)
        Text(
            text = if (mood == null) "Tap to log today's mood" else "Logged: ${mood.name.lowercase()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Mood.entries.forEach {
                val selected = it == mood
                Surface(
                    onClick = {},
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            MoodIcons.getValue(it),
                            contentDescription = it.name,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(
    state: TodayState,
    onAddHabit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth()) { Greeting(state) }
        Spacer(Modifier.height(48.dp))
        EmptyState(
            icon = Icons.Rounded.Park,
            title = "Plant your first habit",
            body = "Start with something small. Two minutes a day is enough to begin.",
        ) {
            Button(onClick = onAddHabit, modifier = Modifier.height(52.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create a habit")
            }
        }
        Text(
            text = "Or try one of these",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(SampleHabits.Water, SampleHabits.Meditate, SampleHabits.Read).forEach {
                AssistChip(
                    onClick = onAddHabit,
                    label = { Text(it.name) },
                    leadingIcon = { Icon(it.icon, contentDescription = null, tint = it.tint.color) },
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(expanded: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(if (expanded) 32.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBlock(Modifier.width(140.dp).height(14.dp))
        SkeletonBlock(Modifier.width(240.dp).height(30.dp))
        Spacer(Modifier.height(4.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(200.dp), MaterialTheme.shapes.large)
        Spacer(Modifier.height(4.dp))
        repeat(4) { SkeletonBlock(Modifier.fillMaxWidth().height(68.dp), MaterialTheme.shapes.medium) }
    }
}

@AutoPreview(
    samplesFrom = TodaySamples::class,
    devices = [Device.Phone, Device.Tablet, Device.Foldable, Device.Desktop],
    themes = [Theme.Light, Theme.Dark],
    navigatesTo = ["HabitDetailScreen", "EditHabitScreen", "InsightsScreen", "ProfileScreen"],
)
@TodayScreenAutoPreviews
@Composable
internal fun TodayScreenPreview(
    @PreviewParameter(TodayScreenPreviewSamplesProvider::class) state: TodayState,
) = BloomTheme { TodayScreen(state) }
