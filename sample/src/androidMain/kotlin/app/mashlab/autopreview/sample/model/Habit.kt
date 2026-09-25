package app.mashlab.autopreview.sample.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class HabitTint(
    val color: Color,
) {
    Sage(Color(0xFF4CAF7D)),
    Sky(Color(0xFF4A90E2)),
    Amber(Color(0xFFE9A23B)),
    Coral(Color(0xFFE8735A)),
    Lavender(Color(0xFF8E7CC3)),
    Rose(Color(0xFFD9689A)),
}

data class Habit(
    val name: String,
    val icon: ImageVector,
    val tint: HabitTint,
    val schedule: String,
    val streak: Int,
    val done: Boolean = false,
)

object SampleHabits {
    val Meditate = Habit("Meditate", Icons.Rounded.SelfImprovement, HabitTint.Lavender, "10 min · every morning", 12)
    val Water = Habit("Drink water", Icons.Rounded.WaterDrop, HabitTint.Sky, "8 glasses · daily", 5)
    val Run = Habit("Morning run", Icons.AutoMirrored.Rounded.DirectionsRun, HabitTint.Coral, "5 km · Mon, Wed, Fri", 3)
    val Read = Habit("Read", Icons.AutoMirrored.Rounded.MenuBook, HabitTint.Amber, "20 pages before bed", 21)
    val Journal = Habit("Journal", Icons.Rounded.EditNote, HabitTint.Sage, "3 lines · evenings", 7)
    val Sleep = Habit("Lights out by 23:00", Icons.Rounded.Bedtime, HabitTint.Rose, "Every night", 0)

    val Today = listOf(
        Meditate.copy(done = true),
        Water.copy(done = true),
        Run,
        Read.copy(done = true),
        Journal,
    )
}

enum class Mood {
    Awful,
    Low,
    Okay,
    Good,
    Great,
}
