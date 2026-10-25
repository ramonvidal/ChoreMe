package com.choreme.core.model.chore

import com.choreme.core.model.Member
import com.choreme.core.model.NameKeys
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

/** Things that often need doing around the house; [OTHER] is anything typed by hand. */
enum class ChoreTemplate {
    DISHES,
    LAUNDRY,
    FOLD_CLOTHES,
    CLEAN_KITCHEN,
    CLEAN_BATHROOM,
    TRASH,
    VACUUM,
    MAKE_BEDS,
    WATER_PLANTS,
    GROCERIES,
    OTHER,
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
    /** Who noticed it needed doing. */
    val reportedBy: String? = null,
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

/** A chore someone typed by hand, kept as a quick pick for next time. */
data class SavedChore(val id: String, val title: String, val createdBy: String)

/** A hand-typed chore (not one of the built-in templates) is saved unless it already is. */
fun shouldSaveForLater(template: ChoreTemplate, title: String, saved: List<SavedChore>): Boolean =
    template == ChoreTemplate.OTHER && title.isNotBlank() &&
        saved.none { NameKeys.key(it.title) == NameKeys.key(title) }

/**
 * A chore with the same name that still needs doing, if there is one, so two people
 * noticing the full sink don't add "Wash dishes" twice.
 */
fun findOpenDuplicate(title: String, chores: List<Chore>): Chore? {
    val key = title.trim().lowercase()
    return chores.firstOrNull { (it.isActive || it.status == ChoreStatus.PENDING_APPROVAL) && it.title.trim().lowercase() == key }
}
