package com.choreme.core.model

/** What happens when [me] leaves the family. */
enum class LeaveOutcome {
    /** Just this person (or this device) goes. */
    LEAVE,

    /** Nobody else is left: the whole family and its data are deleted. */
    DELETE_FAMILY,

    /** The only parent can't leave while others remain: nobody could approve or let people in. */
    BLOCKED_LAST_PARENT,
}

fun leaveOutcome(me: Member, members: List<Member>): LeaveOutcome {
    val others = members.filter { it.id != me.id }
    return when {
        others.isEmpty() -> LeaveOutcome.DELETE_FAMILY
        me.role == Role.PARENT && others.none { it.role == Role.PARENT } -> LeaveOutcome.BLOCKED_LAST_PARENT
        else -> LeaveOutcome.LEAVE
    }
}
