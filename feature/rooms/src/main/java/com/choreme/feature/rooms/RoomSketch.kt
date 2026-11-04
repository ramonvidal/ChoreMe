package com.choreme.feature.rooms

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.choreme.core.model.room.RoomType

/**
 * The standard picture of a room that has no photo: a light, hand-drawn looking sketch of the
 * kind of room (a bed, a bathtub, a sofa...). Drawn in fixed colors on a warm paper tone, so
 * it looks the same in light and dark. The drawing is laid out on a 100 x 60 grid.
 */
@Composable
internal fun RoomSketch(type: RoomType, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = minOf(size.width / GRID_W, size.height / GRID_H)
        val origin = Offset((size.width - GRID_W * unit) / 2, (size.height - GRID_H * unit) / 2)
        drawRect(Paper)
        Sketcher(this, unit, origin).room(type)
    }
}

private const val GRID_W = 100f
private const val GRID_H = 60f

// The floor starts here; everything stands on it.
private const val FLOOR = 42f

private val Paper = Color(0xFFFBF6EA)
private val Floor = Color(0xFFEADFC6)
private val Ink = Color(0xFF55514A)
private val Wood = Color(0xFFD9BE96)
private val Soft = Color(0xFFF1E8D2)
private val Green = Color(0xFF8DB38F)
private val Sky = Color(0xFFBBD9E8)
private val Clay = Color(0xFFD98A72)
private val Sun = Color(0xFFF2C14E)
private val Metal = Color(0xFFDDE3E6)

/** Draws on the 100 x 60 grid: every call takes grid coordinates. */
private class Sketcher(private val scope: DrawScope, private val unit: Float, private val origin: Offset) {
    private fun p(x: Float, y: Float) = Offset(origin.x + x * unit, origin.y + y * unit)
    private val stroke get() = Stroke(width = (unit * 0.8f).coerceAtLeast(1.5f), cap = StrokeCap.Round)

    fun box(x: Float, y: Float, w: Float, h: Float, fill: Color = Soft, radius: Float = 1f) {
        val r = CornerRadius(radius * unit)
        scope.drawRoundRect(fill, p(x, y), Size(w * unit, h * unit), r)
        scope.drawRoundRect(Ink, p(x, y), Size(w * unit, h * unit), r, style = stroke)
    }

    fun oval(x: Float, y: Float, w: Float, h: Float, fill: Color = Soft) {
        scope.drawOval(fill, p(x, y), Size(w * unit, h * unit))
        scope.drawOval(Ink, p(x, y), Size(w * unit, h * unit), style = stroke)
    }

    fun circle(cx: Float, cy: Float, r: Float, fill: Color = Soft) = oval(cx - r, cy - r, 2 * r, 2 * r, fill)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        scope.drawLine(Ink, p(x1, y1), p(x2, y2), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }

    fun window(x: Float, y: Float, w: Float = 18f, h: Float = 16f) {
        box(x, y, w, h, Sky)
        line(x + w / 2, y, x + w / 2, y + h)
        line(x, y + h / 2, x + w, y + h / 2)
    }

    fun plant(x: Float, baseY: Float) {
        circle(x, baseY - 9f, 3.2f, Green)
        circle(x - 3f, baseY - 6f, 2.6f, Green)
        circle(x + 3f, baseY - 6f, 2.6f, Green)
        box(x - 3f, baseY - 4f, 6f, 4f, Clay)
    }

    fun room(type: RoomType) {
        val outdoors = type == RoomType.GARDEN || type == RoomType.BALCONY
        if (outdoors) {
            scope.drawRect(Sky.copy(alpha = 0.4f), p(0f, 0f), Size(GRID_W * unit, FLOOR * unit))
            scope.drawRect(Green.copy(alpha = 0.45f), p(0f, FLOOR), Size(GRID_W * unit, (GRID_H - FLOOR) * unit))
        } else {
            scope.drawRect(Floor, p(0f, FLOOR), Size(GRID_W * unit, (GRID_H - FLOOR) * unit))
            line(0f, FLOOR, GRID_W, FLOOR)
        }
        furniture(type)
    }

    private fun furniture(type: RoomType) {
        when (type) {
            RoomType.LIVING_ROOM -> {
                // A framed picture over a sofa with cushions, a coffee table, a lamp and a plant.
                box(32f, 8f, 26f, 14f, Paper); box(35f, 11f, 20f, 8f, Sky)
                box(14f, 24f, 56f, 12f, Clay.copy(alpha = 0.55f), 3f)
                box(8f, 28f, 10f, 14f, Clay.copy(alpha = 0.75f), 3f)
                box(66f, 28f, 10f, 14f, Clay.copy(alpha = 0.75f), 3f)
                box(16f, 32f, 50f, 10f, Clay.copy(alpha = 0.55f), 2f)
                line(41f, 33f, 41f, 41f)
                line(11f, 42f, 11f, 45f); line(73f, 42f, 73f, 45f)
                box(30f, 47f, 24f, 3f, Wood); line(33f, 50f, 33f, 54f); line(51f, 50f, 51f, 54f)
                line(86f, 18f, 86f, 40f); box(80f, 10f, 12f, 8f, Sun)
                plant(94f, 42f)
            }
            RoomType.BEDROOM -> bed(headboardColor = Wood, small = false)
            RoomType.KIDS_ROOM -> {
                bed(headboardColor = Sky, small = true)
                // Toy blocks and a star on the wall.
                box(68f, 34f, 8f, 8f, Clay); box(76f, 34f, 8f, 8f, Sun); box(72f, 26f, 8f, 8f, Green)
                circle(86f, 12f, 5f, Sun)
                box(4f, 8f, 16f, 14f, Paper); circle(12f, 15f, 3f, Clay)
            }
            RoomType.BATHROOM -> {
                // Bathtub with taps, a toilet with its tank, a mirror and a towel.
                window(30f, 6f, 16f, 14f)
                box(6f, 28f, 48f, 14f, Paper, 6f)
                box(4f, 26f, 52f, 4f, Paper, 2f)
                line(10f, 42f, 8f, 46f); line(50f, 42f, 52f, 46f)
                line(44f, 22f, 44f, 26f); line(44f, 22f, 48f, 22f)
                box(66f, 18f, 12f, 12f, Metal, 2f)
                box(68f, 30f, 10f, 4f, Paper, 1f)
                box(64f, 34f, 18f, 8f, Paper, 4f)
                box(88f, 14f, 6f, 16f, Sky, 1f); line(91f, 12f, 91f, 14f)
            }
            RoomType.KITCHEN -> {
                // Fridge, cupboards over a counter with a pot and a tap, and a window.
                box(4f, 8f, 18f, 34f, Metal, 2f); line(4f, 22f, 22f, 22f); line(8f, 12f, 8f, 18f); line(8f, 26f, 8f, 34f)
                box(26f, 8f, 26f, 14f, Wood, 1f); line(39f, 8f, 39f, 22f)
                window(56f, 8f, 20f, 14f)
                box(80f, 8f, 16f, 14f, Wood, 1f)
                box(24f, 30f, 72f, 12f, Wood, 1f); line(48f, 30f, 48f, 42f); line(72f, 30f, 72f, 42f)
                box(24f, 27f, 72f, 3f, Paper, 1f)
                box(60f, 21f, 12f, 6f, Metal, 1f); line(64f, 21f, 64f, 18f)
                box(30f, 21f, 9f, 6f, Clay, 1f)
            }
            RoomType.DINING_ROOM -> {
                // A table with two chairs, a vase and a hanging lamp.
                line(50f, 0f, 50f, 9f); box(43f, 9f, 14f, 5f, Sun, 2f)
                box(24f, 30f, 52f, 4f, Wood); line(28f, 34f, 28f, 46f); line(72f, 34f, 72f, 46f)
                box(46f, 25f, 8f, 5f, Sky, 1f); circle(50f, 23f, 2.5f, Clay)
                box(10f, 22f, 4f, 20f, Wood); box(10f, 36f, 12f, 3f, Wood); line(12f, 39f, 12f, 46f); line(20f, 39f, 20f, 46f)
                box(86f, 22f, 4f, 20f, Wood); box(78f, 36f, 12f, 3f, Wood); line(80f, 39f, 80f, 46f); line(88f, 39f, 88f, 46f)
                box(6f, 6f, 16f, 12f, Paper); box(9f, 9f, 10f, 6f, Green)
            }
            RoomType.OFFICE -> {
                // A desk with a screen, a chair, a lamp and a bookcase.
                box(74f, 6f, 22f, 36f, Wood, 1f)
                listOf(14f, 22f, 30f).forEach { y -> line(74f, y, 96f, y) }
                box(77f, 8f, 4f, 6f, Clay, 0.5f); box(82f, 8f, 4f, 6f, Green, 0.5f); box(87f, 8f, 4f, 6f, Sky, 0.5f)
                box(78f, 16f, 5f, 6f, Sun, 0.5f); box(85f, 16f, 5f, 6f, Clay, 0.5f)
                box(14f, 31f, 52f, 4f, Wood); line(18f, 35f, 18f, 46f); box(46f, 35f, 16f, 10f, Wood, 1f); line(46f, 40f, 62f, 40f)
                box(26f, 17f, 20f, 12f, Metal, 1f); line(36f, 29f, 36f, 31f); box(31f, 30f, 10f, 1.5f, Ink, 0.5f)
                line(58f, 31f, 58f, 22f); line(58f, 22f, 52f, 20f); box(48f, 18f, 6f, 3f, Sun, 1f)
                box(4f, 26f, 8f, 12f, Green, 3f); line(8f, 38f, 8f, 46f)
            }
            RoomType.LAUNDRY -> {
                // Washer and dryer, a shelf with detergent and a basket of clothes.
                line(6f, 16f, 62f, 16f)
                box(10f, 8f, 6f, 8f, Sky, 1f); box(18f, 10f, 6f, 6f, Clay, 1f); box(26f, 9f, 5f, 7f, Green, 1f)
                box(8f, 22f, 26f, 22f, Paper, 2f); circle(21f, 34f, 7f, Sky); line(10f, 26f, 18f, 26f); circle(30f, 26f, 1.2f, Clay)
                box(36f, 22f, 26f, 22f, Paper, 2f); circle(49f, 34f, 7f, Soft); line(38f, 26f, 46f, 26f); circle(58f, 26f, 1.2f, Clay)
                box(72f, 34f, 20f, 12f, Wood, 2f); box(74f, 30f, 16f, 5f, Clay, 2f); box(78f, 28f, 10f, 4f, Sky, 2f)
            }
            RoomType.GARAGE -> {
                // A garage door with panels and a car parked in front.
                box(8f, 6f, 84f, 36f, Metal, 1f)
                listOf(15f, 24f, 33f).forEach { y -> line(8f, y, 92f, y) }
                box(24f, 32f, 52f, 9f, Clay, 3f)
                box(34f, 24f, 32f, 9f, Clay, 3f); box(37f, 26f, 12f, 6f, Sky, 1f); box(51f, 26f, 12f, 6f, Sky, 1f)
                circle(34f, 42f, 4.5f, Ink.copy(alpha = 0.75f)); circle(66f, 42f, 4.5f, Ink.copy(alpha = 0.75f))
                circle(34f, 42f, 1.5f, Metal); circle(66f, 42f, 1.5f, Metal)
            }
            RoomType.GARDEN -> {
                // A sun, a tree, a picket fence and flowers in the grass.
                circle(84f, 12f, 6f, Sun)
                box(21f, 22f, 5f, 22f, Wood, 1f)
                circle(23.5f, 16f, 11f, Green); circle(15f, 22f, 7f, Green); circle(32f, 22f, 7f, Green)
                line(44f, 36f, 98f, 36f); line(44f, 41f, 98f, 41f)
                listOf(46f, 54f, 62f, 70f, 78f, 86f, 94f).forEach { x -> box(x, 31f, 4f, 14f, Paper, 1f) }
                listOf(10f to 52f, 34f to 55f, 58f to 51f, 80f to 54f).forEachIndexed { i, (x, y) ->
                    line(x, y, x, y + 5f); circle(x, y, 2.4f, if (i % 2 == 0) Clay else Sun)
                }
            }
            RoomType.BALCONY -> {
                // Houses far away, a railing, a pot plant and a little table with a chair.
                box(8f, 10f, 14f, 22f, Soft, 0.5f); box(24f, 4f, 16f, 28f, Paper, 0.5f); box(42f, 14f, 12f, 18f, Soft, 0.5f)
                box(4f, 30f, 92f, 3f, Paper, 1f); box(4f, 42f, 92f, 2f, Paper, 1f)
                (0..9).forEach { i -> line(8f + i * 9.5f, 33f, 8f + i * 9.5f, 42f) }
                plant(16f, 54f)
                box(54f, 48f, 24f, 3f, Wood); line(58f, 51f, 58f, 57f); line(74f, 51f, 74f, 57f)
                box(82f, 40f, 3f, 12f, Wood); box(82f, 48f, 10f, 3f, Wood); line(90f, 51f, 90f, 57f)
            }
            RoomType.HALLWAY -> {
                // The front door, a coat rack, a shoe rack and a mirror over a small table.
                box(38f, 6f, 24f, 36f, Wood, 1f); box(42f, 10f, 16f, 10f, Sky, 1f); circle(58f, 28f, 1.5f, Sun)
                line(6f, 12f, 30f, 12f)
                box(8f, 13f, 7f, 16f, Clay, 2f); box(18f, 13f, 7f, 20f, Green, 2f)
                box(6f, 38f, 28f, 4f, Wood, 1f); box(9f, 34f, 7f, 4f, Soft, 2f); box(19f, 34f, 7f, 4f, Clay, 2f)
                oval(72f, 8f, 18f, 22f, Sky)
                box(68f, 32f, 26f, 3f, Wood); line(72f, 35f, 72f, 44f); line(90f, 35f, 90f, 44f)
                box(78f, 26f, 6f, 6f, Sun, 1f)
            }
            RoomType.OTHER -> {
                // A window, a lamp and a few boxes.
                window(14f, 8f, 20f, 16f)
                box(52f, 26f, 22f, 16f, Wood, 1f); line(52f, 31f, 74f, 31f); box(60f, 15f, 18f, 11f, Wood, 1f); line(60f, 20f, 78f, 20f)
                line(90f, 20f, 90f, 42f); box(83f, 12f, 14f, 8f, Sun, 2f)
                plant(24f, 42f)
            }
        }
    }

    /** A bed seen from the front: headboard, pillows, blanket and a bedside table with a lamp. */
    private fun bed(headboardColor: Color, small: Boolean) {
        val x = if (small) 8f else 14f
        val w = if (small) 56f else 62f
        box(x, 16f, w, 20f, headboardColor, 3f)
        box(x - 2f, 30f, w + 4f, 12f, Soft, 2f)
        oval(x + 6f, 24f, 20f, 9f, Paper); oval(x + w - 26f, 24f, 20f, 9f, Paper)
        box(x - 2f, 34f, w + 4f, 8f, Green, 2f); line(x + 10f, 34f, x + 10f, 42f)
        line(x, 42f, x, 46f); line(x + w, 42f, x + w, 46f)
        val table = x + w + 8f
        box(table, 32f, 10f, 10f, Wood, 1f); line(table + 5f, 32f, table + 5f, 42f)
        line(table + 5f, 32f, table + 5f, 26f); box(table + 1f, 20f, 8f, 6f, Sun, 2f)
        if (!small) window(86f, 4f, 10f, 12f)
    }
}
