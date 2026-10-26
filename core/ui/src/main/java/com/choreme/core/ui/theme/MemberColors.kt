package com.choreme.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Each member gets one of these, so chores and events can be told apart at a glance. All are
 * dark enough for a white initial on top (4.5:1), and differ in lightness, not only in hue.
 */
val MemberColors = listOf(
    Color(0xFF1F5C5A),
    Color(0xFFB45A14),
    Color(0xFF6B4E96),
    Color(0xFFA8452D),
    Color(0xFF2F5F95),
    Color(0xFF5F6B1E),
    Color(0xFFA33E63),
    Color(0xFF6E513D),
)

fun memberColor(index: Int): Color = MemberColors[Math.floorMod(index, MemberColors.size)]
