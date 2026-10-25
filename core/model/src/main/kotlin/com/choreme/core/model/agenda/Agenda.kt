package com.choreme.core.model.agenda

import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** A calendar synced on this phone, e.g. the family's shared Google Calendar. */
data class FamilyCalendar(
    val id: Long,
    val name: String,
    val account: String,
    val color: Int,
    val isWritable: Boolean,
)

data class CalendarEvent(
    val id: Long,
    val calendarId: Long,
    val title: String,
    val start: Instant,
    val end: Instant,
    /** All-day events are stored from UTC midnight to UTC midnight, as Android's calendar does. */
    val allDay: Boolean,
    val location: String?,
    val color: Int,
)

data class AgendaDay(val date: LocalDate, val events: List<CalendarEvent>, val chores: List<Chore>)

/** The days an event shows up on: every day it covers, so a weekend trip is on Saturday and Sunday. */
fun CalendarEvent.days(zone: ZoneId): List<LocalDate> {
    val first: LocalDate
    val last: LocalDate
    if (allDay) {
        first = start.atZone(ZoneOffset.UTC).toLocalDate()
        last = end.atZone(ZoneOffset.UTC).toLocalDate().minusDays(1)
    } else {
        first = start.atZone(zone).toLocalDate()
        // An event ending exactly at midnight doesn't spill into the next day.
        last = end.minusNanos(1).atZone(zone).toLocalDate()
    }
    return generateSequence(first) { it.plusDays(1) }.takeWhile { it <= maxOf(first, last) }.toList()
}

/**
 * Calendar events and chores, day by day, from [from] for [days] days. Days with nothing
 * are left out, except [from] itself. Chores that are late show up on [from].
 */
fun buildAgenda(
    events: List<CalendarEvent>,
    chores: List<Chore>,
    from: LocalDate,
    days: Int,
    zone: ZoneId,
): List<AgendaDay> {
    val until = from.plusDays(days.toLong())
    val inWindow = { date: LocalDate -> date >= from && date < until }
    val eventsByDay = events
        .flatMap { event -> event.days(zone).filter(inWindow).map { it to event } }
        .groupBy({ it.first }, { it.second })
    val choresByDay = chores
        .filter { it.isActive || it.status == ChoreStatus.PENDING_APPROVAL }
        .map { maxOf(it.dueDate, from) to it }
        .filter { inWindow(it.first) }
        .groupBy({ it.first }, { it.second })

    return generateSequence(from) { it.plusDays(1) }.takeWhile { it < until }
        .map { date ->
            AgendaDay(
                date = date,
                events = eventsByDay[date].orEmpty().sortedWith(compareBy({ !it.allDay }, { it.start }, { it.title })),
                chores = choresByDay[date].orEmpty().sortedBy { it.title.lowercase() },
            )
        }
        .filter { it.date == from || it.events.isNotEmpty() || it.chores.isNotEmpty() }
        .toList()
}

/** Calendars that look like the family's, to preselect the first time the agenda opens. */
fun suggestFamilyCalendars(calendars: List<FamilyCalendar>): Set<Long> =
    calendars.filter { cal -> FAMILY_WORDS.any { cal.name.contains(it, ignoreCase = true) } }.map { it.id }.toSet()

// English, Spanish, Catalan, French, Portuguese, German, Italian.
private val FAMILY_WORDS = listOf("famil", "famíl", "casa", "home", "household")

/** What someone fills in to add an event. Times are local; [startTime] null means all day. */
data class NewEvent(
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime?,
    val duration: Duration = Duration.ofHours(1),
    /** Minutes before the start for timed events; for all-day events, an alert the evening before. */
    val alert: Boolean = true,
    val alertMinutesBefore: Int = 30,
) {
    val allDay get() = startTime == null

    fun start(zone: ZoneId): Instant =
        if (startTime == null) date.atStartOfDay(ZoneOffset.UTC).toInstant() else date.atTime(startTime).atZone(zone).toInstant()

    fun end(zone: ZoneId): Instant =
        if (startTime == null) date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() else start(zone).plus(duration)

    /** Minutes before the event's stored start time, as Android's calendar wants them; null for no alert. */
    fun reminderMinutes(zone: ZoneId): Int? = when {
        !alert -> null
        startTime != null -> alertMinutesBefore
        else -> allDayReminderMinutes(date, zone, ALL_DAY_ALERT_TIME)
    }

    companion object {
        val ALL_DAY_ALERT_TIME: LocalTime = LocalTime.of(19, 0)
    }
}

/**
 * All-day events start at UTC midnight, so an alert "the evening before at 19:00" has to be
 * counted in minutes from there, which depends on the time zone.
 */
fun allDayReminderMinutes(date: LocalDate, zone: ZoneId, at: LocalTime): Int {
    val eventStart = date.atStartOfDay(ZoneOffset.UTC).toInstant()
    val alertAt = date.minusDays(1).atTime(at).atZone(zone).toInstant()
    return Duration.between(alertAt, eventStart).toMinutes().toInt()
}

/**
 * Where to put a chore as a calendar event without asking: the one writable calendar the
 * agenda shows. Null when there are none or several, so the person picks in the calendar app.
 */
fun calendarForChores(calendars: List<FamilyCalendar>, shown: Set<Long>?): FamilyCalendar? =
    calendars.filter { it.isWritable && shown.orEmpty().contains(it.id) }.singleOrNull()
