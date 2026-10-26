package com.choreme.core.model

import java.time.LocalTime

/** "Good morning / afternoon / evening" on the Chores tab. */
enum class DayPart { MORNING, AFTERNOON, EVENING }

fun dayPart(time: LocalTime): DayPart = when {
    time.hour in 5..11 -> DayPart.MORNING
    time.hour in 12..17 -> DayPart.AFTERNOON
    else -> DayPart.EVENING
}
