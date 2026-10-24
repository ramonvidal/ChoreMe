package com.choreme.ui.common

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.R
import com.choreme.data.UserFacingException
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
            ActionState(error = (e as? UserFacingException)?.messageRes ?: errorRes)
        }
    }
}
