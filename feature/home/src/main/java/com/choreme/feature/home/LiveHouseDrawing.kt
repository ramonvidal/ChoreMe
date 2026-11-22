package com.choreme.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.choreme.core.model.home.Gag
import com.choreme.core.model.home.Occasion
import com.choreme.core.model.home.SceneInput
import com.choreme.core.model.weather.Sky
import com.choreme.core.ui.theme.memberColor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// ---- drawing helpers ----

internal class Px(val scope: DrawScope, val u: Float) {
    /** A sideways shift for what is being drawn now (the far hills drift more slowly than the rest: parallax). */
    var dx = 0f

    /** A rectangle of the grid: positions and sizes in grid pixels, snapped so edges stay crisp. */
    fun r(xIn: Float, y: Float, w: Float, h: Float, c: Color, a: Float = 1f) {
        val x = xIn + dx
        val x0 = floor(x.roundToInt() * u)
        val y0 = floor(y.roundToInt() * u)
        val x1 = ceil((x.roundToInt() + max(1, w.roundToInt())) * u)
        val y1 = ceil((y.roundToInt() + max(1, h.roundToInt())) * u)
        scope.drawRect(if (a >= 1f) c else c.copy(alpha = c.alpha * a), Offset(x0, y0), Size(x1 - x0, y1 - y0))
    }
}

/** Draws [block] shifted sideways by [by] grid pixels. */
internal inline fun Px.shifted(by: Float, block: () -> Unit) {
    val before = dx
    dx += by
    block()
    dx = before
}

/** Where the house stands, for the things drawn on and around it. */
internal const val HOUSE_X = 92f
internal const val HOUSE_Y = GY - 26f

/** A steady pseudo-random number in 0..1 for a thing (i) and what it is for (salt), so nothing needs state. */
internal fun hash(i: Int, salt: Int): Float {
    var x = i * 374761393 + salt * 668265263
    x = (x xor (x ushr 13)) * 1274126177.toInt()
    x = x xor (x ushr 16)
    return (x and 0xFFFF) / 65535f
}

internal fun wrap(v: Float, size: Float): Float = ((v % size) + size) % size

/** The colours of the sky through the day, around the sun's real rise and set: night, dawn, day, dusk, night. */
private fun skyKeys(s: SceneInput) = listOf(
    0f to Color(0xFF1A2856), (s.sunrise - 1.5f) to Color(0xFF283A6E), s.sunrise to Color(0xFFF4A261), (s.sunrise + 1.5f) to Color(0xFF7EC8F2),
    (s.sunset - 1.5f) to Color(0xFF7EC8F2), s.sunset to Color(0xFFE76F51), (s.sunset + 1.5f) to Color(0xFF283A6E), 24f to Color(0xFF1A2856),
)

private fun skyAt(hour: Float, keys: List<Pair<Float, Color>>): Color {
    for (i in 0 until keys.size - 1) {
        val (a, ca) = keys[i]
        val (b, cb) = keys[i + 1]
        if (hour in a..b) return lerp(ca, cb, (hour - a) / (b - a))
    }
    return keys[0].second
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

/** Where the sun (by day) or the moon (by night) is: across the sky from its rise to its set. */
internal fun celestial(s: SceneInput): Pair<Float, Float> {
    val k = if (s.isDay) (s.hour - s.sunrise) / (s.sunset - s.sunrise)
    else (if (s.hour > s.sunset) s.hour - s.sunset else s.hour + 24f - s.sunset) / (24f - s.sunset + s.sunrise)
    return (10 + k * 140) to (GY - 10 - sin(k * PI.toFloat()) * (GY - 24))
}

/** How much dawn or dusk colours the horizon: 1 at sunrise and sunset, 0 an hour and a half away. */
private fun glow(s: SceneInput): Float = max(0f, max(1 - abs(s.hour - s.sunrise) / 1.5f, 1 - abs(s.hour - s.sunset) / 1.5f))

/** The moon's disc, rows of 3, 5, 7, 7, 7, 5 and 3 pixels around (cx, cy). */
private fun disc(p: Px, cx: Float, cy: Float, c: Color, a: Float) {
    listOf(3, 5, 7, 7, 7, 5, 3).forEachIndexed { row, w -> p.r(cx - w / 2f, cy - 3 + row, w.toFloat(), 1f, c, a) }
}

/** The moon in its real phase: lit from the right while it grows, from the left while it wanes. */
private fun drawMoon(p: Px, cx: Float, cy: Float, phase: Float, behind: Color, vis: Float) {
    val moon = Color(0xFFF4F1DE)
    val full = if (phase < 0.5f) phase * 2 else (1 - phase) * 2
    disc(p, cx, cy, moon, vis * 0.18f) // the dark part still faintly there
    // The lit part: the disc, then the shadow slid over it from one side.
    val shift = full * 8f
    disc(p, cx, cy, moon, vis)
    if (full < 0.97f) disc(p, cx + (if (phase < 0.5f) -shift else shift), cy, behind, 1f)
}

internal fun drawSky(p: Px, s: SceneInput, t: Float) {
    val gray = grayOf(s.sky) * (if (s.dark()) 0.3f else 1f)
    val sky = lerp(skyAt(s.hour, skyKeys(s)), Color(0xFF6E737D), gray)
    // Lighter towards the horizon, and pink and orange there at dawn and dusk (less so under cloud).
    val horizon = lerp(lerp(sky, Color.White, if (s.dark()) 0.08f else 0.25f), Color(0xFFFF8E72), glow(s) * 0.75f * (1 - gray))
    fun at(y: Float) = lerp(sky, horizon, (y / GY).coerceIn(0f, 1f).let { it * it })
    for (y in 0 until GY step 3) p.r(0f, y.toFloat(), W.toFloat(), 3f, at(y.toFloat()))
    if (s.dark() && s.sky in dry) {
        for (i in 0 until 40) {
            val tw = sin(t * (1.5f + hash(i, 18) * 2) + hash(i, 1) * 6)
            if (tw > -0.3f) {
                val x = hash(i, 2) * W
                val y = hash(i, 3) * (GY - 20)
                p.r(x, y, 1f, 1f, Color.White, 0.5f + 0.5f * max(0f, tw))
                // a few bright ones sparkle into a little cross
                if (i % 9 == 0 && tw > 0.8f) { p.r(x - 1, y, 3f, 1f, Color.White, 0.45f); p.r(x, y - 1, 1f, 3f, Color.White, 0.45f) }
            }
        }
    }
    // sun or moon, on their way across
    val day = s.isDay
    val (ax, ay) = celestial(s)
    val vis = when (s.sky) { Sky.STORM -> 0f; Sky.RAIN, Sky.CLOUDY, Sky.DRIZZLE -> 0.35f; Sky.FOG -> 0.3f; else -> 1f }
    if (vis > 0f) {
        if (day) {
            val sun = Color(0xFFFFD84A)
            p.r(ax - 4, ay - 4, 8f, 8f, sun, vis); p.r(ax - 5, ay - 2, 10f, 4f, sun, vis); p.r(ax - 2, ay - 5, 4f, 10f, sun, vis)
            if ((t * 2).toInt() % 2 == 0) { p.r(ax - 8, ay, 2f, 1f, sun, vis); p.r(ax + 7, ay, 2f, 1f, sun, vis); p.r(ax, ay - 8, 1f, 2f, sun, vis); p.r(ax, ay + 7, 1f, 2f, sun, vis) }
        } else {
            drawMoon(p, ax, ay, s.moonPhase, at(ay), vis)
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

/** The sea on the horizon (Bahia) with its glints and the sun's or moon's path, and green hills nearer, drifting slowly. */
internal fun drawDistance(p: Px, s: SceneInput, t: Float) {
    val night = s.dark()
    val gray = grayOf(s.sky)
    val sea = lerp(if (night) Color(0xFF1C3050) else Color(0xFF3F8FC9), Color(0xFF7D8794), gray * 0.6f)
    p.r(0f, GY - 5f, W.toFloat(), 5f, sea)
    for (i in 0 until 14) {
        if (sin(t * 2 + i) > 0.2f) p.r(wrap(hash(i, 30) * W + t * 2f, W.toFloat()), GY - 4f + (i % 3), 2f, 1f, Color.White, 0.35f)
    }
    val (ax, _) = celestial(s)
    if (s.sky in dry) for (j in 0 until 4) {
        if (sin(t * 3 + j * 1.7f) > -0.2f) p.r(ax - 2 + sin(t * 2 + j) * 1.5f, GY - 4f + j, (4 - j % 2).toFloat(), 1f, if (night) Color(0xFFF4F1DE) else Color(0xFFFFE9A8), 0.7f)
    }
    if (s.occasion == Occasion.IEMANJA) {
        // a little boat with its offerings, and flowers floating
        val bx = wrap(20 + t * 1.2f, 70f)
        p.r(bx, GY - 4f, 8f, 2f, Color.White); p.r(bx + 1, GY - 3f, 6f, 1f, Color(0xFF4EA8DE)); p.r(bx + 3, GY - 7f, 1f, 3f, Color(0xFF8B5E34))
        p.r(bx + 4, GY - 7f, 3f, 2f, Color.White, 0.9f)
        for (k in 0 until 6) p.r(wrap(hash(k, 31) * 70 + t * 0.8f, 70f), GY - 3f + (k % 2), 1f, 1f, if (k % 2 == 0) Color(0xFFF48FB1) else Color.White)
    }
    // Hills between the sea and the house, a touch paler with distance; they drift a pixel or two, slower than the rest.
    val snow = s.sky == Sky.SNOW
    val hill = if (snow) Color(0xFFE6ECF2) else if (night) Color(0xFF27483A) else lerp(Color(0xFF8DBF7F), Color(0xFFAFC3B5), gray * 0.6f)
    p.shifted(sin(t / 16f) * 2f) {
        var x = 64
        while (x < W + 4) {
            val h = (6 + 5 * sin(x * 0.07f) + 3 * sin(x * 0.15f + 1f)).coerceAtLeast(2f)
            p.r(x.toFloat(), GY - h, 2f, h, hill)
            x += 2
        }
    }
}

internal fun drawGround(p: Px, s: SceneInput, t: Float) {
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
internal fun drawBackdropDecor(p: Px, s: SceneInput, t: Float) {
    when (s.occasion) {
        Occasion.EASTER -> listOf(30f to 8f, 52f to 14f, 18f to 15f, 64f to 9f, 40f to 17f).forEachIndexed { i, (x, y) ->
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

/** A coconut palm (a Bahia garden), its leaves swaying with the wind; lights on its leaves at Christmas. */
internal fun drawTree(p: Px, s: SceneInput, t: Float) {
    val sway = sin(t * (1 + s.wind * 2)) * s.wind * 1.6f
    val snow = s.sky == Sky.SNOW
    val night = s.dark()
    // a trunk that leans a little, with its rings
    val top = 32
    for (i in 0 until top step 2) {
        val lean = (i.toFloat() / top).let { it * it } * 6f + sway * (i.toFloat() / top) * 0.5f
        p.r(140f - lean, GY + 1f - i - 2, 3f, 2f, if ((i / 2) % 2 == 0) Color(0xFF8B6A45) else Color(0xFF735634))
    }
    val tx = 140f - 6f + sway * 0.5f + 1f
    val ty = GY + 1f - top - 1f
    val leafA = if (snow) Color(0xFFF2F6FA) else if (night) Color(0xFF2C6B3A) else Color(0xFF3F9A47)
    val leafB = if (snow) Color(0xFFDFE8F0) else if (night) Color(0xFF24583A) else Color(0xFF2F7A33)
    val angles = listOf(-2.9f, -2.3f, -1.7f, -1.15f, -0.5f, 0.15f)
    angles.forEachIndexed { k, angle ->
        for (i in 1..14) {
            val bend = sway * (i / 14f)
            val x = tx + cos(angle) * i * 1.1f + bend
            val y = ty + sin(angle) * i * 0.55f + i * i * 0.055f
            p.r(x, y, 2f, 1f, if ((i + k) % 3 == 0) leafB else leafA)
            if (i in 3..11 && i % 2 == 0) p.r(x, y + 1, 1f, 1f, leafB)
        }
    }
    // coconuts
    if (!snow) { p.r(tx - 1, ty + 1, 2f, 2f, Color(0xFF6B4A2B)); p.r(tx + 1, ty + 2, 2f, 2f, Color(0xFF5A3D22)); p.r(tx - 2, ty + 3, 2f, 2f, Color(0xFF6B4A2B)) }
    if (s.occasion == Occasion.CHRISTMAS || s.occasion == Occasion.NEW_YEAR) {
        angles.forEachIndexed { k, angle ->
            val on = (t * 2 + k).toInt() % 3 != 0
            val x = tx + cos(angle) * 12 * 1.1f + sway
            val y = ty + sin(angle) * 12 * 0.55f + 144 * 0.055f
            p.r(x, y + 1, 2f, 2f, if (on) Festive[k % Festive.size] else Color(0xFF444444))
        }
    }
}

internal fun drawHouse(p: Px, s: SceneInput, t: Float) {
    val bx = 92f
    val by = GY - 26f
    val bw = 36f
    val bh = 26f
    val night = s.dark()
    p.r(bx, by, bw, bh, if (night) Color(0xFFC9B9A0) else Color(0xFFF3E3C3))
    p.r(bx, by + bh - 2, bw, 2f, Color(0xFFB9A888))
    val roof = if (s.sky == Sky.SNOW) Color.White else Color(0xFFB5483A)
    for (i in 0 until 10) p.r(bx - 4 + i * 2, by - i, bw + 8 - i * 4, 1f, roof)
    // the tiles: a darker dot every few pixels, offset row by row
    if (s.sky != Sky.SNOW) for (i in 1 until 9 step 2) {
        var x = bx - 4 + i * 2 + (if ((i / 2) % 2 == 0) 1 else 3)
        while (x < bx - 4 + i * 2 + bw + 8 - i * 4 - 1) { p.r(x, by - i, 1f, 1f, Color(0xFF8E362B)); x += 4 }
    }
    // the porch on the left, with its hammock
    p.r(bx - 11, GY - 14f, 12f, 2f, if (s.sky == Sky.SNOW) Color.White else Color(0xFF8E362B))
    p.r(bx - 10, GY - 12f, 1f, 12f, Color(0xFF7B4F2D)); p.r(bx - 1, GY - 12f, 1f, 12f, Color(0xFF7B4F2D))
    val stripes = listOf(Color(0xFFE63946), Color(0xFFFFD84A), Color(0xFF4EA8DE))
    listOf(-9f to -10f, -8f to -9f, -7f to -8f, -6f to -8f, -5f to -8f, -4f to -8f, -3f to -9f, -2f to -10f).forEachIndexed { i, (x, y) ->
        p.r(bx + x, GY + y, 1f, 1f, stripes[i % 3])
    }
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
        // someone at home is seen at a lit window, in their own colour
        if (on && i < s.homeColors.size) { p.r(wx + 1, wy + 3, 2f, 4f, memberColor(s.homeColors[i])); p.r(wx + 1, wy + 1, 2f, 2f, Color(0xFF5A3D2B)) }
        p.r(wx + 3, wy, 1f, 7f, Color.White); p.r(wx, wy + 3, 7f, 1f, Color.White)
        // curtains
        p.r(wx, wy, 1f, 7f, Color(0xFFE8A0A0)); p.r(wx + 6, wy, 1f, 7f, Color(0xFFE8A0A0))
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
        Occasion.MOTHERS_DAY, Occasion.FATHERS_DAY -> heart(p, bx + 16, by + 3, Color(0xFFE63946))
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

/** A little pixel heart, 5 wide. */
internal fun heart(p: Px, x: Float, y: Float, c: Color, a: Float = 1f) {
    p.r(x, y, 2f, 1f, c, a); p.r(x + 3, y, 2f, 1f, c, a); p.r(x, y + 1, 5f, 1f, c, a); p.r(x + 1, y + 2, 3f, 1f, c, a); p.r(x + 2, y + 3, 1f, 1f, c, a)
}

/** A person, 2 wide and 7 tall, in their own colour. */
private fun figure(p: Px, x: Float, y: Float, color: Int, a: Float = 1f) {
    p.r(x, y, 2f, 2f, Color(0xFF5A3D2B), a)
    p.r(x, y + 2, 2f, 3f, memberColor(color), a)
    p.r(x, y + 5, 2f, 2f, Color(0xFF2F3440), a)
}

private val GLOOMY = setOf(Sky.STORM, Sky.RAIN, Sky.FOG, Sky.CLOUDY, Sky.DRIZZLE)

/**
 * The yard and the family: the street lamp, the washing on the line, the bin on bin day, the people at home on the
 * porch, those on their way walking up the path, and the decorations of Bahia's own dates.
 */
internal fun drawYard(p: Px, s: SceneInput, t: Float) {
    val night = s.dark()
    val gloomy = night || s.sky in GLOOMY
    val bx = HOUSE_X
    // the street lamp, lit when it's dark or gloomy
    val lx = 51f
    p.r(lx, GY - 18f, 1f, 26f, Color(0xFF4A4A55)); p.r(lx - 1, GY - 20f, 4f, 2f, Color(0xFF4A4A55))
    p.r(lx, GY - 18f, 3f, 2f, if (gloomy) Color(0xFFFFE08A) else Color(0xFFBFC5CC))
    if (gloomy) { p.r(lx - 5, GY - 16f, 13f, 10f, Color(0xFFFFE08A), 0.12f); p.r(lx - 6, GY + 6f, 15f, 4f, Color(0xFFFFE08A), 0.15f) }
    // the washing, out when it's a good moment to dry it
    if (s.laundryOut) {
        p.r(8f, GY - 14f, 1f, 16f, Color(0xFF7B4F2D)); p.r(40f, GY - 14f, 1f, 16f, Color(0xFF7B4F2D))
        for (x in 9 until 40) p.r(x.toFloat(), GY - 13f + sin((x - 9) / 31f * PI.toFloat()) * 1.5f, 1f, 1f, Color(0xFFDDDDDD))
        listOf(12f to (4f to 5f), 18f to (5f to 4f), 25f to (3f to 6f), 30f to (4f to 4f), 35f to (3f to 5f)).forEachIndexed { i, (x, size) ->
            val swing = sin(t * (2 + s.wind * 3) + i) * s.wind
            p.r(x + swing, GY - 12f + sin((x - 9) / 31f * PI.toFloat()) * 1.5f, size.first, size.second, Festive[(i * 2 + 1) % Festive.size])
        }
    }
    // the bin, out by the path on bin day
    if (s.trashOut) {
        p.r(86f, GY + 7f, 5f, 6f, Color(0xFF3F7F4A)); p.r(85f, GY + 6f, 7f, 1f, Color(0xFF2E5E37)); p.r(86f, GY + 13f, 1f, 1f, Color(0xFF222222)); p.r(90f, GY + 13f, 1f, 1f, Color(0xFF222222))
    }
    // the people at home sit out on the porch on a nice day; otherwise they are at the windows (see drawHouse)
    if (!gloomy) s.homeColors.take(3).forEachIndexed { i, c -> figure(p, bx - 9 + i * 3f, GY - 7f, c) }
    // those on their way home walk up the path to the door
    s.arriving.take(3).forEachIndexed { i, c ->
        val k = wrap(t / 9f + i * 0.37f, 1f)
        figure(p, 75f + i * 3f, H - 9f - k * (H - GY - 4f), c, if (k > 0.85f) (1 - k) / 0.15f else 1f)
    }
    when (s.occasion) {
        // fitinhas do Bonfim tied along the fence
        Occasion.BONFIM -> for (x in 2 until 48 step 3) {
            val len = 4 + (sin(t * 3 + x) * 1.5f).roundToInt()
            p.r(2f + x, GY + 4f, 1f, len.toFloat(), Festive[(x / 3) % Festive.size])
        }
        // blue, white and red for the 2 de Julho, along the fence and under the porch
        Occasion.DOIS_DE_JULHO -> {
            val flag = listOf(Color(0xFF1F4E9C), Color.White, Color(0xFFD62828))
            for (i in 0 until 12) { p.r(2f + i * 4, GY + 3f, 3f, 2f, flag[i % 3]); p.r(3f + i * 4, GY + 5f, 1f, 1f, flag[i % 3]) }
            for (i in 0 until 3) { p.r(bx - 10 + i * 3f, GY - 12f, 2f, 2f, flag[i % 3]) }
        }
        // spring flowers on the lawn
        Occasion.SPRING -> for (i in 0 until 16) {
            val x = hash(i, 40) * 66
            val y = GY + 8 + hash(i, 41) * (H - GY - 10)
            p.r(x, y, 2f, 2f, Festive[i % Festive.size]); p.r(x + 0.5f, y + 0.5f, 1f, 1f, Color(0xFFFFE08A))
        }
        else -> {}
    }
}

internal fun drawWeather(p: Px, s: SceneInput, t: Float) {
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
    // fireflies on warm, dry nights
    if (s.dark() && s.sky in dry && s.warm) {
        for (i in 0 until 7) {
            val a = (sin(t * 3 + hash(i, 15) * 6) + 1) / 2
            if (a > 0.45f) p.r(20 + hash(i, 16) * 120 + sin(t + i) * 5, 55 + hash(i, 17) * 14 + cos(t * 0.8f + i) * 3, 1f, 1f, Color(0xFFE8FF7A), a)
        }
    }
}

// ---- the little events ----

private val Ink = Color(0xFF3A4A6A)

internal fun drawGag(p: Px, gag: Gag, a: Float, s: SceneInput, t: Float) {
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
        Gag.HUMMINGBIRD -> {
            val hx = 126f
            val hy = GY - 30f
            val x = when { a < 0.25f -> 170 - a / 0.25f * (170 - hx); a < 0.75f -> hx + sin(t * 7) * 1.5f; else -> hx - (a - 0.75f) / 0.25f * 150 }
            val y = when { a < 0.25f -> hy - 10 + a / 0.25f * 10; a < 0.75f -> hy + sin(t * 5); else -> hy - (a - 0.75f) / 0.25f * 20 }
            val green = Color(0xFF2FA36B)
            p.r(x, y, 3f, 2f, green); p.r(x - 2, y, 2f, 1f, Color(0xFF333333)); p.r(x + 3, y + 1, 2f, 1f, green)
            if ((t * 30).toInt() % 2 == 0) p.r(x, y - 2, 2f, 2f, Color(0xFFB8E0C8), 0.7f)
        }
        Gag.GECKO -> {
            // up the house wall, beside the right window, and away under the roof
            val x = HOUSE_X + 34f
            val y = GY - 4 - a * 20
            val c = if (s.dark()) Color(0xFF9DAF8F) else Color(0xFF6E9E4E)
            val step = if ((t * 6).toInt() % 2 == 0) 0f else 1f
            p.r(x, y, 1f, 5f, c); p.r(x - 1, y + 1 - step, 1f, 1f, c); p.r(x + 1, y + 1 + step, 1f, 1f, c)
            p.r(x - 1, y + 3 + step, 1f, 1f, c); p.r(x + 1, y + 3 - step, 1f, 1f, c); p.r(x, y + 5, 1f, 2f, c)
        }
        Gag.PLANE_BANNER -> {
            val x = 170 - a * 240
            val y = 9f
            val body = Color(0xFFDDE2E8)
            p.r(x, y, 7f, 2f, body); p.r(x + 5, y - 2, 2f, 2f, body); p.r(x + 2, y + 2, 3f, 1f, Color(0xFFB0B8C2))
            p.r(x + 7, y + 1, 5f, 1f, Color.White, 0.8f)
            for (k in 0 until 6) p.r(x + 12 + k * 3, y + sin(t * 6 + k) * 0.6f, 3f, 3f, if (k % 2 == 0) Color(0xFFE63946) else Color(0xFFFFD84A))
        }
        Gag.ICE_CREAM -> {
            // the ice-cream cart going by on the street, an umbrella over it
            val x = -24 + a * 210
            val y = H - 9f
            figure(p, x - 4, y - 1, 1)
            p.r(x, y, 10f, 6f, Color.White); p.r(x, y + 1, 10f, 1f, Color(0xFF4EA8DE))
            p.r(x + 1, y + 6, 2f, 2f, Color(0xFF333333)); p.r(x + 7, y + 6, 2f, 2f, Color(0xFF333333))
            p.r(x + 5, y - 4, 1f, 4f, Color(0xFF888888)); p.r(x - 1, y - 6, 12f, 2f, Color(0xFFE63946)); p.r(x + 2, y - 7, 6f, 1f, Color(0xFFFFD84A))
        }
        Gag.METEORS -> for (k in 0 until 4) {
            val local = (a * 4 - k * 0.7f).coerceIn(0f, 1f)
            if (local in 0.02f..0.98f) {
                val x = 150 - k * 28 - local * 40
                val y = 4 + k * 5 + local * 20
                for (i in 0 until 6) p.r(x + i * 2, y - i, 2f, 1f, Color.White, (1 - i / 6f) * (1 - local))
            }
        }
        Gag.HEARTS -> for (k in 0 until 6) {
            val local = (a * 1.3f - k * 0.08f).coerceIn(0f, 1f)
            if (local > 0f) heart(p, HOUSE_X + 16 + (k - 3) * 6 + sin(t * 2 + k) * 2, HOUSE_Y + 4 - local * 40, if (k % 2 == 0) Color(0xFFE63946) else Color(0xFFF48FB1), 1 - local * 0.8f)
        }
        Gag.CONFETTI -> for (i in 0 until 60) {
            val x = hash(i, 20) * W + sin(t * 2 + i) * 2
            val y = wrap(hash(i, 21) * H + a * 40f * (0.6f + hash(i, 22)), H.toFloat())
            p.r(x, y, 2f, 2f, Festive[i % Festive.size], min(1f, 3 * (1 - a)))
        }
    }
}
