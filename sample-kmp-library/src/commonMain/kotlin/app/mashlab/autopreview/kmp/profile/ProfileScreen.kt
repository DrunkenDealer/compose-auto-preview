package app.mashlab.autopreview.kmp.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mashlab.autopreview.kmp.theme.KitTheme

data class ProfileState(
    val name: String,
    val handle: String,
    val streak: Int,
    val habits: List<String>,
)

object ProfileSamples {
    val Default = ProfileState(
        name = "Maya Lin",
        handle = "@maya",
        streak = 12,
        habits = listOf("Read 20 pages", "Morning run", "Drink water"),
    )
    val NewUser = ProfileState(name = "Alex Kim", handle = "@alex", streak = 0, habits = emptyList())
    val LongName = Default.copy(name = "Maximilian Alexander von Habsburg-Lothringen", streak = 365)
}

@Composable
fun ProfileScreen(
    state: ProfileState,
    modifier: Modifier = Modifier,
) = KitTheme {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.name.take(1),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(state.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(state.handle, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        text = if (state.streak == 0) "No streak yet" else "${state.streak}-day streak",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                if (state.habits.isEmpty()) {
                    Text("Add your first habit to get started.", style = MaterialTheme.typography.bodyLarge)
                }
                state.habits.forEach { habit ->
                    Text("• $habit", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
