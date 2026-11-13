package com.choreme.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * The pieces of the "Sunny" look (see theme/Theme.kt). Screens use these instead of styling
 * material buttons, cards and chips themselves, so the look stays the same everywhere.
 */

/** True inside a [HeroCard]: its buttons are charcoal with white text, whatever the look, so they never melt into the yellow. */
val LocalOnBrand = androidx.compose.runtime.compositionLocalOf { false }

/** The three button levels: at most one [Filled] per screen, [Tonal] (soft yellow) and [Text] for the rest. */
enum class NosButtonStyle { Filled, Tonal, Text }

/**
 * The app's button. [NosButtonStyle.Filled] = ink with white text (yellow with ink in the dark look), 52 dp tall;
 * [NosButtonStyle.Tonal] = soft yellow with ink text; [NosButtonStyle.Text] = no background. The label is centred.
 */
@Composable
fun NosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NosButtonStyle = NosButtonStyle.Filled,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val label: @Composable () -> Unit = {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
    when (style) {
        NosButtonStyle.Text -> TextButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 44.dp),
            enabled = enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = scheme.onSurface),
        ) { label() }
        else -> {
            val tonal = style == NosButtonStyle.Tonal
            val onBrand = LocalOnBrand.current
            Button(
                onClick = onClick,
                modifier = modifier.heightIn(min = 52.dp),
                enabled = enabled,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        tonal -> if (onBrand) com.choreme.core.ui.theme.Sunny.BrandSoft else softYellow()
                        onBrand -> com.choreme.core.ui.theme.Sunny.Ink
                        else -> scheme.primary
                    },
                    contentColor = when {
                        tonal -> if (onBrand) com.choreme.core.ui.theme.Sunny.Ink else scheme.onSurface
                        onBrand -> Color.White
                        else -> scheme.onPrimary
                    },
                ),
            ) { label() }
        }
    }
}

/** The soft yellow of tonal buttons: a light tint of the brand yellow, a dark one after dark. */
@Composable
private fun softYellow(): Color =
    if (MaterialTheme.colorScheme.background.luminanceIsLow()) Color(0xFF3A3320) else com.choreme.core.ui.theme.Sunny.BrandSoft

private fun Color.luminanceIsLow(): Boolean = (red * 0.2126f + green * 0.7152f + blue * 0.0722f) < 0.3f

/**
 * The ONE selectable chip: selected = filled yellow with ink text and no check (the fill already says
 * "selected"); unselected = the sand tile colour, no border. Pill-shaped, at least 40 dp tall.
 */
@Composable
fun NosChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = scheme.surfaceVariant,
            labelColor = scheme.onSurface,
            selectedContainerColor = scheme.primaryContainer,
            selectedLabelColor = scheme.onPrimaryContainer,
        ),
        border = null,
        modifier = modifier.heightIn(min = 40.dp).semantics { contentDescription = label },
    )
}

/** The ONE card style: 20 dp, white (or [tint]), no border, no shadow. */
@Composable
fun NosCard(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tint ?: MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), content = content)
    }
}

/**
 * The big yellow card at the top of a main tab: a small [kicker] (the date), a large [title], an optional
 * [progress] ring (0 to 1) and room for one button. Ink text on yellow, in light and dark alike.
 */
@Composable
fun HeroCard(
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    line: String? = null,
    progress: Float? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val ink = com.choreme.core.ui.theme.Sunny.Ink
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (kicker != null) Text(kicker, style = MaterialTheme.typography.titleSmall, color = ink.copy(alpha = 0.75f))
                Text(title, style = MaterialTheme.typography.headlineMedium, color = ink)
                if (line != null) Text(line, style = MaterialTheme.typography.bodyLarge, color = ink.copy(alpha = 0.85f))
            }
            if (progress != null) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(64.dp),
                        color = ink.copy(alpha = 0.15f),
                        strokeWidth = 7.dp,
                    )
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(64.dp),
                        color = ink,
                        strokeWidth = 7.dp,
                        trackColor = Color.Transparent,
                    )
                    Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = ink)
                }
            }
        }
        androidx.compose.runtime.CompositionLocalProvider(LocalOnBrand provides true) { content() }
    }
}

/** The app's character: the house of the launcher icon, on its yellow tile with soft corners. Decorative. */
@Composable
fun Mascot(modifier: Modifier = Modifier, size: Dp = 120.dp) {
    Image(
        painter = painterResource(R.drawable.mascot_house),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.22f)),
    )
}

