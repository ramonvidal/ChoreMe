package com.choreme.feature.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.model.AppUpdate
import com.choreme.core.update.UpdateInstaller
import com.choreme.core.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UpdateState {
    data object None : UpdateState
    data class Available(val update: AppUpdate) : UpdateState
    data class NeedsPermission(val update: AppUpdate) : UpdateState
    data class Downloading(val update: AppUpdate, val progress: Float) : UpdateState
    data class Failed(val update: AppUpdate) : UpdateState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updates: UpdateRepository,
    private val installer: UpdateInstaller,
) : ViewModel() {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.None)
    val state: StateFlow<UpdateState> = _state

    init {
        viewModelScope.launch {
            updates.availableUpdate()?.let { _state.value = UpdateState.Available(it) }
        }
    }

    fun install(update: AppUpdate) {
        if (!installer.canInstall()) {
            _state.value = UpdateState.NeedsPermission(update)
            return
        }
        _state.value = UpdateState.Downloading(update, 0f)
        viewModelScope.launch {
            _state.value = try {
                installer.downloadAndInstall(update) { progress -> _state.value = UpdateState.Downloading(update, progress) }
                // Android's own "update this app?" screen takes over from here.
                UpdateState.Available(update)
            } catch (e: Exception) {
                UpdateState.Failed(update)
            }
        }
    }

    /** Back from Settings: carry on if installing is now allowed. */
    fun onResume() {
        val waiting = _state.value as? UpdateState.NeedsPermission ?: return
        if (installer.canInstall()) install(waiting.update)
    }

    fun allowInstallsIntent() = installer.allowInstallsIntent()

    fun later(update: AppUpdate) {
        updates.dismiss(update)
        _state.value = UpdateState.None
    }
}
