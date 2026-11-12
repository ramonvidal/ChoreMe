package com.choreme.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choreme.core.ui.R

/*
 * "Sunny", the look of the owner's other app (Bora pro Treino!): a warm off-white page, plain white cards, a
 * yellow brand colour for what is selected, charcoal ink for text and the main button, green only for "yours",
 * "done" and points, pink sparingly, red only for urgent and errors. Yellow never carries white text: ink on
 * yellow. Font: Figtree (SIL Open Font License, bundled in res/font; license in assets/licenses).
 */

/** The Sunny palette. */
object Sunny {
    val Brand = Color(0xFFFEC204)
    val BrandSoft = Color(0xFFFFF1B8)
    val Ink = Color(0xFF1F1B16)
    val Page = Color(0xFFFAF7F0)
    val Card = Color(0xFFFFFFFF)
    val Tile = Color(0xFFF3EEE3)
    val Line = Color(0xFFEEE8DC)
    val Outline = Color(0xFFD9D1C1)
    val Muted = Color(0xFF6B645A)
    val Ok = Color(0xFF2E6B52)
    val OkSoft = Color(0xFFDDEBE4)
    val Pink = Color(0xFFE6397F)
    val PinkSoft = Color(0xFFFDE3EE)
    val Error = Color(0xFFB42318)
}

private val LightColors = lightColorScheme(
    // The main button and the text: charcoal. What is selected: yellow (primaryContainer).
    primary = Sunny.Ink,
    onPrimary = Color.White,
    primaryContainer = Sunny.Brand,
    onPrimaryContainer = Sunny.Ink,
    // Green: yours, done, points.
    secondary = Sunny.Ok,
    onSecondary = Color.White,
    secondaryContainer = Sunny.OkSoft,
    onSecondaryContainer = Color(0xFF173D2E),
    tertiary = Sunny.Pink,
    onTertiary = Color.White,
    tertiaryContainer = Sunny.PinkSoft,
    onTertiaryContainer = Color(0xFF7A1240),
    background = Sunny.Page,
    onBackground = Sunny.Ink,
    surface = Sunny.Page,
    onSurface = Sunny.Ink,
    surfaceVariant = Sunny.Tile,
    onSurfaceVariant = Sunny.Muted,
    surfaceContainerLowest = Sunny.Card,
    surfaceContainerLow = Sunny.Card,
    surfaceContainer = Sunny.Card,
    surfaceContainerHigh = Sunny.Card,
    surfaceContainerHighest = Sunny.Tile,
    outline = Sunny.Outline,
    outlineVariant = Sunny.Line,
    error = Sunny.Error,
    onError = Color.White,
    errorContainer = Color(0xFFFDECEC),
    onErrorContainer = Color(0xFF7A1A12),
    inverseSurface = Sunny.Ink,
    inverseOnSurface = Color.White,
    inversePrimary = Sunny.Brand,
)

// The same look after dark: the yellow stays, the page turns warm charcoal, the main button is yellow with ink.
private val DarkColors = darkColorScheme(
    primary = Sunny.Brand,
    onPrimary = Sunny.Ink,
    primaryContainer = Sunny.Brand,
    onPrimaryContainer = Sunny.Ink,
    secondary = Color(0xFF8FCBAE),
    onSecondary = Color(0xFF0B3524),
    secondaryContainer = Color(0xFF1F4A38),
    onSecondaryContainer = Color(0xFFCDEBDC),
    tertiary = Color(0xFFF5A3C5),
    onTertiary = Color(0xFF5A0F2E),
    tertiaryContainer = Color(0xFF5A1F3B),
    onTertiaryContainer = Color(0xFFFDE3EE),
    background = Color(0xFF14120F),
    onBackground = Color(0xFFEFEAE0),
    surface = Color(0xFF14120F),
    onSurface = Color(0xFFEFEAE0),
    surfaceVariant = Color(0xFF2C2924),
    onSurfaceVariant = Color(0xFFB8B1A4),
    surfaceContainerLowest = Color(0xFF1F1D19),
    surfaceContainerLow = Color(0xFF1F1D19),
    surfaceContainer = Color(0xFF1F1D19),
    surfaceContainerHigh = Color(0xFF26231F),
    surfaceContainerHighest = Color(0xFF2C2924),
    outline = Color(0xFF6F685C),
    outlineVariant = Color(0xFF38342D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C1A15),
    onErrorContainer = Color(0xFFFFDAD5),
    inverseSurface = Color(0xFFEFEAE0),
    inverseOnSurface = Sunny.Ink,
    inversePrimary = Sunny.Ink,
)

val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight) = TextStyle(
    fontFamily = Figtree,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * 1.3f).sp,
)

private val PlainTypography = Typography(
    displayLarge = style(32, FontWeight.Bold),
    displayMedium = style(32, FontWeight.Bold),
    displaySmall = style(28, FontWeight.Bold),
    headlineLarge = style(30, FontWeight.Bold),
    headlineMedium = style(28, FontWeight.Bold),
    headlineSmall = style(22, FontWeight.Bold),
    titleLarge = style(20, FontWeight.SemiBold),
    titleMedium = style(16, FontWeight.SemiBold),
    titleSmall = style(14, FontWeight.SemiBold),
    bodyLarge = style(16, FontWeight.Normal),
    bodyMedium = style(15, FontWeight.Normal),
    bodySmall = style(13, FontWeight.Normal),
    labelLarge = style(14, FontWeight.SemiBold),
    labelMedium = style(13, FontWeight.SemiBold),
    labelSmall = style(12, FontWeight.Medium),
)

// One card style everywhere: 20 dp (Card's default shape is "medium").
private val PlainShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * [darkTheme] is chosen by the caller (the phone's setting, or the person's choice under This phone).
 * [dynamicColor] is kept only so callers that pass it keep compiling: the app never takes the wallpaper's colors.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ChoreMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = PlainTypography, shapes = PlainShapes, content = content)
}
