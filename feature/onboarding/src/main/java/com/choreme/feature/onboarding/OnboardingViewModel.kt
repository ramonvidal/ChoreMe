package com.choreme.feature.onboarding

import androidx.lifecycle.ViewModel
import com.choreme.core.data.AuthRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.model.DomainError
import com.choreme.core.model.DomainException
import com.choreme.core.model.InviteCode
import com.choreme.core.model.Role
import com.choreme.core.ui.ActionState
import com.choreme.core.ui.runAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val families: FamilyRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ActionState())
    val state: StateFlow<ActionState> = _state

    fun createFamily(familyName: String, yourName: String) = runAction(_state) {
        requireName(yourName)
        if (familyName.isBlank()) throw DomainException(DomainError.FAMILY_NAME_REQUIRED)
        families.createFamily(familyName.trim(), yourName.trim())
    }

    fun join(code: String, yourName: String, asKid: Boolean) = runAction(_state) {
        requireName(yourName)
        val normalized = InviteCode.normalize(code)
        if (!InviteCode.isValid(normalized)) throw DomainException(DomainError.INVITE_CODE_INVALID)
        families.requestToJoin(normalized, yourName.trim(), if (asKid) Role.KID else Role.PARENT)
    }

    fun cancelRequest(familyId: String) = runAction(_state) { families.cancelJoinRequest(familyId) }

    fun signOut() = auth.signOut()

    private fun requireName(name: String) {
        if (name.isBlank()) throw DomainException(DomainError.NAME_REQUIRED)
    }
}
