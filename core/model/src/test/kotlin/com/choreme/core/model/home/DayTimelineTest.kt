package com.choreme.core.model.home

import com.choreme.core.model.agenda.CalendarEvent
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.TodayBoard
import com.choreme.core.model.weather.Advice
import com.choreme.core.model.weather.Moment
import com.choreme.core.model.weather.PlanEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class DayTimelineTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val monday = LocalDate.of(2026, 11, 16)

    private fun event(title: String, startHour: Int, endHour: Int, place: String? = null) = CalendarEvent(
        id = title.hashCode().toLong(), calendarId = 1, title = title,
        start = monday.atTime(startHour, 0).atZone(zone).toInstant(),
        end = monday.atTime(endHour, 0).atZone(zone).toInstant(),
        allDay = false, location = place, color = 0,
    )

    private val holiday = CalendarEvent(
        2, 1, "Grandma visits", monday.atStartOfDay().toInstant(ZoneOffset.UTC), monday.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
        allDay = true, location = null, color = 0,
    )

    private fun trip(title: String, hour: Int) = Moment(
        PlanEntry("t$hour", "ramon", title, setOf(DayOfWeek.MONDAY), LocalTime.of(hour, 0)),
        monday.atTime(hour, 0), monday.atTime(hour, 40),
        Advice(emptySet(), emptyList(), 0, 0.0, 10.0, 12.0, 5.0, 0),
    )

    private fun chore(id: String) = Chore(id, id, dueDate = monday)
    private val board = TodayBoard(emptyList(), listOf(chore("a"), chore("b")), emptyList(), emptyList(), mine = listOf(chore("c")))

    @Test
    fun `events and trips come in the order of the day`() {
        val timeline = dayTimeline(
            monday, listOf(event("Dentist", 15, 16, "Dr. Weber"), event("Standup", 9, 10)), listOf(trip("Work", 8), trip("Gym", 18)),
            mapOf("ramon" to "Ramon"), board, groceriesToBuy = 5, zone = zone,
        )
        assertEquals(listOf(8, 9, 15, 18), timeline.entries.map { it.time.hour })
        assertEquals("Dr. Weber", (timeline.entries[2] as EventEntry).place)
        assertEquals("Ramon", (timeline.entries[0] as TripEntry).who)
    }

    @Test
    fun `all day events are apart and the counts add up`() {
        val timeline = dayTimeline(monday, listOf(holiday, event("Dentist", 15, 16)), emptyList(), emptyMap(), board, 5, zone)
        assertEquals(listOf("Grandma visits"), timeline.allDay)
        assertEquals(2, timeline.eventCount)
        assertEquals(3, timeline.choreCount)
        assertEquals(5, timeline.groceriesToBuy)
        assertTrue(!timeline.isEmpty)
    }

    @Test
    fun `an event of another day is left out, one that began earlier shows at midnight`() {
        val tomorrow = event("Tomorrow", 9, 10).copy(
            start = monday.plusDays(1).atTime(9, 0).atZone(zone).toInstant(), end = monday.plusDays(1).atTime(10, 0).atZone(zone).toInstant(),
        )
        val overnight = event("Night shift", 22, 23).copy(
            start = monday.minusDays(1).atTime(22, 0).atZone(zone).toInstant(), end = monday.atTime(6, 0).atZone(zone).toInstant(),
        )
        val timeline = dayTimeline(monday, listOf(tomorrow, overnight), emptyList(), emptyMap(), board, 0, zone)
        assertEquals(listOf("Night shift"), timeline.entries.map { (it as EventEntry).title })
        assertEquals(LocalTime.MIDNIGHT, timeline.entries.single().time)
    }

    @Test
    fun `an empty day is empty`() {
        assertTrue(dayTimeline(monday, emptyList(), emptyList(), emptyMap(), board, 0, zone).isEmpty)
    }
}
