package app.mashlab.autopreview.kmp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Violet = Color(0xFF7C4DFF)
private val Lavender = Color(0xFFB388FF)

@Composable
fun KitTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Lavender, secondary = Violet)
    } else {
        lightColorScheme(primary = Violet, secondary = Lavender)
    }
    MaterialTheme(colorScheme = colors, content = content)
}
