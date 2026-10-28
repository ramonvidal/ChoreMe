package com.choreme.ui.root

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.data.PushEndpointRepository
import com.choreme.core.model.Role
import com.choreme.core.data.GroceryRepository
import com.choreme.core.model.chore.choresBadge
import com.choreme.core.model.grocery.groceriesToBuy
import java.time.LocalDate
import com.choreme.core.model.session.SessionState
import com.choreme.core.push.PushRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    groceries: GroceryRepository,
    push: PushRegistration,
    private val endpoints: PushEndpointRepository,
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

    /** Shown as a badge on the Chores tab: your chores due by today, and for parents, kids' chores to check. */
    val choresBadge: StateFlow<Int> = session
        .flatMapLatest { state ->
            if (state is SessionState.InFamily) {
                chores.observeChores(state.familyId).map { list -> choresBadge(list, LocalDate.now(), state.memberId, state.role) }
            } else {
                flowOf(0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Shown as a badge on the Groceries tab: what's on the list to buy. */
    val groceriesToBuy: StateFlow<Int> = session
        .flatMapLatest { state ->
            if (state is SessionState.InFamily) groceries.observeItems(state.familyId).map { it.groceriesToBuy() } else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Where other phones can ping this one; null without a UnifiedPush app such as ntfy. */
    val pushEndpoint: StateFlow<String?> = push.endpoint

    init {
        push.register()
    }

    /**
     * Tells the family where this phone can be pinged. Called by the screen while it's
     * visible, so no Firestore listener is kept alive in the background for it.
     */
    fun registerForPings(state: SessionState, url: String?) {
        if (state !is SessionState.InFamily || url == null) return
        viewModelScope.launch {
            try {
                endpoints.register(state.familyId, state.memberId, url)
            } catch (e: Exception) {
                Log.w("ChoreMe", "Couldn't register for pings", e)
            }
        }
    }
}
