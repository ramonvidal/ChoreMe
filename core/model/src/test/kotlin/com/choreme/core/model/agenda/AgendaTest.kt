package com.choreme.core.model.agenda

import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class AgendaTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 10, 24)

    private fun timed(title: String, date: LocalDate, from: String, to: String, endDate: LocalDate = date) = CalendarEvent(
        id = title.hashCode().toLong(), calendarId = 1, title = title,
        start = date.atTime(LocalTime.parse(from)).atZone(madrid).toInstant(),
        end = endDate.atTime(LocalTime.parse(to)).atZone(madrid).toInstant(),
        allDay = false, location = null, color = 0,
    )

    private fun allDay(title: String, first: LocalDate, days: Long = 1) = CalendarEvent(
        id = title.hashCode().toLong(), calendarId = 1, title = title,
        start = first.atStartOfDay(ZoneOffset.UTC).toInstant(),
        end = first.plusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant(),
        allDay = true, location = null, color = 0,
    )

    private fun chore(title: String, due: LocalDate, status: ChoreStatus = ChoreStatus.OPEN, repeats: Boolean = false) =
        Chore(id = title, title = title, dueDate = due, status = status, recurrence = if (repeats) Recurrence.EveryDays(7) else Recurrence.Once)

    @Test
    fun `events show on every day they cover`() {
        assertEquals(listOf(today, today.plusDays(1)), allDay("Trip", today, days = 2).days(madrid))
        assertEquals(listOf(today), timed("Dinner", today, "20:00", "23:00").days(madrid))
        assertEquals(listOf(today, today.plusDays(1)), timed("Party", today, "22:00", "02:00", today.plusDays(1)).days(madrid))
        // Ending at midnight sharp stays on the same day.
        assertEquals(listOf(today), timed("Movie", today, "22:00", "00:00", today.plusDays(1)).days(madrid))
    }

    @Test
    fun `agenda mixes events and chores day by day, skipping empty days`() {
        val agenda = buildAgenda(
            events = listOf(
                timed("Football", today.plusDays(2), "18:00", "19:30"),
                allDay("Grandma's birthday", today.plusDays(2)),
                timed("Long ago", today.minusDays(5), "10:00", "11:00"),
            ),
            chores = listOf(
                chore("Late trash", today.minusDays(2), repeats = true),
                // A one-off "needs doing now" stays in the chores tab.
                chore("Dishes", today),
                chore("Leak", today.minusDays(1)),
                chore("Vacuum", today.plusDays(2)),
                chore("Old shelf", today.minusDays(1), ChoreStatus.DONE),
                chore("Far away", today.plusDays(30)),
            ),
            from = today,
            days = 14,
            zone = madrid,
        )
        assertEquals(listOf(today, today.plusDays(2)), agenda.map { it.date })
        assertEquals(listOf("Late trash"), agenda[0].chores.map { it.title })
        assertEquals(listOf("Grandma's birthday", "Football"), agenda[1].events.map { it.title })
        assertEquals(listOf("Vacuum"), agenda[1].chores.map { it.title })
    }

    @Test
    fun `today is always shown, even when empty`() {
        assertEquals(listOf(AgendaDay(today, emptyList(), emptyList())), buildAgenda(emptyList(), emptyList(), today, 14, madrid))
    }

    @Test
    fun `family calendars are suggested by name`() {
        val calendars = listOf(
            FamilyCalendar(1, "Ramon", "ramon@gmail.com", 0, true),
            FamilyCalendar(2, "Família Vidal", "ramon@gmail.com", 0, true),
            FamilyCalendar(3, "Holidays in Spain", "ramon@gmail.com", 0, false),
        )
        assertEquals(setOf(2L), suggestFamilyCalendars(calendars))
    }

    @Test
    fun `new events become the right instants`() {
        val dinner = NewEvent("Dinner", today, LocalTime.of(20, 30))
        assertEquals(Instant.parse("2026-10-24T18:30:00Z"), dinner.start(madrid))
        assertEquals(Instant.parse("2026-10-24T19:30:00Z"), dinner.end(madrid))
        assertEquals(30, dinner.reminderMinutes(madrid))

        val birthday = NewEvent("Birthday", today, startTime = null)
        assertEquals(Instant.parse("2026-10-24T00:00:00Z"), birthday.start(madrid))
        assertEquals(Instant.parse("2026-10-25T00:00:00Z"), birthday.end(madrid))
    }

    @Test
    fun `all-day alerts land the evening before in local time`() {
        // Madrid is UTC+2 on 23 October (summer time): 19:00 local is 17:00 UTC, 7 hours before midnight UTC.
        assertEquals(7 * 60, allDayReminderMinutes(today, madrid, LocalTime.of(19, 0)))
        // In winter (UTC+1) it is 18:00 UTC, 6 hours before.
        assertEquals(6 * 60, allDayReminderMinutes(LocalDate.of(2026, 12, 10), madrid, LocalTime.of(19, 0)))
        assertEquals(null, NewEvent("x", today, null, alert = false).reminderMinutes(madrid))
    }
}

class CalendarForChoresTest {
    private val family = FamilyCalendar(1, "Family", "a@b", 0, isWritable = true)
    private val mine = FamilyCalendar(2, "Ramon", "a@b", 0, isWritable = true)
    private val holidays = FamilyCalendar(3, "Holidays", "a@b", 0, isWritable = false)

    @Test
    fun `the single writable calendar shown is used, otherwise ask`() {
        assertEquals(family, calendarForChores(listOf(family, mine, holidays), setOf(1L, 3L)))
        assertEquals(null, calendarForChores(listOf(family, mine), setOf(1L, 2L)))
        assertEquals(null, calendarForChores(listOf(family), null))
    }
}
