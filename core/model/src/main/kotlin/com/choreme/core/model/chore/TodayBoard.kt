package com.choreme.core.model.chore

import java.time.LocalDate

/** What the Today tab shows, worked out from all the family's chores. */
data class TodayBoard(
    val awaitingApproval: List<Chore>,
    /** Due today or earlier: urgent ones first, then overdue ones first. */
    val toDo: List<Chore>,
    /** Due in the next [UPCOMING_DAYS] days, soonest first. */
    val upcoming: List<Chore>,
    val doneToday: List<Chore>,
    /**
     * Chores the person looking said they'd do ("I'll do it", joined, or given to them) that
     * are due by today; not in [toDo]. Later ones wait in [upcoming], like the rest.
     */
    val mine: List<Chore> = emptyList(),
    /** Due after [UPCOMING_DAYS] days (next month's, say), soonest first, so none goes missing. */
    val later: List<Chore> = emptyList(),
) {
    companion object {
        const val UPCOMING_DAYS = 7L
    }
}

/** With [memberId], the chores they took move to [TodayBoard.mine], so they stand out. */
fun buildTodayBoard(chores: List<Chore>, today: LocalDate, memberId: String? = null): TodayBoard {
    val byTitle = compareBy<Chore> { it.title.lowercase() }
    val mine = chores.filter {
        memberId != null && it.status == ChoreStatus.CLAIMED && memberId in it.team && it.dueDate <= today
    }
    val others = chores - mine.toSet()
    return TodayBoard(
        mine = mine.sortedWith(compareByDescending<Chore> { it.urgent }.thenBy { it.dueDate }.then(byTitle)),
        awaitingApproval = chores.filter { it.status == ChoreStatus.PENDING_APPROVAL }.sortedWith(byTitle),
        toDo = others
            // Urgent ones come first, but only from their day: "pay the lawyer on the 5th" waits.
            .filter { it.isActive && it.dueDate <= today }
            .sortedWith(compareByDescending<Chore> { it.urgent }.thenBy { it.dueDate }.then(byTitle)),
        upcoming = others
            .filter { it.isActive && it.dueDate > today && it.dueDate <= today.plusDays(TodayBoard.UPCOMING_DAYS) }
            .sortedWith(compareBy<Chore> { it.dueDate }.then(byTitle)),
        doneToday = chores.filter { it.lastDoneOn == today }.sortedWith(byTitle),
        later = others
            .filter { it.isActive && it.dueDate > today.plusDays(TodayBoard.UPCOMING_DAYS) }
            .sortedWith(compareBy<Chore> { it.dueDate }.then(byTitle)),
    )
}
