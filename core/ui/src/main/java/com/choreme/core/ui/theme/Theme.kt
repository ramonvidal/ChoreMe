package com.choreme.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choreme.core.ui.R

/*
 * "Warm home": cream paper, terracotta and deep teal, a rounded font (Nunito, SIL Open
 * Font License, bundled in res/font; license in assets/licenses). Cards are white on cream.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFFA8452D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E3D6),
    onPrimaryContainer = Color(0xFF5A2414),
    secondary = Color(0xFF1F5C5A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEBE9),
    onSecondaryContainer = Color(0xFF0F3A38),
    tertiary = Color(0xFF6B4E96),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE4F6),
    onTertiaryContainer = Color(0xFF3A2558),
    background = Color(0xFFFBF6EF),
    onBackground = Color(0xFF2B2522),
    surface = Color(0xFFFBF6EF),
    onSurface = Color(0xFF2B2522),
    surfaceVariant = Color(0xFFF3EAE0),
    onSurfaceVariant = Color(0xFF6E625B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    outline = Color(0xFFB5A393),
    outlineVariant = Color(0xFFE6D8CA),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFCE4DE),
    onErrorContainer = Color(0xFF5C1210),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF0A58E),
    onPrimary = Color(0xFF561F10),
    primaryContainer = Color(0xFF5A2D20),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFF8FD0CB),
    onSecondary = Color(0xFF003735),
    secondaryContainer = Color(0xFF1E4A48),
    onSecondaryContainer = Color(0xFFC9EEEA),
    tertiary = Color(0xFFCDB8EE),
    onTertiary = Color(0xFF35215A),
    tertiaryContainer = Color(0xFF4A3870),
    onTertiaryContainer = Color(0xFFEBDDFF),
    background = Color(0xFF1F1A17),
    onBackground = Color(0xFFF1E7DE),
    surface = Color(0xFF1F1A17),
    onSurface = Color(0xFFF1E7DE),
    surfaceVariant = Color(0xFF3A322C),
    onSurfaceVariant = Color(0xFFC9BBAF),
    surfaceContainerLowest = Color(0xFF181412),
    surfaceContainerLow = Color(0xFF241F1B),
    surfaceContainer = Color(0xFF2A2420),
    surfaceContainerHigh = Color(0xFF302925),
    surfaceContainerHighest = Color(0xFF362E29),
    outline = Color(0xFF8E7F74),
    outlineVariant = Color(0xFF4A403A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF6B1B16),
    onErrorContainer = Color(0xFFFFDAD5),
)

val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

private fun TextStyle.warm(weight: FontWeight? = null) = copy(fontFamily = Nunito, fontWeight = weight ?: fontWeight)

private val Base = Typography()

/** Bold, friendly headings; body text a touch heavier than the default so it reads warm. */
private val WarmTypography = Typography(
    displayLarge = Base.displayLarge.warm(FontWeight.ExtraBold),
    displayMedium = Base.displayMedium.warm(FontWeight.ExtraBold),
    displaySmall = Base.displaySmall.warm(FontWeight.ExtraBold),
    headlineLarge = Base.headlineLarge.warm(FontWeight.ExtraBold),
    headlineMedium = Base.headlineMedium.warm(FontWeight.ExtraBold),
    headlineSmall = Base.headlineSmall.warm(FontWeight.ExtraBold),
    titleLarge = Base.titleLarge.warm(FontWeight.ExtraBold),
    titleMedium = Base.titleMedium.warm(FontWeight.ExtraBold),
    titleSmall = Base.titleSmall.warm(FontWeight.Bold),
    bodyLarge = Base.bodyLarge.warm(FontWeight.SemiBold),
    bodyMedium = Base.bodyMedium.warm(FontWeight.SemiBold),
    bodySmall = Base.bodySmall.warm(FontWeight.SemiBold),
    labelLarge = Base.labelLarge.warm(FontWeight.ExtraBold),
    labelMedium = Base.labelMedium.warm(FontWeight.Bold),
    labelSmall = Base.labelSmall.warm(FontWeight.Bold),
)

private val WarmShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** [dynamicColor] (the phone's wallpaper colors) is off: the app has its own look. */
@Composable
fun ChoreMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = WarmTypography, shapes = WarmShapes, content = content)
}
