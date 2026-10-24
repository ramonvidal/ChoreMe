package com.choreme.core.model

enum class ChoreStatus {
    /** Nobody has taken it yet; anyone in the family can grab it. */
    OPEN,

    /** Someone grabbed it and is on it. */
    CLAIMED,

    /** A kid finished it and a parent still needs to check it. */
    PENDING_APPROVAL,

    DONE,
}

data class Chore(
    val id: String,
    val title: String,
    val status: ChoreStatus = ChoreStatus.OPEN,
    val claimedBy: String? = null,
    val completedBy: String? = null,
    val approvedBy: String? = null,
)

/** "Whoever grabs it": anyone can take an open chore. */
fun Chore.claim(by: Member): Chore {
    check(status == ChoreStatus.OPEN) { "Only open chores can be grabbed" }
    return copy(status = ChoreStatus.CLAIMED, claimedBy = by.id)
}

/** Parents' work counts straight away; kids' work goes to a parent for approval. */
fun Chore.complete(by: Member): Chore {
    check(status == ChoreStatus.OPEN || status == ChoreStatus.CLAIMED) { "Chore is already finished" }
    return copy(
        status = if (by.role == Role.KID) ChoreStatus.PENDING_APPROVAL else ChoreStatus.DONE,
        claimedBy = claimedBy ?: by.id,
        completedBy = by.id,
    )
}

fun Chore.approve(by: Member): Chore {
    require(by.role == Role.PARENT) { "Only parents can approve chores" }
    check(status == ChoreStatus.PENDING_APPROVAL) { "Chore is not waiting for approval" }
    return copy(status = ChoreStatus.DONE, approvedBy = by.id)
}

/** Sends the chore back to the kid who did it, so they can redo it. */
fun Chore.reject(by: Member): Chore {
    require(by.role == Role.PARENT) { "Only parents can reject chores" }
    check(status == ChoreStatus.PENDING_APPROVAL) { "Chore is not waiting for approval" }
    return copy(status = ChoreStatus.CLAIMED, claimedBy = completedBy, completedBy = null)
}
