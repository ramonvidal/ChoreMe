package com.choreme.core.model.chore

import com.choreme.core.model.Member
import com.choreme.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ChoreLogTest {
    private val today = LocalDate.of(2026, 10, 26)
    private val mom = Member("mom", "Mom", Role.PARENT)
    private val leo = Member("leo", "Leo", Role.KID)
    private val mia = Member("mia", "Mia", Role.KID)

    @Test
    fun `finishing logs it, a kid's "done" only once approved`() {
        val dishes = Chore("1", "Dishes", recurrence = Recurrence.EveryDays(1), dueDate = today)
        assertEquals(ChoreDone("1", "Dishes", "mom", today), choreDone(dishes, dishes.complete(mom, today)))
        val pending = dishes.complete(leo, today)
        assertEquals(null, choreDone(dishes, pending))
        assertEquals(ChoreDone("1", "Dishes", "leo", today), choreDone(pending, pending.approve(mom, today)))
    }

    @Test
    fun `the week counts the last 7 days, most chores first, everyone listed`() {
        val log = listOf(
            ChoreDone("1", "Dishes", "leo", today),
            ChoreDone("1", "Dishes", "leo", today.minusDays(6)),
            ChoreDone("2", "Trash", "leo", today.minusDays(7)),
            ChoreDone("3", "Laundry", "mom", today.minusDays(1)),
        )
        val week = weeklySummary(log, listOf(mia, mom, leo), today)
        assertEquals(listOf("Leo" to 2, "Mom" to 1, "Mia" to 0), week.map { it.member.name to it.done.size })
    }
}

class TimedReminderTest {
    @Test
    fun `a reminder time already passed today means tomorrow`() {
        val now = java.time.LocalDateTime.of(2026, 10, 26, 17, 30)
        assertEquals(now.withHour(18).withMinute(0), nextReminderTime(now, java.time.LocalTime.of(18, 0)))
        assertEquals(now.plusDays(1).withHour(9).withMinute(0), nextReminderTime(now, java.time.LocalTime.of(9, 0)))
    }
}
