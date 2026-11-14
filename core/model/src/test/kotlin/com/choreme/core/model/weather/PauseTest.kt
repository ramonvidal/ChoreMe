package com.choreme.core.model.weather

import com.choreme.core.model.Member
import com.choreme.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class PauseTest {
    private val today = LocalDate.of(2026, 11, 16)
    private val ana = Member("ana", "Ana", Role.PARENT)
    private fun trip(id: String, owner: String = "ana", learned: Boolean = false, place: String? = "gym") =
        PlanEntry(id, owner, "Gym", setOf(DayOfWeek.MONDAY), LocalTime.of(18, 0), placeId = place, learned = learned)

    @Test
    fun `an away owner pauses their trips`() {
        val away = ana.copy(awayUntil = today)
        assertEquals(PauseReason.AWAY, pauseReason(trip("a"), away, today, mine = false, lastVisit = null))
        assertNull(pauseReason(trip("a"), away.copy(awayUntil = today.minusDays(1)), today, mine = false, lastVisit = null))
    }

    @Test
    fun `a learned habit fades after three weeks unseen, only on its owner's phone`() {
        val habit = trip("h", learned = true)
        assertEquals(PauseReason.UNSEEN, pauseReason(habit, ana, today, mine = true, lastVisit = today.minusDays(22)))
        assertNull(pauseReason(habit, ana, today, mine = true, lastVisit = today.minusDays(21)))
        assertNull(pauseReason(habit, ana, today, mine = false, lastVisit = today.minusDays(40)))
        // Never seen on this phone: it cannot tell, so it stays on.
        assertNull(pauseReason(habit, ana, today, mine = true, lastVisit = null))
    }

    @Test
    fun `a trip typed in by a parent never fades`() {
        assertNull(pauseReason(trip("t"), ana, today, mine = true, lastVisit = today.minusDays(200)))
    }

    @Test
    fun `active entries leave out the paused ones`() {
        val entries = listOf(trip("typed"), trip("old", learned = true, place = "pool"), trip("bob", owner = "bob"))
        val members = listOf(ana, Member("bob", "Bob", Role.PARENT, awayUntil = today.plusDays(3)))
        val visits = mapOf("pool" to today.minusDays(30), "gym" to today)
        assertEquals(listOf("typed"), activeEntries(entries, members, today, "ana", visits).map { it.id })
        assertEquals(mapOf("old" to PauseReason.UNSEEN, "bob" to PauseReason.AWAY), pausedEntries(entries, members, today, "ana", visits))
    }
}
