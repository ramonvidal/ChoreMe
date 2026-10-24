package com.choreme.core.model.chore

import com.choreme.core.model.Member
import com.choreme.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ChoreTest {
    private val mom = Member(id = "mom", name = "Mom", role = Role.PARENT)
    private val leo = Member(id = "leo", name = "Leo", role = Role.KID)
    private val mia = Member(id = "mia", name = "Mia", role = Role.KID)
    private val today = LocalDate.of(2026, 10, 24)
    private val dishes = Chore(id = "1", title = "Wash dishes", recurrence = Recurrence.EveryDays(1), dueDate = today)
    private val oneOff = Chore(id = "2", title = "Fix the shelf", dueDate = today)

    @Test
    fun `anyone can grab an open chore, and let it go again`() {
        val grabbed = dishes.claim(leo)
        assertEquals(ChoreStatus.CLAIMED, grabbed.status)
        assertEquals("leo", grabbed.claimedBy)
        assertEquals(dishes, grabbed.unclaim(leo))
    }

    @Test(expected = IllegalStateException::class)
    fun `a grabbed chore cannot be grabbed again`() {
        dishes.claim(leo).claim(mia)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `kids cannot finish a chore someone else grabbed`() {
        dishes.claim(leo).complete(mia, today)
    }

    @Test
    fun `a parent finishing a recurring chore reopens it for the next date`() {
        val done = dishes.claim(mom).complete(mom, today)
        assertEquals(ChoreStatus.OPEN, done.status)
        assertEquals(today.plusDays(1), done.dueDate)
        assertNull(done.claimedBy)
        assertEquals("mom", done.lastDoneBy)
        assertEquals(today, done.lastDoneOn)
    }

    @Test
    fun `a one-off chore ends as done`() {
        val done = oneOff.complete(mom, today)
        assertEquals(ChoreStatus.DONE, done.status)
        assertEquals(today, done.dueDate)
    }

    @Test
    fun `a kid's chore waits for approval, then rolls over`() {
        val pending = dishes.complete(leo, today)
        assertEquals(ChoreStatus.PENDING_APPROVAL, pending.status)
        assertEquals("leo", pending.completedBy)
        assertEquals(dishes.dueDate, pending.dueDate)

        val approved = pending.approve(mom, today)
        assertEquals(ChoreStatus.OPEN, approved.status)
        assertEquals("leo", approved.lastDoneBy)
        assertEquals("mom", approved.approvedBy)
        assertEquals(today.plusDays(1), approved.dueDate)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `kids cannot approve chores`() {
        dishes.complete(leo, today).approve(mia, today)
    }

    @Test
    fun `a rejected chore goes back to the kid who did it`() {
        val rejected = dishes.complete(leo, today).reject(mom)
        assertEquals(ChoreStatus.CLAIMED, rejected.status)
        assertEquals("leo", rejected.claimedBy)
        assertNull(rejected.completedBy)
    }

    @Test
    fun `a parent can record a chore done by a kid without a phone`() {
        val done = oneOff.completeFor(mia, by = mom, today)
        assertEquals(ChoreStatus.DONE, done.status)
        assertEquals("mia", done.lastDoneBy)
        assertEquals("mom", done.approvedBy)
    }
}

class DuplicateChoreTest {
    private val today = LocalDate.of(2026, 10, 25)

    @Test
    fun `an open chore with the same name is a duplicate, a finished one isn't`() {
        val open = Chore(id = "1", title = "Wash dishes", dueDate = today)
        val done = Chore(id = "2", title = "Clean bathroom", dueDate = today, status = ChoreStatus.DONE)
        assertEquals(open, findOpenDuplicate("  wash DISHES ", listOf(open, done)))
        assertNull(findOpenDuplicate("Clean bathroom", listOf(open, done)))
    }
}
