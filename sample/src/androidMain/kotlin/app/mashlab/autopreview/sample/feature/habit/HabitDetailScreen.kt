package app.mashlab.autopreview.sample.feature.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.mashlab.autopreview.sample.PreviewBloom
import app.mashlab.autopreview.sample.model.Habit
import app.mashlab.autopreview.sample.model.SampleHabits
import app.mashlab.autopreview.sample.ui.components.BloomCard
import app.mashlab.autopreview.sample.ui.components.HabitIcon
import app.mashlab.autopreview.sample.ui.components.StatTile
import app.mashlab.autopreview.sample.ui.theme.BloomTheme

/**
 * @param history one char per day of the month: `x` done, `.` missed, `-` not tracked yet.
 */
data class HabitDetailState(
    val habit: Habit,
    val history: String,
    val bestStreak: Int,
    val completion: Int,
    val checkIns: Int,
    val reminder: String? = null,
)

object HabitDetailSamples {
    val OnStreak = HabitDetailState(
        habit = SampleHabits.Read.copy(done = true),
        history = "xx.." + "x".repeat(21),
        bestStreak = 34,
        completion = 92,
        checkIns = 148,
        reminder = "Every day at 21:30",
    )
    val JustStarted = HabitDetailState(
        habit = SampleHabits.Journal.copy(streak = 3, done = true),
        history = "-".repeat(22) + "xxx",
        bestStreak = 3,
        completion = 100,
        checkIns = 3,
    )
    val StreakLost = HabitDetailState(
        habit = SampleHabits.Meditate.copy(streak = 0),
        history = "xxxxx.xxxxxxxxxxxxxx.x...",
        bestStreak = 18,
        completion = 71,
        checkIns = 64,
        reminder = "Weekdays at 07:00",
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    state: HabitDetailState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Edit") }
                    IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = { CheckInButton(state.habit) },
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding)) {
            if (maxWidth >= 600.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Header(state.habit)
                        StreakCard(state)
                        Stats(state)
                        ReminderCard(state.reminder)
                    }
                    Column(Modifier.weight(1f)) { CalendarCard(state) }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Header(state.habit)
                    StreakCard(state)
                    Stats(state)
                    CalendarCard(state)
                    ReminderCard(state.reminder)
                }
            }
        }
    }
}

@Composable
private fun Header(habit: Habit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HabitIcon(habit.icon, habit.tint, size = 64.dp)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(habit.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = habit.schedule,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StreakCard(state: HabitDetailState) {
    val streak = state.habit.streak
    val tint = state.habit.tint.color
    BloomCard(color = tint.copy(alpha = 0.14f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (streak > 0) "$streak days" else "Streak reset",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (streak > 0) {
                        "Current streak · best is ${state.bestStreak}"
                    } else {
                        "Check in today to start a new one"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(tint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun Stats(state: HabitDetailState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile("${state.completion}%", "Completion", Modifier.weight(1f))
        StatTile("${state.bestStreak}", "Best streak", Modifier.weight(1f))
        StatTile("${state.checkIns}", "Check-ins", Modifier.weight(1f))
    }
}

@Composable
private fun CalendarCard(state: HabitDetailState) {
    val tint = state.habit.tint.color
    val firstWeekday = 1
    val daysInMonth = 30
    BloomCard {
        Text("September 2026", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Row {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        (List(firstWeekday) { null } + (1..daysInMonth)).chunked(7).forEach { week ->
            Row {
                repeat(7) { col ->
                    val day = week.getOrNull(col)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) DayCell(day, state.history.getOrNull(day - 1), tint)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    status: Char?,
    tint: Color,
) {
    val colors = MaterialTheme.colorScheme
    val base = Modifier.fillMaxSize().clip(CircleShape)
    val (modifier, textColor) = when (status) {
        'x' -> base.background(tint) to Color.White
        '.' -> base.border(1.5.dp, colors.outlineVariant, CircleShape) to colors.onSurfaceVariant
        else -> base to colors.onSurfaceVariant.copy(alpha = 0.45f)
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Text("$day", style = MaterialTheme.typography.labelMedium, color = textColor)
    }
}

@Composable
private fun ReminderCard(reminder: String?) {
    BloomCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Reminder", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = reminder ?: "Off",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = reminder != null, onCheckedChange = null)
        }
    }
}

@Composable
private fun CheckInButton(habit: Habit) {
    val modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
        .height(56.dp)
    if (habit.done) {
        FilledTonalButton(onClick = {}, modifier = modifier) {
            Icon(Icons.Rounded.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Done for today")
        }
    } else {
        Button(
            onClick = {},
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = habit.tint.color, contentColor = Color.White),
        ) {
            Text("Mark as done")
        }
    }
}

@PreviewBloom(samplesFrom = HabitDetailSamples::class, navigatesTo = ["EditHabitScreen", "DeleteHabitDialog"])
@HabitDetailScreenAutoPreviews
@Composable
internal fun HabitDetailScreenPreview(
    @PreviewParameter(HabitDetailScreenPreviewSamplesProvider::class) state: HabitDetailState,
) = BloomTheme { HabitDetailScreen(state) }
