package com.choreme.ui.root

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.data.PushEndpointRepository
import com.choreme.core.model.Role
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.session.SessionState
import com.choreme.core.push.PushRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    availability: FirebaseAvailability,
    families: FamilyRepository,
    chores: ChoreRepository,
    push: PushRegistration,
    endpoints: PushEndpointRepository,
) : ViewModel() {
    val session: StateFlow<SessionState> =
        (if (availability.isConfigured) families.session else flowOf(SessionState.FirebaseNotConfigured))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)

    /** Shown as a badge on the Family tab, so parents notice someone is waiting. */
    val pendingJoinRequests: StateFlow<Int> = session
        .flatMapLatest { state ->
            if (state is SessionState.InFamily && state.role == Role.PARENT) {
                families.observeJoinRequests(state.familyId).map { it.size }
            } else {
                flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Shown as a badge on the Today tab: kids' chores a parent needs to check. */
    val pendingApprovals: StateFlow<Int> = session
        .flatMapLatest { state ->
            if (state is SessionState.InFamily && state.role == Role.PARENT) {
                chores.observeChores(state.familyId).map { list -> list.count { it.status == ChoreStatus.PENDING_APPROVAL } }
            } else {
                flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        // Instant pings (only if a UnifiedPush app such as ntfy is installed): connect, then
        // tell the family where this phone can be reached.
        push.register()
        viewModelScope.launch {
            combine(session, push.endpoint, ::Pair).collect { (state, url) ->
                if (state is SessionState.InFamily && url != null) {
                    try {
                        endpoints.register(state.familyId, state.memberId, url)
                    } catch (e: Exception) {
                        Log.w("ChoreMe", "Couldn't register for pings", e)
                    }
                }
            }
        }
    }
}
