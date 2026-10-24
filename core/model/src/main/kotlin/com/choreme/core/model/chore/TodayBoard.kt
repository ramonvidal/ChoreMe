package com.choreme.core.model.chore

import java.time.LocalDate

/** What the Today tab shows, worked out from all the family's chores. */
data class TodayBoard(
    val awaitingApproval: List<Chore>,
    /** Due today or earlier, overdue ones first. */
    val toDo: List<Chore>,
    /** Due in the next [UPCOMING_DAYS] days, soonest first. */
    val upcoming: List<Chore>,
    val doneToday: List<Chore>,
) {
    companion object {
        const val UPCOMING_DAYS = 7L
    }
}

fun buildTodayBoard(chores: List<Chore>, today: LocalDate): TodayBoard {
    val byTitle = compareBy<Chore> { it.title.lowercase() }
    return TodayBoard(
        awaitingApproval = chores.filter { it.status == ChoreStatus.PENDING_APPROVAL }.sortedWith(byTitle),
        toDo = chores.filter { it.isActive && it.dueDate <= today }.sortedWith(compareBy<Chore> { it.dueDate }.then(byTitle)),
        upcoming = chores
            .filter { it.isActive && it.dueDate > today && it.dueDate <= today.plusDays(TodayBoard.UPCOMING_DAYS) }
            .sortedWith(compareBy<Chore> { it.dueDate }.then(byTitle)),
        doneToday = chores.filter { it.lastDoneOn == today }.sortedWith(byTitle),
    )
}
