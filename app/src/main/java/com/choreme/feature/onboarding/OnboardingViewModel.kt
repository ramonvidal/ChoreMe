package com.choreme.feature.onboarding

import androidx.lifecycle.ViewModel
import com.choreme.R
import com.choreme.data.AuthRepository
import com.choreme.data.FamilyRepository
import com.choreme.data.UserFacingException
import com.choreme.domain.model.InviteCode
import com.choreme.domain.model.Role
import com.choreme.ui.common.ActionState
import com.choreme.ui.common.runAction
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
        if (familyName.isBlank()) throw UserFacingException(R.string.error_family_name_required)
        families.createFamily(familyName.trim(), yourName.trim())
    }

    fun join(code: String, yourName: String, asKid: Boolean) = runAction(_state) {
        requireName(yourName)
        val normalized = InviteCode.normalize(code)
        if (!InviteCode.isValid(normalized)) throw UserFacingException(R.string.error_invite_code_invalid)
        families.requestToJoin(normalized, yourName.trim(), if (asKid) Role.KID else Role.PARENT)
    }

    fun cancelRequest(familyId: String) = runAction(_state) { families.cancelJoinRequest(familyId) }

    fun signOut() = auth.signOut()

    private fun requireName(name: String) {
        if (name.isBlank()) throw UserFacingException(R.string.error_name_required)
    }
}
