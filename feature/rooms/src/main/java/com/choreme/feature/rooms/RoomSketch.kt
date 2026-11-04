package com.choreme.feature.rooms

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
        with(Sketcher(this, unit, origin)) {
            floorAndWall(type)
            furniture(type)
        }
    }
}

private const val GRID_W = 100f
private const val GRID_H = 60f

private val Paper = Color(0xFFFAF3E3)
private val Ink = Color(0xFF5E5A52)
private val Wash = Color(0xFFE8DCC0)
private val Green = Color(0xFF8DB38F)
private val Sky = Color(0xFFBBD9E8)

/** Draws on the 100 x 60 grid: every call takes grid coordinates. */
private class Sketcher(private val scope: DrawScope, private val unit: Float, private val origin: Offset) {
    private fun p(x: Float, y: Float) = Offset(origin.x + x * unit, origin.y + y * unit)
    private val line get() = Stroke(width = (unit * 0.9f).coerceAtLeast(1.5f), cap = StrokeCap.Round)

    fun box(x: Float, y: Float, w: Float, h: Float, fill: Color = Wash, radius: Float = 1.2f) {
        val r = CornerRadius(radius * unit)
        scope.drawRoundRect(fill, p(x, y), Size(w * unit, h * unit), r)
        scope.drawRoundRect(Ink, p(x, y), Size(w * unit, h * unit), r, style = line)
    }

    fun stroke(x1: Float, y1: Float, x2: Float, y2: Float, dashed: Boolean = false) {
        scope.drawLine(
            Ink, p(x1, y1), p(x2, y2), strokeWidth = line.width, cap = StrokeCap.Round,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(unit * 2f, unit * 2f)) else null,
        )
    }

    fun circle(x: Float, y: Float, r: Float, fill: Color = Wash) {
        scope.drawCircle(fill, r * unit, p(x, y))
        scope.drawCircle(Ink, r * unit, p(x, y), style = line)
    }

    fun window(x: Float, y: Float, w: Float = 18f, h: Float = 14f) {
        box(x, y, w, h, Sky)
        stroke(x + w / 2, y, x + w / 2, y + h)
        stroke(x, y + h / 2, x + w, y + h / 2)
    }

    fun floorAndWall(type: RoomType) {
        val outdoors = type == RoomType.GARDEN || type == RoomType.BALCONY
        if (outdoors) {
            scope.drawRect(Sky.copy(alpha = 0.45f), p(0f, 0f), Size(GRID_W * unit, 38f * unit))
            scope.drawRect(Green.copy(alpha = 0.5f), p(0f, 38f), Size(GRID_W * unit, 22f * unit))
        } else {
            scope.drawRect(Wash.copy(alpha = 0.55f), p(0f, 38f), Size(GRID_W * unit, 22f * unit))
        }
        stroke(0f, 38f, GRID_W, 38f)
    }

    fun furniture(type: RoomType) {
        when (type) {
            RoomType.LIVING_ROOM -> {
                window(70f, 8f)
                box(12f, 28f, 46f, 16f)
                box(10f, 22f, 8f, 22f)
                box(52f, 22f, 8f, 22f)
                box(14f, 24f, 38f, 10f, Paper)
                box(66f, 36f, 22f, 4f)
                stroke(70f, 40f, 70f, 48f)
                stroke(84f, 40f, 84f, 48f)
            }
            RoomType.BEDROOM, RoomType.KIDS_ROOM -> {
                window(10f, 8f)
                box(30f, 18f, 54f, 8f)
                box(24f, 28f, 66f, 18f)
                box(28f, 24f, 22f, 7f, Paper)
                box(54f, 32f, 34f, 12f, Green.copy(alpha = 0.6f))
                box(90f, 34f, 8f, 12f)
                if (type == RoomType.KIDS_ROOM) {
                    circle(14f, 46f, 4f, Green)
                    box(6f, 30f, 8f, 8f, Sky)
                }
            }
            RoomType.BATHROOM -> {
                window(72f, 8f, 16f, 12f)
                box(10f, 28f, 50f, 18f, Paper, radius = 5f)
                box(14f, 31f, 42f, 11f, Sky.copy(alpha = 0.6f), radius = 4f)
                stroke(10f, 24f, 10f, 28f)
                circle(80f, 38f, 5f, Paper)
                box(77f, 28f, 6f, 10f, Paper)
                box(66f, 12f, 10f, 12f, Sky)
            }
            RoomType.KITCHEN -> {
                window(40f, 8f)
                box(6f, 28f, 88f, 18f)
                stroke(6f, 33f, 94f, 33f)
                box(10f, 12f, 22f, 12f)
                box(66f, 12f, 28f, 12f)
                circle(76f, 29f, 2f, Paper)
                circle(86f, 29f, 2f, Paper)
                box(18f, 25f, 14f, 3f, Sky)
            }
            RoomType.DINING_ROOM -> {
                window(70f, 8f)
                box(26f, 30f, 48f, 5f)
                stroke(32f, 35f, 32f, 50f)
                stroke(68f, 35f, 68f, 50f)
                box(18f, 34f, 7f, 14f)
                box(75f, 34f, 7f, 14f)
                circle(50f, 29f, 3f, Green)
                circle(10f, 14f, 4f, Paper)
            }
            RoomType.OFFICE -> {
                window(10f, 8f)
                box(30f, 30f, 46f, 4f)
                stroke(34f, 34f, 34f, 48f)
                stroke(72f, 34f, 72f, 48f)
                box(46f, 18f, 18f, 12f, Sky)
                stroke(55f, 30f, 55f, 31f)
                box(82f, 18f, 12f, 28f)
                stroke(82f, 28f, 94f, 28f)
                stroke(82f, 37f, 94f, 37f)
                circle(40f, 44f, 4f, Green)
            }
            RoomType.LAUNDRY -> {
                window(70f, 8f, 16f, 12f)
                box(12f, 22f, 26f, 26f)
                circle(25f, 36f, 8f, Sky)
                box(44f, 22f, 26f, 26f)
                circle(57f, 36f, 8f, Paper)
                box(76f, 36f, 14f, 12f, Green.copy(alpha = 0.6f))
                stroke(12f, 10f, 70f, 10f, dashed = true)
            }
            RoomType.GARAGE -> {
                box(8f, 10f, 84f, 28f, Paper)
                (0..3).forEach { i -> stroke(8f, 16f + i * 6f, 92f, 16f + i * 6f) }
                box(26f, 30f, 48f, 12f, Sky)
                box(34f, 24f, 32f, 8f, Paper)
                circle(36f, 44f, 4f, Ink.copy(alpha = 0.5f))
                circle(64f, 44f, 4f, Ink.copy(alpha = 0.5f))
            }
            RoomType.GARDEN -> {
                circle(80f, 12f, 6f, Color(0xFFF2C14E))
                stroke(22f, 38f, 22f, 20f)
                circle(22f, 16f, 10f, Green)
                circle(14f, 22f, 7f, Green)
                circle(30f, 22f, 7f, Green)
                stroke(60f, 50f, 60f, 44f)
                circle(60f, 42f, 3f, Color(0xFFE38B7A))
                stroke(72f, 50f, 72f, 43f)
                circle(72f, 41f, 3f, Color(0xFFF2C14E))
                stroke(46f, 44f, 100f, 44f, dashed = true)
            }
            RoomType.BALCONY -> {
                box(6f, 22f, 88f, 4f)
                (0..8).forEach { i -> stroke(12f + i * 10f, 26f, 12f + i * 10f, 40f) }
                box(14f, 30f, 14f, 10f, Wash)
                circle(21f, 26f, 5f, Green)
                box(64f, 34f, 22f, 6f)
                stroke(66f, 40f, 66f, 48f)
                stroke(84f, 40f, 84f, 48f)
            }
            RoomType.HALLWAY -> {
                box(40f, 10f, 20f, 36f, Paper)
                circle(55f, 30f, 1.5f)
                box(8f, 34f, 24f, 4f)
                stroke(12f, 38f, 12f, 46f)
                stroke(28f, 38f, 28f, 46f)
                stroke(14f, 14f, 14f, 22f)
                stroke(20f, 14f, 20f, 22f)
                stroke(10f, 14f, 26f, 14f)
                box(72f, 18f, 16f, 18f, Sky)
                circle(80f, 26f, 4f, Paper)
            }
            RoomType.OTHER -> {
                window(14f, 8f)
                box(46f, 22f, 36f, 22f)
                box(52f, 14f, 24f, 8f, Paper)
                circle(20f, 44f, 4f, Green)
            }
        }
    }
}
