package com.choreme.core.model.chore

import com.choreme.core.model.Member
import com.choreme.core.model.Role
import java.time.LocalDate

enum class ChoreStatus {
    /** Nobody has taken it yet; anyone in the family can grab it. */
    OPEN,

    /** Someone grabbed it and is on it. */
    CLAIMED,

    /** A kid finished it and a parent still needs to check it. */
    PENDING_APPROVAL,

    /** Finished for good. Only one-off chores end here; recurring ones reopen for their next date. */
    DONE,
}

/** Common chores with a sensible default schedule; [OTHER] is anything typed by hand. */
enum class ChoreTemplate(val defaultRecurrence: Recurrence) {
    DISHES(Recurrence.EveryDays(1)),
    LAUNDRY(Recurrence.EveryDays(3)),
    FOLD_CLOTHES(Recurrence.EveryDays(3)),
    CLEAN_KITCHEN(Recurrence.EveryDays(1)),
    CLEAN_BATHROOM(Recurrence.EveryDays(7)),
    TRASH(Recurrence.EveryDays(2)),
    VACUUM(Recurrence.EveryDays(7)),
    MAKE_BEDS(Recurrence.EveryDays(1)),
    WATER_PLANTS(Recurrence.EveryDays(3)),
    GROCERIES(Recurrence.EveryDays(7)),
    OTHER(Recurrence.Once),
}

data class Chore(
    val id: String,
    val title: String,
    val template: ChoreTemplate = ChoreTemplate.OTHER,
    val recurrence: Recurrence = Recurrence.Once,
    val dueDate: LocalDate,
    val status: ChoreStatus = ChoreStatus.OPEN,
    /** Member ids, not device uids, so a kid with two tablets is still one person. */
    val claimedBy: String? = null,
    val completedBy: String? = null,
    val approvedBy: String? = null,
    val lastDoneBy: String? = null,
    val lastDoneOn: LocalDate? = null,
) {
    fun isOverdue(today: LocalDate) = isActive && dueDate < today

    /** Still to be done (as opposed to waiting for approval or finished). */
    val isActive get() = status == ChoreStatus.OPEN || status == ChoreStatus.CLAIMED
}

/** "Whoever grabs it": anyone can take an open chore. */
fun Chore.claim(by: Member): Chore {
    check(status == ChoreStatus.OPEN) { "Only open chores can be grabbed" }
    return copy(status = ChoreStatus.CLAIMED, claimedBy = by.id)
}

/** Lets go of a grabbed chore, so someone else can take it. */
fun Chore.unclaim(by: Member): Chore {
    check(status == ChoreStatus.CLAIMED) { "Chore isn't grabbed" }
    require(claimedBy == by.id || by.role == Role.PARENT) { "Only whoever grabbed it, or a parent, can let it go" }
    return copy(status = ChoreStatus.OPEN, claimedBy = null)
}

/** Parents' work counts straight away; kids' work goes to a parent for approval. */
fun Chore.complete(by: Member, today: LocalDate): Chore {
    check(isActive) { "Chore is already finished" }
    require(claimedBy == null || claimedBy == by.id || by.role == Role.PARENT) { "Someone else grabbed this chore" }
    return if (by.role == Role.KID) {
        copy(status = ChoreStatus.PENDING_APPROVAL, claimedBy = by.id, completedBy = by.id)
    } else {
        finish(doneBy = by.id, approvedBy = null, today = today)
    }
}

/** A parent records that a kid did it, e.g. a kid without a phone. Counts as approved. */
fun Chore.completeFor(kid: Member, by: Member, today: LocalDate): Chore {
    require(by.role == Role.PARENT) { "Only parents can mark chores done for someone else" }
    check(isActive) { "Chore is already finished" }
    return finish(doneBy = kid.id, approvedBy = by.id, today = today)
}

fun Chore.approve(by: Member, today: LocalDate): Chore {
    require(by.role == Role.PARENT) { "Only parents can approve chores" }
    check(status == ChoreStatus.PENDING_APPROVAL) { "Chore is not waiting for approval" }
    return finish(doneBy = checkNotNull(completedBy), approvedBy = by.id, today = today)
}

/** Sends the chore back to the kid who did it, so they can redo it. */
fun Chore.reject(by: Member): Chore {
    require(by.role == Role.PARENT) { "Only parents can reject chores" }
    check(status == ChoreStatus.PENDING_APPROVAL) { "Chore is not waiting for approval" }
    return copy(status = ChoreStatus.CLAIMED, claimedBy = completedBy, completedBy = null)
}

/** One-off chores end as DONE; recurring ones reopen, free for anyone, on their next date. */
private fun Chore.finish(doneBy: String, approvedBy: String?, today: LocalDate): Chore {
    val next = recurrence.nextDueDate(previousDue = dueDate, doneOn = today)
    return if (next == null) {
        copy(status = ChoreStatus.DONE, completedBy = doneBy, approvedBy = approvedBy, lastDoneBy = doneBy, lastDoneOn = today)
    } else {
        copy(
            status = ChoreStatus.OPEN,
            dueDate = next,
            claimedBy = null,
            completedBy = null,
            approvedBy = approvedBy,
            lastDoneBy = doneBy,
            lastDoneOn = today,
        )
    }
}
