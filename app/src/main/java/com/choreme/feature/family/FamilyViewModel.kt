package com.choreme.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.R
import com.choreme.data.AuthRepository
import com.choreme.data.FamilyRepository
import com.choreme.data.UserFacingException
import com.choreme.domain.model.Family
import com.choreme.domain.model.JoinRequest
import com.choreme.domain.model.Member
import com.choreme.domain.model.Role
import com.choreme.domain.session.SessionState
import com.choreme.ui.common.ActionState
import com.choreme.ui.common.runAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FamilyUiState(
    val me: SessionState.InFamily? = null,
    val family: Family? = null,
    val members: List<Member> = emptyList(),
    val joinRequests: List<JoinRequest> = emptyList(),
) {
    val isParent get() = me?.role == Role.PARENT
    val kidProfiles get() = members.filter { it.role == Role.KID }
}

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val families: FamilyRepository,
) : ViewModel() {
    val uiState: StateFlow<FamilyUiState> = families.session
        .filterIsInstance<SessionState.InFamily>()
        .flatMapLatest { me ->
            combine(
                families.observeFamily(me.familyId),
                families.observeMembers(me.familyId),
                if (me.role == Role.PARENT) families.observeJoinRequests(me.familyId) else flowOf(emptyList()),
            ) { family, members, requests -> FamilyUiState(me, family, members, requests) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FamilyUiState())

    private val _actionState = MutableStateFlow(ActionState())
    val actionState: StateFlow<ActionState> = _actionState

    /** Anonymous (kid) devices can't sign back in to the same account, so signing out needs a warning. */
    val signOutNeedsApprovalAgain: Boolean get() = auth.currentAccount()?.isAnonymous == true

    fun approve(request: JoinRequest, linkToKidId: String?) = familyAction { familyId ->
        families.approve(familyId, request, linkToKidId, nextColor())
    }

    fun decline(request: JoinRequest) = familyAction { familyId -> families.decline(familyId, request) }

    fun addKidProfile(name: String) = familyAction { familyId ->
        if (name.isBlank()) throw UserFacingException(R.string.error_name_required)
        families.addKidProfile(familyId, name.trim(), nextColor())
    }

    fun remove(member: Member) = familyAction { familyId -> families.removeMember(familyId, member) }

    fun signOut() = auth.signOut()

    private fun nextColor() = uiState.value.members.size

    private fun familyAction(block: suspend (familyId: String) -> Unit) {
        val familyId = uiState.value.me?.familyId ?: return
        runAction(_actionState) { block(familyId) }
    }
}
