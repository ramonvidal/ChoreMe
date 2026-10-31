package com.choreme.core.model.chore

import com.choreme.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ChoreReminderTest {
    private val today = LocalDate.of(2026, 10, 24)
    private fun chore(due: LocalDate, status: ChoreStatus = ChoreStatus.OPEN) =
        Chore(id = "$due$status", title = "x", dueDate = due, status = status)

    @Test
    fun `counts what's due, and approvals only for parents`() {
        val board = buildTodayBoard(
            listOf(chore(today), chore(today.minusDays(1)), chore(today, ChoreStatus.PENDING_APPROVAL)),
            today,
        )
        assertEquals(ChoreReminder(toDo = 2, overdue = 1, awaitingApproval = 1), reminderFor(board, Role.PARENT, today))
        assertEquals(ChoreReminder(toDo = 2, overdue = 1, awaitingApproval = 0), reminderFor(board, Role.KID, today))
    }

    @Test
    fun `no reminder when nothing is due`() {
        val board = buildTodayBoard(listOf(chore(today.plusDays(1)), chore(today, ChoreStatus.PENDING_APPROVAL)), today)
        assertNull(reminderFor(board, Role.KID, today))
    }

    @Test
    fun `chores someone took are theirs, and remind them`() {
        val mine = Chore("m", "Laundry", dueDate = today, status = ChoreStatus.CLAIMED, claimedBy = "mom")
        val theirs = Chore("t", "Dishes", dueDate = today, status = ChoreStatus.CLAIMED, claimedBy = "leo")
        val open = chore(today)
        val board = buildTodayBoard(listOf(mine, theirs, open), today, memberId = "mom")
        assertEquals(listOf(mine), board.mine)
        assertEquals(setOf(theirs, open), board.toDo.toSet())
        assertEquals(ChoreReminder(toDo = 2, overdue = 0, awaitingApproval = 0, mine = 1), reminderFor(board, Role.PARENT, today))
        // Nothing else due: still a reminder, for what you said you'd do.
        assertEquals(1, reminderFor(buildTodayBoard(listOf(mine), today, "mom"), Role.KID, today)?.mine)
        // Without a member, nothing is set apart.
        assertEquals(emptyList<Chore>(), buildTodayBoard(listOf(mine), today).mine)
    }

    @Test
    fun `yours for a later day waits under coming up until its day`() {
        val tomorrow = Chore("m", "Take medicine", dueDate = today.plusDays(1), status = ChoreStatus.CLAIMED, claimedBy = "mom")
        val board = buildTodayBoard(listOf(tomorrow), today, memberId = "mom")
        assertEquals(emptyList<Chore>(), board.mine)
        assertEquals(listOf(tomorrow), board.upcoming)
        assertEquals(listOf(tomorrow), buildTodayBoard(listOf(tomorrow), today.plusDays(1), memberId = "mom").mine)
    }

    @Test
    fun `the tab badge counts what needs you`() {
        val mine = Chore("m", "Laundry", dueDate = today, status = ChoreStatus.CLAIMED, claimedBy = "leo", helpers = setOf("mom"))
        val later = Chore("l", "Bins", dueDate = today.plusDays(1), status = ChoreStatus.CLAIMED, claimedBy = "mom")
        val toCheck = Chore("c", "Bath", dueDate = today, status = ChoreStatus.PENDING_APPROVAL, completedBy = "mia")
        val chores = listOf(mine, later, toCheck, chore(today))
        assertEquals(2, choresBadge(chores, today, "mom", Role.PARENT))
        assertEquals(1, choresBadge(chores, today, "leo", Role.KID))
    }

    @Test
    fun `chores further out wait under later, so none goes missing`() {
        val rent = Chore("r", "Pay the rent", recurrence = Recurrence.Monthly(20), dueDate = today.plusDays(27))
        val bins = Chore("b", "Bins", dueDate = today.plusDays(3))
        val board = buildTodayBoard(listOf(rent, bins), today)
        assertEquals(listOf(bins), board.upcoming)
        assertEquals(listOf(rent), board.later)
    }
}
