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
    TIDY_UP,
    ORGANIC_WASTE,
    RECYCLING,
    FEED_PETS,
    TAKE_MEDICINE,
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
    /** Others doing it together with [claimedBy] (never including them). */
    val helpers: Set<String> = emptySet(),
    /**
     * Who a parent gave it to. Unlike [claimedBy], it stays: a repeating chore goes back to
     * them every time round ("Take medicine", every day, is always mine).
     */
    val assignedTo: Set<String> = emptySet(),
    val completedBy: String? = null,
    val approvedBy: String? = null,
    val lastDoneBy: String? = null,
    val lastDoneOn: LocalDate? = null,
    /** Who did it together with [lastDoneBy] that time; each of them gets the points too. */
    val lastHelpers: Set<String> = emptySet(),
    /** Who noticed it needed doing. */
    val reportedBy: String? = null,
    /** "Let everyone know when it's done": the family gets "Leo did: Wash dishes". */
    val notifyWhenDone: Boolean = true,
    /** Needs doing as soon as possible: shown first, in red, whatever its date. Cleared once it's done. */
    val urgent: Boolean = false,
    /** What finishing it is worth: bigger jobs count more. Set when it's added; kept every time round. */
    val points: Int = 1,
    /** How many times it has been finished, so finishing the same chore twice in a day counts twice. */
    val doneCount: Int = 0,
) {
    fun isOverdue(today: LocalDate) = isActive && dueDate < today

    /** Everyone on it: whoever grabbed it and those who joined. */
    val team: Set<String> get() = setOfNotNull(claimedBy) + helpers

    companion object {
        /** The points a chore can be worth: 1 for a quick job, up to 5 for a big one. */
        val POINTS = 1..5
    }

    /** Still to be done (as opposed to waiting for approval or finished). */
    val isActive get() = status == ChoreStatus.OPEN || status == ChoreStatus.CLAIMED
}

/** "Whoever grabs it": anyone can take an open chore. */
fun Chore.claim(by: Member): Chore {
    check(status == ChoreStatus.OPEN) { "Only open chores can be grabbed" }
    return copy(status = ChoreStatus.CLAIMED, claimedBy = by.id)
}

/** "Me too": joins whoever is already doing it, for chores done together. */
fun Chore.join(by: Member): Chore {
    if (status == ChoreStatus.OPEN) return claim(by)
    check(status == ChoreStatus.CLAIMED) { "Only chores someone is doing can be joined" }
    check(by.id !in team) { "Already on it" }
    return copy(helpers = helpers + by.id)
}

/**
 * Lets go of a chore. A helper just leaves; whoever grabbed it hands it to a helper, if
 * there is one, or frees it. A parent who isn't on it frees it for everyone.
 */
fun Chore.unclaim(by: Member): Chore {
    check(status == ChoreStatus.CLAIMED) { "Chore isn't grabbed" }
    if (by.id in helpers) return copy(helpers = helpers - by.id)
    require(claimedBy == by.id || by.role == Role.PARENT) { "Only whoever grabbed it, or a parent, can let it go" }
    val next = if (claimedBy == by.id) helpers.minOrNull() else null
    return if (next == null) {
        copy(status = ChoreStatus.OPEN, claimedBy = null, helpers = emptySet())
    } else {
        copy(claimedBy = next, helpers = helpers - next)
    }
}

/** A parent decides who's doing it; nobody frees it again. */
fun Chore.assign(people: Set<String>, by: Member): Chore {
    require(by.role == Role.PARENT) { "Only parents can say who does a chore" }
    check(isActive) { "Chore is already finished" }
    if (people.isEmpty()) return copy(status = ChoreStatus.OPEN, claimedBy = null, helpers = emptySet(), assignedTo = emptySet())
    val lead = claimedBy?.takeIf { it in people } ?: people.min()
    return copy(status = ChoreStatus.CLAIMED, claimedBy = lead, helpers = people - lead, assignedTo = people)
}

/** Parents' work counts straight away; kids' work goes to a parent for approval. */
fun Chore.complete(by: Member, today: LocalDate): Chore {
    check(isActive) { "Chore is already finished" }
    require(claimedBy == null || by.id in team || by.role == Role.PARENT) { "Someone else grabbed this chore" }
    return if (by.role == Role.KID) {
        copy(status = ChoreStatus.PENDING_APPROVAL, claimedBy = claimedBy ?: by.id, completedBy = by.id)
    } else {
        // Done together: everyone on it gets the credit. A parent finishing someone else's gets it alone.
        finish(doneBy = by.id, together = if (by.id in team) team - by.id else emptySet(), approvedBy = null, today = today)
    }
}

/** A parent records who did it (one person or several together), e.g. kids without a phone. Counts as approved. */
fun Chore.completeFor(people: Set<String>, by: Member, today: LocalDate): Chore {
    require(by.role == Role.PARENT) { "Only parents can mark chores done for someone else" }
    require(people.isNotEmpty()) { "Someone has to have done it" }
    check(isActive) { "Chore is already finished" }
    val lead = people.min()
    return finish(doneBy = lead, together = people - lead, approvedBy = by.id, today = today)
}

fun Chore.approve(by: Member, today: LocalDate): Chore {
    require(by.role == Role.PARENT) { "Only parents can approve chores" }
    check(status == ChoreStatus.PENDING_APPROVAL) { "Chore is not waiting for approval" }
    val doneBy = checkNotNull(completedBy)
    return finish(doneBy = doneBy, together = team - doneBy, approvedBy = by.id, today = today)
}

/** Sends the chore back to the kid who did it, so they can redo it. */
fun Chore.reject(by: Member): Chore {
    require(by.role == Role.PARENT) { "Only parents can reject chores" }
    check(status == ChoreStatus.PENDING_APPROVAL) { "Chore is not waiting for approval" }
    return copy(status = ChoreStatus.CLAIMED, claimedBy = completedBy, completedBy = null)
}

/**
 * One-off chores end as DONE; recurring ones reopen on their next date: back with whoever
 * they're assigned to, or free for anyone.
 */
private fun Chore.finish(doneBy: String, together: Set<String>, approvedBy: String?, today: LocalDate): Chore {
    val next = recurrence.nextDueDate(previousDue = dueDate, doneOn = today)
    return if (next == null) {
        copy(
            status = ChoreStatus.DONE,
            completedBy = doneBy,
            approvedBy = approvedBy,
            lastDoneBy = doneBy,
            lastDoneOn = today,
            lastHelpers = together,
            urgent = false,
            doneCount = doneCount + 1,
        )
    } else {
        val lead = assignedTo.minOrNull()
        copy(
            status = if (lead == null) ChoreStatus.OPEN else ChoreStatus.CLAIMED,
            dueDate = next,
            claimedBy = lead,
            helpers = assignedTo - setOfNotNull(lead),
            completedBy = null,
            approvedBy = approvedBy,
            lastDoneBy = doneBy,
            lastDoneOn = today,
            lastHelpers = together,
            // The next time round is ordinary again.
            urgent = false,
            doneCount = doneCount + 1,
        )
    }
}

/**
 * True when [this] is [before] just finished: someone's name went on it as done (a parent
 * finishing it, or approving a kid's). A kid saying "done" doesn't count until approved.
 */
fun Chore.wasJustFinished(before: Chore): Boolean =
    lastDoneBy != null &&
        (doneCount > before.doneCount || lastDoneOn != before.lastDoneOn || lastDoneBy != before.lastDoneBy)

/** A chore someone typed by hand, kept as a quick pick for next time. */
data class SavedChore(
    val id: String,
    val title: String,
    val createdBy: String,
    val points: Int = 1,
    /** The ready-made chore it's closest to, for its group and icon; OTHER for ones saved before. */
    val template: ChoreTemplate = ChoreTemplate.OTHER,
)

/** A hand-typed chore (not one of the built-in templates) is saved unless it already is. */
fun shouldSaveForLater(template: ChoreTemplate, title: String, saved: List<SavedChore>): Boolean =
    template == ChoreTemplate.OTHER && title.isNotBlank() &&
        saved.none { NameKeys.key(it.title) == NameKeys.key(title) }

/** A saved chore picked again with other points: those become its points from now on. */
fun savedChoreToUpdate(title: String, points: Int, saved: List<SavedChore>): SavedChore? =
    saved.firstOrNull { NameKeys.key(it.title) == NameKeys.key(title) && it.points != points }?.copy(points = points)

/**
 * A chore with the same name that still needs doing, if there is one, so two people
 * noticing the full sink don't add "Wash dishes" twice.
 */
fun findOpenDuplicate(title: String, chores: List<Chore>): Chore? {
    val key = title.trim().lowercase()
    return chores.firstOrNull { (it.isActive || it.status == ChoreStatus.PENDING_APPROVAL) && it.title.trim().lowercase() == key }
}
