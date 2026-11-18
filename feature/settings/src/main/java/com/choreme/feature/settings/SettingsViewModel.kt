package com.choreme.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.data.FamilyRepository
import com.choreme.core.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** What the family-wide settings need to know: who is looking, and whether the family plays with points. */
internal data class SettingsUiState(
    val familyId: String? = null,
    val isParent: Boolean = false,
    val pointsOn: Boolean = true,
)

/** The family's own settings (for parents); the settings of this phone and the trips have their own view models. */
@HiltViewModel
class SettingsViewModel @Inject constructor(private val families: FamilyRepository) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state: StateFlow<SettingsUiState> = families.inFamily
        .flatMapLatest { session ->
            families.observeFamily(session.familyId).map { family ->
                SettingsUiState(session.familyId, session.role == Role.PARENT, family?.pointsEnabled ?: true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    internal fun setPointsEnabled(enabled: Boolean) {
        state.value.familyId?.let { families.setPointsEnabled(it, enabled) }
    }
}
