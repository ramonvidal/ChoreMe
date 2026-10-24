package com.choreme.feature.chores

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.GroceryRepository
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.Recurrence
import com.choreme.core.model.chore.TodayBoard
import com.choreme.core.model.chore.approve
import com.choreme.core.model.chore.buildTodayBoard
import com.choreme.core.model.chore.claim
import com.choreme.core.model.chore.complete
import com.choreme.core.model.chore.findOpenDuplicate
import com.choreme.core.model.chore.completeFor
import com.choreme.core.model.chore.reject
import com.choreme.core.model.chore.unclaim
import com.choreme.core.model.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class ChoresUiState(
    val loading: Boolean = true,
    val familyId: String? = null,
    val me: Member? = null,
    val today: LocalDate = LocalDate.now(),
    val board: TodayBoard = TodayBoard(emptyList(), emptyList(), emptyList(), emptyList()),
    val chores: List<Chore> = emptyList(),
    val members: Map<String, Member> = emptyMap(),
    val groceriesToBuy: Int = 0,
) {
    val isParent get() = me?.role == Role.PARENT
    val hasChores get() = chores.isNotEmpty()
    val kids get() = members.values.filter { it.role == Role.KID }.sortedBy { it.name.lowercase() }
    fun nameOf(memberId: String?): String? = memberId?.let { members[it]?.name }
}

/** What the person typed in the chore editor. */
data class ChoreDraft(
    val title: String,
    val template: ChoreTemplate,
    val recurrence: Recurrence,
    val dueDate: LocalDate,
)

@HiltViewModel
class ChoresViewModel @Inject constructor(
    families: FamilyRepository,
    private val chores: ChoreRepository,
    groceries: GroceryRepository,
) : ViewModel() {
    val uiState: StateFlow<ChoresUiState> = families.session
        .filterIsInstance<SessionState.InFamily>()
        .flatMapLatest { session ->
            combine(
                chores.observeChores(session.familyId),
                families.observeMembers(session.familyId),
                groceries.observeItems(session.familyId),
            ) { allChores, members, groceryItems ->
                val today = LocalDate.now()
                ChoresUiState(
                    loading = false,
                    familyId = session.familyId,
                    me = members.firstOrNull { it.id == session.memberId }
                        ?: Member(session.memberId, name = "", role = session.role),
                    today = today,
                    board = buildTodayBoard(allChores, today),
                    chores = allChores,
                    members = members.associateBy { it.id },
                    groceriesToBuy = groceryItems.count { it.isNeeded },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChoresUiState())

    fun claim(chore: Chore) = change(chore) { me, _ -> claim(me) }
    fun unclaim(chore: Chore) = change(chore) { me, _ -> unclaim(me) }
    fun complete(chore: Chore) = change(chore) { me, today -> complete(me, today) }
    fun completeFor(chore: Chore, kid: Member) = change(chore) { me, today -> completeFor(kid, me, today) }
    fun approve(chore: Chore) = change(chore) { me, today -> approve(me, today) }
    fun reject(chore: Chore) = change(chore) { me, _ -> reject(me) }

    /** Adds the chore unless the same thing is already waiting to be done; returns that one if so. */
    fun create(draft: ChoreDraft): Chore? {
        findOpenDuplicate(draft.title, uiState.value.chores)?.let { return it }
        val (familyId, me) = context() ?: return null
        chores.create(
            familyId,
            me.id,
            Chore(id = "", title = draft.title.trim(), template = draft.template, recurrence = draft.recurrence, dueDate = draft.dueDate),
        )
        return null
    }



    fun update(chore: Chore, draft: ChoreDraft) = change(chore) { _, _ ->
        copy(title = draft.title.trim(), template = draft.template, recurrence = draft.recurrence, dueDate = draft.dueDate)
    }

    fun delete(chore: Chore) {
        val (familyId, _) = context() ?: return
        chores.delete(familyId, chore.id)
    }

    /**
     * Applies one of the chore rules from :core:model and saves the result. A rule that
     * refuses (say, someone grabbed the chore a moment ago) leaves the chore as it is.
     */
    private fun change(chore: Chore, transition: Chore.(me: Member, today: LocalDate) -> Chore) {
        val (familyId, me) = context() ?: return
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
    }

    private fun context(): Pair<String, Member>? {
        val state = uiState.value
        return (state.familyId ?: return null) to (state.me ?: return null)
    }
}
