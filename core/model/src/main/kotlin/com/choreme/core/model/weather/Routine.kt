package com.choreme.core.model.weather

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** One arrival of this phone at a saved place. The history never leaves the phone. */
data class Visit(val placeId: String, val arrivedAt: LocalDateTime)

/** A habit the app noticed: this person goes to [placeId] on [days], arriving around [arrival]. */
data class RoutineSuggestion(
    val placeId: String,
    val days: Set<DayOfWeek>,
    /** When the trip would start for the plan: the usual arrival minus the trip's length. */
    val start: LocalTime,
    /** How many arrivals it rests on. */
    val times: Int,
) {
    /** A stable name for "not now", so a dismissed habit does not come back. */
    val key: String get() = "$placeId|${days.sorted().joinToString(",") { it.name }}"
}

private const val WEEKS_LOOKED_AT = 8
private const val MIN_TIMES = 3
/** Arrivals on one weekday must fall within this many minutes of their middle one to count as a habit. */
private const val SPREAD_MINUTES = 45L
/** Weekdays whose usual times are this close are one habit ("Tue and Thu around 18:30"). */
private const val MERGE_MINUTES = 30L

/**
 * Habits in [visits] (the last eight weeks): the same place on the same weekday at least three different days, at
 * about the same time. Weekdays with about the same time join into one habit. A habit the person already has in the
 * plan (same place, a shared day, a start within an hour) and places in [ignore] (Home, say) are left out.
 */
fun learnRoutine(
    visits: List<Visit>,
    plans: List<PlanEntry>,
    memberId: String,
    ignore: Set<String>,
    today: LocalDate,
    tripMinutes: Int = PlanEntry.DEFAULT_MINUTES,
): List<RoutineSuggestion> {
    val since = today.minusWeeks(WEEKS_LOOKED_AT.toLong())
    val recent = visits.filter { it.placeId !in ignore && !it.arrivedAt.toLocalDate().isBefore(since) && !it.arrivedAt.toLocalDate().isAfter(today) }
    // One arrival per place and day: the first of the day.
    val perDay = recent.groupBy { it.placeId to it.arrivedAt.toLocalDate() }.map { (_, v) -> v.minBy { it.arrivedAt } }

    val habits = mutableListOf<Triple<String, DayOfWeek, Pair<LocalTime, Int>>>() // place, weekday, (usual arrival, times)
    perDay.groupBy { it.placeId to it.arrivedAt.dayOfWeek }.forEach { (key, group) ->
        val minutes = group.map { it.arrivedAt.toLocalTime().toSecondOfDay() / 60 }.sorted()
        val middle = minutes[minutes.size / 2]
        val steady = minutes.filter { kotlin.math.abs(it - middle) <= SPREAD_MINUTES }
        if (steady.size >= MIN_TIMES) habits += Triple(key.first, key.second, LocalTime.ofSecondOfDay(middle * 60L) to steady.size)
    }

    return habits.groupBy { it.first }.flatMap { (placeId, perWeekday) ->
        // Join weekdays whose usual times are close.
        val groups = mutableListOf<MutableList<Triple<String, DayOfWeek, Pair<LocalTime, Int>>>>()
        perWeekday.sortedBy { it.third.first }.forEach { habit ->
            val group = groups.lastOrNull()?.takeIf { Duration.between(it.first().third.first, habit.third.first).toMinutes() <= MERGE_MINUTES }
            if (group != null) group += habit else groups += mutableListOf(habit)
        }
        groups.map { group ->
            val arrival = group[group.size / 2].third.first
            RoutineSuggestion(
                placeId = placeId,
                days = group.map { it.second }.toSet(),
                start = arrival.minusMinutes(tripMinutes.toLong()).let { it.withMinute(it.minute / 5 * 5) },
                times = group.sumOf { it.third.second },
            )
        }
    }.filterNot { suggestion -> plans.any { it.memberId == memberId && covers(it, suggestion) } }
        .sortedWith(compareByDescending<RoutineSuggestion> { it.times }.thenBy { it.start })
}

private fun covers(entry: PlanEntry, suggestion: RoutineSuggestion) =
    entry.placeId == suggestion.placeId && entry.days.any { it in suggestion.days } &&
        kotlin.math.abs(Duration.between(entry.start, suggestion.start).toMinutes()) <= 60
