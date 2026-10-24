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
}
