package com.choreme.core.model.chore

import com.choreme.core.model.Member
import com.choreme.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class UndoTest {
    private val today = LocalDate.of(2026, 11, 18)
    private val mom = Member("mom", "Mom", Role.PARENT)
    private val leo = Member("leo", "Leo", Role.KID)
    private val mia = Member("mia", "Mia", Role.KID)
    private val dishes = Chore(id = "d", title = "Dishes", template = ChoreTemplate.DISHES, dueDate = today, recurrence = Recurrence.EveryDays(1))

    @Test
    fun `a parent's finish is undone whole, the next date and who did it included`() {
        val done = dishes.complete(mom, today)
        assertEquals(dishes, undoOf(dishes, done, mom))
    }

    @Test
    fun `a parent's approval is undone back to waiting for approval`() {
        val reported = dishes.claim(leo).complete(leo, today)
        val approved = reported.approve(mom, today)
        assertEquals(reported, undoOf(reported, approved, mom))
    }

    @Test
    fun `a kid takes back their own I'm done and is on it again`() {
        val reported = dishes.complete(leo, today)
        val undone = undoOf(dishes, reported, leo)!!
        assertEquals(ChoreStatus.CLAIMED, undone.status)
        assertEquals("leo", undone.claimedBy)
        assertNull(undone.completedBy)
    }

    @Test
    fun `a kid can't undo someone else's report`() {
        val reported = dishes.complete(leo, today)
        assertNull(undoOf(dishes, reported, mia))
    }

    @Test
    fun `a chore being deleted leaves every part of the board`() {
        val other = dishes.copy(id = "o", title = "Other")
        val board = buildTodayBoard(listOf(dishes, other), today, "mom").without(setOf("d"))
        assertEquals(listOf("o"), board.toDo.map { it.id })
    }
}
