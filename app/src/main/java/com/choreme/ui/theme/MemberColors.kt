package com.choreme.ui.theme

import androidx.compose.ui.graphics.Color

/** Each member gets one of these, so chores and events can be told apart at a glance. */
val MemberColors = listOf(
    Color(0xFF2E7D6B),
    Color(0xFFF2A541),
    Color(0xFF5C6BC0),
    Color(0xFFE5737B),
    Color(0xFF8D6E63),
    Color(0xFF26A69A),
    Color(0xFFAB47BC),
    Color(0xFF7CB342),
)

fun memberColor(index: Int): Color = MemberColors[Math.floorMod(index, MemberColors.size)]
