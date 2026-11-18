package com.choreme.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.model.AlertKind
import com.choreme.core.model.AlertSettings
import com.choreme.core.model.QuietHours
import com.choreme.core.push.PushRegistration
import com.choreme.core.settings.AppearanceSettings
import com.choreme.core.settings.DiagnosticsSettings
import com.choreme.core.settings.NotificationSettings
import com.choreme.core.settings.ThemeMode
import com.choreme.core.settings.WeatherSettings
import com.choreme.core.model.weather.RainUnit
import com.choreme.core.model.weather.SpeedUnit
import com.choreme.core.model.weather.TemperatureUnit
import com.choreme.core.model.weather.WeatherUnits
import com.choreme.core.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How "Check for updates" went. */
internal sealed interface UpdateCheck {
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
    private val appearance: AppearanceSettings,
    private val weatherSettings: WeatherSettings,
    private val diagnostics: DiagnosticsSettings,
) : ViewModel() {
    internal val crashReports: StateFlow<Boolean> = diagnostics.crashReports

    internal fun setCrashReports(on: Boolean) = diagnostics.setCrashReports(on)

    internal val weatherUnits: StateFlow<WeatherUnits> = weatherSettings.units

    internal fun setTemperatureUnit(unit: TemperatureUnit) = weatherSettings.setTemperature(unit)
    internal fun setSpeedUnit(unit: SpeedUnit) = weatherSettings.setSpeed(unit)
    internal fun setRainUnit(unit: RainUnit) = weatherSettings.setRain(unit)

    internal val themeMode: StateFlow<ThemeMode> = appearance.mode

    internal fun setThemeMode(mode: ThemeMode) = appearance.set(mode)

    internal val animatedHouse: StateFlow<Boolean> = appearance.animatedHouse

    internal fun setAnimatedHouse(on: Boolean) = appearance.setAnimatedHouse(on)

    internal val settings: StateFlow<AlertSettings> = notifications.settings
    internal val pushEndpoint: StateFlow<String?> = push.endpoint

    internal fun setEnabled(kind: AlertKind, enabled: Boolean) = notifications.setEnabled(kind, enabled)

    internal fun setQuietHours(quietHours: QuietHours?) = notifications.setQuietHours(quietHours)

    internal val installedVersion: String = updates.installedVersion

    /** False in the Play Store build, which Google updates. */
    internal val canSelfUpdate: Boolean = updates.enabled

    private val _updateCheck = MutableStateFlow<UpdateCheck>(UpdateCheck.Idle)
    internal val updateCheck: StateFlow<UpdateCheck> = _updateCheck

    internal fun checkForUpdates() {
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
