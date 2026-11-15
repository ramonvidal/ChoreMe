package com.choreme.feature.home

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.choreme.core.model.home.Gag
import com.choreme.core.model.home.Occasion
import com.choreme.core.model.home.SceneInput
import com.choreme.core.model.home.pickGag
import com.choreme.core.model.home.rainbowAfter
import com.choreme.core.model.weather.Sky
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** The picture is a grid of 160 x 84 big square pixels, whatever the screen. */
private const val W = 160
private const val H = 84
private const val GY = 60
internal const val HOUSE_RATIO = W.toFloat() / H

/** Frames a second: low on purpose, like an old game, and kind to the battery. */
private const val FPS = 12f
/** The moment shown when nothing moves (animations off, screenshots). */
private const val STILL_T = 4.2f

internal fun nowHour(): Float = LocalTime.now().let { it.hour + it.minute / 60f }

/** The hour of the day, refreshed every minute. */
@Composable
internal fun rememberHour(): Float {
    var hour by remember { mutableFloatStateOf(nowHour()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            hour = nowHour()
        }
    }
    return hour
}

private fun reducedMotion(context: android.content.Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

private class ActiveGag(val gag: Gag, val startedAt: Float)

/**
 * The little house, alive: a pixel-art diorama that follows the real weather and the real hour (sun and moon crossing
 * the sky, clouds, rain, snow, fog, storms, wind in the tree and the smoke), lights its windows for the people at home,
 * raises the mailbox flag for an urgent chore, dresses up for the special dates and now and then lets something happen
 * (a bird lands, the cat passes, a kite, a rainbow...), like the old castaway screensaver. It only moves while on
 * screen and in the foreground, a dozen frames a second, and stands still when [animated] is off or the phone asks for
 * no animations. [previewGag] (with the progress [previewAt], 0 to 1) shows a gag in a still picture, for screenshots.
 */
@Composable
internal fun LiveHouse(
    scene: SceneInput,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    previewGag: Gag? = null,
    previewAt: Float = 0.5f,
) {
    val context = LocalContext.current
    val moves = animated && previewGag == null && !reducedMotion(context)
    var t by remember { mutableFloatStateOf(STILL_T) }
    var gag by remember { mutableStateOf<ActiveGag?>(null) }
    val latest by rememberUpdatedState(scene)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(moves) {
        if (!moves) return@LaunchedEffect
        val random = Random(System.nanoTime())
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = -1L
            var nextGagAt = t + 4f + random.nextFloat() * 6f
            var skyBefore = latest.sky
            while (true) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                if (last < 0 || (now - last) / 1e9f >= 1f / FPS) {
                    t += if (last < 0) 1f / FPS else (now - last) / 1e9f
                    last = now
                    val current = latest
                    val active = gag
                    if (active != null && t - active.startedAt >= active.gag.seconds) {
                        gag = null
                        nextGagAt = t + 8f + random.nextFloat() * 8f
                    }
                    if (gag == null && rainbowAfter(skyBefore, current)) {
                        gag = ActiveGag(Gag.RAINBOW, t)
                    } else if (gag == null && t >= nextGagAt) {
                        pickGag(current, random)?.let { gag = ActiveGag(it, t) } ?: run { nextGagAt = t + 10f }
                    }
                    skyBefore = current.sky
                }
            }
        }
    }

    Canvas(modifier.aspectRatio(HOUSE_RATIO)) {
        val px = Px(this, size.width / W)
        val time = if (moves) t else STILL_T
        drawSky(px, scene, time)
        drawGround(px, scene, time)
        drawBackdropDecor(px, scene, time)
        drawTree(px, scene, time)
        drawHouse(px, scene, time)
        drawWeather(px, scene, time)
        val shown = previewGag?.let { ActiveGag(it, time - previewAt * it.seconds) } ?: gag
        if (shown != null) drawGag(px, shown.gag, ((time - shown.startedAt) / shown.gag.seconds).coerceIn(0f, 1f), scene, time)
    }
}

// ---- drawing helpers ----

private class Px(val scope: DrawScope, val u: Float) {
    /** A rectangle of the grid: positions and sizes in grid pixels, snapped so edges stay crisp. */
    fun r(x: Float, y: Float, w: Float, h: Float, c: Color, a: Float = 1f) {
        val x0 = floor(x.roundToInt() * u)
        val y0 = floor(y.roundToInt() * u)
        val x1 = ceil((x.roundToInt() + max(1, w.roundToInt())) * u)
        val y1 = ceil((y.roundToInt() + max(1, h.roundToInt())) * u)
        scope.drawRect(if (a >= 1f) c else c.copy(alpha = c.alpha * a), Offset(x0, y0), Size(x1 - x0, y1 - y0))
    }
}

/** A steady pseudo-random number in 0..1 for a thing (i) and what it is for (salt), so nothing needs state. */
private fun hash(i: Int, salt: Int): Float {
    var x = i * 374761393 + salt * 668265263
    x = (x xor (x ushr 13)) * 1274126177.toInt()
    x = x xor (x ushr 16)
    return (x and 0xFFFF) / 65535f
}

private fun wrap(v: Float, size: Float): Float = ((v % size) + size) % size

private val SkyKeys = listOf(
    0f to Color(0xFF1A2856), 5f to Color(0xFF283A6E), 6.5f to Color(0xFFF4A261), 8f to Color(0xFF7EC8F2),
    17f to Color(0xFF7EC8F2), 18.5f to Color(0xFFE76F51), 20f to Color(0xFF283A6E), 24f to Color(0xFF1A2856),
)

private fun skyAt(hour: Float): Color {
    for (i in 0 until SkyKeys.size - 1) {
        val (a, ca) = SkyKeys[i]
        val (b, cb) = SkyKeys[i + 1]
        if (hour in a..b) return lerp(ca, cb, (hour - a) / (b - a))
    }
    return SkyKeys[0].second
}

private fun grayOf(sky: Sky) = when (sky) {
    Sky.CLEAR -> 0f; Sky.PARTLY_CLOUDY -> 0.1f; Sky.CLOUDY -> 0.35f; Sky.FOG -> 0.45f
    Sky.SNOW -> 0.4f; Sky.DRIZZLE -> 0.5f; Sky.RAIN -> 0.55f; Sky.STORM -> 0.7f
}

private fun cloudsOf(sky: Sky) = when (sky) {
    Sky.CLEAR -> 1; Sky.PARTLY_CLOUDY -> 4; Sky.CLOUDY -> 16; Sky.FOG -> 8
    Sky.SNOW -> 12; Sky.DRIZZLE -> 14; Sky.RAIN -> 16; Sky.STORM -> 18
}

/** Skies that are all cloud: the clouds are bigger and cover the whole sky, with a grey ceiling above them. */
private fun overcast(sky: Sky) = sky == Sky.CLOUDY || sky == Sky.RAIN || sky == Sky.STORM || sky == Sky.DRIZZLE || sky == Sky.SNOW

private val dry = setOf(Sky.CLEAR, Sky.PARTLY_CLOUDY)
private fun SceneInput.dark() = !isDay
private fun SceneInput.wet() = sky == Sky.RAIN || sky == Sky.DRIZZLE || sky == Sky.STORM

private fun drawSky(p: Px, s: SceneInput, t: Float) {
    val gray = grayOf(s.sky) * (if (s.dark()) 0.3f else 1f)
    val sky = lerp(skyAt(s.hour), Color(0xFF6E737D), gray)
    p.r(0f, 0f, W.toFloat(), GY.toFloat(), sky)
    if (s.dark() && s.sky in dry) {
        for (i in 0 until 36) {
            if (sin(t * 2 + hash(i, 1) * 6) > -0.3f) p.r(hash(i, 2) * W, hash(i, 3) * (GY - 20), 1f, 1f, Color.White)
        }
    }
    // sun or moon, on their way across
    val day = s.isDay
    val k = if (day) (s.hour - SceneInput.DAY_START) / (SceneInput.DAY_END - SceneInput.DAY_START)
    else (if (s.hour > SceneInput.DAY_END) s.hour - SceneInput.DAY_END else s.hour + 4.5f) / 10.5f
    val ax = 10 + k * 140
    val ay = GY - 10 - sin(k * PI.toFloat()) * (GY - 24)
    val vis = when (s.sky) { Sky.STORM -> 0f; Sky.RAIN, Sky.CLOUDY, Sky.DRIZZLE -> 0.35f; Sky.FOG -> 0.3f; else -> 1f }
    if (vis > 0f) {
        if (day) {
            val sun = Color(0xFFFFD84A)
            p.r(ax - 4, ay - 4, 8f, 8f, sun, vis); p.r(ax - 5, ay - 2, 10f, 4f, sun, vis); p.r(ax - 2, ay - 5, 4f, 10f, sun, vis)
            if ((t * 2).toInt() % 2 == 0) { p.r(ax - 8, ay, 2f, 1f, sun, vis); p.r(ax + 7, ay, 2f, 1f, sun, vis); p.r(ax, ay - 8, 1f, 2f, sun, vis); p.r(ax, ay + 7, 1f, 2f, sun, vis) }
        } else {
            val moon = Color(0xFFF4F1DE)
            p.r(ax - 3, ay - 3, 6f, 6f, moon, vis); p.r(ax - 4, ay - 1, 8f, 2f, moon, vis)
            p.r(ax - 1, ay - 4, 4f, 3f, sky); p.r(ax, ay - 3, 3f, 5f, sky)
        }
    }
    // clouds drift with the wind
    val cloud = if (s.dark()) lerp(Color(0xFFC8CDE1), Color(0xFF3C4664), 0.6f) else lerp(Color.White, Color(0xFF969BA5), min(1f, gray * 1.4f))
    val heavy = overcast(s.sky)
    if (heavy) p.r(0f, 0f, W.toFloat(), 14f, cloud, 0.55f)
    for (i in 0 until cloudsOf(s.sky)) {
        val x = wrap(hash(i, 4) * (W + 40) + t * (1.5f + 2.5f * hash(i, 5)) * s.wind, W + 40f) - 24
        val y = if (heavy) -2 + hash(i, 6) * (GY - 30) else 6 + hash(i, 6) * 22
        val sc = if (heavy) 1.5f + hash(i, 7) * 1.2f else 1 + hash(i, 7) * 0.8f
        val w = (14 * sc).roundToInt()
        p.r(x + 2, y + 3, w.toFloat(), 4f, cloud); p.r(x + 5, y, (w - 6).toFloat(), 4f, cloud); p.r(x, y + 4, (w + 4).toFloat(), 3f, cloud)
    }
}

private fun drawGround(p: Px, s: SceneInput, t: Float) {
    val snow = s.sky == Sky.SNOW
    val grass = if (snow) Color(0xFFEEF3F8) else if (s.dark()) Color(0xFF2A5C3A) else Color(0xFF4C9A45)
    val edge = if (snow) Color.White else if (s.dark()) Color(0xFF357048) else Color(0xFF62B259)
    p.r(0f, GY.toFloat(), W.toFloat(), (H - GY).toFloat(), grass)
    p.r(0f, GY.toFloat(), W.toFloat(), 2f, edge)
    if (!snow) for (i in 0 until W step 6) p.r(i + (i % 12).toFloat(), GY + 6f + i % 9, 1f, 2f, if (s.dark()) Color(0xFF18401F) else Color(0xFF3B7C36))
    p.r(70f, GY + 6f, 14f, (H - GY - 6).toFloat(), if (snow) Color(0xFFD8DDE3) else Color(0xFFC9B27C))
    p.r(72f, GY + 6f, 10f, (H - GY - 6).toFloat(), if (snow) Color(0xFFE6EAEE) else Color(0xFFD9C590))
    val fence = if (snow) Color.White else Color(0xFFE8DCC0)
    for (x in 0..44 step 6) p.r(2f + x, GY + 3f, 2f, 7f, fence)
    p.r(2f, GY + 5f, 46f, 1f, fence)
}

private val Festive = listOf(Color(0xFFE63946), Color(0xFFFFD84A), Color(0xFF4EA8DE), Color(0xFF6BBF59), Color(0xFFF48FB1), Color(0xFFFF9F1C))

/** Things of the special dates that stand on the lawn, behind the house. */
private fun drawBackdropDecor(p: Px, s: SceneInput, t: Float) {
    when (s.occasion) {
        Occasion.EASTER -> listOf(30f to 14f, 52f to 20f, 18f to 22f, 64f to 12f, 40f to 24f).forEachIndexed { i, (x, y) ->
            val c = Festive[i % Festive.size]
            p.r(x, GY + y, 4f, 5f, c); p.r(x + 1, GY + y - 1, 2f, 1f, c); p.r(x, GY + y + 2, 4f, 1f, Color.White, 0.8f)
        }
        Occasion.SAO_JOAO -> {
            // the bonfire (fogueira) with its logs and flames
            val bx = 36f
            val by = GY + 10f
            if (s.dark() || s.sky != Sky.CLEAR) { /* the glow */ p.r(bx - 9, by - 8, 20f, 12f, Color(0xFFFFB347), 0.14f) }
            p.r(bx - 5, by + 3, 12f, 2f, Color(0xFF6B4A2B)); p.r(bx - 3, by + 1, 8f, 2f, Color(0xFF8B5E34))
            for (k in 0 until 3) {
                val h = 5 + (3 * (sin(t * 9 + k * 2f) + 1)).toInt()
                p.r(bx - 3 + k * 3f, by + 1 - h, 3f, h.toFloat(), if (k == 1) Color(0xFFFFD84A) else Color(0xFFFF7A1A))
            }
            // bunting from the roof down to the fence
            for (i in 0..8) {
                val x = 88f - i * 6f
                val y = GY - 26f + i * 1.6f + sin(i * 1.3f) * 1.2f
                p.r(x, y, 1f, 1f, Color(0xFF8B5E34))
                p.r(x - 1, y + 1, 3f, 2f, Festive[i % Festive.size]); p.r(x, y + 3, 1f, 1f, Festive[i % Festive.size])
            }
        }
        Occasion.CHILDRENS_DAY -> for (i in 0 until 3) {
            val x = 58f + i * 4 + sin(t * 1.2f + i) * 1.5f
            val y = GY - 24f - i * 3 + sin(t * 1.6f + i * 2) * 1.2f
            p.r(x, y, 4f, 5f, Festive[(i * 2) % Festive.size]); p.r(x + 1, y + 5, 1f, 1f, Festive[(i * 2) % Festive.size])
            p.r(x + 1, y + 6, 1f, (GY - 6 - y - 6).coerceAtLeast(1f), Color.White, 0.7f)
        }
        else -> {}
    }
}

private fun drawTree(p: Px, s: SceneInput, t: Float) {
    val sway = (sin(t * (1 + s.wind * 2)) * s.wind * 1.6f).roundToInt().toFloat()
    p.r(133f, GY - 12f, 4f, 14f, Color(0xFF6B4A2B))
    val snow = s.sky == Sky.SNOW
    val a = if (snow) Color(0xFFF2F6FA) else if (s.dark()) Color(0xFF2C6B3A) else Color(0xFF3F8F3C)
    val b = if (snow) Color(0xFFDFE8F0) else if (s.dark()) Color(0xFF24583A) else Color(0xFF2F7A33)
    p.r(124 + sway, GY - 30f, 22f, 12f, a); p.r(127 + sway, GY - 36f, 16f, 8f, a)
    p.r(121 + sway, GY - 24f, 28f, 10f, b); p.r(126 + sway, GY - 18f, 18f, 6f, b)
    if (s.occasion == Occasion.CHRISTMAS || s.occasion == Occasion.NEW_YEAR) {
        listOf(128f to -30f, 138f to -26f, 132f to -20f, 142f to -20f, 124f to -22f).forEachIndexed { i, (x, y) ->
            val on = (t * 2 + i).toInt() % 3 != 0
            p.r(x + sway, GY + y, 2f, 2f, if (on) Festive[i % Festive.size] else Color(0xFF444444))
        }
    }
}

private fun drawHouse(p: Px, s: SceneInput, t: Float) {
    val bx = 92f
    val by = GY - 26f
    val bw = 36f
    val bh = 26f
    val night = s.dark()
    p.r(bx, by, bw, bh, if (night) Color(0xFFC9B9A0) else Color(0xFFF3E3C3))
    p.r(bx, by + bh - 2, bw, 2f, Color(0xFFB9A888))
    val roof = if (s.sky == Sky.SNOW) Color.White else Color(0xFFB5483A)
    for (i in 0 until 10) p.r(bx - 4 + i * 2, by - i, bw + 8 - i * 4, 1f, roof)
    p.r(bx - 4, by, bw + 8, 2f, if (s.sky == Sky.SNOW) Color(0xFFE6EEF5) else Color(0xFF8E362B))
    if (s.sky == Sky.SNOW) p.r(bx - 2, by - 9f, bw + 4, 2f, Color.White)
    // chimney
    p.r(bx + 24, by - 14f, 5f, 9f, Color(0xFF7A5A4A)); p.r(bx + 23, by - 15f, 7f, 2f, Color(0xFF5D4136))
    if (s.sky == Sky.SNOW) p.r(bx + 23, by - 16f, 7f, 2f, Color.White)
    // door
    p.r(bx + 15, by + 8, 7f, 18f, Color(0xFF7B4F2D)); p.r(bx + 19, by + 17, 1f, 1f, Color(0xFFFFD84A))
    // windows light up for the people at home (when it is dark or gloomy)
    val gloomy = night || s.sky in setOf(Sky.STORM, Sky.RAIN, Sky.FOG, Sky.CLOUDY, Sky.DRIZZLE)
    val lamp = if (s.occasion == Occasion.HALLOWEEN) Color(0xFFFF9F1C) else Color(0xFFFFD84A)
    listOf(bx + 4 to by + 7, bx + 25 to by + 7).forEachIndexed { i, (wx, wy) ->
        val on = s.peopleHome > i && gloomy
        p.r(wx - 1, wy - 1, 9f, 9f, Color.White)
        p.r(wx, wy, 7f, 7f, if (on) lamp else if (night) Color(0xFF2A3555) else Color(0xFF8FC8E8))
        p.r(wx + 3, wy, 1f, 7f, Color.White); p.r(wx, wy + 3, 7f, 1f, Color.White)
        if (on) p.r(wx - 3, wy - 3, 13f, 13f, lamp, 0.18f)
    }
    if (s.peopleHome > 2 && gloomy) { p.r(bx + 14, by + 3, 2f, 2f, lamp); p.r(bx + 13, by + 2, 4f, 4f, lamp, 0.25f) }
    // the occasion
    when (s.occasion) {
        Occasion.CHRISTMAS, Occasion.NEW_YEAR -> {
            var x = bx - 4
            var i = 0
            while (x < bx + bw + 4) {
                val on = (t * 2 + i).toInt() % 3
                val c = if (s.occasion == Occasion.NEW_YEAR) Color(0xFFFFD84A) else Festive[(i + on) % 4]
                p.r(x, by + 2f, 2f, 2f, if (on == 0) c else c.copy(alpha = 0.55f))
                x += 4; i++
            }
            if (s.occasion == Occasion.CHRISTMAS) { p.r(bx + 16, by + 4, 5f, 5f, Color(0xFF2F7A33)); p.r(bx + 18, by + 6, 1f, 1f, Color(0xFFE63946)) }
        }
        Occasion.HALLOWEEN -> listOf(bx + 6, bx + bw - 12).forEach { x ->
            val glow = night
            p.r(x, GY - 6f, 7f, 6f, Color(0xFFFF8C1A)); p.r(x + 3, GY - 8f, 1f, 2f, Color(0xFF3B7C36))
            p.r(x + 1, GY - 4f, 1f, 1f, Color(0xFF3A2200)); p.r(x + 5, GY - 4f, 1f, 1f, Color(0xFF3A2200)); p.r(x + 2, GY - 2f, 3f, 1f, Color(0xFF3A2200))
            if (glow) p.r(x - 1, GY - 7f, 9f, 8f, Color(0xFFFFB347), 0.2f)
        }
        Occasion.CARNIVAL -> {
            for (i in 0 until 10) {
                val x = bx - 4 + i * 4.6f
                p.r(x, by + 2f, 4f, 2f, Festive[i % Festive.size]); p.r(x + 1, by + 4f, 2f, 1f, Festive[i % Festive.size])
            }
            for (k in 0 until 3) { // streamers that sway
                val x = bx + 2 + k * 14f
                for (j in 0 until 8) p.r(x + sin(t * 3 + j * 0.7f + k) * 1.2f, by + 5f + j, 1f, 1f, Festive[(k + j / 2) % Festive.size])
            }
        }
        else -> {}
    }
    // smoke from the chimney, drifting with the wind
    for (k in 0 until 6) {
        val age = wrap(t * 0.35f + k / 6f, 1f)
        val sx = bx + 26 + age * s.wind * 10 + sin(age * 9 + k) * 1f
        p.r(sx, by - 16 - age * 16, 3f, 3f, if (night) Color(0xFF9AA3B5) else Color(0xFFE8E8EE), (1 - age) * 0.8f)
    }
    // the mailbox, with its flag up for an urgent chore
    p.r(60f, GY - 2f, 2f, 8f, Color(0xFF6B4A2B)); p.r(56f, GY - 8f, 10f, 6f, Color(0xFF3B6EA5)); p.r(56f, GY - 9f, 10f, 1f, Color(0xFF2F5782))
    if (s.urgent) { p.r(66f, GY - 14f, 1f, 7f, Color(0xFFE63946)); p.r(66f, GY - 14f, 4f, 3f, Color(0xFFE63946)) } else p.r(66f, GY - 8f, 4f, 1f, Color(0xFFE63946))
}

private fun drawWeather(p: Px, s: SceneInput, t: Float) {
    if (s.sky == Sky.RAIN || s.sky == Sky.STORM || s.sky == Sky.DRIZZLE) {
        val n = when (s.sky) { Sky.STORM -> 60; Sky.RAIN -> 36; else -> 18 }
        for (i in 0 until n) {
            val y = wrap(hash(i, 8) * H + t * 60f * (0.7f + hash(i, 9) * 0.6f), H.toFloat())
            val x = wrap(hash(i, 10) * (W + 30) - y * s.wind * 0.35f, W.toFloat())
            p.r(x, y, 1f, 3f, Color(0xFF9FC3E8))
        }
        if (s.sky == Sky.STORM) {
            val phase = t % 7f
            if (phase < 0.3f && hash((t / 7f).toInt(), 11) < 0.7f) p.r(0f, 0f, W.toFloat(), H.toFloat(), Color.White, (1 - phase / 0.3f) * 0.6f)
        }
    }
    if (s.sky == Sky.SNOW) {
        for (i in 0 until 50) {
            val y = wrap(hash(i, 12) * H + t * 14f, H.toFloat())
            p.r(wrap(hash(i, 13) * W + sin(t * 1.5f + hash(i, 14) * 6) * 3 + y * s.wind * 0.1f, W.toFloat()), y, 1f, 1f, Color.White)
        }
        p.r(30f, GY + 12f, 7f, 7f, Color.White); p.r(32f, GY + 8f, 4f, 4f, Color.White)
        p.r(33f, GY + 9f, 1f, 1f, Color(0xFF222222)); p.r(35f, GY + 9f, 1f, 1f, Color(0xFF222222)); p.r(34f, GY + 10f, 2f, 1f, Color(0xFFE76F51))
    }
    if (s.sky == Sky.FOG) {
        p.r(0f, 0f, W.toFloat(), H.toFloat(), Color(0xFFDFE3E8), 0.5f)
        for (i in 0 until 4) p.r(wrap(t * 6 + i * 50f, 200f) - 30, GY - 14f + i * 6, 50f, 5f, Color.White, 0.4f)
    }
    if (s.dark() && s.sky in dry) {
        for (i in 0 until 7) {
            val a = (sin(t * 3 + hash(i, 15) * 6) + 1) / 2
            if (a > 0.45f) p.r(20 + hash(i, 16) * 120 + sin(t + i) * 5, 55 + hash(i, 17) * 14 + cos(t * 0.8f + i) * 3, 1f, 1f, Color(0xFFE8FF7A), a)
        }
    }
}

// ---- the little events ----

private val Ink = Color(0xFF3A4A6A)

private fun drawGag(p: Px, gag: Gag, a: Float, s: SceneInput, t: Float) {
    when (gag) {
        Gag.BIRD -> {
            val landX = 107f
            val landY = GY - 38f
            val x = when { a < 0.2f -> -8 + a / 0.2f * (landX + 8); a < 0.75f -> landX; else -> landX + (a - 0.75f) / 0.25f * 80 }
            val y = when { a < 0.2f -> landY - 30 + a / 0.2f * 30; a < 0.75f -> landY; else -> landY - (a - 0.75f) / 0.25f * 40 }
            p.r(x, y, 4f, 2f, Ink); p.r(x + 3, y - 1, 2f, 2f, Ink); p.r(x + 4, y, 1f, 1f, Color(0xFFF4A261))
            if (a < 0.2f || a > 0.75f) p.r(x - 1, y - 2 + sin(t * 20).roundToInt(), 3f, 1f, Ink)
        }
        Gag.CAT -> {
            val x = -10 + a * 190
            val y = GY + 14f
            val fur = Color(0xFFD98E4E)
            p.r(x, y, 8f, 4f, fur); p.r(x + 7, y - 2, 4f, 4f, fur); p.r(x + 7, y - 3, 1f, 1f, fur); p.r(x + 10, y - 3, 1f, 1f, fur)
            p.r(x - 3, y - 1 + sin(t * 6).roundToInt(), 3f, 1f, fur); p.r(x + 1, y + 4, 1f, 2f, Color(0xFFB5703A)); p.r(x + 6, y + 4, 1f, 2f, Color(0xFFB5703A))
        }
        Gag.KITE -> {
            val x = 110 + sin(t * 1.5f) * 6
            val y = 14 + sin(t * 2) * 3
            for (i in 0 until 8) p.r(x + 3 + i * 1.5f, y + 8 + i * 5, 1f, 1f, Color.White)
            p.r(x, y, 7f, 7f, Color(0xFFE63946)); p.r(x + 3, y, 1f, 7f, Color(0xFFFFD84A)); p.r(x, y + 3, 7f, 1f, Color(0xFFFFD84A))
        }
        Gag.SHOOTING_STAR -> {
            val x = 130 - a * 100
            val y = 6 + a * 34
            for (i in 0 until 7) p.r(x + i * 2, y - i, 2f, 1f, Color.White, 1 - i / 7f)
        }
        Gag.BUTTERFLY -> {
            val x = 20 + a * 100 + sin(t * 3) * 8
            val y = GY + 4 + sin(t * 4) * 6 + sin(a * 8) * 8
            val w = if (abs(sin(t * 18)) > 0.5f) 3f else 1f
            p.r(x - w, y, w, 2f, Color(0xFFF48FB1)); p.r(x + 1, y, w, 2f, Color(0xFFF48FB1)); p.r(x, y, 1f, 3f, Color(0xFF333333))
        }
        Gag.RAINBOW -> {
            val alpha = 0.55f * sin(a * PI.toFloat())
            listOf(0xFFE63946, 0xFFF4A261, 0xFFFFD84A, 0xFF6BBF59, 0xFF4EA8DE, 0xFF7B5EA7).forEachIndexed { i, c ->
                var ang = 0f
                while (ang <= PI) { p.r(110 + cos(ang) * (34 - i * 2), GY - sin(ang) * (34 - i * 2), 2f, 2f, Color(c), alpha); ang += 0.03f }
            }
        }
        Gag.BATS -> for (k in 0 until 4) {
            val x = -10 + a * 190 - k * 14
            val y = 14 + k * 6 + sin(a * 12 + k) * 5
            val flap = if ((t * 10 + k).toInt() % 2 == 0) 1 else 0
            p.r(x, y, 2f, 2f, Color(0xFF1B1B2B)); p.r(x - 3, y - flap, 3f, 1f, Color(0xFF1B1B2B)); p.r(x + 2, y - flap, 3f, 1f, Color(0xFF1B1B2B))
        }
        Gag.GHOST -> {
            val x = 170 - a * 190
            val y = 30 + sin(t * 3) * 3 + a * 10
            val white = Color(0xFFF5F5FA)
            p.r(x, y, 8f, 9f, white, 0.85f); p.r(x - 1, y + 2, 10f, 6f, white, 0.85f)
            for (i in 0 until 4) p.r(x + i * 2.2f, y + 9 + (i % 2), 2f, 2f, white, 0.85f)
            p.r(x + 2, y + 3, 1f, 2f, Ink); p.r(x + 5, y + 3, 1f, 2f, Ink)
        }
        Gag.SLEIGH -> {
            val x = -40 + a * 210
            val y = 14 + sin(a * PI.toFloat()) * -8 + 10
            val deer = Color(0xFF8B5E34)
            for (k in 0 until 2) { val dx = x + 20 + k * 8; p.r(dx, y + 2, 5f, 3f, deer); p.r(dx + 4, y, 2f, 3f, deer); p.r(dx + 5, y - 2, 1f, 2f, deer); p.r(dx, y + 5, 1f, 2f, deer); p.r(dx + 4, y + 5, 1f, 2f, deer) }
            p.r(x + 22, y + 3, 14f, 1f, Color(0xFF8B5E34))
            p.r(x, y + 1, 10f, 5f, Color(0xFFE63946)); p.r(x + 1, y - 2, 4f, 3f, Color(0xFFE63946)); p.r(x - 1, y + 6, 12f, 1f, Color(0xFFFFD84A))
            for (i in 0 until 5) p.r(x - 4 - i * 3, y + 4 + (i % 2), 1f, 1f, Color(0xFFFFD84A), 1 - i / 5f)
        }
        Gag.BUNNY -> {
            val x = -10 + a * 180
            val hop = abs(sin(a * 22f)) * 6
            val y = GY + 14f - hop
            val fur = Color(0xFFF2EDE4)
            p.r(x, y, 7f, 5f, fur); p.r(x + 6, y - 2, 4f, 4f, fur); p.r(x + 6, y - 6, 1f, 4f, fur); p.r(x + 9, y - 6, 1f, 4f, fur)
            p.r(x - 2, y + 2, 2f, 2f, Color.White); p.r(x + 8, y - 1, 1f, 1f, Color(0xFF222222))
            // an egg left behind every so often
            for (e in 0 until 4) {
                val ex = e * 45f + 20
                if (x > ex + 6) p.r(ex, GY + 18f, 3f, 4f, Festive[e % Festive.size])
            }
        }
        Gag.FIREWORKS -> for (k in 0 until 3) {
            val local = (a * 3f - k * 0.7f).coerceIn(0f, 1.2f)
            if (local in 0.05f..1f) {
                val cx = 30f + k * 45
                val cy = 14f + (k % 2) * 10
                val c = Festive[(k * 2) % Festive.size]
                for (d in 0 until 12) {
                    val ang = d * (2 * PI.toFloat() / 12)
                    val r = local * 14
                    p.r(cx + cos(ang) * r, cy + sin(ang) * r + local * local * 6, 1f, 1f, c, 1 - local)
                }
            }
        }
        Gag.BALLOONS -> for (k in 0 until 5) {
            val x = 20 + k * 28f + sin(t * 1.3f + k * 2) * 3
            val y = H - a * (H + 30) - k * 8
            val c = Festive[k % Festive.size]
            p.r(x, y, 5f, 6f, c); p.r(x + 1, y + 6, 3f, 1f, c); p.r(x + 2, y + 7, 1f, 1f, c); p.r(x + 2, y + 8, 1f, 8f, Color.White, 0.6f)
        }
        Gag.CONFETTI -> for (i in 0 until 60) {
            val x = hash(i, 20) * W + sin(t * 2 + i) * 2
            val y = wrap(hash(i, 21) * H + a * 40f * (0.6f + hash(i, 22)), H.toFloat())
            p.r(x, y, 2f, 2f, Festive[i % Festive.size], min(1f, 3 * (1 - a)))
        }
    }
}
