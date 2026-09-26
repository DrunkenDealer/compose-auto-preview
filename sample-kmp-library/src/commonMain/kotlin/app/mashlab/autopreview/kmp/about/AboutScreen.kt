package app.mashlab.autopreview.kmp.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.mashlab.autopreview.kmp.resources.Res
import app.mashlab.autopreview.kmp.resources.about_tagline
import app.mashlab.autopreview.kmp.resources.about_title
import app.mashlab.autopreview.kmp.resources.about_version
import app.mashlab.autopreview.kmp.resources.logo
import app.mashlab.autopreview.kmp.theme.KitTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

data class AboutState(
    val version: String,
)

object AboutSamples {
    val Release = AboutState(version = "0.2.0")
    val Snapshot = AboutState(version = "4.2.0-SNAPSHOT")
}

@Composable
fun AboutScreen(
    state: AboutState,
    modifier: Modifier = Modifier,
) = KitTheme {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Image(painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(96.dp))
            Text(stringResource(Res.string.about_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(Res.string.about_tagline),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Text(stringResource(Res.string.about_version, state.version), style = MaterialTheme.typography.labelLarge)
        }
    }
}
