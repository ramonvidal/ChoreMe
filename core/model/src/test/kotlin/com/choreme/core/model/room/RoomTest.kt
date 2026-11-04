package com.choreme.core.model.room

import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RoomTest {
    private val today = LocalDate.of(2026, 11, 4)
    private val bathroom = Room("b1", "Bathroom 1", RoomType.BATHROOM)
    private val kitchen = Room("k", "Kitchen", RoomType.KITCHEN)

    @Test
    fun `a task done 4 days ago, every 10 days, is due in 6`() {
        val task = newRoomTask("b1", " Clean the bathroom ", ChoreTemplate.CLEAN_BATHROOM, everyDays = 10, lastDoneOn = today.minusDays(4), today = today)
        assertEquals("Clean the bathroom", task.title)
        assertEquals(today.plusDays(6), task.dueDate)
        assertEquals(today.minusDays(4), task.lastDoneOn)
        assertEquals(Recurrence.EveryDays(10), task.recurrence)
        assertEquals("b1", task.roomId)
        assertEquals(10, task.everyDays)
    }

    @Test
    fun `a task nobody remembers doing is due today, and a future date is ignored`() {
        val unknown = newRoomTask("b1", "Mop", ChoreTemplate.VACUUM, 14, lastDoneOn = null, today = today)
        assertEquals(today, unknown.dueDate)
        assertNull(unknown.lastDoneOn)
        val future = newRoomTask("b1", "Mop", ChoreTemplate.VACUUM, 14, lastDoneOn = today.plusDays(2), today = today)
        assertEquals(today, future.dueDate)
        assertNull(future.lastDoneOn)
        // Done long ago: already late, honestly.
        val old = newRoomTask("b1", "Mop", ChoreTemplate.VACUUM, 10, lastDoneOn = today.minusDays(25), today = today)
        assertTrue(old.isOverdue(today))
    }

    @Test
    fun `a room shows when it was last cleaned, what's next and what's late`() {
        val cleaned = Chore("1", "Clean the bathroom", dueDate = today.plusDays(5), lastDoneOn = today.minusDays(5), roomId = "b1", recurrence = Recurrence.EveryDays(10))
        val towels = Chore("2", "Wash the towels", dueDate = today.minusDays(2), lastDoneOn = today.minusDays(9), roomId = "b1", recurrence = Recurrence.EveryDays(7))
        val done = Chore("3", "Old one", dueDate = today, status = ChoreStatus.DONE, lastDoneOn = today.minusDays(30), roomId = "b1")
        val other = Chore("4", "Clean the kitchen", dueDate = today, roomId = "k")
        val unrelated = Chore("5", "Dishes", dueDate = today)
        val overviews = roomOverviews(listOf(kitchen, bathroom), listOf(cleaned, towels, done, other, unrelated))
        assertEquals(listOf("Bathroom 1", "Kitchen"), overviews.map { it.room.name })
        val bath = overviews.first()
        assertEquals(3, bath.tasks.size)
        assertEquals(today.minusDays(5), bath.lastDoneOn)
        assertEquals(towels, bath.next)          // the most overdue waiting task
        assertEquals(1, bath.lateCount(today))
        assertEquals(5, daysSince(bath.lastDoneOn!!, today))
        assertNull(overviews.last().lastDoneOn)  // the kitchen was never done
    }

    @Test
    fun `every kind of room has suggestions, and every suggestion is a sensible repeat`() {
        RoomType.entries.forEach { type -> assertTrue("$type has ideas", ideasFor(type).isNotEmpty()) }
        TaskIdea.entries.forEach { idea -> assertTrue("$idea repeats every 1 to 90 days", idea.everyDays in 1..Recurrence.MAX_DAYS) }
        assertTrue(TaskIdea.CLEAN_BATHROOM in ideasFor(RoomType.BATHROOM))
    }

    @Test
    fun `pictures compare by content`() {
        assertEquals(RoomPhoto(byteArrayOf(1, 2, 3)), RoomPhoto(byteArrayOf(1, 2, 3)))
        assertTrue(RoomPhoto(byteArrayOf(1)) != RoomPhoto(byteArrayOf(2)))
    }
}
