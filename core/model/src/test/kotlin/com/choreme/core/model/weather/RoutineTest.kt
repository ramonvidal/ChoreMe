package com.choreme.core.model.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class RoutineTest {
    // Friday 2026-11-13; Tuesdays are 10, 3, Oct 27, 20 ...
    private val today = LocalDate.of(2026, 11, 13)
    private fun visit(place: String, date: LocalDate, h: Int, m: Int = 0) = Visit(place, date.atTime(h, m))
    private val tuesdays = listOf(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 3), LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 20))
    private val thursdays = tuesdays.map { it.plusDays(2) }

    @Test
    fun `the same place on the same weekday at the same time is a habit`() {
        val visits = tuesdays.take(3).map { visit("gym", it, 19, 10) }
        val habit = learnRoutine(visits, emptyList(), "me", emptySet(), today).single()
        assertEquals("gym", habit.placeId)
        assertEquals(setOf(DayOfWeek.TUESDAY), habit.days)
        // Arriving at 19:10, a 40-minute trip started at 18:30.
        assertEquals(LocalTime.of(18, 30), habit.start)
        assertEquals(3, habit.times)
    }

    @Test
    fun `two visits are not yet a habit and an erratic time is not either`() {
        assertTrue(learnRoutine(tuesdays.take(2).map { visit("gym", it, 19) }, emptyList(), "me", emptySet(), today).isEmpty())
        val erratic = listOf(visit("gym", tuesdays[0], 7), visit("gym", tuesdays[1], 13), visit("gym", tuesdays[2], 21))
        assertTrue(learnRoutine(erratic, emptyList(), "me", emptySet(), today).isEmpty())
    }

    @Test
    fun `weekdays with about the same time join into one habit`() {
        val visits = (tuesdays.take(3) + thursdays.take(3)).map { visit("gym", it, 19, 0) }
        val habit = learnRoutine(visits, emptyList(), "me", emptySet(), today).single()
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), habit.days)
        assertEquals(6, habit.times)
    }

    @Test
    fun `several arrivals in one day count once`() {
        val visits = listOf(visit("gym", tuesdays[0], 19), visit("gym", tuesdays[0], 20), visit("gym", tuesdays[1], 19), visit("gym", tuesdays[2], 19))
        assertEquals(3, learnRoutine(visits, emptyList(), "me", emptySet(), today).single().times)
    }

    @Test
    fun `old visits, ignored places and habits already in the plan are left out`() {
        val old = listOf(LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 11), LocalDate.of(2026, 8, 18)).map { visit("gym", it, 19) }
        assertTrue(learnRoutine(old, emptyList(), "me", emptySet(), today).isEmpty())
        val visits = tuesdays.take(3).map { visit("home", it, 19) }
        assertTrue(learnRoutine(visits, emptyList(), "me", setOf("home"), today).isEmpty())
        val gym = tuesdays.take(3).map { visit("gym", it, 19, 10) }
        val planned = PlanEntry("p", "me", "Gym", setOf(DayOfWeek.TUESDAY), LocalTime.of(18, 0), placeId = "gym")
        assertTrue(learnRoutine(gym, listOf(planned), "me", emptySet(), today).isEmpty())
        // Someone else's plan does not cover mine.
        assertEquals(1, learnRoutine(gym, listOf(planned.copy(memberId = "other")), "me", emptySet(), today).size)
    }
}
