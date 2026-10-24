package com.choreme.core.model.chore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurrenceTest {
    // A Saturday.
    private val saturday = LocalDate.of(2026, 10, 24)

    @Test
    fun `one-off chores don't repeat`() {
        assertNull(Recurrence.Once.nextDueDate(saturday, saturday))
    }

    @Test
    fun `every N days counts from the later of due date and done date`() {
        val everyThree = Recurrence.EveryDays(3)
        assertEquals(saturday.plusDays(3), everyThree.nextDueDate(previousDue = saturday, doneOn = saturday))
        // Done two days late: next one is three days after it was actually done.
        assertEquals(saturday.plusDays(5), everyThree.nextDueDate(previousDue = saturday, doneOn = saturday.plusDays(2)))
        // Done a day early: the schedule doesn't move forward.
        assertEquals(saturday.plusDays(3), everyThree.nextDueDate(previousDue = saturday, doneOn = saturday.minusDays(1)))
    }

    @Test
    fun `weekly picks the next chosen weekday after the due date`() {
        val monThu = Recurrence.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
        assertEquals(LocalDate.of(2026, 10, 26), monThu.nextDueDate(saturday, saturday))
        // Bathroom due Saturday, cleaned on Friday: next is the Saturday after, not tomorrow.
        val saturdays = Recurrence.Weekly(setOf(DayOfWeek.SATURDAY))
        assertEquals(saturday.plusDays(7), saturdays.nextDueDate(previousDue = saturday, doneOn = saturday.minusDays(1)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `weekly needs at least one day`() {
        Recurrence.Weekly(emptySet())
    }
}
