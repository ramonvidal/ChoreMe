package com.choreme.feature.chores

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
 * "Our home" as 8-bit pixel art: a fixed grid of big square pixels, a few flat colours, and
 * animations that step a few frames a second like an old console. It shows the same things
 * as before: each chore to do is a weed in the grass (overgrown once overdue), finishing one
 * pulls it and plants flowers, the tree grows with the last 7 days' points, the windows are
 * lit and the chimney smokes while nothing is overdue, and the weather turns from sun to
 * clouds, rain and a storm with lightning as chores pile up.
 */
@Composable
internal fun HomePicture(scene: HomeScene, modifier: Modifier = Modifier) {
    // Counted up and down, so a new flower grows in and a pulled weed shrinks away, in steps.
    val flowers by animateFloatAsState(scene.flowers.toFloat(), tween(900), label = "flowers")
    val weeds by animateFloatAsState(scene.weeds.toFloat(), tween(700), label = "weeds")
    val gloom by animateFloatAsState(scene.weather.ordinal.toFloat(), tween(1500), label = "gloom")
    val motion = rememberInfiniteTransition(label = "garden")
    // 8 frames a second, like an old game: everything moves in whole pixels.
    val frame by motion.animateValue(0, FRAMES, Int.VectorConverter, infiniteRepeatable(tween(FRAMES * 125, easing = LinearEasing)), label = "frame")
    val fall by motion.animateValue(0, 64, Int.VectorConverter, infiniteRepeatable(tween(64 * 40, easing = LinearEasing)), label = "rain")
    val storm = scene.weather == Weather.STORM

    Canvas(modifier) {
        val cell = size.height / ROWS
        val cols = (size.width / cell).toInt()
        val grid = PixelGrid(this, cell, (size.width - cols * cell) / 2f)
        val ground = ROWS - 5
        val mid = cols / 2

        // Sky, in bands that darken with the weather; grass with a checked texture.
        val sky = when {
            gloom <= 1f -> lerp(Sky, Overcast, gloom)
            gloom <= 2f -> lerp(Overcast, Rainy, gloom - 1f)
            else -> lerp(Rainy, Stormy, gloom - 2f)
        }
        drawRect(sky, size = Size(size.width, ground * cell))
        val mud = (gloom / 3f) * 0.55f
        val grass = lerp(Grass, Mud, mud)
        val grassDark = lerp(GrassDark, MudDark, mud)
        drawRect(grass, Offset(0f, ground * cell), Size(size.width, size.height - ground * cell))
        for (x in -1..cols) {
            if (x % 2 == 0) grid.px(grassDark, x, ground)
            for (y in ground + 2 until ROWS) if ((x + y) % 4 == 0) grid.px(grassDark, x, y)
        }

        // The sun, rays blinking, fading behind the clouds.
        val sunAlpha = (1f - gloom / 1.5f).coerceIn(0f, 1f)
        if (sunAlpha > 0f) grid.sun(cols - 7, 2, frame % 4 < 2, sunAlpha)

        // Clouds drift a pixel at a time; more and darker as the weather turns.
        val cloudColor = lerp(Cloud, StormCloud, ((gloom - 1f) / 2f).coerceIn(0f, 1f))
        CloudSpots.forEachIndexed { i, (x, y) ->
            val alpha = (gloom * CloudSpots.size / 3f - i).coerceIn(0f, 1f)
            if (alpha > 0f) {
                val dx = ((frame / 4 + i * 3) % 6).let { if (it > 3) 6 - it else it }
                grid.sprite(CLOUD, (cols * x).toInt() + dx, y, mapOf('X' to cloudColor), alpha)
            }
        }

        // Pixel rain, heavier in a storm.
        val drops = ((gloom - 1f).coerceIn(0f, 2f) * 10f).toInt()
        repeat(drops) { i ->
            val x = (i * 37 + 5) % cols
            val y = 6 + (fall + i * 11) % (ground - 6)
            grid.px(RainDrop, x, y, h = 2)
        }

        // The house: red roof, chimney, white walls, two windows, a green door.
        val left = mid - HOUSE_WIDTH / 2
        val top = ground - HOUSE_HEIGHT
        grid.house(left, top, lit = scene.lightsOn)
        // Smoke from the chimney while all is well.
        if (scene.lightsOn && !storm) {
            repeat(3) { k ->
                val step = (frame / 2 + k * 3) % 9
                if (step < 6) grid.px(Smoke, left + 12 + (step / 3) % 2, top - 1 - step, alpha = 1f - step / 7f)
            }
        }

        // A tree that grows in three sizes with the points, bending in the storm.
        val treeSize = when {
            scene.growth >= 0.67f -> 2
            scene.growth >= 0.34f -> 1
            else -> 0
        }
        val wind = if (storm) (if (frame % 4 < 2) 1 else 2) else 0
        grid.tree(left - 9, ground - 1, treeSize, if (scene.tired) lerp(Leaf, Tired, 0.5f) else Leaf, wind)

        // Flowers by the house, one per point; a new one grows a pixel at a time.
        val slots = flowerSlots(left, cols)
        slots.take(ceil(flowers).toInt()).forEachIndexed { i, x ->
            val grown = (flowers - i).coerceIn(0f, 1f)
            val bob = if (storm) 1 else if ((frame / 4 + i) % 2 == 0) 0 else 1
            grid.flower(x, ground - 1, stem = 2 + i % 2, grown, Petals[i % Petals.size], scene.tired, if (scene.tired) 1 else bob)
        }

        // Weeds in the grass in front, one per chore waiting today; overgrown once overdue.
        val weedGap = cols / HomeScene.MAX_WEEDS
        repeat(ceil(weeds).toInt()) { i ->
            val grown = (weeds - i).coerceIn(0f, 1f)
            val slot = HomeScene.MAX_WEEDS / 2 + if (i % 2 == 0) i / 2 else -(i / 2 + 1)
            grid.weed(weedGap * slot + weedGap / 2, ROWS - 1, i < scene.overgrown, grown, (frame / 2 + i) % 2)
        }

        // Lightning: a pixel bolt and a flash of the whole picture, now and then in a storm.
        if (storm && frame in LIGHTNING_FRAMES) {
            grid.sprite(BOLT, (cols * 0.66f).toInt(), 5, mapOf('Y' to Lamp))
            drawRect(Color.White, alpha = if (frame == LIGHTNING_FRAMES.first) 0.45f else 0.2f)
        }
    }
}

/** Flower columns either side of the house, alternating sides, three pixels apart. */
private fun flowerSlots(houseLeft: Int, cols: Int): List<Int> {
    val leftSide = generateSequence(houseLeft - 2) { it - 3 }.takeWhile { it > houseLeft - 7 }.toList()
    val rightSide = generateSequence(houseLeft + HOUSE_WIDTH + 1) { it + 3 }.takeWhile { it < cols - 2 }.toList()
    return (0 until maxOf(leftSide.size, rightSide.size)).flatMap { listOfNotNull(rightSide.getOrNull(it), leftSide.getOrNull(it)) }
}

/** Big square pixels on a [cell]-sized grid, centred by [x0]. */
private class PixelGrid(val scope: DrawScope, val cell: Float, val x0: Float) {
    fun px(color: Color, x: Int, y: Int, w: Int = 1, h: Int = 1, alpha: Float = 1f) {
        // A hair bigger than the cell, so neighbouring pixels never show a seam.
        scope.drawRect(color, Offset(x0 + x * cell, y * cell), Size(w * cell + 0.5f, h * cell + 0.5f), alpha = alpha)
    }

    /** Rows of characters, one per pixel; '.' is see-through. */
    fun sprite(rows: List<String>, x: Int, y: Int, colors: Map<Char, Color>, alpha: Float = 1f) {
        rows.forEachIndexed { dy, row ->
            row.forEachIndexed { dx, c -> colors[c]?.let { px(it, x + dx, y + dy, alpha = alpha) } }
        }
    }

    fun sun(x: Int, y: Int, rays: Boolean, alpha: Float) {
        sprite(SUN, x, y, mapOf('S' to SunCore, 'L' to SunLight), alpha)
        if (rays) sprite(SUN_RAYS, x - 1, y - 1, mapOf('r' to SunLight), alpha)
    }

    fun house(x: Int, y: Int, lit: Boolean) {
        sprite(
            HOUSE,
            x,
            y,
            mapOf(
                'R' to Roof, 'r' to RoofDark, 'C' to Chimney, 'O' to Line, 'W' to Walls,
                'G' to if (lit) Lamp else DarkWindow, 'g' to if (lit) LampEdge else Line,
                'D' to Door, 'K' to Lamp,
            ),
        )
    }

    /** Three sizes: sapling, young tree, full tree. A round crown, shaded on the lower right. */
    fun tree(x: Int, base: Int, size: Int, leaf: Color, wind: Int) {
        val trunk = 2 + size
        val radius = 1 + size * 2
        px(Trunk, x, base - trunk + 1, w = if (size == 2) 2 else 1, h = trunk)
        val cx = x + wind
        val cy = base - trunk - radius + 1
        for (dy in -radius..radius) for (dx in -radius..radius) {
            if (dx * dx + dy * dy <= radius * radius + radius) {
                px(if (dx + dy > radius / 2) LeafDark else leaf, cx + dx, cy + dy)
            }
        }
    }

    /** A plus-shaped head on a short stem; drooping (head moved down and over) when tired. */
    fun flower(x: Int, base: Int, stem: Int, grown: Float, petal: Color, tired: Boolean, bob: Int) {
        val shown = (stem * grown).toInt().coerceAtLeast(if (grown > 0f) 1 else 0)
        if (shown == 0) return
        px(Leaf, x, base - shown + 1, h = shown)
        if (grown < 0.7f) return
        val hx = x + bob
        val hy = base - stem - 1 + if (tired) 1 else 0
        val color = if (tired) lerp(petal, Tired, 0.45f) else petal
        listOf(0 to -1, -1 to 0, 1 to 0, 0 to 1).forEach { (dx, dy) -> px(color, hx + dx, hy + dy) }
        px(SunLight, hx, hy)
    }

    /** A scruffy tuft of uneven blades; overgrown ones are taller, darker, with seed heads. */
    fun weed(x: Int, base: Int, overgrown: Boolean, grown: Float, sway: Int) {
        val heights = if (overgrown) listOf(3, 6, 4, 5, 2) else listOf(2, 4, 3)
        val color = if (overgrown) WeedDark else Weed
        val start = x - heights.size / 2
        heights.forEachIndexed { i, full ->
            val h = (full * grown).toInt()
            if (h > 0) {
                px(color, start + i, base - h + 1, h = h - 1)
                // The tip leans with the wind, a pixel either way.
                px(color, start + i + if (i % 2 == sway) 1 else 0, base - h + 1)
                if (overgrown && full >= 5 && grown >= 1f) px(Seed, start + i, base - h)
            }
        }
    }
}

private const val ROWS = 28
private const val FRAMES = 16
private val LIGHTNING_FRAMES = 12..13
private const val HOUSE_WIDTH = 17
private const val HOUSE_HEIGHT = 15

private val HOUSE = listOf(
    "........R........",
    ".......RRR.CC....",
    "......RRRRRCC....",
    ".....RRrRRRCC....",
    "....RRRRRRRRR....",
    "...RRrRRRRRrRR...",
    "..RRRRRRRRRRRRR..",
    ".rRRRRRrRRRRRRRr.",
    "..OOOOOOOOOOOOO..",
    "..OWWWWWWWWWWWO..",
    "..OWggWWWWWggWO..",
    "..OWGGWDDDWGGWO..",
    "..OWWWWDDDWWWWO..",
    "..OWWWWDDKWWWWO..",
    "..OWWWWDDDWWWWO..",
)

private val SUN = listOf(
    ".SSS.",
    "SSLSS",
    "SLSSS",
    "SSSSS",
    ".SSS.",
)

private val SUN_RAYS = listOf(
    "...r...",
    ".......",
    ".......",
    "r.....r",
    ".......",
    ".......",
    "...r...",
)

private val CLOUD = listOf(
    "...XXX...",
    ".XXXXXXX.",
    "XXXXXXXXX",
    ".XXXXXXX.",
)

private val BOLT = listOf(
    "..YY",
    ".YY.",
    "YYYY",
    "..Y.",
    ".Y..",
    "Y...",
)

/** Where clouds gather, in arrival order: x as a part of the width, y in pixels. */
private val CloudSpots = listOf(0.34f to 1, 0.74f to 3, 0.08f to 2, 0.55f to 0, 0.88f to 6, 0.2f to 6)

// A small, flat palette, the same in light and dark mode.
private val Sky = Color(0xFF9ED8F2)
private val Overcast = Color(0xFFB9C3CB)
private val Rainy = Color(0xFF8E9AA6)
private val Stormy = Color(0xFF4B5563)
private val Grass = Color(0xFF6BBF59)
private val GrassDark = Color(0xFF4E9A43)
private val Mud = Color(0xFF8C7A55)
private val MudDark = Color(0xFF6E5F42)
private val Walls = Color(0xFFF4F1E8)
private val Line = Color(0xFF3B3B3B)
private val Roof = Color(0xFFC0503F)
private val RoofDark = Color(0xFF8E3A2E)
private val Chimney = Color(0xFF7A4A3A)
private val Door = Color(0xFF2E6B52)
private val DarkWindow = Color(0xFF5A6470)
private val Lamp = Color(0xFFFFD966)
private val LampEdge = Color(0xFFE8B84A)
private val Smoke = Color(0xFFEDEDED)
private val SunCore = Color(0xFFFFC93C)
private val SunLight = Color(0xFFFFE08A)
private val Cloud = Color(0xFFF2F4F6)
private val StormCloud = Color(0xFF3F4650)
private val RainDrop = Color(0xFF5B8DEF)
private val Leaf = Color(0xFF3E9A4A)
private val LeafDark = Color(0xFF2C7337)
private val Trunk = Color(0xFF7A4E2D)
private val Tired = Color(0xFFB8AE92)
private val Weed = Color(0xFF8A8A3A)
private val WeedDark = Color(0xFF5E5A26)
private val Seed = Color(0xFF9C6B3A)
private val Petals = listOf(Color(0xFFFF6F61), Color(0xFFFFFFFF), Color(0xFFB98AE0), Color(0xFFFFC93C))
