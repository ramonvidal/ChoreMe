package com.choreme.feature.chores

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
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
import com.choreme.core.model.chore.Weather
import kotlin.math.ceil
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
    scene.weather == Weather.STORM -> R.string.home_mood_storm
    scene.weather == Weather.RAIN -> R.string.home_mood_rain
    scene.tired -> R.string.home_mood_tired
    scene.weather == Weather.CLOUDY -> R.string.home_mood_cloudy
    scene.fullBloom -> R.string.home_mood_bloom
    scene.points == 0 -> R.string.home_mood_start
    !scene.lightsOn -> R.string.home_mood_overdue
    else -> R.string.home_mood_growing
}

/**
 * A small house with a tree and flowers, drawn in the app's few colours. Each chore to do is a
 * scruffy weed in the grass (bigger once it's overdue); finishing one pulls it and grows flowers
 * in, so the picture changes with every chore. The more that waits, the worse the weather:
 * clouds, then rain, then a storm with lightning. The lights are on while nothing is overdue.
 */
@Composable
internal fun HomePicture(scene: HomeScene, modifier: Modifier = Modifier) {
    val growth by animateFloatAsState(scene.growth, tween(1200), label = "growth")
    // Counted up and down smoothly, so a new flower grows in and a pulled weed shrinks away.
    val flowers by animateFloatAsState(scene.flowers.toFloat(), tween(900), label = "flowers")
    val weeds by animateFloatAsState(scene.weeds.toFloat(), tween(700), label = "weeds")
    val light by animateFloatAsState(if (scene.lightsOn) 1f else 0f, tween(800), label = "light")
    // 0 sunny, 1 cloudy, 2 rain, 3 storm; eased so the sky darkens and clears gradually.
    val gloom by animateFloatAsState(scene.weather.ordinal.toFloat(), tween(1500), label = "gloom")
    val motion = rememberInfiniteTransition(label = "garden")
    val sway by motion.animateFloat(-1f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "sway")
    val drift by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
    val fall by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(650, easing = LinearEasing)), label = "rain")
    val flash by motion.animateFloat(
        0f,
        0f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 4200
                0f at 3000
                1f at 3060
                0f at 3200
                0.8f at 3300
                0f at 3450
            },
        ),
        label = "lightning",
    )

    // Its own colours, not the theme's: a picture looks the same in light and dark mode.
    val green = Green
    val leaf = if (scene.tired) lerp(green, Tired, 0.5f) else green
    val sky = when {
        gloom <= 1f -> lerp(Sky, Overcast, gloom)
        gloom <= 2f -> lerp(Overcast, Rainy, gloom - 1f)
        else -> lerp(Rainy, Stormy, gloom - 2f)
    }
    val storm = scene.weather == Weather.STORM

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val ground = h * 0.82f
        drawRect(sky, size = Size(w, ground))
        val grass = lerp(Paper, green, 0.18f + 0.22f * growth)
        drawRect(lerp(grass, Mud, (gloom / 3f) * 0.5f), Offset(0f, ground), Size(w, h - ground))

        // The sun fades behind the clouds.
        val sunAlpha = (1f - gloom / 1.5f).coerceIn(0f, 1f)
        if (sunAlpha > 0f) drawCircle(Sun, radius = h * 0.09f, center = Offset(w * 0.86f, h * 0.2f), alpha = sunAlpha)

        // Clouds: two when cloudy, more and darker with rain and storm. Each fades in as it arrives.
        val cloudColor = lerp(CloudGrey, StormCloud, ((gloom - 1f) / 2f).coerceIn(0f, 1f))
        CloudSpots.forEachIndexed { i, (x, y, r) ->
            val alpha = (gloom * CloudSpots.size / 3f - i).coerceIn(0f, 1f)
            if (alpha > 0f) {
                val dx = (if (i % 2 == 0) drift else 1f - drift) * w * 0.06f
                cloud(Offset(w * x + dx, h * y), h * r, cloudColor, alpha)
            }
        }

        // Rain from under the clouds, heavier in a storm.
        val rain = ((gloom - 1f).coerceIn(0f, 2f) * 12f).toInt()
        val drop = h * 0.07f
        repeat(rain) { i ->
            val x = w * (0.08f + 0.84f * ((i * 0.618f) % 1f))
            val t = (fall + i * 0.37f) % 1f
            val y = h * 0.3f + t * (ground - h * 0.3f)
            drawLine(Rain, Offset(x, y), Offset(x - drop * 0.3f, y + drop), strokeWidth = 1.2.dp.toPx(), alpha = 0.8f)
        }

        // The house, in the middle.
        val houseW = h * 0.62f
        val houseH = h * 0.38f
        val left = w * 0.5f - houseW / 2
        val top = ground - houseH
        drawRect(Walls, Offset(left, top), Size(houseW, houseH))
        drawRect(Line, Offset(left, top), Size(houseW, houseH), style = Stroke(1.5.dp.toPx()))
        val roof = Path().apply {
            moveTo(left - houseW * 0.1f, top)
            lineTo(left + houseW / 2, top - houseH * 0.72f)
            lineTo(left + houseW * 1.1f, top)
            close()
        }
        drawPath(roof, Roof)
        val door = Size(houseW * 0.2f, houseH * 0.52f)
        drawRoundRect(green, Offset(left + houseW / 2 - door.width / 2, ground - door.height), door, CornerRadius(2.dp.toPx()))
        val window = Size(houseW * 0.2f, houseH * 0.28f)
        val windowColor = lerp(DarkWindow, Lamp, light)
        listOf(left + houseW * 0.12f, left + houseW * 0.68f).forEach { x ->
            drawRect(windowColor, Offset(x, top + houseH * 0.22f), window)
            drawRect(Line, Offset(x, top + houseH * 0.22f), window, style = Stroke(1.dp.toPx()))
        }

        // A tree that grows from a sapling, bending in the storm.
        tree(Offset(w * 0.2f, ground), h * (0.22f + 0.38f * growth), leaf, Line, if (storm) sway * 4f + 3f else sway)

        // Flowers either side of the house, one per point (up to a dozen).
        val slots = flowerSlots(w, houseW)
        slots.take(ceil(flowers).toInt()).forEachIndexed { i, x ->
            val grown = (flowers - i).coerceIn(0f, 1f)
            val stem = h * (0.1f + 0.06f * ((i * 7) % 3) / 2f) * (0.6f + 0.4f * growth)
            flower(Offset(x, ground), stem, leaf, Petals[i % Petals.size], if (scene.tired) 24f else sway * 4f, scene.tired, grown)
        }

        // Weeds in front, one per chore waiting today; the overdue ones are overgrown.
        val weedGap = w / HomeScene.MAX_WEEDS
        val strip = h - ground
        repeat(ceil(weeds).toInt()) { i ->
            val grown = (weeds - i).coerceIn(0f, 1f)
            // Spread out from the middle: 5, 4, 6, 3, 7, ...
            val slot = HomeScene.MAX_WEEDS / 2 + if (i % 2 == 0) i / 2 else -(i / 2 + 1)
            val overgrown = i < scene.overgrown
            weed(Offset(weedGap * (slot + 0.5f), h), strip * (if (overgrown) 2.4f else 1.6f) * grown, i, overgrown, sway)
        }

        // Lightning: a bolt and a flash of the whole picture, every few seconds in a storm.
        if (storm && flash > 0f) {
            val bolt = Path().apply {
                val x = w * 0.66f
                moveTo(x, h * 0.28f)
                lineTo(x - h * 0.05f, h * 0.46f)
                lineTo(x + h * 0.01f, h * 0.46f)
                lineTo(x - h * 0.04f, h * 0.66f)
            }
            drawPath(bolt, Lamp, alpha = flash, style = Stroke(2.5.dp.toPx()))
            drawRect(Color.White, alpha = flash * 0.35f)
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

private fun DrawScope.flower(base: Offset, stem: Float, leaf: Color, petal: Color, tilt: Float, tired: Boolean, grown: Float) {
    if (grown <= 0f) return
    rotate(tilt, pivot = base) {
        val head = Offset(base.x, base.y - stem * grown)
        drawLine(leaf, base, head, strokeWidth = 1.5.dp.toPx())
        val r = 3.dp.toPx() * grown
        val color = if (tired) lerp(petal, Tired, 0.45f) else petal
        listOf(Offset(r, 0f), Offset(-r, 0f), Offset(0f, r), Offset(0f, -r)).forEach { drawCircle(color, r * 0.85f, head + it) }
        drawCircle(Sun, r * 0.6f, head)
    }
}

/**
 * A scruffy weed: five uneven blades leaning every which way, a dull olive. Overgrown ones are
 * darker, with a couple of brown seed heads.
 */
private fun DrawScope.weed(base: Offset, height: Float, seed: Int, overgrown: Boolean, sway: Float) {
    if (height <= 0f) return
    val color = if (overgrown) WeedDark else Weed
    val half = 1.8.dp.toPx()
    val spread = 3.5.dp.toPx()
    for (b in -2..2) {
        // Same shape every time for the same weed: lengths and leans from its index.
        val jitter = ((seed * 31 + b * 17) % 7 + 7) % 7 / 6f
        val foot = base.x + b * spread * 0.8f
        val length = height * (0.55f + 0.45f * jitter) * if (b == 0) 1f else 0.85f
        val lean = b * spread * (0.8f + jitter) + sway * half * 1.5f
        val tip = Offset(foot + lean, base.y - length)
        val blade = Path().apply {
            moveTo(foot - half, base.y)
            quadraticTo(foot + lean * 0.2f, base.y - length * 0.5f, tip.x, tip.y)
            quadraticTo(foot + lean * 0.4f, base.y - length * 0.45f, foot + half, base.y)
            close()
        }
        drawPath(blade, color)
        if (overgrown && (b == 0 || b == 2)) drawCircle(Seed, half * 1.3f, tip)
    }
}

private fun DrawScope.cloud(center: Offset, r: Float, color: Color, alpha: Float) {
    drawCircle(color, r, center, alpha = alpha)
    drawCircle(color, r * 0.75f, center + Offset(-r * 1.0f, r * 0.25f), alpha = alpha)
    drawCircle(color, r * 0.8f, center + Offset(r * 1.0f, r * 0.2f), alpha = alpha)
    drawRoundRect(color, center + Offset(-r * 1.6f, r * 0.1f), Size(r * 3.2f, r * 0.9f), CornerRadius(r * 0.45f), alpha = alpha)
}

/** Where clouds gather, in arrival order: (x, y) as parts of the picture, and radius. */
private val CloudSpots = listOf(
    Triple(0.4f, 0.18f, 0.11f),
    Triple(0.78f, 0.16f, 0.1f),
    Triple(0.14f, 0.14f, 0.09f),
    Triple(0.6f, 0.1f, 0.12f),
    Triple(0.93f, 0.26f, 0.08f),
    Triple(0.27f, 0.27f, 0.08f),
)

private val Sky = Color(0xFFE6EFF3)
private val Paper = Color(0xFFFFFFFF)
private val Walls = Color(0xFFFFFFFF)
private val Line = Color(0xFF8E8E8A)
private val Roof = Color(0xFF1C1C1C)
private val DarkWindow = Color(0xFFE4E4E0)
private val Green = Color(0xFF2E6B52)
private val Overcast = Color(0xFFD3D8DC)
private val Rainy = Color(0xFFAEB6BE)
private val Stormy = Color(0xFF6B7480)
private val Sun = Color(0xFFF2C14E)
private val Lamp = Color(0xFFF6D77A)
private val CloudGrey = Color(0xFFB9BEC4)
private val StormCloud = Color(0xFF4A515B)
private val Rain = Color(0xFF6F8FAA)
private val Mud = Color(0xFF8C8466)
private val Tired = Color(0xFFB8AE92)
private val Weed = Color(0xFF76703A)
private val WeedDark = Color(0xFF59532A)
private val Seed = Color(0xFF8A6B3E)
private val Petals = listOf(Color(0xFFE38B7A), Color(0xFFF0F0EC), Color(0xFFC9A0DC), Color(0xFFF2C14E))
