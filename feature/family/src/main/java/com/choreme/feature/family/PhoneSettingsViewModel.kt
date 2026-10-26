package com.choreme.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.model.AlertKind
import com.choreme.core.model.AlertSettings
import com.choreme.core.model.QuietHours
import com.choreme.core.push.PushRegistration
import com.choreme.core.settings.NotificationSettings
import com.choreme.core.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How "Check for updates" went. */
sealed interface UpdateCheck {
    data object Idle : UpdateCheck
    data object Checking : UpdateCheck
    data object UpToDate : UpdateCheck
    data class Found(val version: String) : UpdateCheck
    data object Failed : UpdateCheck
}

/** This phone's own choices, shown in the Family tab under "This phone". */
@HiltViewModel
class PhoneSettingsViewModel @Inject constructor(
    private val notifications: NotificationSettings,
    private val push: PushRegistration,
    private val updates: UpdateRepository,
) : ViewModel() {
    val settings: StateFlow<AlertSettings> = notifications.settings
    val pushEndpoint: StateFlow<String?> = push.endpoint

    fun setEnabled(kind: AlertKind, enabled: Boolean) = notifications.setEnabled(kind, enabled)

    fun setQuietHours(quietHours: QuietHours?) = notifications.setQuietHours(quietHours)

    val installedVersion: String = updates.installedVersion

    private val _updateCheck = MutableStateFlow<UpdateCheck>(UpdateCheck.Idle)
    val updateCheck: StateFlow<UpdateCheck> = _updateCheck

    fun checkForUpdates() {
        _updateCheck.value = UpdateCheck.Checking
        viewModelScope.launch {
            _updateCheck.value = try {
                updates.checkNow()?.let { UpdateCheck.Found(it.versionName) } ?: UpdateCheck.UpToDate
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UpdateCheck.Failed
            }
        }
    }
}
