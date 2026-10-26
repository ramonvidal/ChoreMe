package com.choreme.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.data.AuthRepository
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.NoteRepository
import com.choreme.core.model.DomainError
import com.choreme.core.model.DomainException
import com.choreme.core.model.Family
import com.choreme.core.model.JoinRequest
import com.choreme.core.model.Member
import com.choreme.core.model.Note
import com.choreme.core.model.chore.MemberWeek
import com.choreme.core.model.chore.weekStart
import com.choreme.core.model.chore.weeklySummary
import com.choreme.core.model.Role
import com.choreme.core.model.session.SessionState
import com.choreme.core.ui.ActionState
import com.choreme.core.ui.runAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class FamilyUiState(
    val me: SessionState.InFamily? = null,
    val family: Family? = null,
    val members: List<Member> = emptyList(),
    val joinRequests: List<JoinRequest> = emptyList(),
    /** The family board, newest first. */
    val notes: List<Note> = emptyList(),
    /** Who did what in the last 7 days; shown to parents. */
    val week: List<MemberWeek> = emptyList(),
    val today: LocalDate = LocalDate.now(),
) {
    val isParent get() = me?.role == Role.PARENT
    val meMember get() = members.firstOrNull { it.id == me?.memberId }

    /** You can mark yourself away; parents can mark anyone. */
    fun canSetAway(member: Member) = isParent || member.id == me?.memberId
    val kidProfiles get() = members.filter { it.role == Role.KID }
}

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val families: FamilyRepository,
    private val noteRepository: NoteRepository,
    private val chores: ChoreRepository,
) : ViewModel() {
    val uiState: StateFlow<FamilyUiState> = families.session
        .filterIsInstance<SessionState.InFamily>()
        .flatMapLatest { me ->
            val today = LocalDate.now()
            combine(
                families.observeFamily(me.familyId),
                families.observeMembers(me.familyId),
                if (me.role == Role.PARENT) families.observeJoinRequests(me.familyId) else flowOf(emptyList()),
                noteRepository.observe(me.familyId).onStart { emit(emptyList()) },
                if (me.role == Role.PARENT) chores.observeLog(me.familyId, weekStart(today)).onStart { emit(emptyList()) } else flowOf(emptyList()),
            ) { family, members, requests, notes, log ->
                FamilyUiState(me, family, members, requests, notes, weeklySummary(log, members, today), today)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FamilyUiState())

    fun addNote(text: String) {
        val me = uiState.value.me ?: return
        text.trim().take(Note.MAX_LENGTH).takeIf { it.isNotEmpty() }?.let { noteRepository.add(me.familyId, me.memberId, it) }
    }

    fun editNote(note: Note, text: String) {
        val familyId = uiState.value.me?.familyId ?: return
        text.trim().take(Note.MAX_LENGTH).takeIf { it.isNotEmpty() }?.let { noteRepository.edit(familyId, note, it) }
    }

    fun deleteNote(note: Note) {
        val familyId = uiState.value.me?.familyId ?: return
        noteRepository.delete(familyId, note.id)
    }

    /** Away up to and including [until]; null for back home. */
    fun setAway(member: Member, until: LocalDate?) {
        val familyId = uiState.value.me?.familyId ?: return
        if (!uiState.value.canSetAway(member)) return
        families.setAway(familyId, member.id, until)
    }

    private val _actionState = MutableStateFlow(ActionState())
    val actionState: StateFlow<ActionState> = _actionState

    /** Anonymous (kid) devices can't sign back in to the same account, so signing out needs a warning. */
    val signOutNeedsApprovalAgain: Boolean get() = auth.currentAccount()?.isAnonymous == true

    fun approve(request: JoinRequest, linkToKidId: String?) = familyAction { familyId ->
        families.approve(familyId, request, linkToKidId, nextColor())
    }

    fun decline(request: JoinRequest) = familyAction { familyId -> families.decline(familyId, request) }

    fun addKidProfile(name: String) = familyAction { familyId ->
        if (name.isBlank()) throw DomainException(DomainError.NAME_REQUIRED)
        families.addKidProfile(familyId, name.trim(), nextColor())
    }

    fun remove(member: Member) = familyAction { familyId -> families.removeMember(familyId, member) }

    fun signOut() = auth.signOut()

    /** Leaves the family; the session then moves on to the "set up your family" screen by itself. */
    fun leave() = familyAction { familyId ->
        val me = uiState.value.members.firstOrNull { it.id == uiState.value.me?.memberId } ?: return@familyAction
        families.leave(familyId, me, uiState.value.members)
    }

    /**
     * Leaves the family and deletes the sign-in account. Google accounts are confirmed first
     * ([getGoogleIdToken] shows the account picker), as Google requires for deleting one.
     */
    fun deleteMyData(getGoogleIdToken: suspend () -> String) = familyAction { familyId ->
        val state = uiState.value
        val me = state.members.firstOrNull { it.id == state.me?.memberId } ?: return@familyAction
        if (auth.currentAccount()?.isAnonymous == false) {
            val token = try {
                getGoogleIdToken()
            } catch (e: GetCredentialCancellationException) {
                return@familyAction
            }
            auth.reauthenticateWithGoogle(token)
        }
        families.leave(familyId, me, state.members)
        auth.deleteAccount()
    }

    private fun nextColor() = uiState.value.members.size

    private fun familyAction(block: suspend (familyId: String) -> Unit) {
        val familyId = uiState.value.me?.familyId ?: return
        runAction(_actionState) { block(familyId) }
    }
}
