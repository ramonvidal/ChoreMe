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

/**
 * The number on the Chores tab: chores you said you'd do that are due by today, plus, for
 * parents, kids' chores waiting to be checked. What needs you, not everything.
 */
fun choresBadge(chores: List<Chore>, today: LocalDate, memberId: String, role: Role): Int {
    val mine = chores.count { it.status == ChoreStatus.CLAIMED && memberId in it.team && !it.dueDate.isAfter(today) }
    val toCheck = if (role == Role.PARENT) chores.count { it.status == ChoreStatus.PENDING_APPROVAL } else 0
    return mine + toCheck
}

fun reminderFor(board: TodayBoard, role: Role, today: LocalDate): ChoreReminder? {
    val reminder = ChoreReminder(
        toDo = board.toDo.size,
        overdue = (board.toDo + board.mine).count { it.isOverdue(today) },
        awaitingApproval = if (role == Role.PARENT) board.awaitingApproval.size else 0,
        mine = board.mine.size,
    )
    return reminder.takeIf { it.toDo > 0 || it.awaitingApproval > 0 || it.mine > 0 }
}
