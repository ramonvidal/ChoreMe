package com.choreme.core.model.home

import com.choreme.core.model.agenda.CalendarEvent
import com.choreme.core.model.agenda.days
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.TodayBoard
import com.choreme.core.model.weather.Moment
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** One thing of the day with a time: a calendar event or one of the family's regular trips with its weather. */
sealed interface TimelineEntry {
    val time: LocalTime
}

data class EventEntry(override val time: LocalTime, val title: String, val place: String?, val end: LocalTime? = null) : TimelineEntry

data class TripEntry(override val time: LocalTime, val moment: Moment, val who: String) : TimelineEntry

/**
 * The day at a glance for the Home tab: what is on the calendar (all-day ones apart), the trips, and how many chores and
 * grocery items are waiting. Chores without a time are counted, not listed, so they do not bury the hours.
 */
data class DayTimeline(
    val date: LocalDate,
    val allDay: List<String>,
    /** Events and trips with a time, in the order of the day. */
    val entries: List<TimelineEntry>,
    /** Chores due today or earlier that nobody has taken. */
    val choresToDo: Int,
    /** Chores this person said they would do. */
    val choresMine: Int,
    val groceriesToBuy: Int,
    /** Tasks of the rooms due today or earlier, counted apart from the house's own chores. */
    val roomTasks: Int = 0,
    /** The chores marked urgent that are waiting (the house's and the rooms'), to stand out on the Home tab. */
    val urgent: List<Chore> = emptyList(),
) {
    val eventCount: Int get() = allDay.size + entries.count { it is EventEntry }
    val choreCount: Int get() = choresToDo + choresMine
    val isEmpty: Boolean get() = allDay.isEmpty() && entries.isEmpty()
}

/**
 * Builds [DayTimeline] for [date] from what the phone knows. [names] gives member names for the trips; [board] is the
 * house's own chores and [roomBoard] the tasks of the rooms, counted apart. With [now] on the same day, what is
 * already over (events that ended, trips whose window passed) is left out; all-day events stay.
 */
fun dayTimeline(
    date: LocalDate,
    events: List<CalendarEvent>,
    moments: List<Moment>,
    names: Map<String, String>,
    board: TodayBoard,
    groceriesToBuy: Int,
    zone: ZoneId,
    roomBoard: TodayBoard? = null,
    now: LocalDateTime? = null,
): DayTimeline {
    val clock = now?.takeIf { it.toLocalDate() == date }
    val today = events.filter { date in it.days(zone) }
    val timedEvents = today.filter { !it.allDay }.map { event ->
        // An event that began on an earlier day shows at the start of this one.
        val startsToday = event.start.atZone(zone).toLocalDate() == date
        val endZoned = event.end.atZone(zone)
        // An event that runs past midnight is still going on for the rest of this day.
        val endsToday = endZoned.toLocalDate() == date
        EventEntry(
            if (startsToday) event.start.atZone(zone).toLocalTime() else LocalTime.MIDNIGHT,
            event.title,
            event.location?.takeIf { it.isNotBlank() },
            end = if (endsToday) endZoned.toLocalTime() else null,
        )
    }.filter { event -> clock == null || event.end == null || event.end.isAfter(clock.toLocalTime()) }
    val trips = moments.filter { clock == null || it.end.isAfter(clock) }.map { TripEntry(it.entry.start, it, names[it.entry.memberId].orEmpty()) }
    val entries = (timedEvents + trips).sortedWith(compareBy<TimelineEntry> { it.time }.thenBy { if (it is EventEntry) it.title.lowercase() else (it as TripEntry).moment.entry.title.lowercase() })
    return DayTimeline(
        date = date,
        allDay = today.filter { it.allDay }.map { it.title }.sorted(),
        entries = entries,
        choresToDo = board.toDo.size,
        choresMine = board.mine.size,
        groceriesToBuy = groceriesToBuy,
        roomTasks = roomBoard?.let { it.toDo.size + it.mine.size } ?: 0,
        urgent = (board.toDo + board.mine + roomBoard?.toDo.orEmpty() + roomBoard?.mine.orEmpty()).filter { it.urgent }.distinctBy { it.id },
    )
}
