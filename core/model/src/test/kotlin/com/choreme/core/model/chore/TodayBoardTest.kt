package com.choreme.core.model.chore

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TodayBoardTest {
    private val today = LocalDate.of(2026, 10, 24)
    private fun chore(title: String, due: LocalDate, status: ChoreStatus = ChoreStatus.OPEN, lastDoneOn: LocalDate? = null) =
        Chore(id = title, title = title, dueDate = due, status = status, lastDoneOn = lastDoneOn)

    @Test
    fun `chores are sorted into the right sections`() {
        val board = buildTodayBoard(
            listOf(
                chore("Vacuum", today.plusDays(3)),
                chore("Dishes", today, lastDoneOn = today.minusDays(1)),
                chore("Trash", today.minusDays(2)),
                chore("Bathroom", today.minusDays(1), ChoreStatus.PENDING_APPROVAL),
                chore("Laundry", today.plusDays(1), lastDoneOn = today),
                chore("Windows", today.plusDays(30)),
                chore("Shelf", today, ChoreStatus.DONE, lastDoneOn = today.minusDays(3)),
            ),
            today,
        )
        assertEquals(listOf("Bathroom"), board.awaitingApproval.map { it.title })
        assertEquals(listOf("Trash", "Dishes"), board.toDo.map { it.title })
        assertEquals(listOf("Laundry", "Vacuum"), board.upcoming.map { it.title })
        assertEquals(listOf("Laundry"), board.doneToday.map { it.title })
    }
}
