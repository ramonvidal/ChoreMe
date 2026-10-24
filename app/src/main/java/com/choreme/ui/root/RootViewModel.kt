package com.choreme.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.data.FamilyRepository
import com.choreme.data.FirebaseAvailability
import com.choreme.domain.model.Role
import com.choreme.domain.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    availability: FirebaseAvailability,
    families: FamilyRepository,
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
}
