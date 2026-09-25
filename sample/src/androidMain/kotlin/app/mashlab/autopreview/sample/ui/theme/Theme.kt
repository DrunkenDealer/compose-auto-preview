package app.mashlab.autopreview.sample.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF2D6A4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9EBD6),
    onPrimaryContainer = Color(0xFF0B2A1C),
    secondary = Color(0xFF52634F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EADB),
    onSecondaryContainer = Color(0xFF1B2519),
    tertiary = Color(0xFFC8553D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBD1),
    onTertiaryContainer = Color(0xFF3B0A00),
    background = Color(0xFFF6F5EF),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFF6F5EF),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFE4E6DD),
    onSurfaceVariant = Color(0xFF5C6158),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEFEEE7),
    surfaceContainerHigh = Color(0xFFE9E8E1),
    surfaceContainerHighest = Color(0xFFE3E3DC),
    outline = Color(0xFF8A9086),
    outlineVariant = Color(0xFFDDDFD6),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD5AE),
    onPrimary = Color(0xFF003921),
    primaryContainer = Color(0xFF1C5139),
    onPrimaryContainer = Color(0xFFC9EBD6),
    secondary = Color(0xFFB9CCB4),
    onSecondary = Color(0xFF253423),
    secondaryContainer = Color(0xFF3B4B39),
    onSecondaryContainer = Color(0xFFD5E8CF),
    tertiary = Color(0xFFFFB4A1),
    onTertiary = Color(0xFF5E1604),
    tertiaryContainer = Color(0xFF7D2C18),
    onTertiaryContainer = Color(0xFFFFDBD1),
    background = Color(0xFF0F1411),
    onBackground = Color(0xFFE1E4DD),
    surface = Color(0xFF0F1411),
    onSurface = Color(0xFFE1E4DD),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC0C9BE),
    surfaceContainerLowest = Color(0xFF0A0F0C),
    surfaceContainerLow = Color(0xFF181E1A),
    surfaceContainer = Color(0xFF1C231F),
    surfaceContainerHigh = Color(0xFF262D29),
    surfaceContainerHighest = Color(0xFF313834),
    outline = Color(0xFF8A9389),
    outlineVariant = Color(0xFF353C37),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val BloomTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val BloomShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun BloomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BloomTypography,
        shapes = BloomShapes,
        content = content,
    )
}
