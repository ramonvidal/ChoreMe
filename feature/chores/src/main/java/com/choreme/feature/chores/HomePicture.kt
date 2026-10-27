package com.choreme.feature.chores

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.choreme.core.model.chore.HomeScene
import com.choreme.core.model.chore.MemberWeek
import com.choreme.core.ui.R as CoreUiR

/** "Our home" at the top of the Chores tab: the picture and one line. Tap for the week's points. */
@Composable
internal fun HomeCard(scene: HomeScene, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        HomePicture(scene, Modifier.fillMaxWidth().height(112.dp))
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
            Text(stringResource(R.string.home_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                pluralStringResource(R.plurals.home_points, scene.points, scene.points),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The bigger picture, what it means, and everyone's points this week. */
@Composable
internal fun HomeDialog(scene: HomeScene, week: List<MemberWeek>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card { HomePicture(scene, Modifier.fillMaxWidth().height(150.dp)) }
                Text(stringResource(homeMood(scene)), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.home_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (week.isNotEmpty()) {
                    Column {
                        week.forEachIndexed { index, (member, _) ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            val points = week[index].points
                            Row(Modifier.padding(vertical = 8.dp)) {
                                Text(member.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Text(pluralStringResource(CoreUiR.plurals.points, points, points), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.home_close)) } },
    )
}

private fun homeMood(scene: HomeScene): Int = when {
    scene.cloud -> R.string.home_mood_urgent
    scene.tired -> R.string.home_mood_tired
    scene.fullBloom -> R.string.home_mood_bloom
    scene.points == 0 -> R.string.home_mood_start
    !scene.lightsOn -> R.string.home_mood_overdue
    else -> R.string.home_mood_growing
}

/**
 * A small house with a tree and flowers, drawn in the app's few colours. The garden grows with
 * the week's points; the lights are on while nothing is overdue; a cloud passes while something
 * is urgent; with chores piling up the flowers droop a little.
 */
@Composable
internal fun HomePicture(scene: HomeScene, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val growth by animateFloatAsState(scene.growth, tween(1200), label = "growth")
    val light by animateFloatAsState(if (scene.lightsOn) 1f else 0f, tween(800), label = "light")
    val motion = rememberInfiniteTransition(label = "garden")
    val sway by motion.animateFloat(-1f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "sway")
    val drift by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")

    val green = colors.primary
    val leaf = if (scene.tired) lerp(green, Tired, 0.5f) else green
    val sky = lerp(colors.surfaceContainerLowest, Sky, if (scene.cloud) 0.25f else 0.6f)

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val ground = h * 0.82f
        drawRect(sky, size = Size(w, ground))
        drawRect(lerp(colors.surfaceContainerLowest, green, 0.18f + 0.22f * growth), Offset(0f, ground), Size(w, h - ground))

        // Sun, or a cloud drifting over the house while something's urgent.
        if (scene.cloud) {
            cloud(Offset(w * (0.36f + 0.12f * drift), h * 0.2f), h * 0.11f, CloudGrey)
        } else {
            drawCircle(Sun, radius = h * 0.09f, center = Offset(w * 0.86f, h * 0.2f))
        }

        // The house, in the middle.
        val houseW = h * 0.62f
        val houseH = h * 0.38f
        val left = w * 0.5f - houseW / 2
        val top = ground - houseH
        drawRect(colors.surfaceContainerLowest, Offset(left, top), Size(houseW, houseH))
        drawRect(colors.outline, Offset(left, top), Size(houseW, houseH), style = Stroke(1.5.dp.toPx()))
        val roof = Path().apply {
            moveTo(left - houseW * 0.1f, top)
            lineTo(left + houseW / 2, top - houseH * 0.72f)
            lineTo(left + houseW * 1.1f, top)
            close()
        }
        drawPath(roof, colors.inverseSurface)
        val door = Size(houseW * 0.2f, houseH * 0.52f)
        drawRoundRect(green, Offset(left + houseW / 2 - door.width / 2, ground - door.height), door, CornerRadius(2.dp.toPx()))
        val window = Size(houseW * 0.2f, houseH * 0.28f)
        val windowColor = lerp(colors.surfaceVariant, Lamp, light)
        listOf(left + houseW * 0.12f, left + houseW * 0.68f).forEach { x ->
            drawRect(windowColor, Offset(x, top + houseH * 0.22f), window)
            drawRect(colors.outline, Offset(x, top + houseH * 0.22f), window, style = Stroke(1.dp.toPx()))
        }

        // A tree that grows from a sapling.
        tree(Offset(w * 0.2f, ground), h * (0.22f + 0.38f * growth), leaf, colors.outline, sway)

        // Flowers either side of the house, one per point (up to a dozen).
        val slots = flowerSlots(w, houseW)
        slots.take(scene.flowers).forEachIndexed { i, x ->
            val stem = h * (0.1f + 0.06f * ((i * 7) % 3) / 2f) * (0.6f + 0.4f * growth)
            flower(Offset(x, ground), stem, leaf, Petals[i % Petals.size], if (scene.tired) 24f else sway * 4f, scene.tired)
        }
    }
}

private fun DrawScope.flowerSlots(w: Float, houseW: Float): List<Float> {
    val gap = 11.dp.toPx()
    val houseLeft = w * 0.5f - houseW * 0.65f
    val houseRight = w * 0.5f + houseW * 0.65f
    val leftSide = generateSequence(houseLeft - gap) { it - gap }.takeWhile { it > w * 0.28f }.toList()
    val rightSide = generateSequence(houseRight + gap) { it + gap }.takeWhile { it < w - gap }.toList()
    // Alternate sides so a few flowers spread out instead of bunching up.
    return (0 until maxOf(leftSide.size, rightSide.size)).flatMap { listOfNotNull(rightSide.getOrNull(it), leftSide.getOrNull(it)) }
}

private fun DrawScope.tree(base: Offset, height: Float, leaf: Color, trunk: Color, sway: Float) {
    drawLine(trunk, base, Offset(base.x, base.y - height * 0.55f), strokeWidth = 3.dp.toPx())
    rotate(sway * 2f, pivot = base) {
        drawCircle(leaf, radius = height * 0.32f, center = Offset(base.x, base.y - height * 0.68f))
        drawCircle(leaf, radius = height * 0.24f, center = Offset(base.x - height * 0.2f, base.y - height * 0.5f))
        drawCircle(leaf, radius = height * 0.24f, center = Offset(base.x + height * 0.2f, base.y - height * 0.52f))
    }
}

private fun DrawScope.flower(base: Offset, stem: Float, leaf: Color, petal: Color, tilt: Float, tired: Boolean) {
    rotate(tilt, pivot = base) {
        val head = Offset(base.x, base.y - stem)
        drawLine(leaf, base, head, strokeWidth = 1.5.dp.toPx())
        val r = 3.dp.toPx()
        val color = if (tired) lerp(petal, Tired, 0.45f) else petal
        listOf(Offset(r, 0f), Offset(-r, 0f), Offset(0f, r), Offset(0f, -r)).forEach { drawCircle(color, r * 0.85f, head + it) }
        drawCircle(Sun, r * 0.6f, head)
    }
}

private fun DrawScope.cloud(center: Offset, r: Float, color: Color) {
    drawCircle(color, r, center)
    drawCircle(color, r * 0.75f, center + Offset(-r * 1.0f, r * 0.25f))
    drawCircle(color, r * 0.8f, center + Offset(r * 1.0f, r * 0.2f))
    drawRoundRect(color, center + Offset(-r * 1.6f, r * 0.1f), Size(r * 3.2f, r * 0.9f), CornerRadius(r * 0.45f))
}

private val Sky = Color(0xFFDDEBF2)
private val Sun = Color(0xFFF2C14E)
private val Lamp = Color(0xFFF6D77A)
private val CloudGrey = Color(0xFFB9BEC4)
private val Tired = Color(0xFFB8AE92)
private val Petals = listOf(Color(0xFFE38B7A), Color(0xFFF0F0EC), Color(0xFFC9A0DC), Color(0xFFF2C14E))
