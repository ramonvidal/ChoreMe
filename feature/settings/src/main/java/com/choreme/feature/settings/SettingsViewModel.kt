package com.choreme.feature.settings

import androidx.lifecycle.ViewModel
import com.choreme.core.model.AlertKind
import com.choreme.core.model.AlertSettings
import com.choreme.core.model.QuietHours
import com.choreme.core.push.PushRegistration
import com.choreme.core.settings.NotificationSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val notifications: NotificationSettings,
    private val push: PushRegistration,
) : ViewModel() {
    val settings: StateFlow<AlertSettings> = notifications.settings
    val pushEndpoint: StateFlow<String?> = push.endpoint

    fun setEnabled(kind: AlertKind, enabled: Boolean) = notifications.setEnabled(kind, enabled)

    fun setQuietHours(quietHours: QuietHours?) = notifications.setQuietHours(quietHours)
}
