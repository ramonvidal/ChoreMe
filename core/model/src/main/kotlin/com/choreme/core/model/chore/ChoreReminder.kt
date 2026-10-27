package com.choreme.core.model.chore

import com.choreme.core.model.Role
import java.time.LocalDate

/** The morning summary for one person; null when there's nothing worth a notification. */
data class ChoreReminder(
    val toDo: Int,
    val overdue: Int,
    /** Only counted for parents, who are the ones who can approve. */
    val awaitingApproval: Int,
    /** Chores this person said they'd do, so they don't forget. */
    val mine: Int = 0,
)

fun reminderFor(board: TodayBoard, role: Role, today: LocalDate): ChoreReminder? {
    val reminder = ChoreReminder(
        toDo = board.toDo.size,
        overdue = (board.toDo + board.mine).count { it.isOverdue(today) },
        awaitingApproval = if (role == Role.PARENT) board.awaitingApproval.size else 0,
        mine = board.mine.size,
    )
    return reminder.takeIf { it.toDo > 0 || it.awaitingApproval > 0 || it.mine > 0 }
}
