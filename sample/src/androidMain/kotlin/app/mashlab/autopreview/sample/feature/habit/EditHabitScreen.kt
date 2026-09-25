package app.mashlab.autopreview.sample.feature.habit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.mashlab.autopreview.sample.PreviewBloom
import app.mashlab.autopreview.sample.model.Habit
import app.mashlab.autopreview.sample.model.HabitTint
import app.mashlab.autopreview.sample.ui.components.HabitRow
import app.mashlab.autopreview.sample.ui.theme.BloomTheme

private val HabitIcons = listOf(
    Icons.Rounded.Spa,
    Icons.Rounded.WaterDrop,
    Icons.Rounded.SelfImprovement,
    Icons.AutoMirrored.Rounded.DirectionsRun,
    Icons.AutoMirrored.Rounded.MenuBook,
    Icons.Rounded.EditNote,
    Icons.Rounded.Bedtime,
)

private val Weekdays = listOf("M", "T", "W", "T", "F", "S", "S")

data class EditHabitState(
    val name: String = "",
    val icon: ImageVector = Icons.Rounded.Spa,
    val tint: HabitTint = HabitTint.Sage,
    val days: Set<Int> = (0..6).toSet(),
    val reminder: String? = null,
    val nameError: String? = null,
    val isEditing: Boolean = false,
)

object EditHabitSamples {
    val Blank = EditHabitState()
    val Filled = EditHabitState(
        name = "Morning run",
        icon = Icons.AutoMirrored.Rounded.DirectionsRun,
        tint = HabitTint.Coral,
        days = setOf(0, 2, 4),
        reminder = "07:00",
    )
    val Invalid = EditHabitState(days = emptySet(), nameError = "Give your habit a name")
    val Editing = EditHabitState(
        name = "Read",
        icon = Icons.AutoMirrored.Rounded.MenuBook,
        tint = HabitTint.Amber,
        reminder = "21:30",
        isEditing = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitScreen(
    state: EditHabitState,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onSave: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit habit" else "New habit") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HabitRow(
                    Habit(
                        name = state.name.ifBlank { "Your new habit" },
                        icon = state.icon,
                        tint = state.tint,
                        schedule = scheduleLabel(state),
                        streak = 0,
                    ),
                )
                OutlinedTextField(
                    value = state.name,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Drink water") },
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { { Text(it) } },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )
                Section("Icon") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HabitIcons.forEach { icon ->
                            IconChoice(icon, selected = icon == state.icon, tint = state.tint, Modifier.weight(1f))
                        }
                    }
                }
                Section("Colour") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HabitTint.entries.forEach { ColorChoice(it, selected = it == state.tint) }
                    }
                }
                Section("Repeat", error = "Pick at least one day".takeIf { state.days.isEmpty() }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Weekdays.forEachIndexed { index, day ->
                            DayChoice(day, selected = index in state.days, Modifier.weight(1f))
                        }
                    }
                }
                ReminderRow(state.reminder)
                Spacer(Modifier.height(4.dp))
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(if (state.isEditing) "Save changes" else "Create habit")
                }
                if (state.isEditing) {
                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete habit", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

private fun scheduleLabel(state: EditHabitState): String {
    val days = when (state.days.size) {
        0 -> {
            "No days selected"
        }

        7 -> {
            "Every day"
        }

        else -> {
            state.days
                .sorted()
                .joinToString(" ") { Weekdays[it] }
        }
    }
    return state.reminder?.let { "$days · $it" } ?: days
}

@Composable
private fun Section(
    title: String,
    error: String? = null,
    content: @Composable () -> Unit,
) {
    Column {
        Row {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun IconChoice(
    icon: ImageVector,
    selected: Boolean,
    tint: HabitTint,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = {},
        modifier = modifier.aspectRatio(1f),
        shape = MaterialTheme.shapes.small,
        color = if (selected) tint.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (selected) BorderStroke(2.dp, tint.color) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) tint.color else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ColorChoice(
    tint: HabitTint,
    selected: Boolean,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(tint.color)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.background, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun DayChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (selected) colors.primary else colors.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReminderRow(reminder: String?) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Remind me", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = reminder?.let { "At $it" } ?: "No reminder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = reminder != null, onCheckedChange = null)
        }
    }
}

@PreviewBloom(samplesFrom = EditHabitSamples::class, navigatesTo = ["TodayScreen", "DeleteHabitDialog"])
@EditHabitScreenAutoPreviews
@Composable
internal fun EditHabitScreenPreview(
    @PreviewParameter(EditHabitScreenPreviewSamplesProvider::class) state: EditHabitState,
) = BloomTheme { EditHabitScreen(state) }
