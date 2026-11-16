package com.choreme.feature.chores

import android.util.Log
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.PushEndpointRepository
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.choreDone
import com.choreme.core.model.chore.wasJustFinished
import com.choreme.core.push.PushKinds
import com.choreme.core.push.PushSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Applies one of the chore rules from :core:model and saves the result, for the Chores list and for the tasks of the
 * Rooms alike: a rule that refuses (say, someone grabbed the chore a moment ago) leaves the chore as it is, a parent's
 * finish goes into "who did what this week", and a kid's finish pings the parents to check it.
 */
internal class ChoreChanger @Inject constructor(
    private val chores: ChoreRepository,
    private val endpoints: PushEndpointRepository,
    private val push: PushSender,
) {
    fun change(
        scope: CoroutineScope,
        familyId: String,
        me: Member,
        parents: Set<String>,
        chore: Chore,
        transition: Chore.(me: Member, today: LocalDate) -> Chore,
    ) {
        val updated = try {
            chore.transition(me, LocalDate.now())
        } catch (e: IllegalStateException) {
            Log.i("ChoreMe", "Chore change refused: ${e.message}")
            return
        } catch (e: IllegalArgumentException) {
            Log.i("ChoreMe", "Chore change refused: ${e.message}")
            return
        }
        chores.save(familyId, me.id, chore, updated)
        // For "who did what this week"; finishing is always a parent's step (kids' chores get approved).
        if (me.role == Role.PARENT) choreDone(chore, updated)?.let { chores.logDone(familyId, it) }
        if (updated.notifyWhenDone && updated.wasJustFinished(chore)) {
            ping(scope, PushKinds.CHORE_DONE) { endpoints.othersThan(familyId, me.id) }
        }
        if (updated.status == ChoreStatus.PENDING_APPROVAL && chore.status != ChoreStatus.PENDING_APPROVAL) {
            ping(scope, PushKinds.CHORE_TO_APPROVE) { endpoints.of(familyId, parents) }
        }
    }

    private fun ping(scope: CoroutineScope, kind: String, to: suspend () -> List<String>) {
        scope.launch {
            try {
                push.ping(to(), kind)
            } catch (e: Exception) {
                Log.w("ChoreMe", "Couldn't ping the family", e)
            }
        }
    }
}
