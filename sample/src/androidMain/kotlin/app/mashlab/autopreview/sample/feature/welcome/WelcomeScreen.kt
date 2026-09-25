package app.mashlab.autopreview.sample.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.mashlab.autopreview.annotations.AutoPreview
import app.mashlab.autopreview.annotations.Device
import app.mashlab.autopreview.annotations.Theme
import app.mashlab.autopreview.sample.model.HabitTint
import app.mashlab.autopreview.sample.ui.components.BloomLogo
import app.mashlab.autopreview.sample.ui.components.HabitIcon
import app.mashlab.autopreview.sample.ui.theme.BloomTheme

enum class WelcomePage(
    val title: String,
    val body: String,
    val icon: ImageVector,
    val tint: HabitTint,
    val orbit: List<Pair<ImageVector, HabitTint>>,
) {
    Track(
        title = "Small habits,\nbig bloom",
        body = "Build routines that stick with a gentle daily check-in. One tap, and you're done.",
        icon = Icons.Rounded.Spa,
        tint = HabitTint.Sage,
        orbit = listOf(
            Icons.Rounded.WaterDrop to HabitTint.Sky,
            Icons.AutoMirrored.Rounded.MenuBook to HabitTint.Amber,
            Icons.Rounded.SelfImprovement to HabitTint.Lavender,
        ),
    ),
    Reflect(
        title = "Notice how\nyou feel",
        body = "Log your mood in a second and see which habits lift your days.",
        icon = Icons.Rounded.Mood,
        tint = HabitTint.Lavender,
        orbit = listOf(
            Icons.Rounded.Bedtime to HabitTint.Rose,
            Icons.Rounded.Spa to HabitTint.Sage,
            Icons.Rounded.WaterDrop to HabitTint.Sky,
        ),
    ),
    Grow(
        title = "Grow at\nyour own pace",
        body = "Streaks, insights and reminders that nudge you forward, never nag.",
        icon = Icons.Rounded.EmojiEvents,
        tint = HabitTint.Amber,
        orbit = listOf(
            Icons.AutoMirrored.Rounded.DirectionsRun to HabitTint.Coral,
            Icons.Rounded.SelfImprovement to HabitTint.Lavender,
            Icons.AutoMirrored.Rounded.MenuBook to HabitTint.Amber,
        ),
    ),
}

data class WelcomeState(
    val page: WelcomePage,
)

object WelcomeSamples {
    val Track = WelcomeState(WelcomePage.Track)
    val Reflect = WelcomeState(WelcomePage.Reflect)
    val Grow = WelcomeState(WelcomePage.Grow)
}

@Composable
fun WelcomeScreen(
    state: WelcomeState,
    modifier: Modifier = Modifier,
    onNext: () -> Unit = {},
    onSignIn: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints {
            if (maxWidth >= 840.dp) {
                Row(Modifier.fillMaxSize().padding(24.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Hero(state.page, Modifier.fillMaxHeight(0.8f))
                    }
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Column(Modifier.widthIn(max = 420.dp).padding(horizontal = 32.dp)) {
                            Brand()
                            Spacer(Modifier.height(48.dp))
                            PageText(state.page)
                            Spacer(Modifier.height(40.dp))
                            Actions(state.page, onNext, onSignIn)
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Brand(Modifier.weight(1f))
                        if (state.page != WelcomePage.Grow) TextButton(onClick = onNext) { Text("Skip") }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Hero(state.page, Modifier.fillMaxWidth(0.85f))
                    }
                    PageText(state.page)
                    Spacer(Modifier.height(32.dp))
                    Actions(state.page, onNext, onSignIn)
                }
            }
        }
    }
}

@Composable
private fun Brand(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        BloomLogo(size = 32.dp)
        Spacer(Modifier.width(10.dp))
        Text("Bloom", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun Hero(
    page: WelcomePage,
    modifier: Modifier = Modifier,
) {
    val color = page.tint.color
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .drawBehind {
                drawCircle(color.copy(alpha = 0.08f), radius = size.minDimension / 2)
                drawCircle(color.copy(alpha = 0.14f), radius = size.minDimension * 0.36f)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(page.icon, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize(0.5f))
        }
        val positions = listOf(
            Alignment.TopEnd to Modifier.offset((-16).dp, 24.dp),
            Alignment.CenterStart to Modifier.offset(4.dp, (-24).dp),
            Alignment.BottomEnd to Modifier.offset((-40).dp, (-8).dp),
        )
        page.orbit.zip(positions).forEach { (item, position) ->
            val (icon, tint) = item
            Surface(
                modifier = position.second.align(position.first),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 6.dp,
            ) {
                HabitIcon(icon, tint, Modifier.padding(8.dp), size = 40.dp)
            }
        }
    }
}

@Composable
private fun PageText(page: WelcomePage) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val colors = MaterialTheme.colorScheme
            WelcomePage.entries.forEach {
                val selected = it == page
                Box(
                    Modifier
                        .height(6.dp)
                        .width(if (selected) 24.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (selected) colors.primary else colors.outlineVariant),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(page.title, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Actions(
    page: WelcomePage,
    onNext: () -> Unit,
    onSignIn: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (page == WelcomePage.Grow) "Get started" else "Continue")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSignIn) { Text("I already have an account") }
    }
}

@AutoPreview(
    samplesFrom = WelcomeSamples::class,
    devices = [Device.Phone, Device.Tablet, Device.Foldable],
    themes = [Theme.Light, Theme.Dark],
    navigatesTo = ["SignInScreen"],
    entryPoint = true,
)
@WelcomeScreenAutoPreviews
@Composable
internal fun WelcomeScreenPreview(
    @PreviewParameter(WelcomeScreenPreviewSamplesProvider::class) state: WelcomeState,
) = BloomTheme { WelcomeScreen(state) }
