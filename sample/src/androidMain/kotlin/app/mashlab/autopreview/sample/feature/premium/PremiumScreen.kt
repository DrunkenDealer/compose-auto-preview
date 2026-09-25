package app.mashlab.autopreview.sample.feature.premium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.mashlab.autopreview.sample.PreviewBloom
import app.mashlab.autopreview.sample.ui.components.InfoBanner
import app.mashlab.autopreview.sample.ui.theme.BloomTheme

enum class Plan(
    val title: String,
    val price: String,
    val note: String,
    val badge: String?,
) {
    Yearly("Yearly", "€29.99 / year", "Just €2.50 a month", "Save 58%"),
    Monthly("Monthly", "€5.99 / month", "Cancel anytime", null),
}

data class PremiumState(
    val plan: Plan = Plan.Yearly,
    val isPurchasing: Boolean = false,
    val error: String? = null,
)

object PremiumSamples {
    val Yearly = PremiumState()
    val Monthly = PremiumState(plan = Plan.Monthly)
    val Purchasing = PremiumState(isPurchasing = true)
    val PaymentFailed = PremiumState(error = "Your payment didn't go through. No charge was made.")
}

private val Features = listOf(
    Icons.Rounded.AllInclusive to "Unlimited habits",
    Icons.Rounded.Insights to "Mood and habit insights",
    Icons.Rounded.CloudDone to "Backup and sync across devices",
    Icons.Rounded.Palette to "Custom icons and themes",
)

@Composable
fun PremiumScreen(
    state: PremiumState,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onSubscribe: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Hero(onClose)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier.widthIn(max = 520.dp).padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Features.forEach { (icon, label) -> Feature(icon, label) }
                    Spacer(Modifier.height(8.dp))
                    Plan.entries.forEach { PlanCard(it, selected = it == state.plan) }
                    if (state.error != null) {
                        InfoBanner(
                            icon = Icons.Rounded.ErrorOutline,
                            text = state.error,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onSubscribe,
                        enabled = !state.isPurchasing,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        if (state.isPurchasing) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Processing…")
                        } else {
                            Text(if (state.error != null) "Try again" else "Start 7-day free trial")
                        }
                    }
                    Text(
                        text = "Free for 7 days, then ${state.plan.price}. Cancel anytime in settings.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun Hero(onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.onPrimaryContainer)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 56.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colors.tertiary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = colors.onTertiary,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Bloom Premium", style = MaterialTheme.typography.headlineMedium, color = colors.onPrimaryContainer)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Everything you need to keep growing.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Feature(
    icon: ImageVector,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlanCard(
    plan: Plan,
    selected: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = {},
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primaryContainer.copy(alpha = 0.5f) else colors.surfaceContainerLow,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 8.dp))
            Column(Modifier.weight(1f)) {
                Text(plan.title, style = MaterialTheme.typography.titleMedium)
                Text(plan.note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (plan.badge != null) {
                    Text(
                        text = plan.badge,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.tertiary)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onTertiary,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(plan.price, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@PreviewBloom(samplesFrom = PremiumSamples::class, navigatesTo = ["ProfileScreen"])
@PremiumScreenAutoPreviews
@Composable
internal fun PremiumScreenPreview(
    @PreviewParameter(PremiumScreenPreviewSamplesProvider::class) state: PremiumState,
) = BloomTheme { PremiumScreen(state) }
