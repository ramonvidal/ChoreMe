package com.choreme.feature.onboarding

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import com.choreme.R
import com.choreme.data.AuthRepository
import com.choreme.ui.common.ActionState
import com.choreme.ui.common.runAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(ActionState())
    val state: StateFlow<ActionState> = _state

    fun signInWithGoogle(getIdToken: suspend () -> String) = runAction(_state, R.string.error_sign_in) {
        val token = try {
            getIdToken()
        } catch (e: GetCredentialCancellationException) {
            return@runAction // Closed the account picker; not an error.
        }
        auth.signInWithGoogle(token)
    }

    fun signInAsKid() = runAction(_state, R.string.error_sign_in) { auth.signInAsKid() }
}
