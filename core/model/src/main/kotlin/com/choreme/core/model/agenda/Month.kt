package com.choreme.core.model.agenda

import com.choreme.core.model.chore.Chore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * The days shown for a month: whole weeks from Monday, so the grid starts with the end of
 * the previous month and ends with the start of the next. Always 6 weeks, so the grid
 * doesn't jump in height between months.
 */
fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): List<List<LocalDate>> {
    val start = month.atDay(1).with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    return (0 until WEEKS).map { week -> (0 until 7).map { day -> start.plusDays(week * 7L + day) } }
}

/** First and last day (exclusive) the grid covers, for loading events. */
fun monthGridRange(month: YearMonth, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): Pair<LocalDate, LocalDate> {
    val grid = monthGrid(month, firstDayOfWeek)
    return grid.first().first() to grid.last().last().plusDays(1)
}

private const val WEEKS = 6

/**
 * Everything on each day of the month grid. Late chores are shown on [today], where they
 * still need doing, rather than on the day they were due.
 */
fun buildMonth(
    events: List<CalendarEvent>,
    chores: List<Chore>,
    month: YearMonth,
    today: LocalDate,
    zone: ZoneId,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
): Map<LocalDate, AgendaDay> {
    val (from, until) = monthGridRange(month, firstDayOfWeek)
    val adjusted = chores.map { if (it.isActive && it.dueDate < today) it.copy(dueDate = today) else it }
    val days = ChronoUnit.DAYS.between(from, until).toInt()
    return buildAgenda(events, adjusted.filter { it.dueDate >= from }, from, days, zone).associateBy { it.date }
}
