package com.choreme.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * "Nothing to do": a small sunny house, a title and a line, centred, instead of a lone grey
 * sentence. [action] is an optional button under it.
 */
@Composable
fun EmptyState(title: String, body: String? = null, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SunnyHouse(Modifier.size(width = 120.dp, height = 84.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (body != null) {
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        action?.invoke()
    }
}

/** The garden's house, small and calm: fixed colours, so it looks the same in light and dark. */
@Composable
private fun SunnyHouse(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val ground = h * 0.86f
        drawCircle(HouseSun, radius = h * 0.13f, center = Offset(w * 0.82f, h * 0.2f))
        drawRoundRect(HouseGrass, Offset(w * 0.08f, ground), Size(w * 0.84f, h * 0.08f), CornerRadius(h * 0.04f))
        val houseW = h * 0.56f
        val houseH = h * 0.36f
        val left = w * 0.5f - houseW / 2
        val top = ground - houseH
        drawRect(Color.White, Offset(left, top), Size(houseW, houseH))
        drawRect(HouseLine, Offset(left, top), Size(houseW, houseH), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        drawPath(
            Path().apply {
                moveTo(left - houseW * 0.1f, top)
                lineTo(left + houseW / 2, top - houseH * 0.7f)
                lineTo(left + houseW * 1.1f, top)
                close()
            },
            HouseRoof,
        )
        val door = Size(houseW * 0.22f, houseH * 0.5f)
        drawRoundRect(HouseGreen, Offset(left + houseW / 2 - door.width / 2, ground - door.height), door, CornerRadius(2.dp.toPx()))
        val window = Size(houseW * 0.2f, houseH * 0.28f)
        listOf(left + houseW * 0.12f, left + houseW * 0.68f).forEach { x -> drawRect(HouseLamp, Offset(x, top + houseH * 0.2f), window) }
        // Two flowers by the door.
        listOf(left - h * 0.1f, left + houseW + h * 0.1f).forEach { x ->
            drawLine(HouseGreen, Offset(x, ground), Offset(x, ground - h * 0.12f), strokeWidth = 1.5.dp.toPx())
            drawCircle(HouseFlower, radius = h * 0.04f, center = Offset(x, ground - h * 0.13f))
        }
    }
}

private val HouseSun = Color(0xFFF2C14E)
private val HouseGrass = Color(0xFFCFE0D4)
private val HouseLine = Color(0xFF8E8E8A)
private val HouseRoof = Color(0xFF1C1C1C)
private val HouseGreen = Color(0xFF2E6B52)
private val HouseLamp = Color(0xFFF6D77A)
private val HouseFlower = Color(0xFFE38B7A)
