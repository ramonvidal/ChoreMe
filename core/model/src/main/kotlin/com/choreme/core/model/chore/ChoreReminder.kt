package com.choreme.core.model.chore

import com.choreme.core.model.Role
import java.time.LocalDate

/** The morning summary for one person; null when there's nothing worth a notification. */
data class ChoreReminder(
    val toDo: Int,
    val overdue: Int,
    /** Only counted for parents, who are the ones who can approve. */
    val awaitingApproval: Int,
)

fun reminderFor(board: TodayBoard, role: Role, today: LocalDate): ChoreReminder? {
    val reminder = ChoreReminder(
        toDo = board.toDo.size,
        overdue = board.toDo.count { it.isOverdue(today) },
        awaitingApproval = if (role == Role.PARENT) board.awaitingApproval.size else 0,
    )
    return reminder.takeIf { it.toDo > 0 || it.awaitingApproval > 0 }
}
