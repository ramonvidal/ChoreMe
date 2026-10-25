package com.choreme.core.ui

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.model.DomainError
import com.choreme.core.model.DomainException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Progress and error of a one-off action such as "create family". */
data class ActionState(val busy: Boolean = false, @StringRes val error: Int? = null)

/** Runs [block] unless an action is already in flight, reporting progress and failure in [state]. */
fun ViewModel.runAction(
    state: MutableStateFlow<ActionState>,
    @StringRes errorRes: Int = R.string.error_generic,
    block: suspend () -> Unit,
) {
    if (state.value.busy) return
    viewModelScope.launch {
        state.value = ActionState(busy = true)
        state.value = try {
            block()
            ActionState()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ChoreMe", "Action failed", e)
            ActionState(error = (e as? DomainException)?.error?.message ?: errorRes)
        }
    }
}

@get:StringRes
val DomainError.message: Int
    get() = when (this) {
        DomainError.OFFLINE -> R.string.error_offline
        DomainError.NAME_REQUIRED -> R.string.error_name_required
        DomainError.FAMILY_NAME_REQUIRED -> R.string.error_family_name_required
        DomainError.INVITE_CODE_INVALID -> R.string.error_invite_code_invalid
        DomainError.INVITE_CODE_NOT_FOUND -> R.string.error_invite_code_not_found
        DomainError.LAST_PARENT -> R.string.error_last_parent
        DomainError.RECENT_SIGN_IN_REQUIRED -> R.string.error_recent_sign_in
    }
