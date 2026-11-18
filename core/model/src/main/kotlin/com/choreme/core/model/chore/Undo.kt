package com.choreme.core.model.chore

import com.choreme.core.model.Member
import com.choreme.core.model.Role

/**
 * What Undo puts back after a step on a chore, for a few seconds after it. A parent's step (done, approved) is undone
 * whole: the chore goes back exactly as it was. A kid can only take back their own "I'm done": the chore is theirs
 * again, as before saying it (firestore.rules allows a kid nothing more).
 */
fun undoOf(before: Chore, after: Chore, by: Member): Chore? = when {
    by.role == Role.PARENT -> before
    after.status == ChoreStatus.PENDING_APPROVAL && after.completedBy == by.id ->
        after.copy(status = ChoreStatus.CLAIMED, completedBy = null)
    else -> null
}
